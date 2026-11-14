package me.kright.gametools.physics1d

final class TestOscillator(val k: Double, var x: Double, var v: Double) extends Integrable:
  var resetsCount: Int = 0

  override val stateSize: Int = 2

  override def saveState(dst: Array[Double], offset: Int): Unit =
    dst(offset) = x
    dst(offset + 1) = v

  override def loadState(src: Array[Double], offset: Int): Unit =
    x = src(offset)
    v = src(offset + 1)

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = v
    dst(offset + 1) = -k * x

  override def resetAccum(): Unit =
    resetsCount += 1


final class TestTriple(var a: Double, var b: Double, var c: Double) extends Integrable:
  override val stateSize: Int = 3

  override def saveState(dst: Array[Double], offset: Int): Unit =
    dst(offset) = a
    dst(offset + 1) = b
    dst(offset + 2) = c

  override def loadState(src: Array[Double], offset: Int): Unit =
    a = src(offset)
    b = src(offset + 1)
    c = src(offset + 2)

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = -a
    dst(offset + 1) = -2.0 * b
    dst(offset + 2) = -3.0 * c

  override def resetAccum(): Unit = ()
