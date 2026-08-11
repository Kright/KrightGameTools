# Changelog

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed (breaking)

- Queries that may find nothing return `T | Null` instead of `Option[T]`: `intersection` on
  triangles and AABBs, `deepestContact` on sphere/capsule. No allocation on the BVH/raycast hot
  paths; the library is compiled with `-Yexplicit-nulls`, so the nullability stays in the type.
- Renames for one-name-per-concept consistency:
  - `hasIntersection` -> `intersects` (sphere, circle, rays vs AABB);
  - `dist` / `distSquare` -> `distance` / `distanceSquare` on the nearest-point accumulators;
  - `Pga3dQuaternion` alias removed - the class is `Pga3dRotor`; C++ `Quaternion` -> `Rotor`;
  - `PlaneIdeal` / `LineIdeal` -> `PlaneCentral` / `LineCentral` ("ideal" is reserved for the
    at-infinity part);
  - `Rotor.restore(axes...)` -> `fromAxes`; `log` / `exp` / `split` are parameterless without
    parentheses; the parameter of the generated binary operations is `r` (was `v`);
  - `VectorNd.^` -> `pow` (`^` is the wedge product everywhere else, and its precedence is a trap).
- pga2d types point-valued results as points: 39 generated methods return
  `Pga2dProjectivePoint` instead of `Pga2dRotor` (rotor sandwiches of the point family,
  `PointCenter * scalar`, projections onto central lines, `bulk` of points). Genuine rotors
  carry a scalar part and keep resolving to `Pga2dRotor`.
- Physics:
  - `Pga3dPhysicsSolver` loses its dead type parameter: `step(dynamicBodies: Array[Pga3dPhysicsBody], ...)`;
  - `Pga3dPhysicsBody` stores its pose as a `Pga3dTransform` (`var transform`; motor accessors
    kept, assigning renormalizes); `Pga3dInertiaMovedLocal` caches a transform too - its case
    fields and serialized layout stay motor + inertia. `Pga3dMotorWithMatrix` and
    `Pga3dMatrixForPoints` are replaced by the transform;
  - `Pga3dPhysicsSolverVerletConstrained.step` takes an optional `localBs` output - the honest
    node twists of the consumed poses (observers no longer pay a second `reconstructNode` with
    its force callback); `reconstructNode` writes into a caller array and returns the forques;
  - `Pga3dInertiaPrecomputed` is removed (and `toPrecomputed` with it): the summable's
    closed-form block apply and invert are ~3x faster than its 6x6 matrices and not less
    precise, so `toFastestRepresentation` returns `toSummable` (the diagonal forms still
    return themselves).
- Geometry:
  - `Pga3dCylinder` joins the capsule's shape protocol: `fromCenter`, `center` / `halfAxis` /
    `edge` (replacing the ad-hoc `ab` / `abNormalized` / `halfAb`), `map`, an exact `toAABB` +
    `Pga3dAABB(cylinder)`, sandwich extensions; `expand(dr)` grows the radius only (like sphere
    and capsule), lengthening is `expandAxis(d)` (replacing `expand`/`expandAB`/`expandR`);
  - `Pga3dRay` / `Pga2dRay`: `directionReciprocal` is a body val, not a case field -
    `copy(direction = ...)` can no longer desync it, and only origin + direction are
    serialized (6 doubles in 3d, 4 in 2d) and compared;
  - `AABB.intersects(triangle)` lost its muddled `eps` parameter - for a tolerance, expand the
    box once outside the loop: `aabb.expand(eps).intersects(triangle)`;
  - the nearest-point accumulators' `update` returns whether the candidate won, and is NaN-safe
    in both directions;
  - `getNearestPointsBinSearch` is removed (kept in test sources as a precision reference).
- matrix: the `Matrix` class is removed - and the name with it. Any `ArrayView2d[Double]` is a
  matrix via extension methods (import `me.kright.gametools.matrix.*`), so lazy views compose:
  `a * b.transposed` multiplies without a copy; new-matrix results are `ArrayView2dFlat[Double]`.
  Factories: `matrixFromValues(h, w)(values*)`, `identityMatrix(n)`, plain `ArrayView2dFlat[Double](h, w)`.
  Renames: `det()`/`inverted()` -> `determinant`/`inverted`, `setIdt()` -> `setIdentity()`;
  dropped as duplicates of the view API: `copy()`, `transposedCopy()`, `setZero()`, `isEquals`
  (use `equalsWithEps`), plus the dead `SquaredMatrix` and `Matrix2d/3d/4d.zero`/`id`.
- The symmetric eigendecomposition is `SymmetricMatrixDiagonalization.eigen(m): Eigen(diagonal, vectors)`
  with the eigenvalues as a 1d view (`diagonalMatrix(values)` builds the square form back); the
  old tuple-returning names and the Jacobi internals are gone from the public API.
  `Pga3dInertiaSummable` reuses the shared Jacobi loop through a rotor-accumulating callback.
- mathutil: `IEqualsWithEps` and `EqualityEps` are removed in favor of the `CanEqualWithEps`
  typeclass - pass eps explicitly: `a.equalsWithEps(b, eps)`.
- Numeric behavior: 2d `projectOntoLine` returns the positive-w representative (matching 3d);
  `normalizedByBulk`/`normalizedByNorm` of constant-norm classes return the class itself;
  `rotation(from, to)` switches to the exact-wedge branch at `dot <= -0.9`; the generated sums
  of 4+ terms are grouped in pairs and `cross` groups its mirrored summands (last-bit changes,
  see below). `gametools-pga3d` depends on `gametools-mathutil` at compile scope.

### Added

- `Pga3dTransform` / `Pga2dTransform` and `Pga3dProjectiveTransform` / `Pga2dProjectiveTransform`:
  a motor cached as precomputed sandwich matrices - build once, then every application is a
  plain matrix multiplication (3-6x faster than `motor.sandwich`, see `Pga3dTransformBenchmark`).
  The plain `Transform` requires a normalized motor (`apply` renormalizes, `fromNormalized`
  trusts the caller) and narrows the result types: a sandwich of a point is a point, of a
  translator - a translator. The `ProjectiveTransform` works for any motor and returns
  projective results scaled by `normSquare`. Generated symbolically like the rest of pga2d/pga3d.
- `dexp` / `dexpInv` on the grade-2 generator classes (2d and 3d): the closed-form left Jacobian
  of SE(2)/SE(3) and its inverse, accurate to a few ulps for every argument; slow series
  references live in the `ga` module and property-test the closed forms.
- Physics solvers:
  - `Pga3dPhysicsSolverVerlet`: position Verlet on the motor group - 2nd order, one force
    evaluation per step, no stored velocities (the state is two caller-owned pose arrays);
  - `Pga3dPhysicsSolverVerletConstrained`: RATTLE on the motor group - hard distance constraints
    inside the step, 2nd order, constraints may change between steps (contact-like usage);
  - `Pga3dDistanceConstraint` (rod / rope / strut), `Pga3dConstraintResolver` and
    `Pga3dPhysicsSolverConstrained(inner, resolver)` to add hard constraints to the RK-family
    solvers - constrained RK4 keeps the 4th order, rods hold to ~1e-15;
  - `Pga3dPhysicsSolverRKF45`: fixed-step Fehlberg 4(5) with an essentially exact per-body
    local-error estimate for mapping where the error lives;
  - `Pga3dPhysicsSolverRKMK4`: Runge-Kutta-Munthe-Kaas 4th order - integrates on the bivector
    Lie algebra via `dexpInv`, every stage is an exact motor by construction;
  - `benchmark/PhysicsSolverBenchmark`: per-step overhead of every solver (the numbers are cited
    in pga3dphysics/Solvers.md).
- Geometry:
  - `Pga3dCapsule` / `Pga2dCapsule` with `intersects(sphere/capsule/triangle)` and the 3d
    `deepestContact(triangle): Pga3dContact | Null` (`Pga3dContact(point, normal, depth)` is new);
  - sphere/circle collision queries: `intersects(triangle)`, 3d `deepestContact(triangle)`;
  - segment-triangle nearest pair: `Triangle.getNearestPoints(edge)` / `distanceSquareTo(edge)`;
  - `Pga3dEdge.intersects(other, eps)` (the missing 3d twin), `distanceSquareTo` companions of
    every `distanceTo`, `Triangle.fartherThan(p, maxDistance)` early reject, and a
    `Triangle.intersection(edge, cachedPlane, eps)` overload for precomputed static geometry.
- pga core: `Pga2dProjectivePoint.split` (the 2d sibling of `Pga3dBivector.split`), `pow(t)` on
  motors/rotors/translators, `Pga2dRotor.log`/`exp` and the axes accessors, `Rotor.zero`,
  point projections onto central hyperplanes, `Pga2dMatrix`,
  `Pga3dVector.crossRightHanded`/`crossLeftHanded`.
- Infrastructure: the derivable `CanEqualWithEps` typeclass (componentwise eps-equality, derived
  by every pga/geometry/physics class and the vectors); `FlatDoubleSerializer` derivation
  recurses into nested case classes; `mathutil.ExactArith` (`fma`, `diffOfProducts`).

### Changed

- Performance of the geometry hot paths, each with a JMH benchmark and the legacy implementation
  kept as a test reference:
  - `Triangle.getNearestPoint` rewritten via Voronoi regions - 4-11x faster, degenerate
    triangles fall back deterministically;
  - `AABB.intersects(triangle)` rewritten via SAT - 13x on the hard-miss path, allocation- and
    sqrt-free, exact for degenerate input;
  - `Triangle.intersection(edge)` early reject without AABB construction and without
    normalizing the direction.
- Performance of the physics solvers: poses are applied through cached transforms
  (`Pga3dPhysicsBody`, `Pga3dInertiaMovedLocal`, the VerletConstrained sweeps,
  `motor.sandwich(summable)`) - the pure solver overhead dropped 1.2-1.3x;
  `RKMK4` uses the honest closed-form `dexpInv` instead of a truncated series.
- `Pga3dInertiaSummable.invert` is solved in closed form on the 3x3 blocks of the spatial
  inertia (the public `Pga3dInertiaSummableInverse`, cached lazily: transport the momentum to
  the center of mass, apply the inverse of the inertia tensor about it, restore the velocity)
  instead of the moved-local route with its eigendecomposition. After the first call it costs
  the same as `apply`, and both are the fastest of all the representations
  (`InertiaBenchmark`); the apply-invert round trip is tight (~1e-13 relative).
- Numerics: the generated code groups long sums in parenthesized pairs (halves the dependency
  chains, ~10% on 6-term rows; results change in the last bits), `cross` groups mirrored
  summands (`u.cross(u)` is exactly zero, `a.cross(b) == -b.cross(a)` bit-exact), and the
  cancellation-prone dexp coefficients use wide polynomial windows (~4x cheaper than the
  trigonometric forms).

### Fixed

- `Pga3dCylinder.intersects(edge)` returned `true` for any edge whose axis projection partially
  overlapped the axis range (the final distance check was vacuous, which also hid two wrong
  clamping formulas). Now clamped to the exact cap-plane crossings and tested.
- The two-sided distance constraint (0 < min < max) in `Pga3dPhysicsSolverVerletConstrained`
  was never clamped: within a step an engaged lambda could flip to the opposite bound instead
  of releasing, and the Gauss-Seidel sweeps fought each other, leaving a distorted pose. A
  two-sided lambda now holds its engaged bound and releases through zero.
- The Jacobi eigendecomposition hung forever on a NaN input (and hit `0/0` on an all-zero
  submatrix): the loop got an internal rotation cap far above the measured convergence and
  returns best-effort - a NaN input yields a visible NaN result.
- The published Scala.js `gametools-matrix` could not link: `Math.fma` does not exist in the
  Scala.js javalib (only the linker catches this) - replaced with `ExactArith.fma`. Related:
  scalatest is wired with `%%%`, `sbt test` stays JVM-only (no Node needed, JS test code is
  compiled but not executed), and CI links the JS test code (`matrixJS/Test/fastLinkJS`) to
  catch missing-javalib references.
- `MathUtil.isEquals` did not compare the array lengths (a longer second array could compare
  equal); the `MathUtil.sign` extension silently shadowed `Double.sign` with different NaN
  semantics and is removed.
- Precision and degenerate cases: zero-length edges no longer produce NaN in distance queries;
  `rotation(from, to)` is accurate to ~1e-15 down to exactly antipodal inputs (2d, 3d, C++);
  the small-angle Taylor branches of `exp`/`log` had wrong second-order coefficients; C++
  `Motor::log` cancelled catastrophically for small angles; `Pga3dForque.getTorqueAroundCenter`
  was correct only for axis-aligned forces.

### Internal

- Scala 3.8.4.
- The code generator has single sources of truth shared by the Scala and C++ backends (field
  structure, norm/axis derivations, hand-written formulas); the package is `codegen.scalagen`;
  CI fails on generated-code drift. `pgaNdCodeGen` joined the root aggregate (still unpublished
  and independent of the generated modules).
- `Pga3dInertiaPrecisionTest` pins the accuracy of the moved and the summable inertia as a
  function of the center-of-mass offset against a BigDecimal reference (the moved form loses
  ~1e-16 * R, the summable ~1e-16 * R^2 on invert, energy and acceleration).
- `Seq(...)` literals are `ArraySeq(...)` across the codebase; `cpp/cmake-build-debug/` is
  git-ignored.
