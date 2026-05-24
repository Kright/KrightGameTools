package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*

trait Pga3dInertia:

  def mass: Double

  def centerOfMass: Pga3dPoint

  def centerOfMassProjective: Pga3dProjectivePoint

  def apply(velocity: Pga3dBivector): Pga3dBivector

  def invert(localInertia: Pga3dBivector): Pga3dBivector

  def getAcceleration(velocity: Pga3dBivector, forque: Pga3dBivector): Pga3dBivector =
    invert(velocity.cross(apply(velocity)) + forque)

  def getKineticEnergy(velocity: Pga3dBivector): Double =
    (velocity v apply(velocity)) * 0.5


  def toInertiaMovedLocal: Pga3dInertiaMovedLocal

  def toSummable: Pga3dInertiaSummable

  def toPrecomputed: Pga3dInertiaPrecomputed

  def toFastestRepresentation: Pga3dInertia = toPrecomputed


  def movedBy(motor: Pga3dMotor): Pga3dInertia

  def movedBy(translator: Pga3dTranslator): Pga3dInertia

  def movedBy(quaternion: Pga3dQuaternion): Pga3dInertia


/**
 * class with useful constructor methods for base classes
 */
object Pga3dInertia:
  /**
   * inertia of rod at its center
   */
  def rod(length: Double): Double =
    length * length / 12.0

  def disk(r: Double, innerR: Double = 0.0): Double =
    0.5 * (r * r + innerR * innerR)

  def point(mass: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple(mass, 0.0)

  def cube(mass: Double, r: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple.cube(mass, r)

  def sphere(mass: Double, r: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple.sphere(mass, r)

  def solidSphere(mass: Double, r: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple.solidSphere(mass, r)

  /**
   * @param mass - mass of cube
   * @param rx   - half of size along the x-axis
   * @param ry   - half of size along the y-axis
   * @param rz   - half of size along the z-axis
   */
  def cube(mass: Double, rx: Double, ry: Double, rz: Double): Pga3dInertiaLocal =
    Pga3dInertiaLocal.cube(mass, rx, ry, rz)

  def solidEllipsoid(mass: Double, rx: Double, ry: Double, rz: Double): Pga3dInertiaLocal =
    Pga3dInertiaLocal.solidEllipsoid(mass, rx, ry, rz)

  /**
   * inertia of cylinder with rotation axis along x-axis
   */
  def cylinderX(mass: Double, length: Double, r: Double, innerR: Double = 0.0): Pga3dInertiaLocal =
    Pga3dInertiaLocal.cylinderX(mass, length, r, innerR)

  /**
   * values xx, yy and zz are values in digonal tensor matrix (not multiplied by mass)
   *
   * The moment around x-axis will be `mryz = mass * (yy + zz)` and in the same way for others
   */
  def fromXXYYZZ(mass: Double, xx: Double, yy: Double, zz: Double): Pga3dInertiaLocal =
    Pga3dInertiaLocal.fromXXYYZZ(mass, xx, yy, zz)

  def moved(motor: Pga3dMotor, inertia: Pga3dInertiaLocal): Pga3dInertiaMovedLocal =
    Pga3dInertiaMovedLocal(motor, inertia)

  def movedSimple(translator: Pga3dTranslator, inertia: Pga3dInertiaSimple) =
    Pga3dInertiaMovedSimple(translator, inertia)


  /** transparent inline allows narrower return types */
  extension (motor: Pga3dMotor)
    transparent inline def sandwich(inertia: Pga3dInertia) =
      inertia.movedBy(motor)

  extension (translator: Pga3dTranslator)
    transparent inline def sandwich(inertia: Pga3dInertia) =
      inertia.movedBy(translator)

  extension (quaternion: Pga3dQuaternion)
    transparent inline def sandwich(inertia: Pga3dInertia) =
      inertia.movedBy(quaternion)
