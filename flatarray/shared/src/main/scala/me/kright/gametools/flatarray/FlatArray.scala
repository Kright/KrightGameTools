package me.kright.gametools.flatarray

import scala.collection.{EvidenceIterableFactory, mutable}

/**
 * A fixed-size, compact [[FlatMutableView]]: `array.length == size * FlatDoubleSerializer.getSize[T]`,
 * no spare capacity.
 */
final class FlatArray[T](val array: Array[Double], val size: Int) extends FlatMutableView[T]


object FlatArray extends EvidenceIterableFactory[FlatArray, FlatDoubleSerializer]:
  /** allocates a fresh, zero-initialized `FlatArray` with room for exactly `n` elements. */
  def apply[T](n: Int)(using serializer: FlatDoubleSerializer[T]): FlatArray[T] =
    new FlatArray[T](new Array[Double](n * serializer.size), n)

  /** copies the elements of `view` into a fresh, compact `FlatArray`. */
  def from[T](view: FlatView[T])(using serializer: FlatDoubleSerializer[T]): FlatArray[T] = {
    val n = view.size
    val stride = serializer.size
    val result = new FlatArray[T](new Array[Double](n * stride), n)
    System.arraycopy(view.array, 0, result.array, 0, n * stride)
    result
  }

  /**
   * cold-path collections interop: builds a compact `FlatArray` from any `IterableOnce`, preallocating
   * exactly when `source.knownSize` is known upfront. This is what makes `someIterable.to(FlatArray)` work
   * via the stdlib `EvidenceIterableFactory.toFactory` implicit.
   */
  override def from[T](source: IterableOnce[T])(using FlatDoubleSerializer[T]): FlatArray[T] = {
    val knownSize = source.knownSize
    val buffer = if (knownSize >= 0) FlatBuffer[T](knownSize) else FlatBuffer[T]()
    buffer ++= source
    from(buffer: FlatView[T])
  }

  override def empty[T](using FlatDoubleSerializer[T]): FlatArray[T] =
    FlatArray[T](0)

  override def newBuilder[T](using serializer: FlatDoubleSerializer[T]): mutable.Builder[T, FlatArray[T]] =
    new mutable.Builder[T, FlatArray[T]]:
      private val buffer = FlatBuffer[T]()

      override def sizeHint(size: Int): Unit = buffer.sizeHint(size)

      def addOne(elem: T): this.type = {
        // `T` is abstract here, so the inline, macro-based `FlatBuffer.+=` cannot be used (same restriction
        // as `FlatBuffer`'s own cold `grow`, see the module README). Pre-grow with the buffer's own
        // amortized policy so the single-element `++=` below (which would otherwise reallocate to exactly
        // `size + 1` on every call) finds capacity already sufficient and becomes a no-op reallocation-wise.
        val stride = serializer.size
        if ((buffer.size + 1) * stride > buffer.array.length) {
          buffer.ensureCapacity((buffer.size + 1) * 3 / 2 + 8)
        }
        buffer ++= Iterator.single(elem)
        this
      }

      def clear(): Unit = buffer.clear()

      def result(): FlatArray[T] = FlatArray.from(buffer: FlatView[T])
