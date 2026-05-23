package com.github.kright.math

import scala.collection.{EvidenceIterableFactory, mutable}
import scala.collection.mutable.Builder

/**
 * A mutable indexed view over an array of Doubles that treats it as a sequence of elements of type T.
 *
 * @param array      The underlying array of Doubles.
 * @param offset     The starting index in the array (in Doubles, not in elements).
 * @param length     The number of elements of type T in this view.
 * @param serializer The serializer for type T.
 */
final class FlatDoubleView[T](val array: Array[Double],
                              val offset: Int,
                              val length: Int)
                             (using val serializer: FlatDoubleSerializer[T])
  extends mutable.IndexedSeq[T]:

  def apply(index: Int): T =
    serializer.read(array, offset + index * serializer.size)

  def update(index: Int, value: T): Unit =
    serializer.write(value, array, offset + index * serializer.size)

  override def foreach[U](f: T => U): Unit =
    for (i <- FastRange(length)) {
      f(apply(i))
    }

  override def mapInPlace(f: T => T): this.type =
    for (i <- FastRange(length)) {
      update(i, f(apply(i)))
    }
    this

  override def slice(from: Int, until: Int): FlatDoubleView[T] =
    val lo = math.max(from, 0)
    val hi = math.min(until, length)
    new FlatDoubleView[T](array, offset + lo * serializer.size, math.max(hi - lo, 0))

  override def take(n: Int): FlatDoubleView[T] = slice(0, n)

  override def drop(n: Int): FlatDoubleView[T] = slice(n, length)

  override def takeRight(n: Int): FlatDoubleView[T] = slice(length - math.max(n, 0), length)

  override def dropRight(n: Int): FlatDoubleView[T] = slice(0, length - math.max(n, 0))

  def toArray: Array[T] =
    val result = serializer.classTag.newArray(length)
    for (i <- FastRange(length)) {
      result(i) = apply(i)
    }
    result

  def :+(value: T): FlatDoubleView[T] = {
    val requiredLength = offset + (length + 1) * serializer.size
    if (requiredLength <= array.length) {
      serializer.write(value, array, offset + length * serializer.size)
      new FlatDoubleView[T](array, offset, length + 1)
    } else {
      val newCapacity = (length + 1) * 3 / 2 + 8
      val newArray = new Array[Double](newCapacity * serializer.size)
      System.arraycopy(array, offset, newArray, 0, length * serializer.size)
      serializer.write(value, newArray, length * serializer.size)
      new FlatDoubleView[T](newArray, 0, length + 1)
    }
  }

  def toCompact: FlatDoubleView[T] =
    val newArray = new Array[Double](serializer.size * length)
    System.arraycopy(array, offset, newArray, 0, newArray.length)
    new FlatDoubleView[T](newArray, 0, length)

object FlatDoubleView extends EvidenceIterableFactory[FlatDoubleView, FlatDoubleSerializer]:

  def apply[T](array: Array[Double])(using serializer: FlatDoubleSerializer[T]): FlatDoubleView[T] =
    new FlatDoubleView(array, offset = 0, length = array.length / serializer.size)

  override def from[T](source: IterableOnce[T])(using FlatDoubleSerializer[T]): FlatDoubleView[T] =
    val builder = newBuilder[T]
    val ks = source.knownSize
    if (ks >= 0) builder.sizeHint(ks)
    builder.addAll(source).result()

  override def empty[T](using FlatDoubleSerializer[T]): FlatDoubleView[T] =
    new FlatDoubleView[T](new Array[Double](0), 0, 0)

  override def newBuilder[T](using serializer: FlatDoubleSerializer[T]): mutable.Builder[T, FlatDoubleView[T]] =
    new mutable.Builder[T, FlatDoubleView[T]]:
      private var arr: Array[Double] = new Array[Double](0)
      private var len: Int = 0

      override def sizeHint(size: Int): Unit =
        val needed = size * serializer.size
        if (needed > arr.length) {
          val newArr = new Array[Double](needed)
          System.arraycopy(arr, 0, newArr, 0, len * serializer.size)
          arr = newArr
        }

      def addOne(elem: T): this.type =
        val requiredLength = (len + 1) * serializer.size
        if (requiredLength > arr.length) {
          val newCapacity = (len + 1) * 3 / 2 + 8
          val newArr = new Array[Double](newCapacity * serializer.size)
          System.arraycopy(arr, 0, newArr, 0, len * serializer.size)
          arr = newArr
        }
        serializer.write(elem, arr, len * serializer.size)
        len += 1
        this

      def clear(): Unit =
        len = 0

      def result(): FlatDoubleView[T] =
        new FlatDoubleView[T](arr, 0, len)
