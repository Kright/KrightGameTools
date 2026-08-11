package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq

/**
 * [[Pga3dPhysicsSolverVerletConstrained]] (RATTLE on the motor group) on the same scenarios as
 * the RK4-family constraint tests: 2nd order, constraints at the machine epsilon, bounded
 * energy oscillation and exact momentum of body-body constraints. The solver is stateless -
 * the caller owns the pose arrays and passes the constraints to every step.
 */
class Pga3dVerletConstrainedTest extends AnyFunSuiteLike:
  private val origin = Pga3dPoint(0, 0, 0)
  private val gravity = Pga3dVector(0, -9.8, 0)
  private val linkLength = 0.5

  /** caller-side state plus the constrained step; the observers use reconstructNode */
  private class State(bodies: Array[Pga3dPhysicsBody],
                      constraints: Seq[Pga3dDistanceConstraint],
                      useGravity: Boolean,
                      dt: Double):
    val inertias: Array[Pga3dInertia] = bodies.map(_.inertia)
    var motors: Array[Pga3dMotor] = bodies.map(_.motor)

    val forquesAt: (Array[Pga3dMotor], Array[Pga3dBivector]) => Array[Pga3dBivector] =
      (motors, _) => Array.tabulate(motors.length) { i =>
        if (useGravity) {
          val center = motors(i).sandwich(inertias(i).centerOfMass).toPointUnsafe
          Pga3dForque.force(center, gravity * inertias(i).mass)
        } else {
          Pga3dBivector.zero
        }
      }

    var prevMotors: Array[Pga3dMotor] =
      val localBs = bodies.map(_.localB)
      Pga3dPhysicsSolverVerlet.makePrevMotors(inertias, motors, localBs, forquesAt(motors, localBs), dt)
    private var nextMotors: Array[Pga3dMotor] = new Array[Pga3dMotor](bodies.length)

    /** the localBs output of the last step: the node twists of the poses that step consumed */
    val stepLocalBs: Array[Pga3dBivector] = new Array[Pga3dBivector](bodies.length)

    def step(dt: Double, stepConstraints: Seq[Pga3dDistanceConstraint] = constraints): Unit =
      Pga3dPhysicsSolverVerletConstrained.step(
        inertias, prevMotors, motors, stepConstraints, forquesAt, dt, dt, nextMotors, stepLocalBs)
      val tmp = prevMotors
      prevMotors = motors
      motors = nextMotors
      nextMotors = tmp

    def localBs: Array[Pga3dBivector] =
      val out = new Array[Pga3dBivector](motors.length)
      Pga3dPhysicsSolverVerletConstrained.reconstructNode(
        inertias, prevMotors, motors, constraints, forquesAt, prevDt = dt, out, projectionIterations = 100)
      out

    def maxRodError: Double =
      constraints.map { c =>
        val pA = if (c.bodyA >= 0) motors(c.bodyA).sandwich(c.anchorA).toPointUnsafe else c.anchorA
        val pB = if (c.bodyB >= 0) motors(c.bodyB).sandwich(c.anchorB).toPointUnsafe else c.anchorB
        Math.abs((pA - pB).norm - c.maxDistance)
      }.max

    def energy: Double =
      val bs = localBs
      inertias.indices.map { i =>
        val center = motors(i).sandwich(inertias(i).centerOfMass).toPointUnsafe
        inertias(i).getKineticEnergy(bs(i)) -
          (if (useGravity) inertias(i).mass * gravity.antiDotI(center.toVectorUnsafe) else 0.0)
      }.sum

    def momentum: Pga3dBivector =
      val bs = localBs
      inertias.indices.map(i => motors(i).sandwich(inertias(i)(bs(i)))).reduce(_ + _)

  test("spinning dumbbell: distance, momentum and energy on a long run") {
    val bodies = Array(
      Pga3dPhysicsBody(Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(1, 0, 0)).toMotor, Pga3dBivector(wy = 1.0)),
      Pga3dPhysicsBody(Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(-1, 0, 0)).toMotor, Pga3dBivector(wy = -1.0)))
    val constraints = ArraySeq(Pga3dDistanceConstraint.rod(0, origin, 1, origin, 2.0))
    val state = State(bodies, constraints, useGravity = false, dt = 0.01)

    val initialE = state.energy
    val initialL = state.momentum
    var maxDistError = 0.0
    for (_ <- 0 until 20000) {
      state.step(0.01)
      maxDistError = Math.max(maxDistError, state.maxRodError)
    }
    val errorE = Math.abs(state.energy - initialE) / initialE
    val errorL = (state.momentum - initialL).norm / initialL.norm
    // measured 4.4e-16 / 1.3e-12 / 8.3e-12 over 20k steps: the momentum of a body-body
    // constraint is transported, not integrated, and the reconstruction observers are exact
    assert(maxDistError < 1e-14)
    assert(errorL < 5e-11)
    assert(errorE < 1e-10)
  }

  test("offset pendulum with asymmetric inertia: rod held, energy bounded") {
    val anchorLocal = Pga3dPoint(0.2, 0.3, -0.1)
    val dir = Pga3dVector(1.1, -0.6, 0.2)
    val anchorWorld = dir * (1.5 / dir.norm)
    val translation = anchorWorld - Pga3dVector(anchorLocal.x, anchorLocal.y, anchorLocal.z)
    val bodies = Array(
      Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(2.0, 0.8, 0.5, 0.3),
        Pga3dTranslator.addVector(translation).toMotor))
    val constraints = ArraySeq(
      Pga3dDistanceConstraint.rod(0, anchorLocal, Pga3dDistanceConstraint.world, origin, 1.5))
    val state = State(bodies, constraints, useGravity = true, dt = 0.01)
    val initialEnergy = state.energy

    var maxDistError = 0.0
    var maxEnergyError = 0.0
    for (_ <- 0 until 20000) {
      state.step(0.01)
      maxDistError = Math.max(maxDistError, state.maxRodError)
      maxEnergyError = Math.max(maxEnergyError, Math.abs(state.energy - initialEnergy))
    }
    // measured 1.8e-15 and 2.4e-2 at |E| = 19.8 over 200 seconds: ~0.1% bounded oscillation
    assert(maxDistError < 1e-13)
    assert(maxEnergyError < 0.01 * Math.abs(initialEnergy))
  }

  private def chain(angle: Double): (Array[Pga3dPhysicsBody], ArraySeq[Pga3dDistanceConstraint]) =
    val dir = Pga3dVector(Math.sin(angle), -Math.cos(angle), 0.0)
    val bodies = Array.tabulate(4) { k =>
      Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 0.02, 0.02, 0.02),
        Pga3dTranslator.addVector(dir * (linkLength * (k + 1))).toMotor)
    }
    val constraints = ArraySeq.tabulate(4) { k =>
      if (k == 0) Pga3dDistanceConstraint.rod(0, origin, Pga3dDistanceConstraint.world, origin, linkLength)
      else Pga3dDistanceConstraint.rod(k, origin, k - 1, origin, linkLength)
    }
    (bodies, constraints)

  test("swinging chain: rods held and energy bounded on a long run") {
    val (bodies, constraints) = chain(0.3)
    val state = State(bodies, constraints, useGravity = true, dt = 0.01)
    val initialEnergy = state.energy

    var maxDistError = 0.0
    var maxEnergyError = 0.0
    for (_ <- 0 until 20000) {
      state.step(0.01)
      maxDistError = Math.max(maxDistError, state.maxRodError)
      maxEnergyError = Math.max(maxEnergyError, Math.abs(state.energy - initialEnergy))
    }
    // measured 3.2e-11 and 4.6e-4 (1e-5 relative) over 20k steps; the rod residual is the
    // Newton/Gauss-Seidel exit of the SHAKE stage, not a drift
    assert(maxDistError < 1e-10)
    assert(maxEnergyError < 1e-4 * Math.abs(initialEnergy))
  }

  test("chain: 2nd-order convergence to the RK4-constrained trajectory") {
    def finalState(dt: Double): State =
      val (bodies, constraints) = chain(0.3)
      val state = State(bodies, constraints, useGravity = true, dt = dt)
      for (_ <- 0 until Math.round(1.0 / dt).toInt) {
        state.step(dt)
      }
      state

    val (referenceBodies, referenceConstraints) = chain(0.3)
    val referenceSystem = Pga3dPhysicsSystem(referenceBodies,
      Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, Pga3dConstraintResolver(referenceConstraints)))
    def addGravityToBodies(): Unit =
      for (body <- referenceBodies) {
        body.addGlobalForque(Pga3dForque.force(body.globalCenterOfMass, gravity * body.inertia.mass))
      }
    for (_ <- 0 until 10000) {
      referenceSystem.doStep(1e-4, _ => addGravityToBodies())
    }

    def stateDifference(state: State): Double =
      referenceBodies.indices.map { i =>
        (state.motors(i) - referenceBodies(i).motor).norm +
          (state.localBs(i) - referenceBodies(i).localB).norm
      }.sum

    val dts = Seq(0.02, 0.01, 0.005, 0.0025)
    val errors = dts.map(dt => stateDifference(finalState(dt)))
    for (i <- 1 until dts.length) {
      assert(errors(i) < errors(i - 1), s"errors = $errors do not decrease for dts = $dts")
      val order = math.log(errors(i - 1) / errors(i)) / math.log(2.0)
      // measured 1.998, 1.999, 2.000
      assert(order > 1.7 && order < 2.5, s"between dt=${dts(i - 1)} and dt=${dts(i)} the order is $order")
    }
  }

  test("the localBs output of step equals a separate reconstructNode of the consumed poses") {
    val (bodies, constraints) = chain(0.3)
    val state = State(bodies, constraints, useGravity = true, dt = 0.01)
    for (_ <- 0 until 10) {
      val expected = new Array[Pga3dBivector](bodies.length)
      Pga3dPhysicsSolverVerletConstrained.reconstructNode(
        state.inertias, state.prevMotors, state.motors, constraints, state.forquesAt, prevDt = 0.01, expected)
      state.step(0.01)
      for (i <- bodies.indices) {
        assert(state.stepLocalBs(i) == expected(i))
      }
    }

    // the default localBs = null only drops the twists output, the poses are identical
    val withOutput = new Array[Pga3dMotor](bodies.length)
    val withoutOutput = new Array[Pga3dMotor](bodies.length)
    val twists = new Array[Pga3dBivector](bodies.length)
    Pga3dPhysicsSolverVerletConstrained.step(
      state.inertias, state.prevMotors, state.motors, constraints, state.forquesAt, 0.01, 0.01, withOutput, twists)
    Pga3dPhysicsSolverVerletConstrained.step(
      state.inertias, state.prevMotors, state.motors, constraints, state.forquesAt, 0.01, 0.01, withoutOutput)
    for (i <- bodies.indices) {
      assert(withOutput(i) == withoutOutput(i))
    }
  }

  test("a two-sided constraint stays within its bounds and swings freely inside them") {
    // world --rod-- A --two-sided [0.4, 0.6]-- B: a swinging two-link pendulum whose lower link
    // has slack. The rod correction of every SHAKE sweep moves A and re-evaluates the engaged
    // two-sided constraint, so a wrong two-sided active-set (an unclamped lambda that holds a
    // bound like a rod or flips to the opposite bound instead of releasing through zero) breaks
    // the energy balance and the slack behavior of the lower link.
    val bodies = Array(
      Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 0.02, 0.02, 0.02),
        Pga3dTranslator.addVector(Pga3dVector(0.5, 0, 0)).toMotor),
      Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 0.02, 0.02, 0.02),
        Pga3dTranslator.addVector(Pga3dVector(1.0, 0, 0)).toMotor))
    val constraints = ArraySeq(
      Pga3dDistanceConstraint.rod(0, origin, Pga3dDistanceConstraint.world, origin, 0.5),
      Pga3dDistanceConstraint(1, origin, 0, origin, 0.4, 0.6))
    val state = State(bodies, constraints, useGravity = true, dt = 0.01)

    val initialEnergy = state.energy
    var minDist = Double.PositiveInfinity
    var maxDist = 0.0
    var maxEnergy = Double.NegativeInfinity
    for (_ <- 0 until 20000) {
      state.step(0.01)
      val pA = state.motors(1).sandwich(origin).toPointUnsafe
      val pB = state.motors(0).sandwich(origin).toPointUnsafe
      val dist = (pA - pB).norm
      minDist = Math.min(minDist, dist)
      maxDist = Math.max(maxDist, dist)
      maxEnergy = Math.max(maxEnergy, state.energy)
    }
    assert(minDist >= 0.4 - 1e-9, s"minDist = $minDist")
    assert(maxDist <= 0.6 + 1e-9, s"maxDist = $maxDist")
    // the slack link must actually use its range, not get pinned to one bound
    assert(minDist < 0.55 && maxDist > 0.45, s"the link never moved inside [$minDist, $maxDist]")
    // the taut catches of the slack link legitimately dissipate energy (no restitution), so
    // apart from the small bounded RATTLE oscillation the energy may only go down from the
    // initial level - it must never be created
    assert(maxEnergy <= initialEnergy + 0.5, s"energy grew: $maxEnergy > $initialEnergy")
  }

  test("a two-sided constraint releases within a step instead of flipping to the opposite bound") {
    // a two-sided [1, 2] tether to the origin plus a rod to an off-axis anchor, on one body.
    // The starting pose (a caller-side pose edit) violates the tether's max bound, so the first
    // sweep engages the tether; the rod correction then drags the body deep inside [1, 2], and
    // the accumulated tether lambda must release through zero (the true active set of the step
    // is "rod only"). A wrong active-set flips the lambda sign and pins the opposite bound -
    // the sweeps then fight, burn the iteration limit and end at a distorted pose.
    val rodAnchor = Pga3dPoint(0, -1.2, 0)
    val bodies = Array(
      Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(2.5, 0, 0)).toMotor))
    val constraints = ArraySeq(
      Pga3dDistanceConstraint(0, origin, Pga3dDistanceConstraint.world, origin, 1.0, 2.0),
      Pga3dDistanceConstraint.rod(0, origin, Pga3dDistanceConstraint.world, rodAnchor, 1.0))
    val state = State(bodies, constraints, useGravity = false, dt = 0.01)

    for (stepIndex <- 0 until 20) {
      state.step(0.01)
      val p = state.motors(0).sandwich(origin).toPointUnsafe
      val tetherDist = p.toVectorUnsafe.norm
      val rodDist = (p - rodAnchor).norm
      assert(Math.abs(rodDist - 1.0) < 1e-6, s"step $stepIndex: rodDist = $rodDist")
      assert(tetherDist >= 1.0 - 1e-6 && tetherDist <= 2.0 + 1e-6, s"step $stepIndex: tetherDist = $tetherDist")
    }
  }

  test("rope: free fall until taut, never longer than the bound") {
    val bodies = Array(
      Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(1.0, 0, 0)).toMotor))
    val constraints = ArraySeq(
      Pga3dDistanceConstraint.rope(0, origin, Pga3dDistanceConstraint.world, origin, 2.0))
    val state = State(bodies, constraints, useGravity = true, dt = 0.005)

    var maxDist = 0.0
    var wasSlack = false
    for (_ <- 0 until 2000) {
      state.step(0.005)
      val dist = state.motors(0).sandwich(origin).toPointUnsafe.toVectorUnsafe.norm
      maxDist = Math.max(maxDist, dist)
      wasSlack |= dist < 1.999
    }
    assert(wasSlack, "the rope must be slack during the free fall")
    assert(maxDist <= 2.0 * (1.0 + 1e-9), s"maxDist = $maxDist")
    assert(state.localBs.head.norm < 100.0)
  }

  test("the constraints may change between steps") {
    // the body falls freely, then a rope appears mid-flight and catches it
    val bodies = Array(
      Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(1.0, 0, 0)).toMotor))
    val rope = Pga3dDistanceConstraint.rope(0, origin, Pga3dDistanceConstraint.world, origin, 2.0)
    val state = State(bodies, ArraySeq(rope), useGravity = true, dt = 0.005)

    for (step <- 0 until 2000) {
      state.step(0.005, if (step < 100) ArraySeq.empty else ArraySeq(rope))
    }
    val dist = state.motors(0).sandwich(origin).toPointUnsafe.toVectorUnsafe.norm
    assert(dist <= 2.0 * (1.0 + 1e-9), s"dist = $dist")
    assert(state.localBs.head.norm < 100.0)
  }
