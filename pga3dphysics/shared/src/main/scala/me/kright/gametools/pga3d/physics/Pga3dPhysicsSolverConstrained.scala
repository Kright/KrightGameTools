package me.kright.gametools.pga3d.physics

/**
 * plugs a [[Pga3dConstraintResolver]] into any solver without touching the solver code: the
 * constraint forques are appended to the user force callback (so they are re-evaluated at every
 * stage state, and high-order solvers keep their order), and the residual drift is projected
 * out once after the full step.
 *
 * The Verlet family lives outside this interface (its state is two pose arrays owned by the
 * caller, not (motor, localB) pairs): for constrained Verlet use the dedicated
 * [[Pga3dPhysicsSolverVerletConstrained]].
 */
class Pga3dPhysicsSolverConstrained(val inner: Pga3dPhysicsSolver,
                                    val resolver: Pga3dConstraintResolver) extends Pga3dPhysicsSolver:

  override def step(dynamicBodies: Array[Pga3dPhysicsBody],
                    dt: Double,
                    addForquesToBodies: Double => Unit): Unit =
    inner.step(dynamicBodies, dt, currentDt => {
      addForquesToBodies(currentDt)
      resolver.addConstraintForques(dynamicBodies)
    })
    resolver.project(dynamicBodies)

  override def toString: String =
    s"Pga3dPhysicsSolverConstrained($inner, $resolver)"
