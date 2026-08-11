package me.kright.gametools.pga3d.physics

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga3d.*


/**
 * Every operation converts the argument to the local frame and back; the pose is cached as a
 * [[Pga3dTransform]] (the `transform` val, rebuilt on construction - including deserialization
 * and `copy`), so those conversions are plain matrix multiplications. Only the motor and the
 * local inertia are the case fields: they are what is serialized (12 doubles) and compared.
 */
final case class Pga3dInertiaMovedLocal(localToGlobal: Pga3dMotor,
                                        localInertia: Pga3dInertiaLocal) extends Pga3dInertia derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  /** the cached operator form of localToGlobal (renormalized) */
  val transform: Pga3dTransform = Pga3dTransform(localToGlobal)

  override def toString: String =
    s"Pga3dInertiaMovedLocal(localToGlobal = $localToGlobal, localInertia = $localInertia)"

  override def mass: Double =
    localInertia.mass

  override def centerOfMass: Pga3dPoint =
    transform.sandwich(Pga3dPointCenter)

  override def centerOfMassProjective: Pga3dProjectivePoint =
    centerOfMass * localInertia.mass

  /**
   * @return L - combination of linear impulse and angular momentum
   */
  override def apply(globalB: Pga3dBivector): Pga3dBivector =
    val localB = transform.reverseSandwich(globalB)
    val localI = localInertia(localB)
    transform.sandwich(localI)

  override def invert(globalI: Pga3dBivector): Pga3dBivector =
    val localI = transform.reverseSandwich(globalI)
    val localB = localInertia.invert(localI)
    transform.sandwich(localB)

  def getLocalAcceleration(globalB: Pga3dBivector, globalForque: Pga3dBivector): Pga3dBivector =
    val localB = transform.reverseSandwich(globalB)
    val localF = transform.reverseSandwich(globalForque)
    localInertia.getAcceleration(localB, localF)

  override def getAcceleration(globalB: Pga3dBivector, globalForque: Pga3dBivector): Pga3dBivector =
    val localA = getLocalAcceleration(globalB, globalForque)
    transform.sandwich(localA)

  override def getKineticEnergy(globalB: Pga3dBivector): Double =
    val localB = transform.reverseSandwich(globalB)
    localInertia.getKineticEnergy(localB)

  override def toSummable: Pga3dInertiaSummable =
    transform.motor.sandwich(localInertia.toSummable)

  override def toPrecomputed: Pga3dInertiaPrecomputed =
    Pga3dInertiaPrecomputed(this)

  override def toInertiaMovedLocal: Pga3dInertiaMovedLocal =
    this

  override def movedBy(rotor: Pga3dRotor) =
    Pga3dInertiaMovedLocal(rotor.geometric(localToGlobal), localInertia)

  override def movedBy(motor: Pga3dMotor) =
    Pga3dInertiaMovedLocal(motor.geometric(localToGlobal), localInertia)

  override def movedBy(translator: Pga3dTranslator) =
    Pga3dInertiaMovedLocal(translator.geometric(localToGlobal), localInertia)
