package me.kright.gametools.flatarray

/**
 * A growable [[FlatMutableView]], analogous to `scala.collection.mutable.ArrayBuffer` but storing `T`
 * elements packed as `Double`s instead of boxed references.
 *
 * Contract: single-threaded use only, no synchronization is performed.
 *
 * `array` is a `def`, re-read on every access, not a stored `val`: any `FlatView`/`FlatMutableView` handle
 * that merely wraps `this` (or code that calls `buffer.array` fresh each time, as all the inline extension
 * methods in this module do) keeps observing valid data across a `grow` reallocation, because it always
 * re-reads the current backing array through `def array` rather than holding a stale reference. This is a
 * deliberate design decision: it lets other views stay usable across growth without any invalidation
 * protocol, as long as they always go through `def array`/`def size` rather than caching the array
 * reference themselves.
 */
final class FlatBuffer[T] private (private var backing: Array[Double], private var count: Int) extends FlatMutableView[T]:
  def array: Array[Double] = backing

  def size: Int = count

  def clear(): Unit = {
    count = 0
  }

  /** grows the backing array, if needed, to hold at least `nElements` without changing `size`. */
  def ensureCapacity(nElements: Int)(using serializer: FlatDoubleSerializer[T]): Unit = {
    val stride = serializer.size
    if (nElements * stride > backing.length) {
      reallocate(nElements, stride)
    }
  }

  /** alias for [[ensureCapacity]]. */
  def sizeHint(nElements: Int)(using serializer: FlatDoubleSerializer[T]): Unit =
    ensureCapacity(nElements)

  /**
   * reallocates the backing array to hold exactly `nElements * stride` doubles and copies the live prefix
   * (`count * stride` doubles) over. Only reallocates - never writes a new value, never changes `count`.
   */
  private def reallocate(nElements: Int, stride: Int): Unit = {
    val newBacking = new Array[Double](nElements * stride)
    System.arraycopy(backing, 0, newBacking, 0, count * stride)
    backing = newBacking
  }

  /** cold path: reallocates (growth policy: `(count+1)*3/2 + 8` elements), writing nothing. */
  private def grow(stride: Int): Unit = {
    val newCapacityElements = (count + 1) * 3 / 2 + 8
    reallocate(newCapacityElements, stride)
  }

  /** inline fast path: grows (if needed) then unconditionally writes `value` at the new slot. */
  inline def addOne(value: T): this.type = {
    val stride = FlatDoubleSerializer.getSize[T]
    if ((count + 1) * stride > backing.length) {
      grow(stride)
    }
    FlatDoubleSerializer.write[T](value, backing, count * stride)
    count += 1
    this
  }

  inline def +=(value: T): this.type = addOne(value)

  /**
   * fast path: appends all elements of `view` in one `System.arraycopy`, since both share the same flat
   * layout (no per-element codec). Self-append aliasing (`this eq view`, seen through a wrapping `FlatView`)
   * is out of scope.
   */
  inline def ++=(view: FlatView[T]): this.type = {
    val stride = FlatDoubleSerializer.getSize[T]
    val n = view.size
    if ((count + n) * stride > backing.length) {
      reallocate(count + n, stride)
    }
    System.arraycopy(view.array, 0, backing, count * stride, n * stride)
    count += n
    this
  }

  /** cold path: appends every element of `xs`, pre-growing once if `xs.knownSize` is known. */
  def ++=(xs: IterableOnce[T])(using serializer: FlatDoubleSerializer[T]): this.type = {
    val stride = serializer.size
    val knownSize = xs.knownSize
    if (knownSize >= 0) {
      ensureCapacity(count + knownSize)
    }
    val it = xs.iterator
    while (it.hasNext) {
      val elem = it.next()
      if ((count + 1) * stride > backing.length) {
        grow(stride)
      }
      serializer.write(elem, backing, count * stride)
      count += 1
    }
    this
  }


object FlatBuffer:
  /** allocates a fresh, empty `FlatBuffer` with room for `sizeHint` elements. */
  def apply[T](sizeHint: Int = 8)(using serializer: FlatDoubleSerializer[T]): FlatBuffer[T] =
    new FlatBuffer[T](new Array[Double](sizeHint * serializer.size), 0)
