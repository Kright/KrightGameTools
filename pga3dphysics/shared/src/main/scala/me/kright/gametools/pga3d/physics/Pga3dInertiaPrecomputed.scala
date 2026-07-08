package me.kright.gametools.pga3d.physics

import me.kright.gametools.matrix.Matrix
import me.kright.gametools.pga3d.*

class Pga3dInertiaPrecomputed(val inertia: Pga3dInertia) extends Pga3dInertia:

  override def toString: String = s"Pga3dInertiaPrecomputed(inertia = $inertia)"

  override val mass: Double =
    inertia.mass

  override val centerOfMass: Pga3dPoint =
    inertia.centerOfMass

  override val centerOfMassProjective: Pga3dProjectivePoint =
    centerOfMass * mass

  private val matrixFwd: Matrix =
    Pga3dMatrix.linearMapping(inertia.apply)

  private val matrixInv: Matrix =
    Pga3dMatrix.linearMapping(inertia.invert)

  override def apply(globalB: Pga3dBivector): Pga3dBivector =
    Pga3dMatrix.multiply(matrixFwd, globalB)

  override def invert(globalInertia: Pga3dBivector): Pga3dBivector =
    Pga3dMatrix.multiply(matrixInv, globalInertia)

  override def toInertiaMovedLocal: Pga3dInertiaMovedLocal =
    inertia.toInertiaMovedLocal

  override def toSummable: Pga3dInertiaSummable =
    toInertiaMovedLocal.toSummable

  override def toPrecomputed: Pga3dInertiaPrecomputed =
    this

  override def toFastestRepresentation: Pga3dInertiaPrecomputed =
    this


  override def movedBy(rotor: Pga3dRotor): Pga3dInertia =
    inertia.movedBy(rotor)

  override def movedBy(motor: Pga3dMotor): Pga3dInertia =
    inertia.movedBy(motor)

  override def movedBy(translator: Pga3dTranslator): Pga3dInertia =
    inertia.movedBy(translator)
