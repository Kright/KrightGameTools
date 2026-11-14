package me.kright.gametools.physics1d

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.physics1d.OdeSolverUtil.saveDerivative

/**
 * Runge-Kutta-Fehlberg 4(5) with a fixed step: the state is advanced by the 4th-order solution,
 * the embedded 5th-order one only gives the error estimate. The step size is never adjusted.
 */
class OdeSolverRKF45 extends OdeSolver:
  /** |5th - 4th order solution| of every double of the state after the last step: an estimate of its local error, scales as dt^5 */
  var lastErrors: Array[Double] = Array.emptyDoubleArray

  def lastMaxError: Double =
    var result = 0.0
    for (j <- FastRange(lastErrors.length)) {
      result = Math.max(result, lastErrors(j))
    }
    result

  override def step(system: Integrable, dt: Double, addForces: (timeInsideStep: Double) => Unit, cache: OdeSolverCache | Null): Unit = {
    val size = system.stateSize
    val arrays = OdeSolverCache.getArrays(cache, 9, size)
    val initial = arrays(0)
    val k1 = arrays(1)
    val k2 = arrays(2)
    val k3 = arrays(3)
    val k4 = arrays(4)
    val k5 = arrays(5)
    val k6 = arrays(6)
    val stage = arrays(7)
    val difference = arrays(8)

    system.saveState(initial, 0)
    saveDerivative(system, 0.0, addForces, k1)

    loadStage(system, stage, size, initial, dt, (k1, 1.0 / 4.0))
    saveDerivative(system, dt * (1.0 / 4.0), addForces, k2)

    loadStage(system, stage, size, initial, dt, (k1, 3.0 / 32.0), (k2, 9.0 / 32.0))
    saveDerivative(system, dt * (3.0 / 8.0), addForces, k3)

    loadStage(system, stage, size, initial, dt, (k1, 1932.0 / 2197.0), (k2, -7200.0 / 2197.0), (k3, 7296.0 / 2197.0))
    saveDerivative(system, dt * (12.0 / 13.0), addForces, k4)

    loadStage(system, stage, size, initial, dt, (k1, 439.0 / 216.0), (k2, -8.0), (k3, 3680.0 / 513.0), (k4, -845.0 / 4104.0))
    saveDerivative(system, dt, addForces, k5)

    loadStage(system, stage, size, initial, dt, (k1, -8.0 / 27.0), (k2, 2.0), (k3, -3544.0 / 2565.0), (k4, 1859.0 / 4104.0), (k5, -11.0 / 40.0))
    saveDerivative(system, dt * (1.0 / 2.0), addForces, k6)

    loadStage(system, stage, size, initial, dt, (k1, 25.0 / 216.0), (k3, 1408.0 / 2565.0), (k4, 2197.0 / 4104.0), (k5, -1.0 / 5.0))

    // (5th - 4th order) difference: b* - b = 1/360, 0, -128/4275, -2197/75240, 1/50, 2/55
    for (j <- FastRange(size)) {
      difference(j) =
        k1(j) * (1.0 / 360.0)
          + k3(j) * (-128.0 / 4275.0)
          + k4(j) * (-2197.0 / 75240.0)
          + k5(j) * (1.0 / 50.0)
          + k6(j) * (2.0 / 55.0)
    }

    lastErrors = new Array[Double](size)
    for (j <- FastRange(size)) {
      lastErrors(j) = Math.abs(difference(j)) * dt
    }
  }

  private def loadStage(system: Integrable,
                        stage: Array[Double],
                        size: Int,
                        initial: Array[Double],
                        dt: Double,
                        terms: (Array[Double], Double)*): Unit =
    System.arraycopy(initial, 0, stage, 0, size)
    for ((k, c) <- terms) {
      val scale = dt * c
      for (j <- FastRange(size)) {
        stage(j) += k(j) * scale
      }
    }
    system.loadState(stage, 0)

  override def toString: String =
    "OdeSolverRKF45"
