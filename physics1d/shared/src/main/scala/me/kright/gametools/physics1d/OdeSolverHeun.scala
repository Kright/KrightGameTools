package me.kright.gametools.physics1d

import me.kright.gametools.physics1d.OdeSolverUtil.*

/** Second order of precision */
object OdeSolverHeun extends OdeSolver:
  override def step(system: Integrable, dt: Double, addForces: (timeInsideStep: Double) => Unit, cache: OdeSolverCache | Null): Unit = {
    val size = system.stateSize
    val arrays = OdeSolverCache.getArrays(cache, 4, size)
    val initial = arrays(0)
    val k1 = arrays(1)
    val k2 = arrays(2)
    val stage = arrays(3)

    system.saveState(initial, 0)
    saveDerivative(system, 0.0, addForces, k1)

    combine(stage, size, initial, dt, k1)
    system.loadState(stage, 0)
    saveDerivative(system, dt, addForces, k2)

    combine(stage, size, initial, dt, k1, 0.5, k2, 0.5)
    system.loadState(stage, 0)
  }

  override def toString: String =
    "OdeSolverHeun"
