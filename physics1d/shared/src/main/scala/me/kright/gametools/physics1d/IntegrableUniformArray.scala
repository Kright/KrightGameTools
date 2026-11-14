package me.kright.gametools.physics1d

import me.kright.gametools.mathutil.FastRange

import scala.annotation.nowarn

trait IntegrableUniformArray[T <: Integrable] extends IntegrableArray[T]:
  val elementSize: Int

  override def offsetOf(index: Int): Int =
    index * elementSize

  override def toString: String =
    s"IntegrableUniformArray(${elements.length} elements of $elementSize doubles)"


object IntegrableUniformArray:
  /**
   * Inlined into the caller, so every call site gets its own class with the loops over the
   * element type and the element size known there. A loop shared by all element types would be
   * one bytecode calling the elements through the interface, and the JIT inlines such calls
   * only while it sees a single element type there.
   */
  @nowarn("msg=New anonymous class definition will be duplicated at each inline site")
  inline def apply[T <: Integrable](array: Array[T], inline sizeOfElement: Int, checkElementSizes: Boolean = true): IntegrableUniformArray[T] =
    if (checkElementSizes) {
      requireStateSizes(array, sizeOfElement)
    }
    new IntegrableUniformArray[T]:
      override val elements: Array[T] = array

      override val elementSize: Int = sizeOfElement

      override val stateSize: Int = array.length * sizeOfElement

      override def saveState(dst: Array[Double], offset: Int): Unit =
        for (i <- FastRange(array.length)) {
          array(i).saveState(dst, offset + i * sizeOfElement)
        }

      override def loadState(src: Array[Double], offset: Int): Unit =
        for (i <- FastRange(array.length)) {
          array(i).loadState(src, offset + i * sizeOfElement)
        }

      override def saveDerivative(dst: Array[Double], offset: Int): Unit =
        for (i <- FastRange(array.length)) {
          array(i).saveDerivative(dst, offset + i * sizeOfElement)
        }

      override def resetForces(): Unit =
        for (i <- FastRange(array.length)) {
          array(i).resetForces()
        }

  def requireStateSizes(elements: Array[? <: Integrable], expected: Int): Unit =
    for (i <- FastRange(elements.length)) {
      if (elements(i).stateSize != expected) {
        throw new IllegalArgumentException(s"element $i has the state size ${elements(i).stateSize}, expected $expected")
      }
    }
