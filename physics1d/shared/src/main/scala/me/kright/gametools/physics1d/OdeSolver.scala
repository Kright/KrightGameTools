package me.kright.gametools.physics1d

trait OdeSolver:
  def step(system: Integrable,
           dt: Double,
           addForces: (timeInsideStep: Double) => Unit,
           cache: OdeSolverCache | Null = null,
           resolver: OdeConstraintResolver | Null = null): Unit
