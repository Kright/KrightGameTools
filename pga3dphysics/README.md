## Module with physics engine

Reference implementation, I prefer simple and correct code. But I profiled the code and eliminated some bottlenecks, it
is quite efficient now.

`Pga3dPhysicsBody` stores its pose as a [`Pga3dTransform`](../pga3d/shared/src/main/scala/me/kright/gametools/pga3d/Pga3dTransform.scala) -
the cached matrix form of a motor - so applying the pose to points, vectors and bivectors many times per step
is a plain matrix multiplication. There is an auxiliary constructor from a plain `Pga3dMotor`, and the
`body.motor` accessors are derived from the transform (assigning a motor rebuilds it).
`Pga3dBivectorMutable` is made for performance reasons.

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

## Hysteretic friction in 3d

`Pga3dHystereticFriction` is the 3d counterpart of the
[`HystereticFriction`](../physics1d/README.md) contract of physics1d: a point held in a volume
(e.g. a rubber bushing around the attachment point of a rod) with a friction force that depends on
the motion history. The pure `forceAt(x, y, z)` serves the solver stages (read it at the stage
position of the attachment point), `advance(x, y, z)` commits once after every completed step,
plus `maxForce`, `tangentStiffness` (isotropic, enters the omega*dt budget) and `deepCopy()`; the
`Pga3dPoint` overloads are inlined on top of the scalar ones. Both models are rotation-invariant,
rate-independent (substeps of one straight move compose exactly) and reduce exactly to their 1d
counterparts on a collinear path, reversals included:

* **Pga3dDahlFriction(maxForce, saturationTravel)**: the vector Dahl model, the force relaxes
  exponentially towards `-maxForce * u` along a straight move; three doubles of state. Passive
  (storage `|F|^2 / 2 sigma`, non-negative dissipation). Its soft slider shows on a curved path: on
  a steady circle of radius A the force settles at `maxForce / sqrt(1 + (saturationTravel / A)^2)`,
  below saturation, negligible while the curvature radius is far above saturationTravel.
* **Pga3dIwanFriction(elements)**: N parallel Jenkins elements (`IwanElement(stiffness, breakForce)`
  of physics1d). Each element keeps a force vector inside the ball `|f_i| <= breakForce_i` in one
  flat `Array[Double]`; a straight move adds `stiffness_i * dx` until the sphere and then turns the
  force towards the move direction along the great circle (the closed-form step of the continuous
  element; a plain "add and project back on the sphere" would not compose). A slipping element
  stays on its sphere, lagging a steady circle by `sin(theta) = breakDeformation_i / A`.

```scala
val friction = Pga3dIwanFriction(IwanElement(4e6, 150.0), IwanElement(1e6, 250.0))
friction.advance(body.globalCenterOfMass) // latch the starting point

Pga3dPhysicsSolverRK4.step(bodies, dt, addForquesToBodies = { _ =>
  val p = body.globalCenterOfMass
  body.addGlobalForque(Pga3dForque.force(p, friction.forceAt(p)))
})
friction.advance(body.globalCenterOfMass)
```

The 3d-specific caveat: between two commits the path is replaced by its chord. With the step travel
below the saturation travel (break deformations) the error is second order in the step (a midpoint
rule); far beyond it the force snaps to the chord direction and lags the true tangent by
`travel * curvature / 2`, first order. RK4 keeps its 4th order on a straight monotone path, drops to
about 2nd on a reversal inside a step (as in 1d) and to 1st-2nd on a curved slipping path;
`Pga3dIwanFrictionTest` and `Pga3dDahlFrictionTest` measure it. As in 1d, the frozen-force reading
(forceAt at the step-start point) must be paired with a viscous term.

## Inertia representations:

* **Pga3dInertia**: common interface
* **Pga3dInertiaSimple**: inertia for a case when the body is a fully symmetrical sphere or cube. It has no precession and
  very efficient computation of accelerations and moments of inertia.
* **Pga3dInertiaLocal**: inertia for a common case with three different main axes of inertia
* **Pga3dInertiaMovedSimple**: case when a body center of mass is not in the center of current coordinates system
* **Pga3dInertiaMovedLocal**: the same for Pga3dInertiaLocal. This representation can describe the inertia of any solid
  body.
* **Pga3dInertiaSummable**: summable representation of inertia (the first and second moments of the mass
  distribution). Helpful when you want to find the inertia of combined connected bodies, and also the fastest
  general representation for repeated use: its apply and its closed-form block invert
  (`Pga3dInertiaSummableInverse`, cached lazily) are ~2x faster than the moved-local route
  (see `InertiaBenchmark`), so `toFastestRepresentation` returns it.
  Precision caveat: the second moments bake in the parallel-axis terms (~ mass * R^2 for a center of mass at
  distance R from the origin), and their near-cancellation costs the invert, the kinetic energy and the
  acceleration ~1e-16 * R^2 of relative precision (about 6 lost digits at R = 1e4), while
  `Pga3dInertiaMovedLocal` loses only ~1e-16 * R on every operation. Numbers: `Pga3dInertiaPrecisionTest`.
  Keep the center of mass reasonably close to the origin, or prefer the moved form when that precision matters.
