package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*

/**
 * Fully symmetrical tensor of inertia (for example, for cube or sphere).
 * It has no precession, and computations are much simpler and faster
 *
 * @param mass - mass
 * @param mr2  - square of effective radius multiplied by mass
 */
final case class Pga3dInertiaSimple(mass: Double,
                                    mr2: Double) extends Pga3dInertia:

  private val massInv = 1.0 / mass
  private val mr2Inv = 1.0 / mr2

  override def toString: String =
    s"Pga3dInertiaSimple(mass = $mass, mr2 = $mr2)"

  override def centerOfMass: Pga3dPoint =
    Pga3dPoint.center

  override def centerOfMassProjective: Pga3dProjectivePoint =
    Pga3dProjectivePoint(x = 0.0, y = 0.0, z = 0.0, w = mass)

  override def apply(localB: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = localB.yz * mr2,
      wy = -localB.xz * mr2,
      wz = localB.xy * mr2,
      xy = localB.wz * mass,
      xz = -localB.wy * mass,
      yz = localB.wx * mass,
    )

  override def invert(localInertia: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = localInertia.yz * massInv,
      wy = -localInertia.xz * massInv,
      wz = localInertia.xy * massInv,
      xy = localInertia.wz * mr2Inv,
      xz = -localInertia.wy * mr2Inv,
      yz = localInertia.wx * mr2Inv,
    )

  /** invert(localB.cross(apply(localB)) + localForque) */
  override def getAcceleration(localB: Pga3dBivector, localForque: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = localForque.yz * massInv + localB.wy * localB.xy + localB.wz * localB.xz,
      wy = -localForque.xz * massInv + localB.wz * localB.yz - localB.wx * localB.xy,
      wz = localForque.xy * massInv - localB.wx * localB.xz - localB.wy * localB.yz,
      xy = localForque.wz * mr2Inv,
      xz = -localForque.wy * mr2Inv,
      yz = localForque.wx * mr2Inv,
    )

  //todo add test for it
  override def toSummable: Pga3dInertiaSummable =
    Pga3dInertiaSummable(
      ww = mass,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xx = mr2 * 0.5,
      yy = mr2 * 0.5,
      zz = mr2 * 0.5,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
    )

  def toInertiaLocal: Pga3dInertiaLocal =
    Pga3dInertiaLocal(mass, mr2, mr2, mr2)

  override def toInertiaMovedLocal: Pga3dInertiaMovedLocal =
    toInertiaLocal.toInertiaMovedLocal

  /**
   * Pga3dInertiaSimple is much faster than Pga3dInertiaPrecomputed, so no much need in this conversion 
   */
  override def toPrecomputed: Pga3dInertiaPrecomputed =
    toInertiaLocal.toPrecomputed

  def toMovedSimple: Pga3dInertiaMovedSimple =
    Pga3dInertiaMovedSimple(Pga3dTranslator.id, this)

  override def toFastestRepresentation: Pga3dInertiaSimple =
    this

  override def movedBy(translator: Pga3dTranslator): Pga3dInertiaMovedSimple =
    Pga3dInertiaMovedSimple(translator, this)

  override def movedBy(rotor: Pga3dRotor): Pga3dInertiaSimple =
    this

  override def movedBy(motor: Pga3dMotor): Pga3dInertiaMovedSimple =
    movedBy(Pga3dTranslator.addVector(motor.sandwich(Pga3dPointCenter).toVectorUnsafe))

object Pga3dInertiaSimple:

  inline val componentsCount = 2
  
  def point(mass: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple(mass, mr2 = 0.0)

  def cube(mass: Double, rx: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple(
      mass,
      mr2 = rx * rx * mass * (2.0 / 3.0),
    )

  def sphere(mass: Double, r: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple(mass, mr2 = r * r * mass * (2.0 / 3.0))

  def solidSphere(mass: Double, r: Double): Pga3dInertiaSimple =
    Pga3dInertiaSimple(mass, mr2 = r * r * mass * (2.0 / 5.0))
