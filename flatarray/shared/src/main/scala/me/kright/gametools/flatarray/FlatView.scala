package me.kright.gametools.flatarray

import me.kright.gametools.mathutil.FastRange

/**
 * A read-only view over a run of `T` elements packed as `Double`s into a flat `Array[Double]`,
 * with no padding: elements 0 .. size-1 are stored consecutively starting at array index 0, each
 * occupying `FlatDoubleSerializer.getSize[T]` doubles.
 *
 * There is no `offset` field: zero-copy slices (viewing a sub-range of a larger backing array) are
 * deliberately deferred to a future version, see the module README.
 *
 * All hot-path operations are `inline def`s defined as extension methods in the companion object,
 * calling the `FlatDoubleSerializer` macros (`getSize`/`read`/`write`) directly instead of going through
 * the virtual `FlatDoubleSerializer[T]` typeclass, so that after inlining a call site is left with raw
 * array reads/writes and `new T(...)` construction only - friendly to JVM escape analysis. See the module
 * README for the full rationale.
 */
trait FlatView[T]:
  /** backing array; may be larger than `size * stride` doubles */
  def array: Array[Double]

  /** number of `T` elements stored in this view (NOT the length of `array`) */
  def size: Int


object FlatView:
  extension [T](view: FlatView[T]) {
    /** user-facing element access; bounds-checked against `size` (the backing array capacity may exceed
     * `size * stride`, so relying on the raw array bounds check is not enough). */
    inline def apply(i: Int): T = {
      if (i < 0 || i >= view.size) {
        throw new IndexOutOfBoundsException(s"index $i is out of bounds for FlatView of size ${view.size}")
      }
      FlatDoubleSerializer.read[T](view.array, i * FlatDoubleSerializer.getSize[T])
    }

    inline def foreach(inline f: T => Unit): Unit = {
      val a = view.array
      val n = view.size
      val stride = FlatDoubleSerializer.getSize[T]
      for (i <- FastRange(n)) {
        f(FlatDoubleSerializer.read[T](a, i * stride))
      }
    }

    inline def foreachWithIndex(inline f: (T, Int) => Unit): Unit = {
      val a = view.array
      val n = view.size
      val stride = FlatDoubleSerializer.getSize[T]
      for (i <- FastRange(n)) {
        f(FlatDoubleSerializer.read[T](a, i * stride), i)
      }
    }

    /**
     * Reads every `T` from this view and writes the mapped `U` into `dst`, which must have the same
     * `size` as this view. `dst` MAY alias a source (e.g. `this eq dst` when `T =:= U`) because each
     * element is fully read before the corresponding element is written.
     */
    inline def mapTo[U](dst: FlatMutableView[U])(inline f: T => U): Unit = {
      val a = view.array
      val n = view.size
      val srcStride = FlatDoubleSerializer.getSize[T]
      val dstArray = dst.array
      val dstStride = FlatDoubleSerializer.getSize[U]
      for (i <- FastRange(n)) {
        val elem = FlatDoubleSerializer.read[T](a, i * srcStride)
        FlatDoubleSerializer.write[U](f(elem), dstArray, i * dstStride)
      }
    }

    /**
     * Reads every `T` from this view and every `U` from `b` (both must have `size` equal to this view's
     * size), combines them with `f`, and writes the resulting `R` into `dst`. `dst` MAY alias `this` or
     * `b` because each pair is fully read before the corresponding result is written.
     */
    inline def zipTo[U, R](b: FlatView[U], dst: FlatMutableView[R])(inline f: (T, U) => R): Unit = {
      val a = view.array
      val n = view.size
      val aStride = FlatDoubleSerializer.getSize[T]
      val bArray = b.array
      val bStride = FlatDoubleSerializer.getSize[U]
      val dstArray = dst.array
      val dstStride = FlatDoubleSerializer.getSize[R]
      for (i <- FastRange(n)) {
        val elemA = FlatDoubleSerializer.read[T](a, i * aStride)
        val elemB = FlatDoubleSerializer.read[U](bArray, i * bStride)
        FlatDoubleSerializer.write[R](f(elemA, elemB), dstArray, i * dstStride)
      }
    }

    /** cold-path interop: materializes all elements into a boxed `Array[T]` using the virtual typeclass */
    def toArray(using serializer: FlatDoubleSerializer[T]): Array[T] = {
      val a = view.array
      val n = view.size
      val stride = serializer.size
      val result = new Array[T](n)(using serializer.classTag)
      for (i <- FastRange(n)) {
        result(i) = serializer.read(a, i * stride)
      }
      result
    }

    /** cold-path interop: an `Iterator[T]` over this view, reading each element through the virtual
     * typeclass on demand. */
    def iterator(using serializer: FlatDoubleSerializer[T]): Iterator[T] = new Iterator[T] {
      private val a = view.array
      private val n = view.size
      private val stride = serializer.size
      private var i = 0

      def hasNext: Boolean = i < n

      def next(): T = {
        val elem = serializer.read(a, i * stride)
        i += 1
        elem
      }
    }

    /** cold-path interop: materializes this view into any standard-library collection built by `factory`,
     * e.g. `view.to(IndexedSeq)` or `view.to(Vector)`. */
    def to[C1](factory: scala.collection.Factory[T, C1])(using FlatDoubleSerializer[T]): C1 =
      factory.fromSpecific(view.iterator)
  }
