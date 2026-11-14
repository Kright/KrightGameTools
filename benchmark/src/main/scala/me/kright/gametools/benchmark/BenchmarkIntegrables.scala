package me.kright.gametools.benchmark

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.physics1d.{Integrable, IntegrableUniformArray}

abstract class BenchmarkOscillator(var x: Double, var v: Double) extends Integrable:
  override val stateSize: Int = 2

  override def saveState(dst: Array[Double], offset: Int): Unit =
    dst(offset) = x
    dst(offset + 1) = v

  override def loadState(src: Array[Double], offset: Int): Unit =
    x = src(offset)
    v = src(offset + 1)

  override def resetAccum(): Unit = ()


final class BenchmarkOscillatorA(x: Double, v: Double) extends BenchmarkOscillator(x, v):
  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = v
    dst(offset + 1) = -x


final class BenchmarkOscillatorB(x: Double, v: Double) extends BenchmarkOscillator(x, v):
  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = v
    dst(offset + 1) = -2.0 * x


final class BenchmarkOscillatorC(x: Double, v: Double) extends BenchmarkOscillator(x, v):
  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = v
    dst(offset + 1) = -3.0 * x


final class BenchmarkSharedLoopsUniformArray[T <: Integrable](override val elements: Array[T], override val elementSize: Int) extends IntegrableUniformArray[T]:
  override val stateSize: Int = elements.length * elementSize

  override def saveState(dst: Array[Double], offset: Int): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).saveState(dst, offset + i * elementSize)
    }

  override def loadState(src: Array[Double], offset: Int): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).loadState(src, offset + i * elementSize)
    }

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).saveDerivative(dst, offset + i * elementSize)
    }

  override def resetAccum(): Unit =
    for (i <- FastRange(elements.length)) {
      elements(i).resetAccum()
    }
