# Flatarray module

Flat array-of-structs (AoS) storage for homogeneous, all-`Double` elements, packed into a single
`Array[Double]`.

1. **Goal.** Pack many small all-`Double` structs (points, vectors, quaternions, matrix rows, ...)
   contiguously into one `Array[Double]`, one element after another, with no gaps between them.
   This module deliberately does **not** do struct-of-arrays (SoA) layout, sub-ranges/offsets/
   strides, or padding - just plain flat AoS with a fixed per-element stride.
2. **Two containers.** `FlatArray[T]` is a fixed-size, compact container: exactly
   `size * stride` doubles, no spare capacity. `FlatBuffer[T]` is a growable buffer, analogous to
   `scala.collection.mutable.ArrayBuffer`, but storing `T` packed as `Double`s instead of boxed
   references.
3. **Not thread-safe.** `FlatBuffer` performs no synchronization around resizing (growing its
   backing array); use it from a single thread only.
4. **Inlined for escape analysis.** Element access (`apply`/`update`) and the `foreach`/`map`-style
   combinators (`mapTo`, `zipTo`, `mapInPlace`, `fill`) are `inline def`s that read/write the
   backing array directly, without going through a virtual typeclass call. That keeps the JIT able
   to scalar-replace the temporary `T` values in a hot loop instead of allocating them.
5. **Two view traits.** `FlatView[T]` is the minimal read-only interface (`array`, `size`, plus the
   read-side combinators above); `FlatMutableView[T] extends FlatView[T]` adds in-place writes -
   it does not support resizing, only `FlatBuffer` does.

Compared to a `scala.Array[T]` of boxed immutable case-class objects, this flat storage has radically
lower memory and GC pressure (zero per-element allocation, zero collections) and is several times
faster in hot loops - up to ~30x for scattered access under ZGC. See [performance.md](performance.md)
for the measurements, conditions, and how to reproduce them.

## Example

```scala
case class Vec3d(x: Double, y: Double, z: Double) derives FlatDoubleSerializer

import FlatView.*
import FlatMutableView.*

val a = FlatArray[Vec3d](3)
val b = FlatArray[Vec3d](3)
a(0) = Vec3d(1.0, 2.0, 3.0)
b(0) = Vec3d(0.5, 0.5, 0.5)

val buffer = FlatBuffer[Vec3d](sizeHint = 8)
buffer += Vec3d(4.0, 5.0, 6.0)
buffer ++= a

buffer.mapInPlace(v => Vec3d(v.x * 2, v.y * 2, v.z * 2))
a.zipTo(b, a)((x, y) => Vec3d(x.x + y.x, x.y + y.y, x.z + y.z))
val first: Vec3d = a(0)

// collections interop (cold path)
val plain: Array[Vec3d] = a.to(Array)
val backToFlat: FlatArray[Vec3d] = plain.to(FlatArray)
```
