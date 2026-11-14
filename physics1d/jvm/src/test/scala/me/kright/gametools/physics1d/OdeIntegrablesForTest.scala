package me.kright.gametools.physics1d

final class PointMass1d(val mass: Double, var x: Double, var v: Double) extends Integrable:
  private var force = 0.0

  def addForce(f: Double): Unit =
    force += f

  override val stateSize: Int = 2

  override def saveState(dst: Array[Double], offset: Int): Unit =
    dst(offset) = x
    dst(offset + 1) = v

  override def loadState(src: Array[Double], offset: Int): Unit =
    x = src(offset)
    v = src(offset + 1)

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = v
    dst(offset + 1) = force / mass

  override def resetForces(): Unit =
    force = 0.0


final class UnitDirection(val damping: Double, initialAngle: Double, var omega: Double,
                          normalizeOnLoad: Boolean = true) extends Integrable:
  var cos: Double = Math.cos(initialAngle)
  var sin: Double = Math.sin(initialAngle)

  def norm: Double =
    Math.sqrt(cos * cos + sin * sin)

  def exactAngle(initialAngle: Double, initialOmega: Double, time: Double): Double =
    initialAngle - initialOmega * Math.expm1(-damping * time) / damping

  override val stateSize: Int = 3

  override def saveState(dst: Array[Double], offset: Int): Unit =
    dst(offset) = cos
    dst(offset + 1) = sin
    dst(offset + 2) = omega

  override def loadState(src: Array[Double], offset: Int): Unit =
    val c = src(offset)
    val s = src(offset + 1)
    val scale = if (normalizeOnLoad) 1.0 / Math.sqrt(c * c + s * s) else 1.0
    cos = c * scale
    sin = s * scale
    omega = src(offset + 2)

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    dst(offset) = -omega * sin
    dst(offset + 1) = omega * cos
    dst(offset + 2) = -damping * omega

  override def resetForces(): Unit = ()
