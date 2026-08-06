package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq

class Pga3dPhysicsSolverRKMK4Test extends AnyFunSuiteLike:

  private def precessionMaxError(solver: Pga3dPhysicsSolver,
                                 motor: Pga3dMotor,
                                 stepsCount: Int = 1000,
                                 dt: Double = 0.01): ErrorOfEnergyAndMomentum =
    val system = Pga3dPhysicsSystemForTest(Array(Pga3dPhysicsSystemForTest.simpleBody(motor)), solver)
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
      val maxError = precessionMaxError(Pga3dPhysicsSolverRKMK4, Pga3dTranslator.addVector(center.toVectorUnsafe).toMotor)
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
      val maxError = precessionMaxError(Pga3dPhysicsSolverRKMK4, motor)
      // println(s"motor = $motor, maxError = $maxError")
      assert(maxError < ErrorOfEnergyAndMomentum(errorE = 6e-11, errorL = 1e-9))
    }
  }

  private def precessionFinalState(solver: Pga3dPhysicsSolver,
                                   dt: Double,
                                   totalTime: Double): Pga3dBodyState =
    val system = Pga3dPhysicsSystemForTest(Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)), solver)
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
    val reference = precessionFinalState(Pga3dPhysicsSolverRKMK4, dt = 1e-4, totalTime)

    def run(solver: Pga3dPhysicsSolver, dt: Double): (Pga3dBodyState, ErrorOfEnergyAndMomentum) =
      val system = Pga3dPhysicsSystemForTest(Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)), solver)
      val stepsCount = Math.round(totalTime / dt).toInt
      val errors = for (_ <- 0 until stepsCount) yield {
        system.doStep(dt, _ => ())
        system.getError()
      }
      (Pga3dBodyState(system.state.head), errors.reduce(_ max _))

    for (dt <- Seq(0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.75, 1.0)) {
      val (rk4State, rk4Err) = run(Pga3dPhysicsSolverRK4, dt)
      val (rkmk4State, rkmk4Err) = run(Pga3dPhysicsSolverRKMK4, dt)

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
    val reference = precessionFinalState(Pga3dPhysicsSolverRKMK4, dt = 1e-4, totalTime)

    def error(state: Pga3dBodyState): Double =
      (state.motor - reference.motor).norm + (state.localB - reference.localB).norm

    val dts = ArraySeq(0.04, 0.02, 0.01, 0.005)
    val errors = dts.map { dt =>
      val rk4 = error(precessionFinalState(Pga3dPhysicsSolverRK4, dt, totalTime))
      val rkmk4 = error(precessionFinalState(Pga3dPhysicsSolverRKMK4, dt, totalTime))
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
