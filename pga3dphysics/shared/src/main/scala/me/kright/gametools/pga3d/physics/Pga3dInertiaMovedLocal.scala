package me.kright.gametools.pga3d.physics

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga3d.*


/**
 * Every operation converts the argument to the local frame and back, so the pose is stored
 * as a [[Pga3dTransform]] and those conversions are plain matrix multiplications.
 */
final case class Pga3dInertiaMovedLocal(localToGlobal: Pga3dTransform,
                                        localInertia: Pga3dInertiaLocal) extends Pga3dInertia derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  override def toString: String =
    s"Pga3dInertiaMovedLocal(localToGlobal = ${localToGlobal.motor}, localInertia = $localInertia)"

  override def mass: Double =
    localInertia.mass

  override def centerOfMass: Pga3dPoint =
    localToGlobal.sandwich(Pga3dPointCenter).toPointUnsafe

  override def centerOfMassProjective: Pga3dProjectivePoint =
    centerOfMass * localInertia.mass

  /**
   * @return L - combination of linear impulse and angular momentum
   */
  override def apply(globalB: Pga3dBivector): Pga3dBivector =
    val localB = localToGlobal.reverseSandwich(globalB)
    val localI = localInertia(localB)
    localToGlobal.sandwich(localI)

  override def invert(globalI: Pga3dBivector): Pga3dBivector =
    val localI = localToGlobal.reverseSandwich(globalI)
    val localB = localInertia.invert(localI)
    localToGlobal.sandwich(localB)

  def getLocalAcceleration(globalB: Pga3dBivector, globalForque: Pga3dBivector): Pga3dBivector =
    val localB = localToGlobal.reverseSandwich(globalB)
    val localF = localToGlobal.reverseSandwich(globalForque)
    localInertia.getAcceleration(localB, localF)

  override def getAcceleration(globalB: Pga3dBivector, globalForque: Pga3dBivector): Pga3dBivector =
    val localA = getLocalAcceleration(globalB, globalForque)
    localToGlobal.sandwich(localA)

  override def getKineticEnergy(globalB: Pga3dBivector): Double =
    val localB = localToGlobal.reverseSandwich(globalB)
    localInertia.getKineticEnergy(localB)

  override def toSummable: Pga3dInertiaSummable =
    localToGlobal.motor.sandwich(localInertia.toSummable)

  override def toPrecomputed: Pga3dInertiaPrecomputed =
    Pga3dInertiaPrecomputed(this)

  override def toInertiaMovedLocal: Pga3dInertiaMovedLocal =
    this

  override def movedBy(rotor: Pga3dRotor) =
    Pga3dInertiaMovedLocal(rotor.geometric(localToGlobal.motor), localInertia)

  override def movedBy(motor: Pga3dMotor) =
    Pga3dInertiaMovedLocal(motor.geometric(localToGlobal.motor), localInertia)

  override def movedBy(translator: Pga3dTranslator) =
    Pga3dInertiaMovedLocal(translator.geometric(localToGlobal.motor), localInertia)


object Pga3dInertiaMovedLocal:
  def apply(localToGlobal: Pga3dMotor, localInertia: Pga3dInertiaLocal): Pga3dInertiaMovedLocal =
    Pga3dInertiaMovedLocal(Pga3dTransform(localToGlobal), localInertia)
