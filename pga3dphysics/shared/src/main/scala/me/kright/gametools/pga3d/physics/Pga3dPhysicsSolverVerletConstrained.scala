package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dMotor, Pga3dPoint, Pga3dTransform, Pga3dVector}

/**
 * a RATTLE-style constrained Verlet on the motor group: 2nd order of precision with hard
 * [[Pga3dDistanceConstraint]]s built into the step itself (a generic projection composite
 * over Verlet only reaches the 1st order).
 *
 * Like [[Pga3dPhysicsSolverVerlet]] this is a stateless computation strategy, not a
 * [[Pga3dPhysicsSolver]]: the state is two consecutive pose arrays owned by the caller, the
 * constraints are an argument of [[step]] and may change freely between steps (e.g.
 * contact-like constraints regenerated every frame), and pose edits between steps implicitly
 * edit the velocity, like in position-based dynamics.
 *
 * The step reconstructs the node momenta from the poses (redoing the second RATTLE half kick
 * of the previous segment: segment momentum + prevDt/2 of the forques + the velocity-stage
 * impulses), then performs the first half kick of the new step with the position-stage
 * constraint impulses lambda * J, whose gradients J are evaluated at the OLD poses - the
 * variational ingredient of SHAKE/RATTLE. The lambdas are solved by Newton/Gauss-Seidel
 * sweeps through the midframe drift, and the accepted poses go to `nextMotors`. Thanks to the
 * reconstruction the user force callback runs once per step (the forques of the second half
 * kick of a step are recomputed at the start of the next one).
 *
 * One-sided constraints participate with the active-set logic: the position stage acts only
 * on violated bounds (the accumulated lambda is clamped to the admissible sign and may
 * release), the velocity stage only stops the motion into the violation.
 *
 * The first step needs virtual previous poses: use [[Pga3dPhysicsSolverVerlet.makePrevMotors]]
 * (the reconstruction then recovers exactly the caller-provided node momenta).
 */
object Pga3dPhysicsSolverVerletConstrained:

  /**
   * one constrained Verlet step; the poses are the whole state, like in the classic Verlet,
   * but the honest node twists the step reconstructs anyway are exposed through `localBs` -
   * for observers (energy, momentum, velocity-dependent logic outside the solver) they are
   * exactly what a separate [[reconstructNode]] call would produce, without paying its second
   * force callback;
   * the raw segment twist [[Pga3dPhysicsSolverVerlet.segmentLocalB]] is half a step stale.
   *
   * @param addForques (motors, localBs) => world forques at that state; called once
   * @param nextMotors output; may alias prevMotors, must not alias motors
   * @param localBs    output: the honest node-time twists at `motors` (the poses this step
   *                   consumes), after the velocity-stage constraint projection; null (the
   *                   default) when the caller does not need them
   */
  def step(inertias: Array[Pga3dInertia],
           prevMotors: Array[Pga3dMotor],
           motors: Array[Pga3dMotor],
           constraints: Seq[Pga3dDistanceConstraint],
           addForques: (Array[Pga3dMotor], Array[Pga3dBivector]) => Array[Pga3dBivector],
           prevDt: Double,
           dt: Double,
           nextMotors: Array[Pga3dMotor],
           localBs: Array[Pga3dBivector] | Null = null,
           shakeIterations: Int = 100,
           projectionIterations: Int = 100): Unit = {
    val n = motors.length
    // the poses this step consumes are fixed for the whole step and are applied many times
    // (the reconstruction, the gradients, up to `projectionIterations` velocity sweeps) -
    // cache them as transforms; the motors are normalized (this very step renormalizes its output)
    val transforms = buildTransforms(motors)
    val nodeLocalBs: Array[Pga3dBivector] = if (localBs eq null) new Array[Pga3dBivector](n) else localBs
    val forques =
      reconstructNodeImpl(inertias, prevMotors, motors, transforms, constraints, addForques, prevDt, nodeLocalBs, projectionIterations)

    // the first half kick of this step
    val lHalf = new Array[Pga3dBivector](n)
    for (pos <- FastRange(n)) {
      lHalf(pos) = transforms(pos).sandwich(inertias(pos)(nodeLocalBs(pos))) + forques(pos) * (0.5 * dt)
    }

    // the constraint gradients at the old poses, then SHAKE: solve the position-stage
    // impulses so the drifted poses satisfy the constraints; Gauss-Seidel - after each
    // impulse the affected bodies are re-drifted immediately
    val geometries = constraints.map(c => geometry(inertias, transforms, nodeLocalBs, c)).toArray
    for (pos <- FastRange(n)) {
      driftBody(inertias, motors, nodeLocalBs, lHalf, dt, nextMotors, pos)
    }

    val lambdas = new Array[Double](geometries.length)
    Pga3dConstraintResolver.sweepToConvergence(shakeIterations) { () =>
      var sweepMax = 0.0
      var ci = 0
      for (c <- constraints) {
        for (g <- geometries(ci)) {
          val pA = anchorWorld(nextMotors, c.bodyA, c.anchorA)
          val pB = anchorWorld(nextMotors, c.bodyB, c.anchorB)
          val dist = (pA - pB).norm

          // active-set: an engaged one-sided constraint is held at its bound and may release
          val target =
            if (lambdas(ci) != 0.0) {
              if (c.maxDistance <= c.minDistance || lambdas(ci) < 0.0) c.maxDistance else c.minDistance
            } else {
              Math.min(Math.max(dist, c.minDistance), c.maxDistance)
            }
          val error = dist - target
          if (error != 0.0) {
            sweepMax = Math.max(sweepMax, Math.abs(error))

            val total = clampTotal(c, lambdas(ci), lambdas(ci) - error / (dt * g.wSum))
            val delta = total - lambdas(ci)
            lambdas(ci) = total
            if (delta != 0.0) {
              val impulse = g.j * delta
              if (c.bodyA >= 0) {
                lHalf(c.bodyA) = lHalf(c.bodyA) + impulse
                driftBody(inertias, motors, nodeLocalBs, lHalf, dt, nextMotors, c.bodyA)
              }
              if (c.bodyB >= 0) {
                lHalf(c.bodyB) = lHalf(c.bodyB) - impulse
                driftBody(inertias, motors, nodeLocalBs, lHalf, dt, nextMotors, c.bodyB)
              }
            }
          }
        }
        ci += 1
      }
      sweepMax
    }

    for (pos <- FastRange(n)) {
      nextMotors(pos) = nextMotors(pos).renormalized
    }
  }

  /**
   * the node momenta reconstruction: the segment momenta derived from the poses, plus the
   * second RATTLE half kick of the previous segment and its velocity-stage impulses. The
   * node-time twists at `motors` - the honest observer velocities - are written into the
   * `localBs` output. Calls `addForques` once.
   *
   * @param localBs output: the node-time twists at `motors`; null (the default) when the
   *                caller only needs the forques
   * @return the forques of the callback at that state
   */
  def reconstructNode(inertias: Array[Pga3dInertia],
                      prevMotors: Array[Pga3dMotor],
                      motors: Array[Pga3dMotor],
                      constraints: Seq[Pga3dDistanceConstraint],
                      addForques: (Array[Pga3dMotor], Array[Pga3dBivector]) => Array[Pga3dBivector],
                      prevDt: Double,
                      localBs: Array[Pga3dBivector] | Null = null,
                      projectionIterations: Int = 100): Array[Pga3dBivector] =
    reconstructNodeImpl(inertias, prevMotors, motors, buildTransforms(motors), constraints,
      addForques, prevDt, localBs, projectionIterations)

  private def buildTransforms(motors: Array[Pga3dMotor]): Array[Pga3dTransform] =
    motors.map(Pga3dTransform.fromNormalized)

  private def reconstructNodeImpl(inertias: Array[Pga3dInertia],
                                  prevMotors: Array[Pga3dMotor],
                                  motors: Array[Pga3dMotor],
                                  transforms: Array[Pga3dTransform],
                                  constraints: Seq[Pga3dDistanceConstraint],
                                  addForques: (Array[Pga3dMotor], Array[Pga3dBivector]) => Array[Pga3dBivector],
                                  prevDt: Double,
                                  localBs: Array[Pga3dBivector] | Null,
                                  projectionIterations: Int): Array[Pga3dBivector] =
    val n = motors.length
    val twists: Array[Pga3dBivector] = if (localBs eq null) new Array[Pga3dBivector](n) else localBs
    val lSegments = new Array[Pga3dBivector](n)
    for (pos <- FastRange(n)) {
      val d = prevMotors(pos).reverse.geometric(motors(pos)).log
      val bHalf = d * (-2.0 / prevDt)
      val midFrame = prevMotors(pos).geometric((d * 0.5).exp)
      lSegments(pos) = midFrame.sandwich(inertias(pos)(bHalf))
      twists(pos) = inertias(pos).invert(transforms(pos).reverseSandwich(lSegments(pos)))
    }

    val forques = addForques(motors, twists)

    for (pos <- FastRange(n)) {
      twists(pos) = inertias(pos).invert(
        transforms(pos).reverseSandwich(lSegments(pos) + forques(pos) * (0.5 * prevDt)))
    }

    projectVelocities(inertias, transforms, twists, constraints, projectionIterations)

    forques

  /** the trial drift of one body from its current half-step momentum */
  private def driftBody(inertias: Array[Pga3dInertia],
                        motors: Array[Pga3dMotor],
                        localBs: Array[Pga3dBivector],
                        lHalf: Array[Pga3dBivector],
                        dt: Double,
                        nextMotors: Array[Pga3dMotor],
                        pos: Int): Unit =
    val displacement =
      Pga3dPhysicsSolverVerlet.solveDisplacement(inertias(pos), motors(pos), localBs(pos), lHalf(pos), dt)
    nextMotors(pos) = motors(pos).geometric(displacement.exp)

  /** the accumulated lambda of a one-sided constraint keeps its admissible sign; a two-sided
   * constraint holds the bound it engaged (max for a negative lambda, min for a positive one)
   * and releases through zero - the sign may not flip within a sweep, the constraint re-engages
   * from the inactive state on a later sweep instead */
  private def clampTotal(c: Pga3dDistanceConstraint, prevTotal: Double, total: Double): Double =
    if (c.minDistance >= c.maxDistance) total // rod
    else if (c.maxDistance.isInfinity) Math.max(total, 0.0) // strut only pushes apart
    else if (c.minDistance <= 0.0) Math.min(total, 0.0) // rope only pulls together
    else if (prevTotal < 0.0) Math.min(total, 0.0) // two-sided engaged at the max bound
    else if (prevTotal > 0.0) Math.max(total, 0.0) // two-sided engaged at the min bound
    else total // two-sided just engaging: the sign of the first impulse picks the bound

  /** paired impulses driving the relative anchor velocities along the constraints to zero */
  private def projectVelocities(inertias: Array[Pga3dInertia],
                                transforms: Array[Pga3dTransform],
                                localBs: Array[Pga3dBivector],
                                constraints: Seq[Pga3dDistanceConstraint],
                                projectionIterations: Int): Unit =
    Pga3dConstraintResolver.sweepToConvergence(projectionIterations) { () =>
      var sweepMax = 0.0
      for (c <- constraints) {
        for (g <- geometry(inertias, transforms, localBs, c)) {
          val vn = (pointVelocity(transforms, localBs, c.bodyA, g.pA) -
            pointVelocity(transforms, localBs, c.bodyB, g.pB)).antiDotI(g.n)
          val lambda = c.clampLambda(g.dist, -vn / g.wSum)
          if (lambda != 0.0) {
            sweepMax = Math.max(sweepMax, Math.abs(lambda))
            if (c.bodyA >= 0) localBs(c.bodyA) = localBs(c.bodyA) + g.gA * lambda
            if (c.bodyB >= 0) localBs(c.bodyB) = localBs(c.bodyB) - g.gB * lambda
          }
        }
      }
      sweepMax
    }

  /** the array-based twin of [[Pga3dConstraintResolver.geometry]] */
  private def geometry(inertias: Array[Pga3dInertia],
                       transforms: Array[Pga3dTransform],
                       localBs: Array[Pga3dBivector],
                       c: Pga3dDistanceConstraint): Option[Pga3dConstraintGeometry] =
    val pA = anchorWorld(transforms, c.bodyA, c.anchorA)
    val pB = anchorWorld(transforms, c.bodyB, c.anchorB)
    val delta = pA - pB
    val dist = delta.norm
    if (dist < 1e-100) return None

    val n = delta * (1.0 / dist)
    val j = Pga3dForque.force(pA, n)

    val gA = responseTwist(inertias, transforms, c.bodyA, j)
    val gB = responseTwist(inertias, transforms, c.bodyB, j)
    val wA = responseAlongN(transforms, c.bodyA, gA, pA, n)
    val wB = responseAlongN(transforms, c.bodyB, gB, pB, n)
    val wSum = wA + wB
    if (!(wSum > 0.0)) return None

    Some(Pga3dConstraintGeometry(pA, pB, dist, n, j, gA, gB, wSum))

  private def anchorWorld(transforms: Array[Pga3dTransform], bodyIndex: Int, anchor: Pga3dPoint): Pga3dPoint =
    if (bodyIndex >= 0) transforms(bodyIndex).sandwich(anchor) else anchor

  /** the motor-based twin for the poses SHAKE mutates between sweeps (caching those as
   * transforms would be rebuilt on every accepted impulse and never amortize) */
  private def anchorWorld(motors: Array[Pga3dMotor], bodyIndex: Int, anchor: Pga3dPoint): Pga3dPoint =
    if (bodyIndex >= 0) motors(bodyIndex).sandwich(anchor).toPointUnsafe else anchor

  private def responseTwist(inertias: Array[Pga3dInertia], transforms: Array[Pga3dTransform],
                            bodyIndex: Int, j: Pga3dBivector): Pga3dBivector =
    if (bodyIndex >= 0) inertias(bodyIndex).invert(transforms(bodyIndex).reverseSandwich(j))
    else Pga3dBivector.zero

  private def responseAlongN(transforms: Array[Pga3dTransform], bodyIndex: Int,
                             gLocal: Pga3dBivector, p: Pga3dPoint, n: Pga3dVector): Double =
    if (bodyIndex >= 0) (-(transforms(bodyIndex).sandwich(gLocal) cross p)).antiDotI(n)
    else 0.0

  private def pointVelocity(transforms: Array[Pga3dTransform], localBs: Array[Pga3dBivector],
                            bodyIndex: Int, p: Pga3dPoint): Pga3dVector =
    if (bodyIndex >= 0) -(transforms(bodyIndex).sandwich(localBs(bodyIndex)) cross p)
    else Pga3dVector(0.0, 0.0, 0.0)
