# Changelog

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed (breaking)

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

### Changed

- `Pga3dTriangle.getNearestPoint` is rewritten via Voronoi regions (Ericson, "Real-Time
  Collision Detection", 5.1.5): no intermediate collections, no square roots, 4-8x faster
  (JMH: 77 -> 9 ns far from the triangle, 64 -> 12 ns near the surface, 53 -> 12 ns for
  points projecting inside; see `benchmark/NearestPointBenchmark`). Degenerate triangles now
  deterministically fall back to the nearest point of the longest edge (the old implementation
  could return NaN or an arbitrary point of the supporting plane for collinear vertices).
  The legacy implementation is kept in `Pga3dTriangleNearestPointTest` as a correctness
  reference.

### Added

- `Pga3dTriangle.fartherThan(p, maxDistance)`: conservative early reject against the triangle's
  bounding box - a few comparisons, no multiplications and no allocations (4.4 vs 6.7 ns for
  the allocating `toAABB.contains` equivalent). A prefilter before `getNearestPoint` when
  scanning many triangles; `true` is reliable for any input including degenerate triangles,
  NaN or infinite arguments.
- `distanceSquareTo` on `Pga3dTriangle` (point), `Pga3dEdge` (point and edge-edge) and
  `Pga3dAABB` (point): sqrt-free companions of `distanceTo` for comparisons, symmetric with
  `norm`/`normSquare`. `distanceTo` delegates to them; `contains(p, eps)` compares squares
  (one sqrt less on the `intersection` path) and is guarded against negative/NaN eps.
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

### Fixed

- Zero-length (degenerate) edges no longer produce NaN: `Pga3dEdge.getNearestPoint` had 0/0
  in the interpolation factor, `getNearestPoints` divided 0/0 for a pair of zero-length edges.
  A degenerate edge now behaves as a single point in all distance queries.
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
