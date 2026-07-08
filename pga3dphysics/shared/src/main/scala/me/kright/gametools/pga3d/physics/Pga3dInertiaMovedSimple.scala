package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*

/**
 * Pga3dInertiaSimple is fully symmetrical. So rotation is not needed and Pga3dTranslator is enough.
 */
class Pga3dInertiaMovedSimple(val localToGlobal: Pga3dTranslator, val localInertia: Pga3dInertiaSimple) extends Pga3dInertia:

  override def toString: String =
    s"Pga3dInertiaMovedSimple(localToGlobal = $localToGlobal, localInertia = $localInertia)"
    

  override def mass: Double =
    localInertia.mass

  override def centerOfMass: Pga3dPoint =
    localToGlobal.sandwich(Pga3dPointCenter)

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
    localToGlobal.sandwich(localInertia.toSummable)

  override def toPrecomputed: Pga3dInertiaPrecomputed =
    toInertiaMovedLocal.toPrecomputed

  override def toInertiaMovedLocal: Pga3dInertiaMovedLocal =
    Pga3dInertiaMovedLocal(localToGlobal.toMotor, localInertia.toInertiaLocal)


  override def movedBy(motor: Pga3dMotor): Pga3dInertiaMovedSimple =
    val translator = Pga3dTranslator.addVector(motor.geometric(localToGlobal).sandwich(Pga3dPointCenter).toVectorUnsafe)
    Pga3dInertiaMovedSimple(translator, localInertia)

  override def movedBy(rotor: Pga3dRotor): Pga3dInertiaMovedSimple =
    val translator = Pga3dTranslator.addVector(rotor.geometric(localToGlobal).sandwich(Pga3dPointCenter).toVectorUnsafe)
    Pga3dInertiaMovedSimple(translator, localInertia)

  override def movedBy(translator: Pga3dTranslator): Pga3dInertiaMovedSimple =
    Pga3dInertiaMovedSimple(translator.geometric(localToGlobal), localInertia)
  