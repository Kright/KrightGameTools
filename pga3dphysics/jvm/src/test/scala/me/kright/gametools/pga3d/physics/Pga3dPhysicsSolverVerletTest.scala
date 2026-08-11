package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike

/** caller-side state of the stateless Verlet: the two pose arrays and the derived twists */
private class VerletState(bodies: Array[Pga3dPhysicsBody], dt: Double,
                          forquesAt: (Array[Pga3dMotor], Array[Pga3dBivector]) => Array[Pga3dBivector]):
  val inertias: Array[Pga3dInertia] = bodies.map(_.inertia)
  var motors: Array[Pga3dMotor] = bodies.map(_.motor)
  var localBs: Array[Pga3dBivector] = bodies.map(_.localB)
  var prevMotors: Array[Pga3dMotor] =
    Pga3dPhysicsSolverVerlet.makePrevMotors(inertias, motors, localBs, forquesAt(motors, localBs), dt)
  private var nextMotors: Array[Pga3dMotor] = new Array[Pga3dMotor](bodies.length)

  def step(dt: Double): Unit =
    Pga3dPhysicsSolverVerlet.step(
      inertias, prevMotors, motors, forquesAt(motors, localBs), dt, dt, nextMotors, localBs)
    val tmp = prevMotors
    prevMotors = motors
    motors = nextMotors
    nextMotors = tmp

  def kineticEnergy: Double =
    inertias.indices.map(i => inertias(i).getKineticEnergy(localBs(i))).sum

  def momentum: Pga3dBivector =
    inertias.indices.map(i => motors(i).sandwich(inertias(i)(localBs(i))))
      .reduce(_ + _)

object VerletState:
  val noForques: (Array[Pga3dMotor], Array[Pga3dBivector]) => Array[Pga3dBivector] =
    (motors, _) => Array.fill(motors.length)(Pga3dBivector.zero)

class Pga3dPhysicsSolverVerletTest extends AnyFunSuiteLike:

  private def freeBody(): Array[Pga3dPhysicsBody] =
    Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id))

  test("calculate free rotation body precession for Verlet") {
    val dt = 0.01
    val state = VerletState(freeBody(), dt, VerletState.noForques)
    val initialE = state.kineticEnergy
    val initialL = state.momentum

    var maxErrorE = 0.0
    var maxErrorL = 0.0
    for (_ <- 0 until 1000) {
      state.step(dt)
      maxErrorE = Math.max(maxErrorE, Math.abs(state.kineticEnergy - initialE) / initialE)
      maxErrorL = Math.max(maxErrorL, (state.momentum - initialL).norm / initialL.norm)
    }
    // measured 4.2e-6 / ~1e-15: the momentum is transported, not integrated
    assert(maxErrorE < 1e-5)
    assert(maxErrorL < 1e-12)
  }

  test("Verlet trajectory error converges with the 2nd order") {
    val totalTime = 10.0

    def finalMotor(dt: Double): Pga3dMotor =
      val state = VerletState(freeBody(), dt, VerletState.noForques)
      for (_ <- 0 until Math.round(totalTime / dt).toInt) {
        state.step(dt)
      }
      state.motors.head

    val referenceSystem = Pga3dPhysicsSystemForTest(freeBody(), Pga3dPhysicsSolverRKMK4)
    for (_ <- 0 until 100000) {
      referenceSystem.doStep(totalTime / 100000, _ => ())
    }
    val reference = referenceSystem.state.head.motor

    val dts = Seq(0.02, 0.01, 0.005, 0.0025)
    val errors = dts.map(dt => (finalMotor(dt) - reference).norm)
    for (i <- 1 until dts.length) {
      val order = math.log(errors(i - 1) / errors(i)) / math.log(2.0)
      assert(order > 1.7 && order < 2.3, s"between dt=${dts(i - 1)} and dt=${dts(i)} the order is $order")
    }
  }

  test("no secular energy drift on a long run") {
    val dt = 0.01

    def maxErrorE(stepsCount: Int): Double =
      val state = VerletState(freeBody(), dt, VerletState.noForques)
      val initialE = state.kineticEnergy
      var maxError = 0.0
      for (_ <- 0 until stepsCount) {
        state.step(dt)
        maxError = Math.max(maxError, Math.abs(state.kineticEnergy - initialE) / initialE)
      }
      maxError

    val shortRun = maxErrorE(1000)
    val longRun = maxErrorE(100000)
    // measured 4.16655e-6 vs 4.16658e-6: a bounded oscillation, the hallmark of Verlet - the
    // displacement solves run to convergence, so re-deriving the momentum from the poses
    // every step loses nothing
    assert(longRun < shortRun * 2.0,
      s"energy error grew from $shortRun to $longRun over 100x more steps")
  }

  test("pure translation is exact") {
    val bodies = Array(Pga3dPhysicsBody(
      Pga3dInertiaLocal(2.0, 1.0, 1.0, 1.0),
      Pga3dMotor.id,
      Pga3dBivector(wx = 1.0, wy = -0.5, wz = 0.25)))
    val dt = 0.01
    val state = VerletState(bodies, dt, VerletState.noForques)
    for (_ <- 0 until 1000) {
      state.step(dt)
    }

    val center = state.motors.head.sandwich(Pga3dPointCenter).toPointUnsafe
    val expected = Pga3dPoint(10.0, -5.0, 2.5) // the localB translation part is the velocity
    assert((center - expected).norm < 1e-11, s"center = $center")
  }

  test("editing a motor between steps edits the velocity, like in position-based dynamics") {
    val dt = 0.01
    val bodies = Array(Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 1.0, 1.0, 1.0), Pga3dMotor.id))
    val state = VerletState(bodies, dt, VerletState.noForques)

    state.step(dt) // the body is at rest
    assert(state.localBs.head.norm < 1e-15)

    // depenetration-style pose correction: push the body by 0.1 along x
    state.motors(0) = Pga3dTranslator.addVector(Pga3dVector(0.1, 0.0, 0.0)).toMotor.geometric(state.motors(0))

    state.step(dt)
    // the solver must interpret the jump as a velocity of 0.1 / dt = 10
    assert(math.abs(state.localBs.head.wx - 10.0) < 1e-9, s"localB = ${state.localBs.head}")
  }
