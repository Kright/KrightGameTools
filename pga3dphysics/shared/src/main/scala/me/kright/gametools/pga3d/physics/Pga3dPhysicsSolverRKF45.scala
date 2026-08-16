package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.pga3d.Pga3dTransform
import me.kright.gametools.pga3d.physics.Pga3dPhysicsSolverUtil.getDerivative

/**
 * Runge-Kutta-Fehlberg 4(5) embedded pair with a fixed step.
 *
 * The committed state is the 4th-order solution (the same accuracy class as
 * [[Pga3dPhysicsSolverRK4]]); the extra stages build the embedded 5th-order solution, and the
 * norm of their difference is a nearly free estimate of the local truncation error of every
 * step. The step size is never adjusted - the estimate is exposed via [[lastMotorErrors]] /
 * [[lastLocalBErrors]] / [[lastMaxError]] so a dev build can log it during a simulation and
 * map where the error actually lives (impacts, stiff joints, fast-spinning wheels) without
 * rerunning at a smaller dt; a release build just uses another solver.
 *
 * The estimate is refreshed by each [[step]] and is body-indexed in the `dynamicBodies` order.
 * It measures the local error per step, so it scales as dt^5: halving the step drops it ~32x.
 */
class Pga3dPhysicsSolverRKF45 extends Pga3dPhysicsSolver:
  /** per-body norm of the (5th - 4th order) motor difference of the last step */
  var lastMotorErrors: Array[Double] = Array.emptyDoubleArray
  /** per-body norm of the (5th - 4th order) localB difference of the last step */
  var lastLocalBErrors: Array[Double] = Array.emptyDoubleArray

  def lastMaxError: Double =
    var result = 0.0
    for (pos <- FastRange(lastMotorErrors.length)) {
      result = Math.max(result, lastMotorErrors(pos) + lastLocalBErrors(pos))
    }
    result

  override def step[T <: Pga3dPhysicsBody](dynamicBodies: Array[T],
                                           dt: Double,
                                           addForquesToBodies: Double => Unit): Unit = {
    val n = dynamicBodies.length
    val initial = dynamicBodies.map(Pga3dBodyState(_))

    val k1 = getDerivative(dynamicBodies, 0.0, addForquesToBodies)

    setStage(dynamicBodies, initial, dt, (k1, 1.0 / 4.0))
    val k2 = getDerivative(dynamicBodies, dt * (1.0 / 4.0), addForquesToBodies)

    setStage(dynamicBodies, initial, dt, (k1, 3.0 / 32.0), (k2, 9.0 / 32.0))
    val k3 = getDerivative(dynamicBodies, dt * (3.0 / 8.0), addForquesToBodies)

    setStage(dynamicBodies, initial, dt, (k1, 1932.0 / 2197.0), (k2, -7200.0 / 2197.0), (k3, 7296.0 / 2197.0))
    val k4 = getDerivative(dynamicBodies, dt * (12.0 / 13.0), addForquesToBodies)

    setStage(dynamicBodies, initial, dt, (k1, 439.0 / 216.0), (k2, -8.0), (k3, 3680.0 / 513.0), (k4, -845.0 / 4104.0))
    val k5 = getDerivative(dynamicBodies, dt, addForquesToBodies)

    setStage(dynamicBodies, initial, dt, (k1, -8.0 / 27.0), (k2, 2.0), (k3, -3544.0 / 2565.0), (k4, 1859.0 / 4104.0), (k5, -11.0 / 40.0))
    val k6 = getDerivative(dynamicBodies, dt * (1.0 / 2.0), addForquesToBodies)

    // the committed 4th-order solution
    setStage(dynamicBodies, initial, dt, (k1, 25.0 / 216.0), (k3, 1408.0 / 2565.0), (k4, 2197.0 / 4104.0), (k5, -1.0 / 5.0))

    // (5th - 4th order) difference: b* - b = 1/360, 0, -128/4275, -2197/75240, 1/50, 2/55
    lastMotorErrors = new Array[Double](n)
    lastLocalBErrors = new Array[Double](n)
    for (pos <- FastRange(n)) {
      val dMotor =
        k1(pos).motor * (1.0 / 360.0)
          + k3(pos).motor * (-128.0 / 4275.0)
          + k4(pos).motor * (-2197.0 / 75240.0)
          + k5(pos).motor * (1.0 / 50.0)
          + k6(pos).motor * (2.0 / 55.0)

      val dLocalB =
        k1(pos).localB * (1.0 / 360.0)
          + k3(pos).localB * (-128.0 / 4275.0)
          + k4(pos).localB * (-2197.0 / 75240.0)
          + k5(pos).localB * (1.0 / 50.0)
          + k6(pos).localB * (2.0 / 55.0)

      lastMotorErrors(pos) = dMotor.norm * dt
      lastLocalBErrors(pos) = dLocalB.norm * dt
    }
  }

  private def setStage(dynamicBodies: Array[? <: Pga3dPhysicsBody],
                       initial: Array[Pga3dBodyState],
                       dt: Double,
                       terms: (Array[Pga3dBodyState], Double)*): Unit =
    for (pos <- FastRange(dynamicBodies.length)) {
      val i = initial(pos)
      var motor = i.motor
      var localB = i.localB
      for ((k, c) <- terms) {
        motor += k(pos).motor * (dt * c)
        localB += k(pos).localB * (dt * c)
      }

      val body = dynamicBodies(pos)
      body.transform = Pga3dTransform(motor)
      body.localB = localB
    }

  override def toString: String =
    "Pga3dPhysicsSolverRKF45"
