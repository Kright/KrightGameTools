package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*

/**
 * methods are trivial, but it is easy to forget them
 */
object Pga3dVelocity:
  def linear(v: Pga3dVector): Pga3dBivectorWeight =
    Pga3dBivectorWeight(wx = v.x, wy = v.y, wz = v.z)

  def angular(xy: Double, xz: Double, yz: Double): Pga3dBivectorBulk =
    Pga3dBivectorBulk(xy, xz, yz)

  def angular(w: Pga3dBivectorBulk, rotationCenter: Pga3dPoint): Pga3dBivector =
    w - linear(pointLinearVelocity(rotationCenter, w))


  def pointLinearVelocity(point: Pga3dPoint, velocity: Pga3dBivector): Pga3dVector =
    point.cross(velocity)

  def pointLinearVelocity(point: Pga3dPoint, velocity: Pga3dBivectorWeight): Pga3dVector =
    point.cross(velocity)

  def pointLinearVelocity(point: Pga3dPoint, velocity: Pga3dBivectorBulk): Pga3dVector =
    point.cross(velocity)