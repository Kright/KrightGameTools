package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dPoint, Pga3dVector}

/**
 * resolves hard [[Pga3dDistanceConstraint]]s. Two independent parts, both solver-agnostic:
 *
 *  - [[addConstraintForques]] computes the exact acceleration-level constraint forques (the
 *    Lagrange-multiplier forces, including the centripetal bias) against the current state and
 *    the already accumulated forques, and adds them as ordinary paired forques. Call it inside
 *    the solver's force callback after the user forces - the high-order solvers then integrate
 *    a smooth ODE and keep their order.
 *  - [[project]] removes the residual drift once after the full step: a Gauss-Seidel pass over
 *    the positions (multiplicative pose corrections `motor * exp(...)` weighted by the inverse
 *    inertia, so the motors stay unit by construction), then over the velocities (paired
 *    impulses along the constraint). One-sided constraints participate only when at the bound
 *    and moving into the violation.
 *
 * Both parts distribute corrections by the generalized inverse masses `J * I^-1(J)` of the
 * constraint forque line J = anchor v direction and apply them equal-and-opposite, so the total
 * momentum of the constrained bodies is preserved exactly.
 *
 * See [[Pga3dPhysicsSolverConstrained]] for wiring both parts into any solver.
 */
class Pga3dConstraintResolver(val constraints: Seq[Pga3dDistanceConstraint],
                              val projectionIterations: Int = 100,
                              val forqueIterations: Int = 100):

  /**
   * part 1: add the acceleration-level constraint forques (call after the user forces).
   *
   * Constraints sharing a body are coupled, so the lambdas are found by Gauss-Seidel sweeps:
   * the rhs is evaluated against the already accumulated forques, which makes every sweep an
   * incremental correction. The sweeps run until the increments fall 1e-12 below the first
   * sweep (the leftover force residual does not shrink with dt, so it would cap the accuracy
   * of a high-order solver); [[forqueIterations]] only bounds them. The Gauss-Seidel ratio for
   * a hanging chain of n rods is ~cos^2(pi / (n + 1)), so long chains use many sweeps.
   */
  def addConstraintForques(dynamicBodies: Array[Pga3dPhysicsBody]): Unit =
    Pga3dConstraintResolver.sweepToConvergence(forqueIterations) { () =>
      addConstraintForquesSweep(dynamicBodies)
    }

  /** @return the largest |lambda| applied in this sweep */
  private def addConstraintForquesSweep(dynamicBodies: Array[Pga3dPhysicsBody]): Double =
    var sweepMax = 0.0
    for (c <- constraints) {
      for (g <- geometry(dynamicBodies, c)) {
        // relative velocity of the anchors: the along-n rate and the perpendicular part
        val vA = pointVelocity(dynamicBodies, c.bodyA, g.pA)
        val vB = pointVelocity(dynamicBodies, c.bodyB, g.pB)
        val vRel = vA - vB
        val vn = vRel.antiDotI(g.n)
        val vPerpSquare = Math.max(vRel.normSquare - vn * vn, 0.0)

        // d^2/dt^2 of the distance at lambda = 0: the applied + inertial anchor accelerations
        // plus the centripetal term of the rotating direction n
        val aN = (pointAcceleration(dynamicBodies, c.bodyA, g.pA) -
          pointAcceleration(dynamicBodies, c.bodyB, g.pB)).antiDotI(g.n)
        val rhs = aN + vPerpSquare / g.dist

        val lambda = c.clampLambda(g.dist, -rhs / g.wSum)
        if (lambda != 0.0) {
          sweepMax = Math.max(sweepMax, Math.abs(lambda))
          val forque = g.j * lambda
          if (c.bodyA >= 0) dynamicBodies(c.bodyA).addGlobalForque(forque)
          if (c.bodyB >= 0) dynamicBodies(c.bodyB).addGlobalForque(-forque)
        }
      }
    }
    sweepMax

  /**
   * part 2: remove the residual drift of the positions and velocities (call after the step).
   * Both Gauss-Seidel passes run until their corrections fall 1e-12 below the first sweep;
   * [[projectionIterations]] only bounds them.
   */
  def project(dynamicBodies: Array[Pga3dPhysicsBody]): Unit =
    projectPositions(dynamicBodies)
    projectVelocities(dynamicBodies)

  /** the position half of [[project]] */
  def projectPositions(dynamicBodies: Array[Pga3dPhysicsBody]): Unit =
    Pga3dConstraintResolver.sweepToConvergence(projectionIterations) { () =>
      var sweepMax = 0.0
      for (c <- constraints) {
        for (g <- geometry(dynamicBodies, c)) {
          val target = Math.min(Math.max(g.dist, c.minDistance), c.maxDistance)
          val error = g.dist - target
          if (error != 0.0) {
            sweepMax = Math.max(sweepMax, Math.abs(error))
            val lambda = -error / g.wSum
            // the pose twist gLocal held for "time" lambda: exp keeps the motor unit
            applyPoseCorrection(dynamicBodies, c.bodyA, g.gA, lambda)
            applyPoseCorrection(dynamicBodies, c.bodyB, g.gB, -lambda)
          }
        }
      }
      sweepMax
    }

    // hygiene against the rounding noise of the correction products
    for (c <- constraints) {
      if (c.bodyA >= 0) {
        val body = dynamicBodies(c.bodyA)
        body.motor = body.motor.renormalized
      }
      if (c.bodyB >= 0) {
        val body = dynamicBodies(c.bodyB)
        body.motor = body.motor.renormalized
      }
    }

  /** the velocity half of [[project]] */
  def projectVelocities(dynamicBodies: Array[Pga3dPhysicsBody]): Unit =
    Pga3dConstraintResolver.sweepToConvergence(projectionIterations) { () =>
      var sweepMax = 0.0
      for (c <- constraints) {
        for (g <- geometry(dynamicBodies, c)) {
          val vn =
            (pointVelocity(dynamicBodies, c.bodyA, g.pA) - pointVelocity(dynamicBodies, c.bodyB, g.pB))
              .antiDotI(g.n)
          val lambda = c.clampLambda(g.dist, -vn / g.wSum)
          if (lambda != 0.0) {
            sweepMax = Math.max(sweepMax, Math.abs(lambda))
            if (c.bodyA >= 0) {
              val body = dynamicBodies(c.bodyA)
              body.localB = body.localB + g.gA * lambda
            }
            if (c.bodyB >= 0) {
              val body = dynamicBodies(c.bodyB)
              body.localB = body.localB - g.gB * lambda
            }
          }
        }
      }
      sweepMax
    }

  /**
   * the shared per-constraint geometry: world anchors, direction, the constraint forque line
   * J (the same line for both bodies - the anchors lie on it), the inverse-inertia response
   * twists gX = I^-1(J in local frame) and the total generalized inverse mass. None for a
   * degenerate (coincident anchors) or fully static constraint.
   */
  private[physics] def geometry(dynamicBodies: Array[Pga3dPhysicsBody],
                                c: Pga3dDistanceConstraint): Option[Pga3dConstraintGeometry] =
    val pA = anchorWorld(dynamicBodies, c.bodyA, c.anchorA)
    val pB = anchorWorld(dynamicBodies, c.bodyB, c.anchorB)
    val delta = pA - pB
    val dist = delta.norm
    if (dist < 1e-100) return None

    val n = delta * (1.0 / dist)
    val j = Pga3dForque.force(pA, n)

    val gA = responseTwist(dynamicBodies, c.bodyA, j)
    val gB = responseTwist(dynamicBodies, c.bodyB, j)
    val wA = responseAlongN(dynamicBodies, c.bodyA, gA, pA, n)
    val wB = responseAlongN(dynamicBodies, c.bodyB, gB, pB, n)
    val wSum = wA + wB
    if (!(wSum > 0.0)) return None

    Some(Pga3dConstraintGeometry(pA, pB, dist, n, j, gA, gB, wSum))

  private[physics] def anchorWorld(dynamicBodies: Array[Pga3dPhysicsBody], bodyIndex: Int, anchor: Pga3dPoint): Pga3dPoint =
    if (bodyIndex >= 0) dynamicBodies(bodyIndex).motorSandwich(anchor) else anchor

  /** the local twist response to a unit world constraint forque; zero for the world side */
  private def responseTwist(dynamicBodies: Array[Pga3dPhysicsBody], bodyIndex: Int, j: Pga3dBivector): Pga3dBivector =
    if (bodyIndex >= 0) {
      val body = dynamicBodies(bodyIndex)
      body.inertia.invert(body.transform.reverseSandwich(j))
    } else {
      Pga3dBivector.zero
    }

  /** how fast the anchor moves along n per unit of the response twist: the generalized inverse mass */
  private def responseAlongN(dynamicBodies: Array[Pga3dPhysicsBody], bodyIndex: Int,
                             gLocal: Pga3dBivector, p: Pga3dPoint, n: Pga3dVector): Double =
    if (bodyIndex >= 0) {
      val gWorld = dynamicBodies(bodyIndex).transform.sandwich(gLocal)
      (-(gWorld cross p)).antiDotI(n)
    } else {
      0.0
    }

  /** world velocity of the body point p: -(W cross p) for the world twist W */
  private def pointVelocity(dynamicBodies: Array[Pga3dPhysicsBody], bodyIndex: Int, p: Pga3dPoint): Pga3dVector =
    if (bodyIndex >= 0) {
      val body = dynamicBodies(bodyIndex)
      -(body.transform.sandwich(body.localB) cross p)
    } else {
      Pga3dVector(0.0, 0.0, 0.0)
    }

  /**
   * world acceleration of the body point p under the currently accumulated forques:
   * a = -(W' cross p) - (W cross v), where W' = sandwich of the local acceleration
   * (the sandwich derivative terms cancel: [B, B] = 0)
   */
  private def pointAcceleration(dynamicBodies: Array[Pga3dPhysicsBody], bodyIndex: Int, p: Pga3dPoint): Pga3dVector =
    if (bodyIndex >= 0) {
      val body = dynamicBodies(bodyIndex)
      val wWorld = body.transform.sandwich(body.localB)
      val v = -(wWorld cross p)
      val dwWorld = body.transform.sandwich(body.inertia.getAcceleration(body.localB, body.localForque))
      -(dwWorld cross p) - (wWorld cross v)
    } else {
      Pga3dVector(0.0, 0.0, 0.0)
    }

  private def applyPoseCorrection(dynamicBodies: Array[Pga3dPhysicsBody], bodyIndex: Int,
                                  gLocal: Pga3dBivector, lambda: Double): Unit =
    if (bodyIndex >= 0 && lambda != 0.0) {
      val body = dynamicBodies(bodyIndex)
      body.motor = body.motor.geometric((gLocal * (-0.5 * lambda)).exp)
    }

  override def toString: String =
    s"Pga3dConstraintResolver(constraints = ${constraints.size}, projectionIterations = $projectionIterations)"


object Pga3dConstraintResolver:
  /**
   * runs Gauss-Seidel sweeps until their maximum correction falls 1e-12 below the first sweep;
   * maxIterations is only a safety bound - a leftover residual would not shrink with dt, so it
   * would cap the reachable accuracy no matter the solver order
   */
  private[physics] def sweepToConvergence(maxIterations: Int)(sweep: () => Double): Unit =
    var tolerance = -1.0
    for (_ <- FastRange(maxIterations)) {
      val sweepMax = sweep()
      if (tolerance < 0.0) tolerance = 1e-12 * sweepMax
      if (sweepMax <= tolerance) return
    }


