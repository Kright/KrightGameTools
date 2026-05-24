package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.MathUtil.clamp
import me.kright.gametools.pga3d.Pga3dVector

trait Pga3dFriction:
  /**
   * for positive velocity result force should be negative or zero
   * for zero it should be zero
   * for negative velocity result force should be positive or zero
   */
  def apply(velocity: Double): Double

  def apply(velocity: Pga3dVector): Pga3dVector

  def scale(m: Double): Pga3dFriction


object Pga3dFriction:
  def const(minVelocity: Double, force: Double): Clamped =
    require(minVelocity > 0.0)
    Clamped(Linear(force / minVelocity), force)


  object Zero extends Pga3dFriction:
    override def apply(velocity: Double): Double =
      0.0

    override def apply(velocity: Pga3dVector): Pga3dVector =
      Pga3dVector.zero

    override def scale(m: Double): Zero.type =
      Zero


  case class Linear(k: Double) extends Pga3dFriction:
    require(k >= 0.0)

    override def apply(velocity: Double): Double =
      velocity * -k

    override def apply(velocity: Pga3dVector): Pga3dVector =
      velocity * -k

    override def scale(m: Double): Linear =
      Linear(k * m)


  case class Quadratic(k2: Double) extends Pga3dFriction:
    require(k2 >= 0.0)

    override def apply(velocity: Double): Double =
      -velocity * velocity * k2 * velocity.sign

    override def apply(velocity: Pga3dVector): Pga3dVector =
      velocity * (-k2 * velocity.norm)

    override def scale(m: Double): Quadratic =
      Quadratic(k2 * m)


  case class Clamped(friction: Pga3dFriction, maxForce: Double) extends Pga3dFriction:
    require(maxForce >= 0)

    override def apply(velocity: Double): Double =
      friction(velocity).clamp(-maxForce, maxForce)

    override def apply(velocity: Pga3dVector): Pga3dVector =
      val result = friction(velocity)
      val norm = result.norm

      if (norm > maxForce) {
        result * (maxForce / norm)
      } else {
        result
      }

    override def scale(m: Double): Clamped =
      Clamped(friction.scale(m), maxForce * m)
