## Module with physics engine

Reference implementation, I prefer simple and correct code. But I profiled the code and eliminated some bottlenecks, it
is quite efficient now.

Classes Pga3dMatrixForPoints and Pga3dBivectorMutable are made for performance reasons.

## Example of usage:

```scala
val dynamicBodies: Array[Pga3dPhysicsBody] =
  Array(
    Pga3dPhysicsBody.motionless(Pga3dInertia.sphere(mass = 2.0, r = 1), Pga3dMotor.id),
    Pga3dPhysicsBody.motionless(Pga3dInertia.cube(mass = 2.0, rx = 1, ry = 2, rz = 0.5),
      Pga3dTranslator.addVector(Pga3dVector(1, 2, 3)).toMotor),
  )

def addGravity(): Unit = {
  val g: Pga3dVector = Pga3dVector(0, -9.8, 0) // y axis down

  for (body <- dynamicBodies) {
    val forque = Pga3dForque.force(body.globalCenterOfMass, g * body.inertia.mass)
    // same as body.globalCenterOfMass v (g * body.inertia.mass)

    body.addGlobalForque(forque)
  }
}

val timeStep = 0.001

for (step <- 0 until stepsCount) {
  Pga3dPhysicsSolverRK4.step(dynamicBodies, timeStep, addForquesToBodies = { dtInsideStep =>
    addGravity()
    addSomeOtherForques()
  })
}
```

Actually, for the Runge-Kutta method of fourth order, the function addForquesToBodies is called four times.

## Solvers

Eight integrators with orders from 1 (Euler) to 4 (RK4, RKMK4, RKF45, GaussLegendre), including
a Verlet on the motor group, plus hard distance constraints (rod / rope / strut) wired in through
`Pga3dPhysicsSolverConstrained` (any solver except Verlet) and the RATTLE-style
`Pga3dPhysicsSolverVerletConstrained`. These are reference implementations - simple and readable first, and usable as a
test baseline and an order-of-precision example when you write an optimized solver for your
own data layout. See [Solvers.md](Solvers.md) for the full table with orders, costs and
measured accuracies, a selection guide and the constraint resolver details.

## Inertia representations:

* **Pga3dInertia**: common interface
* **Pga3dInertiaSimple**: inertia for a case when the body is a fully symmetrical sphere or cube. It has no precession and
  very efficient computation of accelerations and moments of inertia.
* **Pga3dInertiaLocal**: inertia for a common case with three different main axes of inertia
* **Pga3dInertiaMovedSimple**: case when a body center of mass is not in the center of current coordinates system
* **Pga3dInertiaMovedLocal**: the same for Pga3dInertiaLocal. This representation can describe the inertia of any solid
  body.
* **Pga3dInertiaPrecomputed**: class caching two 6x6 matrices (application and inverse of application) and some properties.
  Suits for speed up computation when one intertia is used in computations a lot of times.
* **Pga3dInertiaSummable**: summable representation of inertia. Helpful when you want to find inertia of a combined
  connected bodies.
