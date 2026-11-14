package me.kright.gametools.physics1d

import me.kright.gametools.physics1d.OdeSolverUtil.*

object OdeSolverRK4 extends OdeSolver:
  override def step(system: Integrable,
                    dt: Double,
                    addForces: (timeInsideStep: Double) => Unit,
                    cache: OdeSolverCache | Null,
                    resolver: OdeConstraintResolver | Null): Unit = {
    val size = system.stateSize
    val arrays = OdeSolverCache.getArrays(cache, 6, size)
    val initial = arrays(0)
    val k1 = arrays(1)
    val k2 = arrays(2)
    val k3 = arrays(3)
    val k4 = arrays(4)
    val stage = arrays(5)

    system.saveState(initial, 0)
    saveDerivative(system, 0.0, addForces, resolver, k1)

    combine(stage, size, initial, 0.5 * dt, k1)
    system.loadState(stage, 0)
    saveDerivative(system, 0.5 * dt, addForces, resolver, k2)

    combine(stage, size, initial, 0.5 * dt, k2)
    system.loadState(stage, 0)
    saveDerivative(system, 0.5 * dt, addForces, resolver, k3)

    combine(stage, size, initial, dt, k3)
    system.loadState(stage, 0)
    saveDerivative(system, dt, addForces, resolver, k4)

    combine(stage, size, initial, dt, k1, 1.0 / 6.0, k2, 2.0 / 6.0, k3, 2.0 / 6.0, k4, 1.0 / 6.0)
    loadFinalState(system, stage, resolver)
  }

  override def toString: String =
    "OdeSolverRK4"
