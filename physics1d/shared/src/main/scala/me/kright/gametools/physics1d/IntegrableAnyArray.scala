package me.kright.gametools.physics1d

import me.kright.gametools.mathutil.FastRange

import scala.reflect.ClassTag

final class IntegrableAnyArray[T <: Integrable](val elements: Array[T]) extends IntegrableArray[T]:
  private val offsets: Array[Int] = new Array[Int](elements.length)

  override val stateSize: Int =
    var size = 0
    for (i <- FastRange(elements.length)) {
      offsets(i) = size
      size += elements(i).stateSize
    }
    size

  override def offsetOf(index: Int): Int =
    offsets(index)

  override def saveState(dst: Array[Double], offset: Int): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).saveState(dst, offset + offsets(i))
    }

  override def loadState(src: Array[Double], offset: Int): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).loadState(src, offset + offsets(i))
    }

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).saveDerivative(dst, offset + offsets(i))
    }

  override def resetForces(): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).resetForces()
    }

  override def toString: String =
    s"IntegrableAnyArray(${elements.mkString(", ")})"


object IntegrableAnyArray:
  def apply[T <: Integrable](elements: Array[T]): IntegrableAnyArray[T] =
    new IntegrableAnyArray[T](elements)

  def apply[T <: Integrable : ClassTag](elements: T*): IntegrableAnyArray[T] =
    new IntegrableAnyArray[T](elements.toArray)
