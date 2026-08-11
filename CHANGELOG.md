# Changelog

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed (breaking)

- `Pga3dInertiaMovedLocal` caches its pose as a `Pga3dTransform` (the `transform` val, rebuilt
  on construction including deserialization): every operation converts the argument to the local
  frame and back, and with the transform those conversions are plain matrix multiplications. The
  case fields stay `(localToGlobal: Pga3dMotor, localInertia)`, so the serialized layout is still
  the 12 doubles of motor + inertia, and `copy`/equality work on the motor. The pure solver
  overhead of every integrator dropped 1.2-1.3x (see `PhysicsSolverBenchmark` and the updated
  table in Solvers.md).
- `Pga3dPhysicsBody` stores its pose as a `Pga3dTransform` (`var transform`), and the primary
  constructor takes the transform; an auxiliary constructor from a `Pga3dMotor` is kept, and the
  `body.motor` accessors are derived from the transform (assigning a motor renormalizes it and
  rebuilds the transform). The internal
  matrix cache classes `Pga3dMotorWithMatrix` and `Pga3dMatrixForPoints` are removed - the transform
  replaces them; `motorSandwich` and `globalCenter` on the body keep working on top of it.
- pga2d point-valued results are typed as points, not rotors: the `xy` blade of 2d PGA lives in
  both `Pga2dRotor` and the point family, and the class-selection tie-break used to hand a value
  with a structurally zero scalar part to the rotor. 39 generated methods change their result
  type from `Pga2dRotor` to `Pga2dProjectivePoint`: rotor sandwiches of the point family
  (`rotor.sandwich(point)` now agrees with `motor.sandwich(point)`), `Pga2dPointCenter * scalar`
  / `+` / `madd`, point projections onto central lines, the `bulk` of the point family. A genuine
  rotor always carries its scalar part, which a point cannot hold, so rotor-valued results keep
  resolving to `Pga2dRotor`.
- `Pga3dQuaternion` alias is removed: the class is `Pga3dRotor`. The C++ `Quaternion` is renamed
  to `Rotor` as well (`TranslatorWithRotor` / `RotorWithTranslator`, `toRotorUnsafe`).
- `Pga3dPlaneIdeal` / `Pga2dLineIdeal` are renamed to `Pga3dPlaneCentral` / `Pga2dLineCentral`
  (and `PlaneCentral` in C++): these are the hyperplanes through the origin (pure bulk). The word
  "ideal" is now reserved for its standard PGA meaning - the at-infinity (pure weight) part.
- `Rotor.restore(axes...)` is renamed to `fromAxes` (2d, 3d, C++).
- `log`, `exp` and `split` are parameterless methods without parentheses: `motor.log.exp(t)`,
  `bivector.split` (the `exp(t: Double)` overload is unchanged).
- 2d `projectOntoLine` now returns the projective point with `w = line.normSquare > 0`
  (was negative), matching 3d; `toPointUnsafe` after projecting onto a normalized line is now
  correct. Bugfix, but the sign of the returned homogeneous representative changes.
- `normalizedByBulk` / `normalizedByNorm` of constant-norm classes (`Point`, `PointCenter`,
  `Translator`) now return the class itself instead of widening to the projective type, and the
  constant norms fold to `1.0` (no runtime `sqrt(1.0)`).
- The parameter of the generated binary operations is renamed `v` -> `r` (affects named-argument
  call sites; also removes the `def v(v: ...)` join-alias shadowing).
- `rotation(from, to)`: the exact-wedge branch now starts at `dot <= -0.9` (was `-1 + 1e-6`).
- `gametools-pga3d` now depends on `gametools-mathutil` at compile scope (it was already a
  transitive dependency through `gametools-matrix`).
- `Pga3dNearestPoint.update` / `Pga3dPairOfNearestPoints.update` (and the 2d twins) return
  `Boolean` - whether the candidate improved the accumulator (e.g. to know which of several
  queried volumes produced the final contact). The comparison is NaN-safe in both
  directions: a NaN candidate never wins, and a NaN stored distance is replaced by the
  first real candidate instead of silently blocking all further updates.
- `getNearestPointsBinSearch` is removed from `Pga3dEdge` / `Pga2dEdge`: slow, allocating,
  and easy to call by accident instead of the analytic `getNearestPoints`. The
  implementation lives on in the test sources (`Pga3dEdgeBinSearchReference` /
  `Pga2dEdgeBinSearchReference`) as a near-perfect-precision reference.
- `mathutil.IEqualsWithEps` and `mathutil.EqualityEps` are removed in favor of the
  `CanEqualWithEps` typeclass: the `===` operator with an implicit eps is gone - pass eps
  explicitly (`a.equalsWithEps(b, eps)` via `CanEqualWithEps`, or the plain `isEquals(other, eps)`
  methods that `Matrix` and `VectorNd` keep).
- `hasIntersection` is renamed to `intersects` - the one name for every boolean overlap query:
  `Pga3dSphere` / `Pga2dCircle` (which briefly had both names) and `Pga3dRay` / `Pga2dRay`
  against an AABB.
- The nearest-point accumulators spell out `distance`: `Pga3dPairOfNearestPoints.dist` /
  `Pga2dPairOfNearestPoints.dist` are renamed to `distance` (matching the single-point
  accumulators), and the `distSquare` field of all four accumulator classes is renamed to
  `distanceSquare` (matching `normSquare` / `distanceSquareTo` everywhere else).
- `Pga3dPhysicsSolver` loses its type parameter: it is `trait Pga3dPhysicsSolver` with
  `step(dynamicBodies: Array[Pga3dPhysicsBody], ...)`. The parameter was dead - the solvers
  call `Pga3dPhysicsBody` methods directly, so no other body type could ever instantiate it,
  and every implementation and call site already used exactly this type.
- The geometry queries that may find nothing return `T | Null` instead of `Option[T]`:
  `intersection` on `Pga3dTriangle` / `Pga2dTriangle` (all overloads, `intersectionWithPlane`
  included) and on `Pga3dAABB` / `Pga2dAABB`, and `deepestContact` on `Pga3dSphere` /
  `Pga3dCapsule`. `null` means "no intersection"; the library is compiled with
  `-Yexplicit-nulls`, so the nullability stays in the type and a `ne null` check (with flow
  typing) replaces `isDefined`/`.get`. Motivation: these are the BVH/raycast hot-path calls,
  and the `Option` wrapper was one more allocation per query on top of the result itself.
- `Pga3dPhysicsSolverVerletConstrained.step` takes an optional `localBs` output array (after
  `nextMotors`, before the trailing iteration limits; default `null` skips the output): the
  honest node twists of the consumed poses, which the step reconstructs anyway. Observers
  (energy, momentum, velocity-dependent logic) previously had to run a second
  `reconstructNode` - and pay a second force callback per step - to get the same values;
  `reconstructNode` itself now writes into a caller-supplied array and returns the forques.

### Changed

- `Pga3dPhysicsSolverVerletConstrained` caches the consumed poses as `Pga3dTransform`s for the
  whole step (they are fixed until the new poses are accepted): the momenta reconstruction, the
  first half kick, the constraint gradients and every velocity-projection sweep now apply
  precomputed matrices instead of re-expanding `motor.sandwich`/`reverseSandwich` per call
  (up to `projectionIterations` sweeps over all constraints). The SHAKE stage keeps plain motors
  for `nextMotors` - those mutate after every accepted impulse, a cache would never amortize.
  Results are unchanged up to floating-point rounding.
- `motor.sandwich(summable: Pga3dInertiaSummable)` builds one `Pga3dProjectiveTransform` and
  applies it to the 8 projective points of the two-sided conjugation instead of 8 full motor
  sandwiches (the projective transform matches `motor.sandwich` for any motor, normalized or not).
- The generated code emits sums of 4 or more terms with the summands grouped in parenthesized
  pairs, recursively: `a + b - c + d + e + f` becomes `(a + b) - (c - d) + (e + f)` (a pair
  starting with a negative term is subtracted as a whole, so no group opens with a minus).
  The JVM must not reassociate floating point additions, so a flat sum is a sequential
  left-to-right chain; the grouping halves the dependency chain and lets the CPU add the pairs
  in parallel. Results may differ from the previous code in the last bits (the association
  order changed); all precision tests pass unchanged. Measured effect: ~10% on the 6-term rows
  of `Pga3dTransform.sandwich(bivector)`, 0-5% on the physics solvers.
- `Pga3dPhysicsSolverRKMK4` uses the closed-form `(-u).dexpInv(omega)` from pga3d instead of
  its own Bernoulli-series truncation (which preserved the 4th order but was approximate).
  The honest form pays a sqrt + sin + cos per stage: ~15% more pure per-step solver overhead
  in `benchmark/PhysicsSolverBenchmark` (empty force callback; with real forces the share is
  smaller).
- The dexp/dexpInv coefficients with an inherent cancellation (`sinMinusCosDivLen2`, `k2`)
  use wide polynomial windows (up to `bulkNorm = 0.5`, degree 7-8 in `bulkNorm^2`) instead of
  the narrow 1e-5 series windows of exp/log: the closed forms are now accurate to a few ulps
  for every argument, where previously a tiny bulk combined with a unit-scale weight lost up
  to ~4e-11 * weightNorm(u) * norm(b) just above the threshold (harmless for integrators,
  visible when dexp is used as a standalone Jacobian). The Horner polynomials are ~4x cheaper
  than the trigonometric forms they replace (`benchmark/TrigVsPolynomialBenchmark`); exp/log
  keep their narrow windows - their cancellations are damped by the structure of exp itself.
- The generated `cross` groups the mirrored summands of the commutator into parenthesized
  differences: `(a.f * b.g - a.g * b.f) + ...` instead of the interleaved canonical sort
  (Scala, both dimensions, and C++). Two identities become bit-exact: `u.cross(u * 2^k)`
  (in particular `u.cross(u)`) is exactly zero, and `a.cross(b) == -b.cross(a)` for
  same-class operands - so `dexp`/`dexpInv` pass collinear arguments through exactly, and a
  constant twist integrates without drift. Results of `cross` on unrelated arguments may
  change by ~1 ulp (the summation order changed); the operation count is the same.
- `Pga3dTriangle.getNearestPoint` is rewritten via Voronoi regions (Ericson, "Real-Time
  Collision Detection", 5.1.5): no intermediate collections, no square roots, 4-8x faster
  (JMH: 77 -> 9 ns far from the triangle, 64 -> 12 ns near the surface, 53 -> 12 ns for
  points projecting inside; see `benchmark/NearestPointBenchmark`). Degenerate triangles now
  deterministically fall back to the nearest point of the longest edge (the old implementation
  could return NaN or an arbitrary point of the supporting plane for collinear vertices).
  The legacy implementation is kept in `Pga3dTriangleNearestPointTest` as a correctness
  reference.
- `Pga2dTriangle.getNearestPoint` gets the same Voronoi rewrite (9-11x faster on outside
  points, see `benchmark/NearestPoint2dBenchmark`), with a 2d bonus: a point inside the
  triangle is returned as is, exactly. The 2d-as-z=0-slice-of-3d correspondence is
  property-tested in `Pga2dTo3dCorrespondenceTest`.
- `Pga3dAABB.intersects(triangle)` / `Pga2dAABB.intersects(triangle)` are rewritten via the
  separating axis theorem (Akenine-Moller; 13 axes in 3d, 5 in 2d): no allocations, square
  roots or divisions. The box-face phase runs on raw coordinates (no multiplications,
  exact comparisons) with a vertex-inside early accept; the box-center frame is built only
  for candidates that survive it. JMH (`benchmark/AABBTriangleBenchmark`): the hard miss
  (bounding boxes overlap, the triangle is diagonally beyond a corner) 204 -> 15 ns (13x),
  overlap 8.4 -> 6.2 ns, far miss 5.2 -> 6.8 ns (the one case slightly behind; one avoided
  hard miss pays for ~100 far misses, and this is the once-per-build grid path).
  The `eps` parameter is removed (breaking): its meaning was muddled (a fuzzy edge-distance
  tolerance in 3d, silently unused in 2d) - for a tolerance, expand the box once outside
  the scan loop: `aabb.expand(eps).intersects(triangle)`, which is exactly equivalent
  and takes the additions off the per-triangle path.
  Degenerate triangles work (segments and points are covered by the remaining axes; the
  noise-normal plane axis is guarded by a relative threshold), and zero-thickness boxes
  are handled exactly - the legacy implementation returned false for collinear triangles
  (NaN plane) and was ulp-unstable on flat boxes; it is kept in `Pga3dAABBTriangleTest` /
  `Pga2dAABBTriangleTest` as a reference for the non-degenerate cases.
- `Pga3dTriangle.intersection` / `Pga2dTriangle.intersection` hot path: the AABB early reject
  compares coordinates directly instead of constructing two AABBs (allocation-free by
  construction - the JIT scalar-replaced them in monomorphic code, but Scala.js/Native and
  polymorphic call sites get the guarantee too), and the 3d parallelism check compares
  dot^2 against |dir|^2 instead of normalizing the direction (one sqrt less per call).
  The previous implementation is kept in `Pga3dTriangleIntersectionTest` /
  `Pga2dTriangleIntersectionTest` as a correctness reference. A degenerate (collinear)
  triangle behaves as a segment through the parallel-branch fallback (unit-tested).

### Added

- `Pga3dTransform` / `Pga3dProjectiveTransform`: cached, immutable forms of a motor for repeated
  applications - the sandwich operator stored as flat public matrix coefficients (rotation 3x3, the
  moment block for bivectors, the two translations), case classes deriving `CanEqual`,
  `CanEqualWithEps` and `FlatDoubleSerializer` like the other pga3d classes - so `sandwich` /
  `reverseSandwich` become plain matrix multiplications (a bivector costs 36 multiplications instead
  of ~63, a point costs 9). Both support every argument class the motor sandwich supports, including
  motor-like arguments (`Motor`, `Rotor`, the translators) - for those the sandwich is not a
  composition but the change of coordinates of the transformation itself.
  - `Pga3dTransform` requires a normalized motor - built with `Pga3dTransform(motor)`, which
    renormalizes its argument, or `Pga3dTransform.fromNormalized(motor)`, which trusts the caller
    and skips the `sqrt`. The assumption drops the Study-number corrections from the formulas
    (32 doubles: the motor plus 24 coefficients) and narrows the result types: `sandwich(point)`
    is a `Pga3dPoint` (not a projective point, no `toPointUnsafe` at the call sites),
    `sandwich(translator)` is a `Pga3dTranslator`, `Pga3dPointCenter` maps to a `Pga3dPoint`.
  - `Pga3dProjectiveTransform` works for any motor, normalized or not: it additionally caches the
    Study number `normSquare` / `normSquareI` (34 doubles) and returns projective results scaled
    by `normSquare` (a point maps to `Pga3dProjectivePoint` with `w = normSquare`), matching
    `motor.sandwich` up to rounding.
  Generated symbolically by `Pga3dTransformCodeGen` (one generator, a `normalized` flag): each
  coefficient is derived as a column of the linear sandwich operator and registered once, then the
  cached names are substituted back into the full symbolic sandwich expression of every method, so
  the emitted code is provably that expression with the coefficient groups named; a leftover motor
  field fails generation. In the normalized variant `normSquare` / `normSquareI` are then replaced
  by 1 and 0 and the result class is looked up on the simplified expression - that is where the
  narrower result types come from.
  The 2d twins come from the same shared generator; there is no moment block and no `normSquareI`
  in 2d, and a 2d rotation matrix has only two independent entries. In `Pga2dTransform`
  (6 cached values) they are the honest `cos` and `sin` of the rotation angle and are named so;
  in `Pga2dProjectiveTransform` (7 values, with `normSquare`) they are scaled by `normSquare`,
  so they keep the matrix-entry names `r00` and `r01`.
- `Pga2dProjectivePoint.split`: the 2d sibling of `Pga3dBivector.split`, the commuting
  (rotation, translation) decomposition of a motor generator with `this == first + second` and
  `exp == first.exp.geometric(second.exp)`. There are no screw motions in 2d, so the split is
  all-or-nothing: a generator with `w != 0` is entirely a rotation around the point `(x/w, y/w)`
  with a zero shift, an ideal generator (`w == 0`) is entirely a translation. The 3d `split`
  gained the scaladoc it was missing.
- `dexp` / `dexpInv` on the generator (grade-2) classes: `Pga3dBivector`, `Pga3dBivectorBulk`,
  `Pga3dBivectorWeight`, `Pga2dProjectivePoint`, `Pga2dVector` - the differential of `exp`
  and its inverse in closed form (the left Jacobian of SE(3)/SE(2) and its inverse; the right
  variant is `(-u).dexp(b)`), the building block of Lie-group ODE integrators like RKMK4.
  Same study-number branches and thresholds as `exp`/`log` (in 2d the angle is real, no dual
  corrections); degenerate arguments are exact (`zero.dexp(b) == b`, a pure-weight `u` gives
  `b ± u.cross(b)`), `dexpInv` is singular at `bulkNorm == pi`. The generic `ga` module gains
  slow series reference implementations (`PGA.dexp` with `ad^k/(k+1)!`, `PGA.dexpInv` with
  Bernoulli numbers), and the closed forms are property-tested against them, including the
  finite-difference defining property `(u + b*h).exp ≈ (u.dexp(b) * h).exp * u.exp`.
- Hard distance constraints for pga3dphysics: `Pga3dDistanceConstraint` (rod / rope / strut
  between two body or world anchor points), `Pga3dConstraintResolver` (exact acceleration-level
  constraint forques for the force callback + post-step Gauss-Seidel projection of positions
  and velocities in the inverse-inertia metric) and `Pga3dPhysicsSolverConstrained(inner,
  resolver)` wiring both into any solver except Verlet. The constrained RK4 keeps the 4th
  order (measured 4.06-4.10 on a rigid pendulum with an off-center anchor), holds distances to
  ~1e-15 and conserves the momentum of body-body constraints to ~1e-12. Coupled constraints
  (chains) are solved by running the Gauss-Seidel sweeps to convergence (the iteration counts
  are only safety bounds): on a swinging 4-body rod chain every solver keeps its own order
  (euler 1.21, heun/midPoint 2.05, rk4/rkmk4 4.15, rkf45 4.33, gaussLegendre(6) 3.94), the
  rods hold to the machine epsilon and the energy error stays at the truncation level.
- `Pga3dPhysicsSolverVerletConstrained`: a RATTLE on the motor group - the Verlet-family
  solver with hard distance constraints built into the step itself (a generic projection
  composite over Verlet only reaches the 1st order). Like the plain Verlet it is a stateless
  strategy over caller-owned pose arrays, not a `Pga3dPhysicsSolver`; the constraints (and the
  iteration caps) are arguments of `step`, so they may change freely between steps - e.g.
  contact-like constraints regenerated every frame. The step reconstructs the node momenta
  from the poses (redoing the second RATTLE half kick of the previous segment with its
  velocity-stage impulses - so the user force callback runs once per step), then performs the
  first half kick with the position-stage constraint impulses whose gradients are evaluated at
  the old poses (the variational SHAKE/RATTLE ingredient), solved by Newton/Gauss-Seidel
  sweeps through the midframe drift. The poses go to `nextMotors` and the honest observer
  velocities of the consumed poses to the `localBs` output (the same values a separate
  `reconstructNode(...)` computes). Measured on the rod chain: 2nd-order convergence (2.00, 2.00, 2.00),
  rods to ~3e-11 (the sweep exit threshold; single constraints to ~1e-15), dumbbell momentum
  to ~8e-12 and energy to ~1e-12 over 20k steps, bounded energy oscillation (~0.1% on the
  off-center pendulum over 200 s, 1e-5 relative on the chain). Pose edits between steps still
  become velocity edits, like in the plain Verlet.
- `benchmark/PhysicsSolverBenchmark`: JMH comparison of the per-step overhead of all physics
  solvers with an empty force callback (ns per body per step: euler 117, midPoint 230, heun 239,
  verlet 406, rk4 472, rkmk4 657, rkf45 1056, gaussLegendre(3) 1079); the solvers table in
  pga3dphysics/Solvers.md cites these numbers.
- `Pga3dPhysicsSolverVerlet`: a position-Verlet (leapfrog) on the motor group, 2nd order, one
  force evaluation per step. Like the classic Verlet it stores no velocities at all: it is a
  stateless strategy (not a `Pga3dPhysicsSolver`) whose state is two consecutive pose arrays
  owned by the caller - `step(inertias, prevMotors, motors, globalForques, prevDt, dt,
  nextMotors, nextLocalBs)`, with `makePrevMotors` building the virtual previous poses for the
  first step. The half-step twist is reconstructed as the Lie derivative of the pose
  (`-2 * (prevMotor.reverse * motor).log / dt`), and the kick is applied to the world-frame
  momentum (the discrete Moser-Veselov idea) with the next displacement solved from the
  momentum by a fixed-point iterated to convergence, so the gyroscopic term never appears
  explicitly and re-deriving the momentum from the poses every step loses nothing. Free
  precession at dt = 0.01: momentum exact to rounding (~1e-15), energy error bounded
  (4.1666e-6 after 1k steps and after 100k) instead of drifting (midpoint: 1.7e-4). Editing a
  motor between steps implicitly edits the velocity, like in position-based dynamics.
- `Pga3dPhysicsSolverRKF45`: a Runge-Kutta-Fehlberg 4(5) embedded pair with a fixed step. The
  committed state is the 4th-order solution; the extra stages build the embedded 5th-order one
  and the norm of their difference is a per-step, per-body local-error estimate exposed via
  `lastMotorErrors` / `lastLocalBErrors` / `lastMaxError` - a dev build can log it during the
  simulation and map where the error lives (impacts, stiff constraints, fast-spinning bodies)
  instead of rerunning at a smaller dt. The estimate is essentially exact (measured 0.98-1.01
  of the true single-step error) and scales as dt^5. It is a class, not an object: each
  instance keeps its own last-step estimate. Bonus: the Fehlberg 4th-order tableau shows ~8x
  smaller free-precession error constants than the classic RK4 one.
- `Pga3dPhysicsSolverRKMK4`: a Runge-Kutta-Munthe-Kaas 4th-order solver. Instead of adding
  scaled motor derivatives in flat R^8 like `Pga3dPhysicsSolverRK4`, it integrates
  `u' = dexpInv(-u, -localB / 2)` (the series truncated at the `ad^2/12` term) on the flat
  bivector Lie algebra and updates with a single `M0 * exp(u)`, so every stage and the result
  are exact motors by construction and renormalization only removes rounding noise of the
  product. On the free-precession test it matches renormalized RK4 (both are geometric
  4th-order methods) with a slightly smaller error constant (~0.5% at practical step sizes).
- `Pga3dTriangle.fartherThan(p, maxDistance)` / `Pga2dTriangle.fartherThan`: conservative early
  reject against the triangle's bounding box - a few comparisons, no multiplications and no
  allocations (4.4 vs 6.7 ns for the allocating `toAABB.contains` equivalent). A prefilter
  before `getNearestPoint` when scanning many triangles; `true` is reliable for any input
  including degenerate triangles, NaN or infinite arguments.
- `intersection(edge, normalizedPlane, eps)` overload on `Pga3dTriangle`: for static geometry
  the caller can compute `normalizedPlane` once (e.g. in an array parallel to the triangles)
  and take the per-call plane construction with its sqrt off the raycast hot path. JMH
  wheel-raycast scenario (`benchmark/IntersectionBenchmark`): hit 59 -> 52 ns, with the cached
  plane 37 ns; the AABB-reject path stays at ~6 ns.
- `Pga3dCapsule(a, b, r)`: all points within r of the segment [a, b], stored by the two
  hemisphere centers - no unit or normalization invariants, `a == b` degenerates to a
  sphere (`Pga3dCapsule(sphere)`). The engine-style representation is bidirectional:
  `Pga3dCapsule.fromCenter(center, halfAxis, r)` and the `center` / `halfAxis` accessors.
  Plus `edge`, `toAABB` (the edge AABB expanded by r), `expand(dr)`, `map` and
  motor/rotor/translator sandwich.
- Capsule collision queries: `intersects(sphere/capsule/triangle)` (with the symmetric
  `sphere.intersects(capsule)` / `circle.intersects(capsule)` delegates) and
  `deepestContact(triangle): Pga3dContact | Null`. The non-piercing contact mirrors the
  sphere (nearest pair, radial normal, depth `r - distance`); when the axis pierces the
  triangle the normal is the plane normal oriented towards the larger part of the axis
  (computed geometrically, independent of the plane orientation convention) and the depth
  is `r + reach` - pushing by depth fully separates the capsule. Property-tested: a
  zero-radius capsule behaves as its edge, an `a == b` capsule matches the sphere answers.
- `Pga2dCapsule(a, b, r)` (a stadium shape) mirrors the 3d capsule: the same accessors,
  constructors, sandwich extensions and `intersects(circle/capsule/triangle)`, backed by
  `Pga2dTriangle.getNearestPoints(edge)` / `distanceSquareTo(edge)` - the 2d triangle is
  filled, so an edge endpoint inside it or a proper boundary crossing (detected by
  orientation predicates) yields an exactly zero distance. 2d `deepestContact` is deferred
  together with the circle one (the interior-contact convention needs a design decision).
- `Pga3dTriangle.getNearestPoints(edge)` / `distanceSquareTo(edge)` / `distanceTo(edge)`:
  the segment-triangle nearest pair (Ericson 5.1.10-style candidates: three edge-edge
  pairs, two endpoint-triangle pairs and the plane crossing). A piercing edge yields
  exactly zero distance; the interior test goes through the degeneracy-hardened
  getNearestPoint, so nearly degenerate triangles and extreme scales (1e+-30) stay correct.
- Sphere and circle as collision queries: `Pga3dSphere.intersects(triangle)` (a comparison of
  `distanceSquareTo` against r^2) and `Pga3dSphere.deepestContact(triangle): Pga3dContact | Null` -
  the nearest triangle point, the unit normal towards the sphere center and the penetration
  depth; a center exactly on the triangle falls back to the plane normal, and the pathological
  "center exactly on a degenerate triangle" returns null. `Pga3dContact(point, normal, depth)`
  is a new case class (7 flat doubles). `Pga2dCircle.intersects(triangle)` mirrors the 2d side
  (2d `deepestContact` is deferred: an interior center is common in 2d and its normal/depth
  convention deserves a design decision). The sphere-sphere / circle-circle overlap query is
  `intersects` as well (the old `hasIntersection` name is renamed, see the breaking section).
- `distanceSquareTo` on `Pga3dTriangle`/`Pga2dTriangle` (point), `Pga3dEdge`/`Pga2dEdge`
  (point and edge-edge) and `Pga3dAABB`/`Pga2dAABB` (point): sqrt-free companions of
  `distanceTo` for comparisons, symmetric with `norm`/`normSquare`. `distanceTo` delegates to
  them; `contains(p, eps)` and 2d `Pga2dEdge.intersects(other, eps)` compare squares
  (one sqrt less on the `intersection` path) and are guarded against negative/NaN eps.
- `pow(t)` on Motor, Rotor and Translator in 2d and 3d (`motor.pow(0.5)` is the half motion);
  C++ gains the `restoreRotationInPlaneX/Y/Z` wrappers for parity.
- `Pga2dRotor.log: Double` (the half-angle) and `Pga2dRotor.exp(halfAngle)`; `slerp` no longer
  detours through a motor. `axisX` / `axisY` on the 2d rotor and motor.
- `Pga3dRotor.zero` / `Pga2dRotor.zero` (the zero element, e.g. for derivatives; `id` remains the
  identity).
- Point projections onto `PlaneCentral` / `LineCentral`; all projections document the positive-w
  convention (`w = 1` for a normalized hyperplane).
- `Pga2dMatrix`: conversions between linear operators on `Pga2dProjectivePoint` and 3x3 matrices
  (the matrix generator is shared between dimensions now).
- `mathutil.ExactArith`: `fma` (the JVM intrinsic; a Dekker-based portable emulation on Scala.js)
  and `diffOfProducts` (Kahan's algorithm, <= 2 ulp relative error even under catastrophic
  cancellation).
- Test suites: `PrecisionTest` and `GeometricProductTest` for pga3d, pow/log/exp round trips,
  extreme-magnitude sweeps (1e-300..1e300, the 2^53 integer boundary).
- `Pga3dVector.crossRightHanded(a, b)` / `crossLeftHanded(a, b)` (Scala and C++): the classical
  cross product for both basis orientations. Not a GA operation (the GA-native form is the join
  `a v b`); provided for convenience and for adapting code from other libraries.
- `FlatDoubleSerializer` derivation now recurses into nested case classes: a field may be a
  `Double` or another case class whose fields are (recursively) `Double`s, flattened into
  consecutive doubles at compile time - so `FlatArray[Pga3dTriangle]` packs 9 doubles per
  element with no per-field indirection.
- The geometry case classes derive `CanEqual` and `FlatDoubleSerializer`: `Triangle`, `Edge`,
  `AABB`, `Ray`, plus `Sphere`/`Cylinder` in 3d and `Circle` in 2d (`Edge` already had
  `CanEqual`). Round trips are property-tested in `FlatDoubleSerializerPga3dGeomTest` /
  `FlatDoubleSerializerPga2dGeomTest`, including flattened-size checks.
- Benchmarks `Pga3dVectorRotorBenchmark` and `Pga3dTriangleRotorBenchmark`: rotor sandwich over
  10k elements - the cache-resident complement to `FlatArrayRotorBenchmark`'s 500k-8M. Findings:
  write-back through `FlatArray` stays ~6x faster even when everything fits in L2 (the boxed cost
  is dominated by allocating the replacement objects, 4 per triangle); boxed sequential *reads*
  are on par with flat for vectors, while for triangles the naive materializing flat read
  (`flatArray(i)` builds a triangle + 3 points) loses badly when the JIT fails to
  scalar-replace the nested construction (~432 B/element, JMH `gc.alloc.rate.norm`) - boxed
  reads scalar-replace to zero allocation.
- `mathutil.CanEqualWithEps`: a derivable typeclass for approximate equality with an explicit
  tolerance - `a.equalsWithEps(b, eps)` is true iff the Chebyshev (L-infinity) distance over all
  `Double` components is within `eps`. `derives CanEqualWithEps` follows the `FlatDoubleSerializer`
  derivation rules (fields are `Double`s or, recursively, such case classes) and inlines to a flat
  `&&`-chain with early exit and no allocations; equal infinities compare equal, NaN never does.
  The comparison is componentwise: projective types compare homogeneous representatives, and a
  rotor is not equal to its negation. Derived by every generated pga3d/pga2d class and the
  geometry case classes; a `CanEqualWithEps[Double]` instance lives in the companion
  (`import CanEqualWithEps.given` for direct calls on `Double`). The array-backed `Matrix`
  gets a hand-written instance in its companion delegating to `Matrix.isEquals` (matrices of
  different sizes are not equal), since the derivation macro needs a case class of `Double`s.
- `Vector2d` / `Vector3d` / `Vector4d` derive `CanEqual` and `CanEqualWithEps` as well.
- The pga3dphysics case classes `Pga3dInertiaLocal`, `Pga3dInertiaSimple`, `Pga3dInertiaSummable`
  and `Pga3dBodyState` derive `CanEqual`, `CanEqualWithEps` and `FlatDoubleSerializer`
  (`Pga3dBodyState` = motor + bivector exercises the nested derivation); round trips are covered
  in `FlatDoubleSerializerPga3dPhysicsTest`. `Pga3dInertiaMovedLocal` and `Pga3dInertiaMovedSimple`
  are converted from plain classes to `final case class` (equality becomes structural) and derive
  the same three typeclasses; `Pga3dInertiaPrecomputed` stays a plain class - it wraps the
  `Pga3dInertia` trait (a dynamic type cannot be flattened statically) and caches matrices.

### Fixed

- `Pga3dCylinder.intersects(edge)` returned `true` for any edge whose axis projection partially
  overlapped the axis range: the final branch measured the distance from a sub-segment of the
  edge to the edge itself (identically zero) instead of to the axis. On top of that, two of the
  four clamping formulas mixed up their interpolation factors - invisible while the final
  comparison was vacuous. The edge is now clamped to the exact cap-plane crossings and compared
  against the axis segment; both the separation and the cap-crossing cases are unit-tested.
- The two-sided distance constraint (0 < min < max < infinity) in
  `Pga3dPhysicsSolverVerletConstrained`: `clampTotal` did not clamp it at all, so within a SHAKE
  step an engaged lambda could cross zero and flip to the opposite bound instead of releasing -
  the sweeps then fought each other, burned the iteration limit and left a distorted pose (the
  new test: a body pose-edited beyond the tether's max bound with a rod whose correction brings
  it back deep inside the range; the buggy active-set ended the step with the rod violated by
  1.8). A two-sided lambda now holds the bound it engaged (max for negative, min for positive)
  and releases through zero, matching the one-sided release logic; the constraint re-engages
  from the inactive state on a later sweep. Also covered: a swinging two-link pendulum whose
  slack link must stay inside its bounds, use the whole range and never create energy.
- `MathUtil.isEquals(arr1, arr2, eps)` compared `arr1.length` elements without checking the
  lengths: a longer `arr2` could compare equal, a shorter one threw
  `ArrayIndexOutOfBoundsException`. Now it requires equal lengths (and got a test).
- The `MathUtil.sign` extension is removed: it silently shadowed the standard `Double.sign`
  with different NaN semantics (`0.0` instead of `NaN`) wherever `import MathUtil.*` was in
  scope, and had no callers.
- The published Scala.js `gametools-matrix` could not link: `Matrix.*` and `frobeniusNormSquare`
  called `Math.fma`, which does not exist in the Scala.js javalib. The Scala.js compiler checks
  calls against the JDK signatures and only the linker reports missing methods for reached code,
  so this surfaced only in a downstream app's link step. Both call sites now use
  `ExactArith.fma` (the same `Math.fma` intrinsic on the JVM, the portable emulation on JS) -
  JS results change accordingly.
- scalatest is wired with `%%%` instead of `%%`, so the JS halves of the cross projects get the
  sjs test artifacts (with the JVM ones the JS test code compiled against TASTy but could never
  be linked or executed). The JVM stays the primary platform: `sbt test` compiles the JS test
  code but does not execute it (`jsTestsCompileNotRun`), so the tests run without Node
  installed; a JS suite can be run explicitly with `<module>JS/testOnly *SuiteName`. CI gained a
  `matrixJS/Test/fastLinkJS` step: the linker needs no JS runtime and catches references to
  methods missing from the Scala.js javalib - exactly the `Math.fma` failure mode - in all code
  reachable from the test suites.
- Zero-length (degenerate) edges no longer produce NaN: `Pga3dEdge.getNearestPoint` /
  `Pga2dEdge.getNearestPoint` had 0/0 in the interpolation factor, `getNearestPoints` divided
  0/0 for a pair of zero-length edges. A degenerate edge now behaves as a single point in all
  distance queries.
- Precision of `rotation(from, to)` near antipodal inputs: the rotation axis is recovered with
  error-free products (fma) and the deviation angle via asin, keeping the mapping error at
  ~1e-15 for any deviation down to exactly antipodal (was up to ~1e-8 near the old branch
  threshold). Applies to 2d, 3d and C++.
- Small-angle Taylor branches of `Pga3dBivector.exp` and `Pga3dMotor.log` had wrong second-order
  coefficients (numerically negligible below the branch threshold, but incorrect); all series now
  carry step-by-step derivations in the generated comments.
- C++ `Motor::log` / `Rotor::log` used `1/(1 - s*s)`, which cancels catastrophically for small
  angles; now shares the exact formulas with Scala.
- `Pga3dForque.getTorqueAroundCenter` multiplied the torque and force components elementwise
  instead of projecting the torque onto the force direction; it was correct only for
  axis-aligned forces. Now `force(getCenter, getLinearForce) + torque(getTorqueAroundCenter)`
  reconstructs the original forque (property-tested with force couples).
- Miscellaneous: redundant `sqrt`/division round trips in `log`, a broken documentation URL in
  the motor renormalization scaladoc, whitespace defects in the generated C++ headers.

### Internal

- `Pga3dInertiaPrecisionTest` measures the accuracy of `Pga3dInertiaMovedLocal` and
  `Pga3dInertiaPrecomputed` against a BigDecimal reference as a function of the center of mass
  offset R, on physically bounded local twists. The moved form loses relative precision as
  ~1e-16 * R on every operation, and so do apply/invert of the precomputed form; the kinetic
  energy and the acceleration of the precomputed form go through the near-cancellation of the
  parallel-axis terms baked into its 6x6 matrices and lose ~1e-16 * R^2 (2.2e-6 at R = 1e5 vs
  9.8e-12 for the moved form) - the documented price of the fast matrix path (see the README).
  The test pins both bounds.
- Scala is updated to 3.8.4.
- `pgaNdCodeGen` joined the root aggregate, so `sbt test` covers its tests too. It stays
  unpublished and depends only on `ga`/`symbolic` - broken generated code cannot break the
  generator, which must always be able to regenerate it (see pgaNdCodeGen/README.md).
- `cpp/cmake-build-debug/` is ignored by git.
- The code generator now has single sources of truth shared by the Scala and C++ backends:
  class field structure (`PgaSubclassFields`), norm and axis derivations (`NormSymbolics`,
  `AxesSymbolics`) and all hand-written numerical formulas (`SharedFormulas` rendered by
  `FormulaTemplate`) - a numerical fix can no longer land in one backend and be forgotten in
  the other.
- The generator package `codegen.scala` is renamed to `codegen.scalagen` (no more shadowing of
  the root `scala` package); CI compiles the generator and fails on generated-code drift
  (`runCodeGenCheck`).
- `Seq(...)` literals are created as `ArraySeq(...)` across the codebase (array-backed instead
  of the default linked list); `Seq` remains the interface type.
