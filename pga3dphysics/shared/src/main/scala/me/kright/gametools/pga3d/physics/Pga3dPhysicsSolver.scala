package me.kright.gametools.pga3d.physics

trait Pga3dPhysicsSolver[Body]:
  /**
   * @param dynamicBodies      - list of bodies to update
   * @param dt                 - step size
   * @param addForquesToBodies - Callback which receives currentDt and adds forces to bodies. 
   *                           Could be called several times. For example [[Pga3dPhysicsSolverRK4]] calls with 0, 0.5 * dt, 0.5 * dt, dt for different points of time.
   */
  def step(dynamicBodies: Array[Body],
           dt: Double,
           addForquesToBodies: Double => Unit): Unit
