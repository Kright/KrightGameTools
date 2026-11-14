package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.physics1d.{Integrable, IntegrableUniformArray, OdeSolver, OdeSolverRK4, PointMass1d}
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq

class Pga3dPhysicsSolverRKMK4Test extends AnyFunSuiteLike:

  private def precessionMaxError(motor: Pga3dMotor,
                                 stepsCount: Int = 1000,
                                 dt: Double = 0.01): ErrorOfEnergyAndMomentum =
    val bodies = Array(Pga3dPhysicsSystemForTest.simpleBody(motor))
    val system = Pga3dPhysicsSystemForTest(bodies, Pga3dPhysicsSolverRKMK4(bodies))
    val errors = for (_ <- 0 until stepsCount) yield {
      system.doStep(dt, _ => ())
      system.getError()
    }
    errors.reduce(_ max _)

  test("calculate free rotation body precession for RKMK4 for different centers") {
    val expectedMaxError = ErrorOfEnergyAndMomentum(6.0E-11, 2.0E-9)

    val centers = ArraySeq[Pga3dPoint](
      Pga3dPoint(0, 0, 0),
      Pga3dPoint(1, 1, 1),
      Pga3dPoint(1e1, 1e1, 1e1),
      Pga3dPoint(1e2, 1e2, 1e2),
      Pga3dPoint(1e3, 1e3, 1e3),
      Pga3dPoint(1e4, 1e4, 1e4),
      Pga3dPoint(1e5, 1e5, 1e5),
      Pga3dPoint(1e6, 1e6, 1e6),
    )

    for (center <- centers) {
      val maxError = precessionMaxError(Pga3dTranslator.addVector(center.toVectorUnsafe).toMotor)
      // println(s"center = $center, maxError = $maxError")
      assert(maxError < expectedMaxError, s"center = $center, maxError = $maxError, but expected $expectedMaxError")
    }
  }

  test("calculate free rotation body precession for RKMK4 for different orientation") {
    val motors = ArraySeq(
      Pga3dMotor.id,
      Pga3dTranslator.addVector(Pga3dVector(1, 0, 0)).toMotor,
      Pga3dTranslator.addVector(Pga3dVector(0, 1, 0)).toMotor,
      Pga3dTranslator.addVector(Pga3dVector(0, 0, 1)).toMotor,
      Pga3dTranslator.addVector(Pga3dVector(1, 2, 3)).toMotor,
      Pga3dRotor.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 0)).toMotor,
      Pga3dRotor.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 1)).toMotor,
      Pga3dRotor.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 1)).geometric(Pga3dTranslator.addVector(Pga3dVector(1, 2, 3)).toMotor),
    )

    for (motor <- motors) {
      val maxError = precessionMaxError(motor)
      // println(s"motor = $motor, maxError = $maxError")
      assert(maxError < ErrorOfEnergyAndMomentum(errorE = 6e-11, errorL = 1e-9))
    }
  }

  private def precessionFinalState(makeSolver: Array[Pga3dPhysicsBody] => OdeSolver,
                                   dt: Double,
                                   totalTime: Double): Pga3dBodyState =
    val bodies = Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id))
    val system = Pga3dPhysicsSystemForTest(bodies, makeSolver(bodies))
    val stepsCount = Math.round(totalTime / dt).toInt
    for (_ <- 0 until stepsCount) {
      system.doStep(dt, _ => ())
    }
    Pga3dBodyState(system.state.head)

  /**
   * checks whether RKMK4 pulls ahead of RK4 when the rotation per step is large - it does not.
   * 12s of free precession, |omega| ~ sqrt(3); motorErr is the final-state motor error against
   * an RKMK4 reference at dt = 1e-4, errL is the max relative momentum error over the run
   * (the energy error is identical by construction: for a free body localB sees the same
   * flat RK4 stages in both solvers):
   *
   * dt = 0.05 (0.09 rad/step): motorErr rk4/rkmk4 = 1.000, errL rk4/rkmk4 = 1.084
   * dt = 0.10 (0.17 rad/step): motorErr rk4/rkmk4 = 0.999, errL rk4/rkmk4 = 1.078
   * dt = 0.20 (0.35 rad/step): motorErr rk4/rkmk4 = 0.998, errL rk4/rkmk4 = 1.066
   * dt = 0.30 (0.52 rad/step): motorErr rk4/rkmk4 = 0.997, errL rk4/rkmk4 = 1.047
   * dt = 0.50 (0.87 rad/step): motorErr rk4/rkmk4 = 0.997, errL rk4/rkmk4 = 1.022
   * dt = 0.75 (1.30 rad/step): motorErr rk4/rkmk4 = 1.003, errL rk4/rkmk4 = 0.997
   * dt = 1.00 (1.73 rad/step): motorErr rk4/rkmk4 = 1.024, errL rk4/rkmk4 = 0.951
   *
   * the dominant O(h^5) local error is the RK4 tableau truncation itself (shared by both
   * methods); the off-manifold part that RKMK4 removes and the renormalization projects out
   * is a small fraction of it, and at dt >= 0.75 the absolute errors are already useless
   * (motorErr ~ 0.1-0.2) for either solver.
   */
  test("large step diagnostic") {
    assume(false, "skip test")

    val totalTime = 12.0
    val reference = precessionFinalState(Pga3dPhysicsSolverRKMK4(_), dt = 1e-4, totalTime)

    def run(makeSolver: Array[Pga3dPhysicsBody] => OdeSolver, dt: Double): (Pga3dBodyState, ErrorOfEnergyAndMomentum) =
      val bodies = Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id))
      val system = Pga3dPhysicsSystemForTest(bodies, makeSolver(bodies))
      val stepsCount = Math.round(totalTime / dt).toInt
      val errors = for (_ <- 0 until stepsCount) yield {
        system.doStep(dt, _ => ())
        system.getError()
      }
      (Pga3dBodyState(system.state.head), errors.reduce(_ max _))

    for (dt <- Seq(0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.75, 1.0)) {
      val (rk4State, rk4Err) = run(_ => OdeSolverRK4, dt)
      val (rkmk4State, rkmk4Err) = run(Pga3dPhysicsSolverRKMK4(_), dt)

      def motorError(state: Pga3dBodyState): Double = (state.motor - reference.motor).norm

      println(f"dt = $dt%.2f (angle/step = ${dt * math.sqrt(3.0)}%.2f rad): " +
        f"motorErr rk4 = ${motorError(rk4State)}%.3e, rkmk4 = ${motorError(rkmk4State)}%.3e, ratio = ${motorError(rk4State) / motorError(rkmk4State)}%.3f | " +
        f"errL rk4 = ${rk4Err.errorL}%.3e, rkmk4 = ${rkmk4Err.errorL}%.3e, ratio = ${rk4Err.errorL / rkmk4Err.errorL}%.3f")
    }
  }

  /**
   * measured trajectory error of the final (motor, localB) state after 10s of free precession,
   * against an RKMK4 reference at dt = 1e-4 (|omega| ~ sqrt(3), so dt = 0.4 is ~0.7 rad per step):
   *
   * dt = 0.4,   rk4 error = 7.543e-3,  rkmk4 error = 7.536e-3,  rk4 / rkmk4 = 1.0010
   * dt = 0.2,   rk4 error = 4.377e-4,  rkmk4 error = 4.362e-4,  rk4 / rkmk4 = 1.0036
   * dt = 0.1,   rk4 error = 2.600e-5,  rkmk4 error = 2.587e-5,  rk4 / rkmk4 = 1.0052
   * dt = 0.04,  rk4 error = 6.445e-7,  rkmk4 error = 6.406e-7,  rk4 / rkmk4 = 1.0061
   * dt = 0.02,  rk4 error = 3.985e-8,  rkmk4 error = 3.960e-8,  rk4 / rkmk4 = 1.0065
   * dt = 0.01,  rk4 error = 2.478e-9,  rkmk4 error = 2.461e-9,  rk4 / rkmk4 = 1.0066
   * dt = 0.005, rk4 error = 1.543e-10, rkmk4 error = 1.533e-10, rk4 / rkmk4 = 1.0067
   *
   * both solvers converge with the 4th order; RK4 with the final renormalization is also a
   * geometric 4th-order method, so RKMK4 wins only a fraction of a percent of the error constant.
   * The structural difference is that the RKMK4 motor is unit by construction (exp of a bivector),
   * while RK4 relies on the renormalization to project back onto the motor manifold.
   */
  test("RKMK4 precession trajectory error: 4th order and not worse than RK4") {
    val totalTime = 10.0
    val reference = precessionFinalState(Pga3dPhysicsSolverRKMK4(_), dt = 1e-4, totalTime)

    def error(state: Pga3dBodyState): Double =
      (state.motor - reference.motor).norm + (state.localB - reference.localB).norm

    val dts = ArraySeq(0.04, 0.02, 0.01, 0.005)
    val errors = dts.map { dt =>
      val rk4 = error(precessionFinalState(_ => OdeSolverRK4, dt, totalTime))
      val rkmk4 = error(precessionFinalState(Pga3dPhysicsSolverRKMK4(_), dt, totalTime))
      // println(s"dt = $dt, rk4 error = $rk4, rkmk4 error = $rkmk4, ratio = ${rk4 / rkmk4}")

      assert(rkmk4 <= rk4, s"dt = $dt, rkmk4 error = $rkmk4 is worse than rk4 error = $rk4")
      (rk4, rkmk4)
    }

    for (i <- 1 until dts.length) {
      val order = math.log(errors(i - 1)._2 / errors(i)._2) / math.log(2.0)
      // println(s"rkmk4 order between dt=${dts(i - 1)} and dt=${dts(i)}: $order")
      assert(order > 3.8 && order < 4.2, s"between dt=${dts(i - 1)} and dt=${dts(i)} the order is $order")
    }
  }

  test("only the motors differ from OdeSolverRK4: localB of free bodies and an uncoupled object of the same world keep every bit") {
    def makeBodies(): Array[Pga3dPhysicsBody] =
      Array.tabulate(3)(i => Pga3dPhysicsSystemForTest.simpleBody(Pga3dTranslator.addVector(Pga3dVector(i, 2.0 * i, -i)).toMotor))

    def makeMass(): PointMass1d = PointMass1d(2.0, x = 1.0, v = 0.5)

    def addSpringForce(mass: PointMass1d): Unit =
      mass.addForce(-40.0 * mass.x)

    val rk4Bodies = makeBodies()
    val rk4Mass = makeMass()
    val rk4World = Integrable(IntegrableUniformArray(rk4Bodies, Pga3dBodyState.componentsCount), rk4Mass)

    val bodies = makeBodies()
    val mass = makeMass()
    val world = Integrable(IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount), mass)
    val rkmk4 = Pga3dPhysicsSolverRKMK4(bodies)

    for (_ <- 0 until 1000) {
      OdeSolverRK4.step(rk4World, 0.01, _ => addSpringForce(rk4Mass))
      rkmk4.step(world, 0.01, _ => addSpringForce(mass))
    }

    for (i <- bodies.indices) {
      assert(bodies(i).localB == rk4Bodies(i).localB, s"body $i")
      assert(bodies(i).motor != rk4Bodies(i).motor, s"body $i: the group update must not coincide with the flat one")
    }
    assert(mass.x == rk4Mass.x && mass.v == rk4Mass.v)
  }

  private val tumblingAtTwoRadiansPerSecond = Pga3dBivector(xy = 1.0, yz = 1.0, xz = -1.0) * (2.0 / Math.sqrt(3.0))
  private val spinTime = 8.0

  private def finalMotorOfFreeBody(makeSolver: Array[Pga3dPhysicsBody] => OdeSolver,
                                   inertia: Pga3dInertiaLocal,
                                   localB: Pga3dBivector,
                                   dt: Double): Pga3dMotor =
    val bodies = Array(Pga3dPhysicsBody(inertia, Pga3dMotor.id, localB))
    val system = Pga3dPhysicsSystem(bodies, makeSolver(bodies))
    for (_ <- 0 until Math.round(spinTime / dt).toInt) {
      system.doStep(dt, _ => ())
    }
    bodies.head.motor

  private def motorErrorsOfFreeBody(inertia: Pga3dInertiaLocal,
                                    localB: Pga3dBivector,
                                    dt: Double,
                                    reference: Pga3dMotor): (rk4: Double, rkmk4: Double) =
    (rk4 = (finalMotorOfFreeBody(_ => OdeSolverRK4, inertia, localB, dt) - reference).norm,
      rkmk4 = (finalMotorOfFreeBody(Pga3dPhysicsSolverRKMK4(_), inertia, localB, dt) - reference).norm)

  test("a twist constant in the body frame: RKMK4 is exact up to rounding at any step, RK4 keeps the error of its rotation per step") {
    val sphere = Pga3dInertiaLocal(1.0, 1.0, 1.0, 1.0)
    val asymmetric = Pga3dInertiaLocal(1.0, 3.0, 2.0, 1.0)
    val bodiesWithConstantTwist = Seq[(name: String, inertia: Pga3dInertiaLocal, localB: Pga3dBivector)](
      ("a sphere, any axis", sphere, tumblingAtTwoRadiansPerSecond),
      ("an asymmetric body about its principal axis xy", asymmetric, Pga3dBivector(xy = 2.0)),
      ("an asymmetric body about its principal axis xz", asymmetric, Pga3dBivector(xz = 2.0)),
      ("an asymmetric body about its principal axis yz", asymmetric, Pga3dBivector(yz = 2.0)),
    )
    val stepsAndMinRk4Errors = Seq[(dt: Double, minRk4Error: Double)]((0.02, 3e-9), (0.1, 2e-6), (0.4, 5e-4))

    for (body <- bodiesWithConstantTwist; step <- stepsAndMinRk4Errors) {
      val exact = (body.localB * (-0.5 * spinTime)).exp
      val errors = motorErrorsOfFreeBody(body.inertia, body.localB, step.dt, exact)
      assert(errors.rkmk4 < 1e-12, s"${body.name}, dt = ${step.dt}")
      assert(errors.rk4 > step.minRk4Error, s"${body.name}, dt = ${step.dt}")
    }
  }

  test("RK4 lags a constant twist by (half of the rotation per step)^5 / 320 of the motor per step") {
    val sphere = Pga3dInertiaLocal(1.0, 1.0, 1.0, 1.0)
    val localB = tumblingAtTwoRadiansPerSecond
    val exact = (localB * (-0.5 * spinTime)).exp
    val stepsAndMeasuredToPredictedRatios = Seq[(dt: Double, minRatio: Double, maxRatio: Double)](
      (0.02, 0.99, 1.01),
      (0.1, 0.99, 1.01),
      (0.4, 0.93, 0.97),
    )

    for (step <- stepsAndMeasuredToPredictedRatios) {
      val halfOfRotationPerStep = localB.norm * step.dt / 2.0
      val predicted = Math.round(spinTime / step.dt) * Math.pow(halfOfRotationPerStep, 5) / 320.0
      val measured = motorErrorsOfFreeBody(sphere, localB, step.dt, exact).rk4
      val ratio = measured / predicted
      assert(ratio > step.minRatio && ratio < step.maxRatio, s"dt = ${step.dt}: measured $measured, predicted $predicted")
    }
  }

  test("the advantage of RKMK4 over RK4 fades as the twist stops being constant in the body frame") {
    val asymmetric = Pga3dInertiaLocal(1.0, 3.0, 2.0, 1.0)
    val bodiesAndRk4ToRkmk4ErrorRatios = Seq[(name: String, inertia: Pga3dInertiaLocal, localB: Pga3dBivector, minRatio: Double, maxRatio: Double)](
      ("moments of inertia (1, 1.01, 0.99)", Pga3dInertiaLocal(1.0, 1.0, 1.01, 0.99), tumblingAtTwoRadiansPerSecond, 60.0, 120.0),
      ("moments of inertia (1, 1.1, 0.9)", Pga3dInertiaLocal(1.0, 1.0, 1.1, 0.9), tumblingAtTwoRadiansPerSecond, 9.0, 20.0),
      ("moments of inertia (3, 2, 1), 0.05 rad off the principal axis", asymmetric, Pga3dBivector(xy = 2.0 * Math.cos(0.05), yz = 2.0 * Math.sin(0.05)), 2.5, 4.5),
      ("moments of inertia (3, 2, 1), tumbling", asymmetric, tumblingAtTwoRadiansPerSecond, 0.95, 1.05),
    )

    for (body <- bodiesAndRk4ToRkmk4ErrorRatios; dt <- Seq(0.02, 0.1, 0.4)) {
      val reference = finalMotorOfFreeBody(Pga3dPhysicsSolverRKMK4(_), body.inertia, body.localB, dt / 200.0)
      val errors = motorErrorsOfFreeBody(body.inertia, body.localB, dt, reference)
      val ratio = errors.rk4 / errors.rkmk4
      assert(ratio > body.minRatio && ratio < body.maxRatio, s"${body.name}, dt = $dt: rk4 error ${errors.rk4}, rkmk4 error ${errors.rkmk4}")
    }
  }
