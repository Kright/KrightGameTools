package me.kright.gametools.flatarray

import me.kright.gametools.mathutil.FastRange

/** A `FlatView[T]` that also allows overwriting elements in place. */
trait FlatMutableView[T] extends FlatView[T]


object FlatMutableView:
  extension [T](view: FlatMutableView[T]) {
    /** user-facing element update; bounds-checked against `size`, enables `view(i) = value` syntax. */
    inline def update(i: Int, value: T): Unit = {
      if (i < 0 || i >= view.size) {
        throw new IndexOutOfBoundsException(s"index $i is out of bounds for FlatMutableView of size ${view.size}")
      }
      FlatDoubleSerializer.write[T](value, view.array, i * FlatDoubleSerializer.getSize[T])
    }

    inline def mapInPlace(inline f: T => T): Unit = {
      val a = view.array
      val n = view.size
      val stride = FlatDoubleSerializer.getSize[T]
      for (i <- FastRange(n)) {
        val off = i * stride
        val elem = FlatDoubleSerializer.read[T](a, off)
        FlatDoubleSerializer.write[T](f(elem), a, off)
      }
    }

    inline def fill(value: T): Unit = {
      val a = view.array
      val n = view.size
      val stride = FlatDoubleSerializer.getSize[T]
      for (i <- FastRange(n)) {
        FlatDoubleSerializer.write[T](value, a, i * stride)
      }
    }

    /** copies all elements from `other` into this view; both must have the same `size`. */
    inline def :=(other: FlatView[T]): Unit = {
      if (other.size != view.size) {
        throw new IllegalArgumentException(
          s"cannot copy FlatView of size ${other.size} into FlatMutableView of size ${view.size}: sizes must match")
      }
      val stride = FlatDoubleSerializer.getSize[T]
      System.arraycopy(other.array, 0, view.array, 0, view.size * stride)
    }
  }
