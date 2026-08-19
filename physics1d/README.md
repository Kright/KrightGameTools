# Scalar 1d force models

Force models along one scalar coordinate (a rod length, a suspension travel): a nonlinear
elasticity curve and hysteretic (memory-carrying) friction. No PGA here — the module depends
only on mathutil, and pga3dphysics builds its joints on top of it (e.g. `Pga3dForque.spring`).

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
