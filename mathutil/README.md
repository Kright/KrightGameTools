# Mathutil module

Small utility module with the low-level math helpers shared by the other modules.
No external dependencies; everything is implemented from scratch and cross-builds for JVM and Scala.js.

## Features

### FastRange

The standard Scala `Range` boxes int indexes into `Integer`, which hurts hot loops (measured ~x15 slowdown on
nested loops multiplying 4x4 matrices). `FastRange` is an inline replacement with no boxing. Because the loop body
is inlined, `return` from inside the loop also works.

`import FastRange.*` shadows the standard `until`/`to`, so ordinary-looking `for` loops become fast loops:

```scala
import me.kright.gametools.mathutil.FastRange.*

for (i <- 0 until n) {
  // no Integer boxing, body is inlined
}
```

There is also `FastRange.cfor` for the general C-style loop:

```scala
FastRange.cfor(0, _ < n, _ + 1) { i =>
  ...
}
```

### Precision helpers

* `CanEqualWithEps[T]` — a derivable typeclass for approximate equality with an explicit tolerance:
  `a.equalsWithEps(b, eps)` is true iff the Chebyshev (L-infinity) distance over all `Double` components is
  within `eps`. `derives CanEqualWithEps` works for case classes whose fields are `Double`s or, recursively,
  such case classes, and inlines to a flat `&&`-chain with early exit and no allocations; equal infinities
  compare equal, NaN never does. Derived by the generated pga3d/pga2d classes and the geometry case classes;
  `import CanEqualWithEps.given` provides the instance for plain `Double`.
* `ExactArith` — error-free arithmetic building blocks: `fma(a, b, c)` (the JVM intrinsic; a Dekker-based
  portable emulation on Scala.js) and `diffOfProducts(a, b, c, d)` for `a*b - c*d` via Kahan's algorithm,
  at most 2 ulp of relative error even under catastrophic cancellation.

### MathUtil

Assorted numeric helpers:

* constants `Pi`, `Tau` (2*Pi) and `TauDiv` (1/Tau) as inline vals;
* `isEquals(arr1, arr2, eps)` for element-wise array comparison;
* `pow(x, power, mult)` — generic fast exponentiation by squaring;
* `interpolate(a, b, t)` — linear interpolation;
* `minNanSafe(a, b)`, `maxNanSafe(a, b)` (and the 3-argument overloads) — like `math.min`/`math.max`, but NaN
  arguments are ignored instead of being propagated; used by the ray/AABB slab tests in pga3dgeom and pga2dgeom;
* `Double` extensions `clamp(lower, upper)`, `square`;
* `Array[T].swap(i, j)`.

### Other

* `Sign` — a three-valued (`Negative` / `Zero` / `Positive`) sign type with multiplication, negation and `power`.
* `AbsoluteRotationTracker` — accumulates a continuous rotation angle across `2*Pi` wraparound (assuming less than
  half a turn between updates), useful for tracking absolute orientation over time.
