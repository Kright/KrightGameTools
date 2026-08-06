package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq

class Pga3dConstraintResolverTest extends AnyFunSuiteLike:
  private val origin = Pga3dPoint(0, 0, 0)

  /** two unit-mass bodies at (+-1, 0, 0) connected by a rod of length 2, spinning in the xy plane */
  private def dumbbell(): (Array[Pga3dPhysicsBody], Pga3dConstraintResolver) =
    val v = 1.0
    val bodies = Array(
      Pga3dPhysicsBody(
        Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(1, 0, 0)).toMotor,
        Pga3dBivector(wy = v)),
      Pga3dPhysicsBody(
        Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(-1, 0, 0)).toMotor,
        Pga3dBivector(wy = -v)),
    )
    val resolver = Pga3dConstraintResolver(ArraySeq(
      Pga3dDistanceConstraint.rod(0, origin, 1, origin, 2.0)))
    (bodies, resolver)

  private def anchorDistance(bodies: Array[Pga3dPhysicsBody], c: Pga3dDistanceConstraint): Double =
    val pA = if (c.bodyA >= 0) bodies(c.bodyA).motorSandwich(c.anchorA) else c.anchorA
    val pB = if (c.bodyB >= 0) bodies(c.bodyB).motorSandwich(c.anchorB) else c.anchorB
    (pA - pB).norm

  test("spinning dumbbell: distance, momentum and energy are conserved") {
    val (bodies, resolver) = dumbbell()
    val solver = Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, resolver)
    val system = Pga3dPhysicsSystemForTest(bodies, solver)
    val constraint = resolver.constraints.head

    var maxDistError = 0.0
    for (_ <- 0 until 1000) {
      system.doStep(0.01, _ => ())
      maxDistError = Math.max(maxDistError, Math.abs(anchorDistance(bodies, constraint) - 2.0))
    }
    val error = system.getError()
    // measured 4.4e-16 / 1.4e-11 / 6.9e-12
    assert(maxDistError < 1e-12)
    assert(error.errorL < 1e-10)
    assert(error.errorE < 1e-9)
  }

  test("the constraint forques alone keep the dumbbell on the circle, without any projection") {
    val (bodies, resolver) = dumbbell()
    val system = Pga3dPhysicsSystemForTest(bodies, Pga3dPhysicsSolverRK4)
    val constraint = resolver.constraints.head

    var maxDistError = 0.0
    for (_ <- 0 until 100) {
      system.doStep(0.01, _ => resolver.addConstraintForques(bodies))
      maxDistError = Math.max(maxDistError, Math.abs(anchorDistance(bodies, constraint) - 2.0))
    }
    // measured 1.4e-12 / 1.4e-12: part 1 supplies the exact centripetal force, so even with
    // no projection the RK4 integration barely drifts
    assert(maxDistError < 1e-10)
    assert(system.getError().errorE < 1e-10)
  }

  test("point velocity and acceleration formulas match finite differences") {
    val body = Pga3dPhysicsBody(
      Pga3dInertiaLocal(2.0, 0.8, 0.5, 0.3),
      Pga3dRotor.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 1)).toMotor
        .geometric(Pga3dTranslator.addVector(Pga3dVector(1, 2, 3)).toMotor),
      Pga3dBivector(0.3, -0.2, 0.5, 1.0, -0.7, 0.4))
    val q = Pga3dPoint(0.2, 0.3, -0.1)

    // the formulas the resolver is built on: v = -(W cross p), a = -(W' cross p) - (W cross v)
    val w = body.motor.sandwich(body.localB)
    val p0 = body.motorSandwich(q)
    val vFormula = -(w cross p0)
    body.resetForqueAccum()
    val dw = body.motor.sandwich(body.inertia.getAcceleration(body.localB, body.localForque))
    val aFormula = -(dw cross p0) - (w cross vFormula)

    val h = 1e-5
    def anchorAfter(dt: Double): Pga3dPoint =
      val copy = body.deepCopy
      Pga3dPhysicsSystem(Array(copy), Pga3dPhysicsSolverRK4).doStep(dt, _ => ())
      copy.motorSandwich(q)

    val pPlus = anchorAfter(h)
    val pMinus = anchorAfter(-h)
    val vNumeric = (pPlus - pMinus) * (1.0 / (2.0 * h))
    val aNumeric = ((pPlus - p0) + (pMinus - p0)) * (1.0 / (h * h))

    assert((vFormula - vNumeric).norm < 1e-8, s"vFormula = $vFormula, vNumeric = $vNumeric")
    assert((aFormula - aNumeric).norm < 1e-4, s"aFormula = $aFormula, aNumeric = $aNumeric")
  }

  /**
   * pendulum on a rod with an off-center anchor and an asymmetric inertia: the constraint
   * force creates torque, so the resolver must handle the rotational response correctly
   */
  private def offsetPendulum(): (Array[Pga3dPhysicsBody], Pga3dConstraintResolver) =
    // the initial pose satisfies the rod exactly: a projection snap at the start would be an
    // O(dt)-sized transient that ruins convergence measurements
    val anchorLocal = Pga3dPoint(0.2, 0.3, -0.1)
    val dir = Pga3dVector(1.1, -0.6, 0.2)
    val anchorWorld = dir * (1.5 / dir.norm)
    val translation = anchorWorld - Pga3dVector(anchorLocal.x, anchorLocal.y, anchorLocal.z)

    val bodies = Array(
      Pga3dPhysicsBody.motionless(
        Pga3dInertiaLocal(2.0, 0.8, 0.5, 0.3),
        Pga3dTranslator.addVector(translation).toMotor))
    val resolver = Pga3dConstraintResolver(ArraySeq(
      Pga3dDistanceConstraint.rod(0, anchorLocal, Pga3dDistanceConstraint.world, origin, 1.5)))
    (bodies, resolver)

  private val gravity = Pga3dVector(0, -9.8, 0)

  private def addGravity(bodies: Array[Pga3dPhysicsBody]): Unit =
    for (body <- bodies) {
      body.addGlobalForque(Pga3dForque.force(body.globalCenterOfMass, gravity * body.inertia.mass))
    }

  private def pendulumEnergy(system: Pga3dPhysicsSystem): Double =
    val body = system.state.head
    system.getKineticEnergy() - body.inertia.mass * gravity.antiDotI(body.globalCenterOfMass.toVectorUnsafe)

  test("offset pendulum: constraint is held and energy is conserved") {
    val (bodies, resolver) = offsetPendulum()
    val solver = Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, resolver)
    val system = Pga3dPhysicsSystem(bodies, solver)
    val constraint = resolver.constraints.head

    // the initial pose does not satisfy the constraint exactly: the first projection snaps it
    system.doStep(0.01, _ => addGravity(bodies))
    val initialEnergy = pendulumEnergy(system)

    var maxDistError = 0.0
    var maxEnergyError = 0.0
    for (_ <- 0 until 1000) {
      system.doStep(0.01, _ => addGravity(bodies))
      maxDistError = Math.max(maxDistError, Math.abs(anchorDistance(bodies, constraint) - 1.5))
      maxEnergyError = Math.max(maxEnergyError, Math.abs(pendulumEnergy(system) - initialEnergy))
    }
    // measured 1.3e-15 / 2.6e-5 at |E| = 19.8
    assert(maxDistError < 1e-12)
    // ~4.4e-6 relative: the plain RK4 truncation for the ~5 rad/s body-swing mode at dt = 0.01
    assert(maxEnergyError < 1e-5 * Math.abs(initialEnergy))
  }

  test("the constrained RK4 keeps a high order on the offset pendulum") {
    // a short horizon: a rigid body on a rod is a double-pendulum relative, so trajectories
    // diverge exponentially and long-horizon order measurements only see the chaos
    def finalState(dt: Double): Pga3dBodyState =
      val (bodies, resolver) = offsetPendulum()
      val system = Pga3dPhysicsSystem(bodies, Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, resolver))
      for (_ <- 0 until Math.round(1.0 / dt).toInt) {
        system.doStep(dt, _ => addGravity(bodies))
      }
      Pga3dBodyState(bodies.head)

    val reference = finalState(1e-4)

    val dts = Seq(0.02, 0.01, 0.005)
    val errors = dts.map { dt =>
      val state = finalState(dt)
      (state.motor - reference.motor).norm + (state.localB - reference.localB).norm
    }
    for (i <- 1 until dts.length) {
      val order = math.log(errors(i - 1) / errors(i)) / math.log(2.0)
      // measured 4.10 and 4.06: the exact stage-level constraint forques preserve the order,
      // projection only removes the tiny residual drift
      assert(order > 3.5, s"between dt=${dts(i - 1)} and dt=${dts(i)} the order is $order")
    }
  }

  test("rope: free fall until taut, never longer than the bound") {
    val bodies = Array(
      Pga3dPhysicsBody.motionless(
        Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(1.0, 0, 0)).toMotor))
    val resolver = Pga3dConstraintResolver(ArraySeq(
      Pga3dDistanceConstraint.rope(0, origin, Pga3dDistanceConstraint.world, origin, 2.0)))
    val system = Pga3dPhysicsSystem(bodies, Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, resolver))
    val constraint = resolver.constraints.head

    var maxDist = 0.0
    var wasSlack = false
    for (_ <- 0 until 2000) {
      system.doStep(0.005, _ => addGravity(bodies))
      val dist = anchorDistance(bodies, constraint)
      maxDist = Math.max(maxDist, dist)
      wasSlack |= dist < 1.999
    }
    assert(wasSlack, "the rope must be slack during the free fall")
    assert(maxDist <= 2.0 * (1.0 + 1e-9), s"maxDist = $maxDist")
    // after it is taut the body swings on the rope: still moving, no explosion
    assert(bodies.head.localB.norm < 100.0)
  }

  test("strut: overlapping bodies are pushed apart, momentum stays zero") {
    val bodies = Array(
      Pga3dPhysicsBody.motionless(
        Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(0.25, 0, 0)).toMotor),
      Pga3dPhysicsBody.motionless(
        Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(-0.25, 0, 0)).toMotor),
    )
    val resolver = Pga3dConstraintResolver(ArraySeq(
      Pga3dDistanceConstraint.strut(0, origin, 1, origin, 1.0)))
    val system = Pga3dPhysicsSystem(bodies, Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, resolver))
    val constraint = resolver.constraints.head

    for (_ <- 0 until 10) {
      system.doStep(0.01, _ => ())
    }
    val dist = anchorDistance(bodies, constraint)
    assert(dist >= 1.0 - 1e-9, s"dist = $dist")
    assert(system.getL().norm < 1e-12)
    // symmetric masses: the push is symmetric
    assert(Math.abs(bodies(0).globalCenter.x + bodies(1).globalCenter.x) < 1e-12)
  }

  test("the resolver works with every solver except Verlet") {
    // the momentum leak comes from the pose projection, so it scales with the solver's own
    // per-step drift: tiny for the 4th-order solvers, visible for the 2nd-order ones, and
    // only the distance is guaranteed for Euler
    val solversAndMaxErrorL = ArraySeq[(Pga3dPhysicsSolver, Double)](
      (Pga3dPhysicsSolverEuler, 1.0),
      (Pga3dPhysicsSolverHeun, 1e-5),
      (Pga3dPhysicsSolverMidPoint, 1e-5),
      (Pga3dPhysicsSolverRK4, 1e-9),
      (Pga3dPhysicsSolverRKMK4, 1e-9),
      (Pga3dPhysicsSolverRKF45(), 1e-9),
      (Pga3dPhysicsSolverGaussLegendre(iterations = 3), 1e-9),
    )

    for ((inner, maxErrorL) <- solversAndMaxErrorL) {
      val (bodies, resolver) = dumbbell()
      val system = Pga3dPhysicsSystemForTest(bodies, Pga3dPhysicsSolverConstrained(inner, resolver))
      val constraint = resolver.constraints.head

      var maxDistError = 0.0
      for (_ <- 0 until 500) {
        system.doStep(0.01, _ => ())
        maxDistError = Math.max(maxDistError, Math.abs(anchorDistance(bodies, constraint) - 2.0))
      }
      val error = system.getError()
      // measured errorL: euler 2.6e-2, heun/midPoint 6.3e-7, rk4/rkmk4 3.5e-12,
      // rkf45 1.3e-12, gaussLegendre(3) 4.8e-16
      assert(maxDistError < 1e-9, s"solver = $inner, maxDistError = $maxDistError")
      assert(error.errorL < maxErrorL, s"solver = $inner, error = $error")
    }
  }
