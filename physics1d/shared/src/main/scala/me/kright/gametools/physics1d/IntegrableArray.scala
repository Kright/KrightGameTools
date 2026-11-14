package me.kright.gametools.physics1d

trait IntegrableArray[T <: Integrable] extends Integrable:
  val elements: Array[T]

  def offsetOf(index: Int): Int
