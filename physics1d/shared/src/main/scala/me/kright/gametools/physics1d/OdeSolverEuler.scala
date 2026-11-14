package me.kright.gametools.physics1d

import me.kright.gametools.physics1d.OdeSolverUtil.*

/** First order of precision. Very imprecise */
object OdeSolverEuler extends OdeSolver:
  override def step(system: Integrable, dt: Double, addForces: (timeInsideStep: Double) => Unit, cache: OdeSolverCache | Null): Unit = {
    val size = system.stateSize
    val arrays = OdeSolverCache.getArrays(cache, 3, size)
    val initial = arrays(0)
    val k1 = arrays(1)
    val stage = arrays(2)

    system.saveState(initial, 0)
    saveDerivative(system, 0.0, addForces, k1)

    combine(stage, size, initial, dt, k1)
    system.loadState(stage, 0)
  }

  override def toString: String =
    "OdeSolverEuler"
