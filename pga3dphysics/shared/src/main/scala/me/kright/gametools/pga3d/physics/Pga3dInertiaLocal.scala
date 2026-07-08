package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*

final case class Pga3dInertiaLocal(mass: Double,
                                   mryz: Double,
                                   mrxz: Double,
                                   mrxy: Double) extends Pga3dInertia derives CanEqual:

  override def toString: String =
    s"Pga3dInertiaLocal(mass = $mass, mryz = $mryz, mrxz = $mrxz, mrxy = $mrxy)"

  override def centerOfMass: Pga3dPoint =
    Pga3dPoint.center

  override def centerOfMassProjective: Pga3dProjectivePoint =
    Pga3dProjectivePoint(x = 0.0, y = 0.0, z = 0.0, w = mass)

  override def apply(localB: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = localB.yz * mryz,
      wy = -localB.xz * mrxz,
      wz = localB.xy * mrxy,
      xy = localB.wz * mass,
      xz = -localB.wy * mass,
      yz = localB.wx * mass,
    )

  override def invert(localInertia: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = localInertia.yz / mass,
      wy = -localInertia.xz / mass,
      wz = localInertia.xy / mass,
      xy = localInertia.wz / mrxy,
      xz = -localInertia.wy / mrxz,
      yz = localInertia.wx / mryz,
    )

  /** invert(localB.cross(apply(localB)) + localForque) */
  override def getAcceleration(localB: Pga3dBivector, localForque: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = localForque.yz / mass + localB.wy * localB.xy + localB.wz * localB.xz,
      wy = -localForque.xz / mass + localB.wz * localB.yz - localB.wx * localB.xy,
      wz = localForque.xy / mass - localB.wx * localB.xz - localB.wy * localB.yz,
      xy = (localForque.wz + localB.xz * localB.yz * (mrxz - mryz)) / mrxy,
      xz = (-localForque.wy + localB.xy * localB.yz * (mryz - mrxy)) / mrxz,
      yz = (localForque.wx + localB.xy * localB.xz * (mrxy - mrxz)) / mryz,
    )

  override def toSummable: Pga3dInertiaSummable =
    val mrxyz2 = (mrxy + mrxz + mryz) * 0.5

    val mrx2 = mrxyz2 - mryz
    val mry2 = mrxyz2 - mrxz
    val mrz2 = mrxyz2 - mrxy

    Pga3dInertiaSummable(
      ww = mass,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xx = mrx2,
      yy = mry2,
      zz = mrz2,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
    )

  override def toInertiaMovedLocal: Pga3dInertiaMovedLocal =
    Pga3dInertiaMovedLocal(Pga3dMotor.id, this)

  override def toPrecomputed: Pga3dInertiaPrecomputed =
    Pga3dInertiaPrecomputed(this)

  override def toFastestRepresentation: Pga3dInertiaLocal =
    this

  override def movedBy(motor: Pga3dMotor): Pga3dInertiaMovedLocal =
    Pga3dInertiaMovedLocal(motor, this)

  override def movedBy(rotor: Pga3dRotor): Pga3dInertiaMovedLocal =
    movedBy(rotor.toMotor)

  override def movedBy(translator: Pga3dTranslator): Pga3dInertiaMovedLocal =
    movedBy(translator.toMotor)


object Pga3dInertiaLocal:

  inline val componentsCount = 4

  def cube(mass: Double, rx: Double, ry: Double, rz: Double): Pga3dInertiaLocal =
    val rx2 = rx * rx
    val ry2 = ry * ry
    val rz2 = rz * rz
    Pga3dInertiaLocal(
      mass,
      mryz = (ry2 + rz2) * mass * (1.0 / 3.0),
      mrxz = (rx2 + rz2) * mass * (1.0 / 3.0),
      mrxy = (rx2 + ry2) * mass * (1.0 / 3.0),
    )

  def solidEllipsoid(mass: Double, rx: Double, ry: Double, rz: Double): Pga3dInertiaLocal =
    val rx2 = rx * rx
    val ry2 = ry * ry
    val rz2 = rz * rz
    Pga3dInertiaLocal(
      mass,
      mryz = (ry2 + rz2) * mass * (1.0 / 5.0),
      mrxz = (rx2 + rz2) * mass * (1.0 / 5.0),
      mrxy = (rx2 + ry2) * mass * (1.0 / 5.0),
    )

  def fromXXYYZZ(mass: Double, xx: Double, yy: Double, zz: Double): Pga3dInertiaLocal =
    Pga3dInertiaLocal(
      mass,
      mryz = mass * (yy + zz),
      mrxz = mass * (xx + zz),
      mrxy = mass * (xx + yy),
    )

  def cylinderX(mass: Double, length: Double, r: Double, innerR: Double = 0.0): Pga3dInertiaLocal = {
    val xx = Pga3dInertia.disk(r, innerR) * 0.5
    val yy = Pga3dInertia.rod(length)
    fromXXYYZZ(mass, yy, xx, xx)
  }
