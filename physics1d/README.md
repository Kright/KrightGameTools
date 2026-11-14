# Physics without PGA: scalar 1d force models and ODE solvers

Force models along one scalar coordinate (a rod length, a suspension travel): a nonlinear
elasticity curve and hysteretic (memory-carrying) friction. And the universal
[ODE solvers](#ode-solvers) with the `Integrable` contract of the objects they integrate. No PGA
here — the module depends only on mathutil, and pga3dphysics builds on top of it: its joints
(e.g. `Pga3dForque.spring`), and its rigid bodies as one more kind of `Integrable`.

One sign convention everywhere, matching the viscous `Pga3dFriction` (`Linear(k)(v) = -k*v`):
the force is a generalized force along the coordinate r — **negative acts to decrease r,
positive to increase r**. A stretched spring returns a negative force, friction after
lengthening tends to its negative saturation, so the elastic, viscous and hysteretic terms
of one joint are simply summed.

## SpringElasticity

A spring curve with a soft zone around zero deflection: the stiffness blends from `softK`
at zero to `stiffK` at `|deflection| = softZoneTravel` (parabolic blend, C1-continuous
force) and is exactly `stiffK` outside. `SpringElasticity.linear(k)` degenerates to a plain
linear spring. `maxStiffness` is statically visible for the integrator's omega*dt budget.

## HystereticFriction

Rate-independent friction with memory: the force depends on the motion history, not on the
instantaneous velocity. The contract:

* `forceAt(x)` — PURE evaluation: the force after a monotone move from the last committed
  coordinate to `x`. Integrator stages read this and never mutate the state.
* `advance(x)` — commits one completed step and latches `x`; the very first call only
  latches the starting coordinate. Never call it inside trial RK stages.
* `tangentStiffness` — the stiffness of the stuck state; add it to the spring stiffness in
  the omega*dt stability budget.
* `deepCopy()` — an independent state copy for deterministic physics snapshots.

Two ways to read the force from an integrator:

* **frozen force**: evaluate `forceAt` at the step-start coordinate in every stage. Robust
  first-order splitting; MUST be paired with a viscous term on the same coordinate,
  otherwise the undamped internal stiffness ratchets the joint through a constant load.
* **stage-consistent**: evaluate `forceAt` at the stage's trial coordinate. Branches
  continue each other exactly while the motion is monotone, so the integrator keeps its
  full order there (a reversal inside a step is approximated by its net displacement), and
  the viscous pairing becomes a physical choice rather than a scheme requirement.

## The models

* **DahlFriction(maxForce, saturationTravel)**: exponential relaxation to saturation - the
  exact solution of the Dahl equation for a monotone move; the smallest state (one force).
  `saturationTravel` is the e-fold distance; the stuck stiffness is `maxForce/saturationTravel`.
* **BergFriction(maxForce, halfSaturationTravel)**: the friction part of the Berg bushing
  model (1997-1998, the automotive standard for rubber bushings) - a hyperbolic approach to
  saturation with a longer tail that fits measured rubber loops better. From zero force the
  travel `halfSaturationTravel` gives exactly `maxForce/2`; every branch starts with the
  slope `maxForce/halfSaturationTravel`. Keeps the last reversal point in its state (the
  curve is not invariant under mid-branch re-anchoring).
* **IwanFriction(elements)**: N parallel Jenkins elements (`IwanElement(stiffness, breakForce)` -
  a spring in series with a Coulomb slider); the discrete Iwan / Prandtl-Ishlinskii model.
  Piecewise-linear Masing loops, exact finite saturation at the sum of break forces, and -
  the reason to prefer it when fitting measurements - spreading the break deformations keeps
  the loss factor roughly constant across an amplitude range, which measured rubber bushings
  show and single-element models cannot do. For a car suspension bushing 2-3 elements are
  enough (a stiff "micro" + a soft "macro" one).

All three are rate-independent (substeps of one direction compose exactly as one big step)
and unconditionally stable for any step size: a step far beyond the saturation travel
degrades to bang-bang +-maxForce instead of exploding.

## AbsoluteRotationTracker

Accumulates a continuous rotation angle across the `2*Pi` wraparound (assuming less than
half a turn between updates) - turns a wrapped angle into an unbounded scalar coordinate.

## ODE solvers

A simulated world is rarely made of rigid bodies only: a wheel has its own spin, an engine its
revolutions, a tire its relaxation state. Such objects must be integrated together with the
bodies, through the same Runge-Kutta stages - the forces on the bodies depend on their states,
and their derivatives depend on the bodies. So the solvers here know nothing about the kind of
the objects: they integrate one `Integrable`.

| solver                          | order            | force evaluations per step | notes                                      |
|---------------------------------|------------------|----------------------------|--------------------------------------------|
| `OdeSolverEuler`                | 1                | 1                          | very imprecise, for reference only         |
| `OdeSolverMidPoint`             | 2                | 2                          |                                            |
| `OdeSolverHeun`                 | 2                | 2                          |                                            |
| `OdeSolverRK4`                  | 4                | 4                          | the default workhorse                      |
| `OdeSolverRKF45`                | 4 (+ embedded 5) | 6                          | free per-step local-error estimate         |
| `OdeSolverGaussLegendre(iter)`  | 4 (implicit)     | 3 + 2·iter                 | far better energy behavior on stiff forces |

All of them implement `OdeSolver.step(system, dt, addForces, cache)`; `addForces` is called
once per derivative evaluation with the time offset inside the step. The orders are measured
in `OdeSolverTest` on two masses joined by a spring against the exact solution: 1.04, 2.00,
2.00, 4.00, 4.01, 4.00. `OdeSolverRKF45` exposes `lastErrors`, the estimate of the local error
of every double of the state (measured 0.97-0.99 of the actual error of a step, scaling as
dt^5), and `lastMaxError`. For the rigid bodies of pga3dphysics, timings and a selection guide
see [pga3dphysics/Solvers.md](../pga3dphysics/Solvers.md).

### Integrable

The state of an object is a fixed number of doubles. The object does not know which solver
integrates it and does no stage arithmetic: it only converts between its own fields and a run
of doubles in an array owned by the solver.

```scala
trait Integrable:
  val stateSize: Int
  def saveState(dst: Array[Double], offset: Int): Unit
  def loadState(src: Array[Double], offset: Int): Unit
  def saveDerivative(dst: Array[Double], offset: Int): Unit
  def resetForces(): Unit
```

* `stateSize` is a `val`: the size cannot change during the life of the object, so a solver
  and an `IntegrableArray` may compute the layout once.
* `saveState` / `loadState` write and read `stateSize` doubles starting at `offset`.
* `saveDerivative` writes d(state)/dt in the same layout: the double number j is the derivative
  of the double number j of the state.
* `resetForces` drops whatever the previous force callback accumulated (forces, torques, any
  other inputs); an object without accumulators leaves it empty. The method is abstract on
  purpose: a forgotten reset silently sums the forces of all the stages.

One derivative evaluation of a solver is always the same sequence:

1. `loadState` of every object with the trial state of the stage (skipped for the first
   stage - the objects already hold the state);
2. `resetForces` of every object;
3. the force callback of the step: user code reads the objects and adds forces to them;
4. `saveDerivative` of every object.

So inside the force callback and inside `saveDerivative` all the objects of the step hold the
same trial state: an object may read its neighbours, and a force between two objects is
computed once and added to both. Inside `loadState` the other objects may still hold the
previous stage - do not read them there.

### States that need a normalization

A solver combines states and derivatives linearly: `initial + dt * (c1 * k1 + c2 * k2 + ...)`.
For a state living on a manifold - a unit vector, a rotor, a motor - such a combination leaves
the manifold. `loadState` is the place to return: it receives the raw combination and stores
the projected state, exactly as a rigid body renormalizes its motor.

* The solver never reads the projected state back inside a step: every combination starts from
  the saved initial state and the raw derivatives.
* The projection must not move a valid state and must be smooth around the manifold, then the
  order of the solver survives (measured in `OdeSolverTest`: a unit vector normalized in
  `loadState` keeps the 4th order under RK4 and stays unit to 1e-15). A clamp or another
  non-smooth correction does not break the solver, but the order drops where it fires.
* The derivative is the plain derivative of the stored doubles; it does not have to be
  projected.

The same two methods serve outside a solver: `saveState` / `loadState` make a snapshot and a
rollback of the object, and a blend of two saved states loaded back is an interpolated state,
normalized by the same code.

### Example

```scala
/** a flywheel: the state is (angle, angular velocity) */
final class Flywheel(val inertia: Double, var angle: Double, var omega: Double) extends Integrable:
  private var torque = 0.0

  def addTorque(t: Double): Unit =
    torque += t

  override val stateSize: Int = 2

  override def saveState(dst: Array[Double], offset: Int): Unit =
    dst(offset) = angle
    dst(offset + 1) = omega

  override def loadState(src: Array[Double], offset: Int): Unit =
    angle = src(offset)
    omega = src(offset + 1)

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = omega
    dst(offset + 1) = torque / inertia

  override def resetForces(): Unit =
    torque = 0.0
```

```scala
val bodies: Array[Pga3dPhysicsBody] = ...
val flywheel = Flywheel(inertia = 0.2, angle = 0.0, omega = 100.0)

val bodiesArray = IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)
val world = Integrable(bodiesArray, flywheel)
val cache = OdeSolverCache()

OdeSolverRK4.step(world, dt, currentDt => {
  val torque = brakeTorque(bodies(0), flywheel) // reads the trial states of both
  flywheel.addTorque(-torque)
  bodies(0).addGlobalForque(Pga3dForque.torque(axis * torque))
}, cache)
```

A model with a memory of the path instead of a smooth state - like `HystereticFriction`
above - is not an `Integrable`: its stages read the pure `forceAt`, and `advance` commits once
after the completed step.

### IntegrableArray

A solver takes one `Integrable`. Several objects are joined by an `IntegrableArray[T]`: a thin
wrapper over an `Array[T]` that is an `Integrable` itself and packs the states of the elements
one after another. The trait has the public `elements: Array[T]` and `offsetOf(i)`, where the
element number i starts. The wrappers use the given array itself, without a copy, and they
nest - a car is an array of its wheels and engine, a world is an array of cars - with exactly
the layout of the flattened one. The sizes are taken once, at the construction: to add or
remove an object, or to replace one by an object of another state size, build a new wrapper.
`Integrable.empty` is the object without a state.

Two kinds implement the trait:

* `IntegrableAnyArray(a, b, c)` or `IntegrableAnyArray(array)` - objects of any types and state
  sizes, with a table of offsets. It calls the elements through the interface.
* `IntegrableUniformArray(array, elementSize)` - objects of one type and one state size: the
  element number i starts at `i * elementSize`. `elementSize` is public as well. The state
  size of every element is checked at the construction, unless `checkElementSizes = false`
  is passed.

`Integrable(a, b, c)` or `Integrable(array)` is the plain way to join objects without thinking
about the kind of the array: it gives an `IntegrableAnyArray`. The difference in speed is a few
percent, so the special `Integrable.uniformElementSize(array, elementSize)` - the same as
`IntegrableUniformArray(array, elementSize)` - is for the places where it matters.

The call `IntegrableUniformArray(array, elementSize)` is inlined: every call site gets its own
class, with the loops written for the element type and the element size known at that place.
So the calls of the elements are direct, whatever else is in the world, and nothing has to be
written by hand for a new type - the rigid bodies of pga3dphysics are joined by the same call,
`IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)`.

There is deliberately no uniform array as a class with loops shared by all element types:
such a loop is one bytecode calling the elements through the interface, the JIT inlines it
only while a single element type passes through it, and it would only look like a specialized
one. See the measurements in
[pga3dphysics/Solvers.md](../pga3dphysics/Solvers.md#one-system-flat-state).

### OdeSolverCache

The work arrays of a solver, kept between the steps. `cache.getArrays(groupSize, size)` gives
`groupSize` separate arrays of `size` doubles each by one call - a solver never asks for them
one by one. The same arrays come back for the same request, a different groupSize or size
replaces all of them at once. So a world that keeps one `OdeSolverCache` and passes it to every
step allocates the stage arrays once. The arrays are never cleared: their content is whatever
the previous step left, and no solver depends on it (tested by filling them with NaN). Not
thread-safe; one instance per world, and no nested steps with the same instance.

The cache is optional: a solver takes `cache: OdeSolverCache | Null = null`, and without a
cache it creates new arrays on every step - `OdeSolverRK4.step(world, dt, addForces)`. That is
the choice when a step is rare or the allocations do not matter.

For a solver author: `OdeSolverCache.getArrays(cache, groupSize, size)` handles both cases.
Take `size = system.stateSize` and one group of arrays for the initial state, the stage
derivatives and the trial state; the rest is plain loops over doubles
(`OdeSolverUtil.saveDerivative` and `combine`) - see `OdeSolverRK4`:

```scala
val size = system.stateSize
val arrays = OdeSolverCache.getArrays(cache, 6, size)
val initial = arrays(0)
val k1 = arrays(1)
// ...
```
