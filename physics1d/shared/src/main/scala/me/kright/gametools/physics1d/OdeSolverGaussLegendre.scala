package me.kright.gametools.physics1d

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.physics1d.OdeSolverUtil.*

/**
 * [[https://en.wikipedia.org/wiki/Gauss%E2%80%93Legendre_method]]
 *
 * @param iterations it looks like optimal iterations count is 3 or more. 
 *                   With two iterations precision is similar with [[OdeSolverRK4]], but RK4 is faster to compute
 */
class OdeSolverGaussLegendre(val iterations: Int = 3) extends OdeSolver:
  override def step(system: Integrable,
                    dt: Double,
                    addForces: (timeInsideStep: Double) => Unit,
                    cache: OdeSolverCache | Null,
                    resolver: OdeConstraintResolver | Null): Unit = {
    val c1 = 0.5 - Math.sqrt(3) / 6
    val c2 = 0.5 + Math.sqrt(3) / 6

    val a11 = 0.25
    val a12 = 0.25 - Math.sqrt(3) / 6
    val a21 = 0.25 + Math.sqrt(3) / 6
    val a22 = 0.25

    val b1 = 0.5
    val b2 = 0.5

    val size = system.stateSize
    val arrays = OdeSolverCache.getArrays(cache, 6, size)
    val initial = arrays(0)
    val y1 = arrays(1)
    val y2 = arrays(2)
    val fy1 = arrays(3)
    val fy2 = arrays(4)
    val stage = arrays(5)

    system.saveState(initial, 0)

    saveDerivative(system, dt * c1, addForces, resolver, fy1)
    combine(y1, size, initial, dt * (a11 + a12), fy1)
    combine(y2, size, initial, dt * (a21 + a22), fy1)

    for (i <- FastRange(iterations)) {
      system.loadState(y1, 0)
      saveDerivative(system, dt * c1, addForces, resolver, fy1)
      system.loadState(y2, 0)
      saveDerivative(system, dt * c2, addForces, resolver, fy2)

      combine(y1, size, initial, dt, fy1, a11, fy2, a12)
      combine(y2, size, initial, dt, fy1, a21, fy2, a22)
    }

    system.loadState(y1, 0)
    saveDerivative(system, dt * c1, addForces, resolver, fy1)
    system.loadState(y2, 0)
    saveDerivative(system, dt * c2, addForces, resolver, fy2)

    combine(stage, size, initial, dt, fy1, b1, fy2, b2)
    loadFinalState(system, stage, resolver)
  }

  override def toString: String =
    s"OdeSolverGaussLegendre(iterations=$iterations)"
