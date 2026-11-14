package me.kright.gametools.pga3d.physics

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.physics1d.{IntegrableUniformArray, OdeSolverRKF45}

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
@deprecated("use OdeSolverRKF45 of physics1d over IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)", "0.13.0")
class Pga3dPhysicsSolverRKF45 extends Pga3dPhysicsSolver:
  private val solver = OdeSolverRKF45()

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
    val bodies = IntegrableUniformArray(dynamicBodies, Pga3dBodyState.componentsCount)
    solver.step(bodies, dt, addForquesToBodies)

    val n = dynamicBodies.length
    lastMotorErrors = new Array[Double](n)
    lastLocalBErrors = new Array[Double](n)
    for (pos <- FastRange(n)) {
      val errors = FlatDoubleSerializer.read[Pga3dBodyState](solver.lastErrors, bodies.offsetOf(pos))
      lastMotorErrors(pos) = errors.motor.norm
      lastLocalBErrors(pos) = errors.localB.norm
    }
  }

  override def toString: String =
    "Pga3dPhysicsSolverRKF45"
