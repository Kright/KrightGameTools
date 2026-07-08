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

### FlatDoubleSerializer and FlatDoubleView

`FlatDoubleSerializer[T]` is a typeclass for reading and writing an object as a flat run of `Double`s in an
`Array[Double]` at a given offset. `derived` uses a macro that inspects a case class whose fields are all `Double`
and generates the `read`/`write`/`size` code at compile time:

```scala
case class Vec3(x: Double, y: Double, z: Double) derives FlatDoubleSerializer
```

`FlatDoubleView[T]` builds on it: a mutable `IndexedSeq[T]` view over an `Array[Double]`, so a packed double buffer
can be used as a sequence of `T` (with `apply`/`update`, `slice`, `:+`, a builder, etc.) without per-element boxing.

### Precision helpers

* `EqualityEps` — a wrapper around an epsilon value, passed implicitly.
* `IEqualsWithEps[T]` — a typeclass for approximate equality; provides `a === b` given an implicit `EqualityEps`,
  used throughout the tests to compare floating-point results.

### MathUtil

Assorted numeric helpers:

* constants `Pi`, `Tau` (2*Pi) and `TauDiv` (1/Tau) as inline vals;
* `isEquals(arr1, arr2, eps)` for element-wise array comparison;
* `pow(x, power, mult)` — generic fast exponentiation by squaring;
* `interpolate(a, b, t)` — linear interpolation;
* `Double` extensions `clamp(lower, upper)`, `sign`, `square`;
* `Array[T].swap(i, j)`.

### Other

* `Sign` — a three-valued (`Negative` / `Zero` / `Positive`) sign type with multiplication, negation and `power`.
* `AbsoluteRotationTracker` — accumulates a continuous rotation angle across `2*Pi` wraparound (assuming less than
  half a turn between updates), useful for tracking absolute orientation over time.
