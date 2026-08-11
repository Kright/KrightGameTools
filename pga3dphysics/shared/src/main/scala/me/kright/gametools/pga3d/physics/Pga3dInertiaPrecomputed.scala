package me.kright.gametools.pga3d.physics

import me.kright.gametools.matrix.Matrix
import me.kright.gametools.pga3d.*

/**
 * Caches the inertia as two 6x6 matrices (apply and invert), so each operation is a plain
 * matrix multiplication - the fastest form when one inertia is applied many times.
 *
 * Precision caveat: the matrix entries bake in the parallel-axis terms ~ mass * R^2, where R is
 * the distance from the reference origin of the representation to the center of mass. For twists
 * passing near the center of mass (a body rotating about itself - the common case) those terms
 * nearly cancel on application, and the rounding of the cancelled products stays: the kinetic
 * energy and the acceleration lose relative precision as ~1e-16 * (R / rg)^2, where
 * rg = sqrt(mr / mass) is the radius of gyration (the body's own size). apply/invert look fine
 * in norm (the result itself is large), but their small components are corrupted to the same degree.
 *
 * So the quality is governed by the dimensionless ratio R / rg:
 *  - R <= rg (the center of mass moved within about the body's size, e.g. an off-center part):
 *    no loss at all, this is the intended use;
 *  - R / rg ~ 1e4 (a body represented in the global frame far from the origin): ~6 digits lost;
 *  - larger R / rg: grows quadratically, up to garbage at ~1e8.
 *
 * Pga3dInertiaMovedLocal loses only ~1e-16 * (R / rg) - linear, four orders of magnitude better
 * at R / rg = 1e4 - because it cancels the R-sized terms before they get multiplied into R^2-sized
 * ones. Prefer it when the center of mass is far from the origin, or keep the origin near the
 * body. Measured in Pga3dInertiaPrecisionTest.
 */
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
