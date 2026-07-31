# Physics solvers

These are reference implementations: written to be simple and readable first, fast second.
For real data and storage layouts you may well write a more optimal version - the ones here
are then meant to serve as a trusted baseline to test yours against, and as a worked example
of how to reach the stated order of precision, which is easy to lose to a subtle mistake
(stale velocities, a projection in the wrong place, a clamp that fires on a rod). The
convergence-order tests in `pga3dphysics/jvm/src/test` show how to verify the order of an
implementation against a fine-step reference trajectory.

The Runge-Kutta-family solvers implement `Pga3dPhysicsSolver.step(dynamicBodies, dt,
addForquesToBodies)` and integrate the pair (motor, localB) of every body with a fixed step;
`addForquesToBodies` is called once per derivative evaluation with the time offset inside the
step, so forces are re-evaluated at the intermediate states. The Verlet family deliberately
lives outside that interface: for Verlet the state is two consecutive pose arrays owned by the
caller and the velocity exists only implicitly, so its solvers are stateless computation
strategies over arrays of inertias and motors (see the Verlet bullet below).

| solver                                  | order            | force evaluations per step | ns / body / step | notes                                    |
|-----------------------------------------|------------------|----------------------------|------------------|------------------------------------------|
| `Pga3dPhysicsSolverEuler`               | 1                | 1                          | 117              | very imprecise, for reference only       |
| `Pga3dPhysicsSolverMidPoint`            | 2                | 2                          | 230              |                                          |
| `Pga3dPhysicsSolverHeun`                | 2                | 2                          | 239              |                                          |
| `Pga3dPhysicsSolverVerlet`              | 2                | 1                          | 406              | exact momentum, bounded energy error     |
| `Pga3dPhysicsSolverRK4`                 | 4                | 4                          | 472              | the default workhorse                    |
| `Pga3dPhysicsSolverRKMK4`               | 4                | 4                          | 657              | Lie-group (Munthe-Kaas) variant of RK4   |
| `Pga3dPhysicsSolverRKF45`               | 4 (+ embedded 5) | 6                          | 1056             | free per-step local-error estimate       |
| `Pga3dPhysicsSolverGaussLegendre(iter)` | 4 (implicit)     | 3 + 2·iter                 | 1079 (iter = 3)  | far better energy behavior on stiff forces |

The timing column is the pure solver overhead with an empty force callback
(`benchmark/PhysicsSolverBenchmark`, free rotating bodies); with real forces the cost of the
force callback multiplies by the "force evaluations per step" column.

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
midframe drift. Thanks to the reconstruction the user force callback runs once per step. Only
the poses come out; for observers (energy, momentum) `reconstructNode(...)` completes the
reconstruction honestly. Measured: 2nd-order convergence on the rod chain (2.00, 2.00, 2.00),
rods to ~1e-15 (chain: 3e-11, the sweep exit threshold), the dumbbell momentum to ~8e-12 and
energy to ~1e-12 over 20k steps, bounded energy oscillation (~0.1% on the off-center pendulum
over 200 s, 1e-5 relative on the chain). Pose edits between steps still become velocity edits,
like in the plain Verlet.
