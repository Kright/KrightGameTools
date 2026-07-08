# PGA concepts shared by pga2d and pga3d

The [pga2d](pga2d/README.md) and [pga3d](pga3d/README.md) modules implement plane-based geometric
algebra (PGA) for 2 and 3 dimensions with the same code generators, the same conventions and the
same terminology. This page describes the dimension-independent ideas once; the module READMEs list
the concrete classes and code examples.

This is a summary of the library's conventions, not a PGA textbook.
For the theory behind PGA see [https://bivector.net](https://bivector.net).

## Specialized classes and the narrowest result type

A general multivector has one coefficient per basis blade (8 in 2d, 16 in 3d), but useful geometric
objects use only a few of them: a 3d point has 3 variable fields, a quaternion 4. The code
generators evaluate every operation symbolically for every pair of classes and emit a dedicated
method whose result is the *narrowest* generated class that can represent all possibly non-zero
components of the result.

Consequences:

* Operations on specialized classes are as fast as hand-written vector math: no wasted fields,
  no branching.
* The result type is known at compile time: the dot product of two bivectors is a `Double`,
  the geometric product of two planes is a motor, the sum of two vectors is a vector.
* If an operation is identically zero for a pair of types, the method simply does not exist.

The full mapping (operation, left type, right type) -> result type is generated next to each module:
[pga3d/operations.md](pga3d/operations.md) and [pga2d/operations.md](pga2d/operations.md).

## Immutability

All classes are immutable. Modern JVMs are good at JIT and escape analysis, so short-lived small
objects are cheap. Geometric algebra is complex enough by itself; not having to think about
mutation makes coding and debugging much easier.

## The products

* **geometric** — the fundamental product of the algebra. It composes transformations: the
  geometric product of two planes (lines in 2d) is a rotation around their intersection by twice
  the angle between them, the product of a translator and a quaternion/rotor is a motor.
* **dot** — the inner product. It contracts grades and measures the metric relation between
  elements; the dot product of an element with itself gives its norm squared.
* **wedge** (aliases `^`, `meet`) — the outer product. Geometrically it intersects elements and
  *contracts* degrees of freedom: two planes meet in a line, three planes meet in a point.
* **antiWedge** (aliases `v`, `join`) — the dual of the wedge. It spans elements and *expands*
  degrees of freedom: two points join into a line, a point and a vector join into a line,
  three points join into a plane.
* **sandwich** / **reverseSandwich** — how transformations are applied: `a.sandwich(b)`
  transforms `b` by `a` (conjugation `a * b * a.reverse`). Quaternions/rotors rotate, translators
  translate, motors move rigidly, and a plane or line reflects. `a.reverseSandwich(b)` applies the
  inverse transformation.
* **antiGeometric**, **antiDot** — the duals of the geometric and dot products (the same
  operation performed on the duals of the arguments).
* **cross** — the commutator product `(a * b - b * a) / 2`, useful for rates of change in physics.

## Bulk and weight

Every class splits into two parts with respect to the degenerate generator `w` (the one with
`w * w = 0`):

* **bulk** — the components that do *not* contain `w`. They describe the element's relation to
  the origin (e.g. the rotational part of a bivector).
* **weight** — the components that contain `w`. They describe the ideal ("at infinity") part
  (e.g. the translational part of a bivector, or the offset of a plane from the origin).

Each class provides `bulk`, `weight`, and the corresponding norms `bulkNorm`, `weightNorm`
and `norm` with `normalizedBy...` helpers.

## Duality

`dual` maps grade `k` to grade `n - k` and exchanges meet with join, bulk with weight.
Points are stored in *dual representation*: the fields of `Pga3dPoint` are named x, y, z even
though a point is a grade-3 element (grade-2 in 2d). This keeps the human-friendly view — a point
is just its coordinates — while the algebra works on the underlying blades. The class scaladoc
of every generated class lists the exact basis blade behind every field.

## exp and log

Grade-2 elements are the "generators" of rigid motion: the exponent of a grade-2 element is a
finite motion, and `log()` recovers it back. Scaling the argument scales the motion, which makes
`exp`/`log` the tool for interpolation and for integrating velocities in physics
(`exp(t: Double)` overloads are generated for this).

| generator (grade 2)      | exp -> motion       | 3d/2d |
|--------------------------|---------------------|-------|
| `Pga3dBivector`          | `Pga3dMotor`        | 3d    |
| `Pga3dBivectorBulk`      | `Pga3dRotor`        | 3d    |
| `Pga3dBivectorWeight`    | `Pga3dTranslator`   | 3d    |
| `Pga2dProjectivePoint`   | `Pga2dMotor`        | 2d    |
| `Pga2dVector`            | `Pga2dTranslator`   | 2d    |

In 2d the grade-2 elements *are* the (projective) points: the exponent of a point is a rotation
around that point, and the exponent of an ideal point (a vector) is a translation.

`Pga3dRotor`/`Pga3dMotor` and `Pga2dRotor`/`Pga2dMotor` also expose `slerp`/`nlerp` for interpolation:
`slerp` follows the exact geodesic via `log`/`exp` (constant angular velocity), while `nlerp` is a
cheaper renormalized-lerp approximation of it.

## Normalization and homogeneous coordinates

There is deliberately no "normalized" type in this library, and operations do not renormalize their results.

The product of two normalized versors is not exactly normalized — floating-point rounding creeps in — so a type
that promised "I am normalized" would be lying after the very first multiplication. The honest options are either to
run a `Math.sqrt` after every operation (expensive, and paid even when the caller doesn't care) or to not promise
normalization at all. I chose the latter: the library never forces a `sqrt` on you.

It also would not fit the design. The number of generated binary operations grows as N² in the number of classes,
and the flat list of classes is the whole point — adding `Normalized*` variants (or, say, a `NormalizedQuaternion`
inheriting `Quaternion`) would multiply that list and destroy the simplicity.

Because the algebra works in homogeneous coordinates, this costs nothing in correctness. `rotor.sandwich(point)` is
exactly correct on homogeneous coordinates and yields a homogeneous (projective) point. You then choose how to read
it out: `toPointUnsafe` if you already know the versor was normalized, or `toPoint` to renormalize (divide by the
weight) on the spot.

This is intentional: the basic primitives — `sandwich`, `reverse`, `renormalized`, `toPoint` / `toPointUnsafe` —
compose to cover the corner cases, so the library does not add method families for things like "treat this
unnormalized motor as if it were normalized". If you need a normalized value, normalize it yourself. It is the same
reasoning behind there being no `inverse` method: `reverse` plus an explicit renormalization is all you need.

## Naming conventions

* The scalar blade is named `s` and the pseudoscalar `i`. There is no scalar class — plain
  `Double` is used instead.
* A grade-1 element is a hyperplane: `Pga3dPlane` in 3d, `Pga2dLine` in 2d — otherwise the two
  modules use the same names (`Pga2dRotor` is the 2d sibling of `Pga3dRotor`). `Pga3dQuaternion`
  remains as a backward-compatible alias for `Pga3dRotor` (the older name for the same class).
* `toXxx` conversions are lossless widenings; `toXxxUnsafe` drop components that may be non-zero.
* The generated classes are never edited by hand: change the generator
  (pgaNdCodeGen) and re-run it.
