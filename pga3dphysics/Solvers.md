# Physics solvers

These are reference implementations: written to be simple and readable first, fast second.
For real data and storage layouts you may well write a more optimal version - the ones here
are then meant to serve as a trusted baseline to test yours against, and as a worked example
of how to reach the stated order of precision, which is easy to lose to a subtle mistake
(stale velocities, a projection in the wrong place, a clamp that fires on a rod). The
convergence-order tests in `pga3dphysics/jvm/src/test` show how to verify the order of an
implementation against a fine-step reference trajectory.

The Runge-Kutta solvers themselves are universal and live in
[physics1d](../physics1d/README.md#ode-solvers): `OdeSolver.step(system, dt, addForces, cache)`
integrates one `Integrable` with a fixed step - the bodies, as an `IntegrableUniformArray` with
the pair (motor, localB) of every body, together with any other objects of the same world (see
[below](#one-system-flat-state)). `addForces` is called once per derivative evaluation with the
time offset inside the step, so forces are re-evaluated at the intermediate states.

The `Pga3dPhysicsSolver`s of this module, `step(dynamicBodies, dt, addForquesToBodies)`, work
on arrays of bodies. The Runge-Kutta ones are deprecated: they only wrap the array into the
`IntegrableUniformArray` and call the universal solver of the same name. `Pga3dPhysicsSolverRKMK4` and
`Pga3dPhysicsSolverConstrained` exist only in this form. The Verlet family deliberately
lives outside both interfaces: for Verlet the state is two consecutive pose arrays owned by the
caller and the velocity exists only implicitly, so its solvers are stateless computation
strategies over arrays of inertias and motors (see the Verlet bullet below).

| solver                                                               | order            | force evaluations per step | ns / body / step | notes                                    |
|----------------------------------------------------------------------|------------------|----------------------------|------------------|------------------------------------------|
| `OdeSolverEuler` / `Pga3dPhysicsSolverEuler`                         | 1                | 1                          | 94               | very imprecise, for reference only       |
| `OdeSolverMidPoint` / `Pga3dPhysicsSolverMidPoint`                   | 2                | 2                          | 177              |                                          |
| `OdeSolverHeun` / `Pga3dPhysicsSolverHeun`                           | 2                | 2                          | 174              |                                          |
| `OdeSolverRK4` / `Pga3dPhysicsSolverRK4`                             | 4                | 4                          | 339              | the default workhorse                    |
| `OdeSolverRKF45` / `Pga3dPhysicsSolverRKF45`                         | 4 (+ embedded 5) | 6                          | 571              | free per-step local-error estimate       |
| `Pga3dPhysicsSolverVerlet`                                           | 2                | 1                          | 580              | exact momentum, bounded energy error     |
| `Pga3dPhysicsSolverRKMK4`                                            | 4                | 4                          | 666              | Lie-group (Munthe-Kaas) variant of RK4   |
| `OdeSolverGaussLegendre(iter)` / `Pga3dPhysicsSolverGaussLegendre(iter)` | 4 (implicit) | 3 + 2·iter                 | 768 (iter = 3)   | far better energy behavior on stiff forces |

The timing column is the pure solver overhead with an empty force callback
(`benchmark/PhysicsSolverBenchmark`, 8 free rotating bodies, through the `Pga3dPhysicsSolver`
step for an array, which allocates the work buffers on every call; `OdeSolverRK4` with a reused
`OdeSolverCache` takes 310); with real forces the cost of the force callback multiplies by the
"force evaluations per step" column.

## A short guide

* **Use `Pga3dPhysicsSolverRK4` by default.** It adds scaled derivatives in flat coordinates and
  renormalizes the motor after the full step; projecting after the step (unlike projecting the
  intermediate stages) does not hurt the 4th order, so it is effectively a geometric method too.
* **`Pga3dPhysicsSolverRKF45` is the same order plus a built-in diagnostic.** The committed state is
  the 4th-order solution of the Fehlberg 4(5) embedded pair (its tableau even shows ~8x smaller
  free-precession error constants than the classic RK4 one); the difference from the embedded
  5th-order solution is a per-step, per-body local-error estimate exposed as `lastMotorErrors` /
  `lastLocalBErrors` / `lastMaxError`. The estimate is essentially exact (measured 0.98–1.01 of the
  true single-step error) and scales as dt^5. Log it in a dev build to map where the error lives —
  impacts, stiff constraints, fast-spinning bodies: a stiff spring (k = 1000 vs k = 1) at the same
  dt raises the estimate by ~9 orders of magnitude. In short: a stiffness detector for the price of
  two extra force evaluations. It is a class, not an object — each instance keeps the estimate of
  its own last step.
* **`Pga3dPhysicsSolverRKMK4` has no practical advantage on our measurements.** It integrates on the
  Lie algebra of bivectors (`u' = dexpInv(-u, -localB / 2)`) and updates with a single
  `M0 * exp(u)`, so every stage and the result are exact motors by construction — geometrically the
  cleanest formulation. But the shared Runge-Kutta truncation error dominates at every step size:
  against RK4 it wins fractions of a percent of trajectory error and ~7% of momentum error at
  practical steps, converging to parity as the step grows. Kept as a reference implementation.
* **`Pga3dPhysicsSolverGaussLegendre` pays off on stiff forces.** An implicit two-stage 4th-order
  scheme solved by fixed-point iteration (use `iterations >= 3`). On a stiff spring (k = 1e6,
  dt = 1e-4) its relative energy error is 5.8e-8 at 3 iterations and 8.7e-14 at 8, versus 1.4e-4
  for RK4 — at the price of many more force evaluations per step (the position error stays
  comparable to RK4, though). Measurement tables live in the tests
  (`Pga3dInertiaLocalTest`, "mass on spring precistion").
* **`Pga3dPhysicsSolverVerlet` is a true Verlet on the motor group.** At first sight Verlet does
  not fit this engine at all: it has no explicit velocities — the state is two consecutive
  positions — while here `localB` is a first-class part of the state, needed by the gyroscopic
  term, kinetic energy, momentum and friction-like forces. The trick is to reconstruct the twist
  on the fly as the Lie derivative of the pose, `B = -2 * (prevMotor.reverse * motor).log / dt` —
  the group analog of "velocity is the difference of two positions" — and to apply the kick to
  the world-frame momentum (the discrete Moser-Veselov idea), solving the next displacement from
  the momentum by a fixed-point iterated to convergence, so the gyroscopic term never appears
  explicitly. Accordingly the API is a stateless strategy, not a `Pga3dPhysicsSolver`:
  `step(inertias, prevMotors, motors, globalForques, prevDt, dt, nextMotors, nextLocalBs)` over
  caller-owned arrays (`nextMotors` may alias `prevMotors` — the classic two-array rotation),
  with `makePrevMotors(...)` building the virtual previous poses for the first step. Measured
  on free precession at dt = 0.01: 2nd order, momentum error ~1e-15 (the momentum is
  transported, not integrated), and the energy error is a bounded oscillation instead of a
  drift — 4.1666e-6 after 1'000 steps and 4.1666e-6 after 100'000, where the same-order
  midpoint drifts to 1.7e-4. One force evaluation per step. Bonus: because the poses are the
  only state, editing a motor between steps (position-based depenetration) implicitly edits the
  velocity, exactly like in position-based dynamics; a teleport must rewrite both pose arrays.
  Costs: velocity-dependent forces see a half-step-stale `localB`, and the rotation per step
  must stay below pi (the motor log wraps).

## One system, flat state

A universal solver sees one [`Integrable`](../physics1d/README.md#integrable): an object that
saves its state and its derivative into a run of doubles and loads a state back. So a solver is
plain arithmetic over double arrays - save the initial state, and for every stage combine, load,
evaluate the derivative:

```scala
combine(stage, size, initial, 0.5 * dt, k1)   // stage = initial + k1 * (0.5 * dt)
system.loadState(stage, 0)                    // the motors are renormalized here
saveDerivative(system, 0.5 * dt, addForces, k2)
```

* **The bodies** are joined by
  `IntegrableUniformArray(dynamicBodies, Pga3dBodyState.componentsCount)`: the body number i at
  the offset `14 * i` (the motor, then localB - the flat form of `Pga3dBodyState`). The class is
  generated at the call site by the inlined factory, so its loops are written for
  `Pga3dPhysicsBody` and the constant size 14: the solver makes one interface call per stage
  operation, and the calls of the bodies are direct.
* **Other objects** implement `Integrable` themselves and are joined with the bodies by an
  `Integrable(bodiesArray, flywheel, ...)`. All of them go through the same stages, and
  the force callback sees them at the same trial state, so the coupling keeps the order of the
  solver: a spinning body and a 1d point mass joined by a spring converge to the exact solution
  with the measured orders 1.04 (euler), 2.00 (midPoint, heun) and 4.00-4.01 (rk4, rkf45,
  gaussLegendre(6)), see `Pga3dBodiesOdeSolverTest`.
* **`OdeSolverCache`** holds the stage arrays between the steps: a world that passes its own
  instance to every step allocates them once. The cache is optional (`OdeSolverCache | Null`):
  without it a solver creates new arrays on every step, as the `Pga3dPhysicsSolver` step for
  an array does.
* **`OdeSolverRKF45`** reports `lastErrors`, one value per double of the system;
  `Pga3dPhysicsSolverRKF45` folds them into the body-indexed `lastMotorErrors` /
  `lastLocalBErrors`.
* **`Pga3dPhysicsSolverRKMK4`** stays a solver for an array of bodies only: the group structure
  it uses is the one of the motors.
* **Constraints** with a universal solver are plugged in by hand, exactly as
  `Pga3dPhysicsSolverConstrained` does it for an array of bodies:

  ```scala
  OdeSolverRK4.step(world, dt, currentDt => {
    addForques(currentDt)
    resolver.addConstraintForques(dynamicBodies)
  }, cache)
  resolver.project(dynamicBodies)
  ```

The linear combinations keep the order of the floating point operations of the previous
object-based solvers: the trajectories of all the solvers are bit-identical to them (the
per-body estimates of RKF45 are now folded from the per-double ones and differ in the last bit).

What the generality costs, measured for one RK4 step with an empty force callback
(`benchmark/Rk4BodiesCountBenchmark`, ns per step, three forks):

| RK4 step                                                                | 2 bodies | 10 bodies | 100 bodies |
|-------------------------------------------------------------------------|----------|-----------|------------|
| object-based solver of 0.12.0                                           | 724      | 3456      | 35368      |
| `Pga3dPhysicsSolverRK4.step(dynamicBodies, dt, f)`, no cache            | 716      | 3438      | 35259      |
| a solver hardwired to an array of bodies, with a cache                  | 661      | 3069      | 31653      |
| `OdeSolverRK4` over an `IntegrableUniformArray` of bodies, with a cache | 645      | 3082      | 32079      |

The step for an array costs the same as before. A cache takes 9-11% off, and 29% of the
allocations: 1728 instead of 2445 bytes per body per step, 16 objects instead of 29 (what is
left are the objects a body keeps after every stage: its `Pga3dTransform` with the motor and
`localB`, plus the motor before the renormalization). The bodies array is within 1.5% of a solver
written directly for an array of bodies.

The three ways to join the same bodies (`benchmark/BodiesArrayDispatchBenchmark`, RK4 with a
cache, ns per step): alone, and in a world with three more arrays of other small objects
(their own work is included, about 400 ns):

| the bodies joined by                                   | 2 bodies | 10 bodies | 100 bodies | 2, in the world | 10, in the world | 100, in the world |
|--------------------------------------------------------|----------|-----------|------------|-----------------|------------------|-------------------|
| `IntegrableUniformArray(bodies, 14)`: loops made for the body class | 650 | 3116   | 32087      | 1043            | 3507             | 32336             |
| a uniform array with loops shared by all element types | 656      | 3067      | 33143      | 1027            | 3607             | 34753             |
| `Integrable(bodies)`                              | 657      | 3167      | 33215      | 1107            | 3735             | 34557             |

For a couple of bodies the choice does not matter. The shared loops (a class kept only in
the benchmark) are as fast as the generated ones only while a single element type passes
through them, and even then 3% slower at 100 bodies: the element size is a field there, not a
constant. In a world with several kinds of objects they stop being inlined: 3-7% slower, and 6%
more allocations at 100 bodies. An offset table instead of a multiplication, the only
difference between the last two rows, does not show at all. That is why the library has no
such class: `IntegrableUniformArray` is a trait, and its implementation is generated at the
call site.

## Hard constraints

`Pga3dDistanceConstraint` keeps the distance between two anchor points (body-body or
body-world) inside `[minDistance, maxDistance]`: equal bounds make a rigid rod, `rope(...)`
bounds only the maximum, `strut(...)` only the minimum. `Pga3dConstraintResolver` resolves a
list of them in two solver-agnostic parts:

* **constraint forques inside the force callback**: the exact acceleration-level Lagrange
  forces (with the centripetal bias), recomputed at every stage state and added as ordinary
  paired forques - this is what lets high-order solvers keep their order (a projection-only
  scheme degrades to the 1st-2nd order no matter the solver);
* **projection after the step**: a Gauss-Seidel pass over the positions (multiplicative
  `motor * exp(...)` corrections weighted by the inverse inertia, so the motors stay unit by
  construction) and then over the velocities, removing the residual drift.

Constraints coupled through shared bodies (chains, trees) are solved by running the
Gauss-Seidel sweeps of both parts to convergence - a leftover force residual would not shrink
with dt and would cap the reachable accuracy, so the iteration limits are only safety bounds.
On a swinging 4-body rod chain every solver converges to the common trajectory with its own
order (measured: euler 1.21, heun/midPoint 2.05, rk4/rkmk4 4.15, rkf45 4.33,
gaussLegendre(6) 3.94), the rods hold to the machine epsilon and the energy error stays at the
solver's truncation level.

`Pga3dPhysicsSolverConstrained(inner, resolver)` wires both into any solver without touching
the solver code. Measured on a spinning dumbbell and a rigid pendulum with an off-center
anchor (RK4, dt = 0.01): the distance is held to ~1e-15, the trajectory keeps the 4th order
(measured 4.06-4.10), the momentum of body-body constraints is conserved to ~1e-12 (the
corrections are equal-and-opposite by construction) and the energy error stays at the plain
RK4 truncation level.

For Verlet there is the dedicated `Pga3dPhysicsSolverVerletConstrained` - a RATTLE on the
motor group with the constraints built into the step itself (a generic projection composite
over Verlet only reaches the 1st order). Like the plain Verlet it is a stateless strategy over
caller-owned pose arrays; the constraints are an argument of `step` and may change freely
between steps - e.g. contact-like constraints regenerated every frame. The step reconstructs
the node momenta from the poses (redoing the second RATTLE half kick of the previous segment,
including its velocity-stage impulses), then performs the first half kick with the
position-stage constraint impulses whose gradients are evaluated at the old poses - the
variational ingredient of SHAKE/RATTLE - solved by Newton/Gauss-Seidel sweeps through the
midframe drift. Thanks to the reconstruction the user force callback runs once per step. The
poses go to `nextMotors`, and the honest node twists of the consumed poses - what a separate
`reconstructNode(...)` would return - come out through the `localBs` output, so observers
(energy, momentum) do not pay a second reconstruction with its second force callback.
Measured: 2nd-order convergence on the rod chain (2.00, 2.00, 2.00),
rods to ~1e-15 (chain: 3e-11, the sweep exit threshold), the dumbbell momentum to ~8e-12 and
energy to ~1e-12 over 20k steps, bounded energy oscillation (~0.1% on the off-center pendulum
over 200 s, 1e-5 relative on the chain). Pose edits between steps still become velocity edits,
like in the plain Verlet.
