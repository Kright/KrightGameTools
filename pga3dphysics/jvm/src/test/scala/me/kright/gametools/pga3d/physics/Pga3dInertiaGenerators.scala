package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dGenerators, Pga3dTranslator}
import org.scalacheck.Gen

object Pga3dInertiaGenerators:
  def inertiaLocal(minMass: Double, maxMass: Double, minR: Double, maxR: Double): Gen[Pga3dInertiaLocal] =
    for {
      mass <- Pga3dVectorMathGenerators.doubleInRange(minMass, maxMass)
      rx <- Pga3dVectorMathGenerators.doubleInRange(minR, maxR)
      ry <- Pga3dVectorMathGenerators.doubleInRange(minR, maxR)
      rz <- Pga3dVectorMathGenerators.doubleInRange(minR, maxR)
    } yield Pga3dInertiaLocal.cube(mass, rx, ry, rz)

  def inertiaSimple(minMass: Double, maxMass: Double, minR: Double, maxR: Double): Gen[Pga3dInertiaSimple] =
    for {
      mass <- Pga3dVectorMathGenerators.doubleInRange(minMass, maxMass)
      r <- Pga3dVectorMathGenerators.doubleInRange(minR, maxR)
    } yield Pga3dInertiaSimple.cube(mass, r)

  val summableInertiaProbes = Seq(
    Pga3dInertiaSummable(1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    Pga3dInertiaSummable(0, 1, 0, 0, 0, 0, 0, 0, 0, 0),
    Pga3dInertiaSummable(0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
    Pga3dInertiaSummable(0, 0, 0, 1, 0, 0, 0, 0, 0, 0),
    Pga3dInertiaSummable(0, 0, 0, 0, 1, 0, 0, 0, 0, 0),
    Pga3dInertiaSummable(0, 0, 0, 0, 0, 1, 0, 0, 0, 0),
    Pga3dInertiaSummable(0, 0, 0, 0, 0, 0, 1, 0, 0, 0),
    Pga3dInertiaSummable(0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
    Pga3dInertiaSummable(0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
    Pga3dInertiaSummable(0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
  )

  val inertiaMovedLocal: Gen[Pga3dInertiaMovedLocal] = for {
    inertiaLocal <- inertiaLocal(0.1, 10.0, 0.1, 10.0)
    q <- Pga3dGenerators.normalizedRotors
    shift <- Pga3dGenerators.vectors
    motor = Pga3dTranslator.addVector(shift).geometric(q)
  } yield Pga3dInertia.moved(motor, inertiaLocal)

  val inertiaMovedSimple: Gen[Pga3dInertiaMovedSimple] = for {
    inertiaLocal <- inertiaSimple(0.1, 10.0, 0.1, 10.0)
    shift <- Pga3dGenerators.vectors
    translator = Pga3dTranslator.addVector(shift)
  } yield Pga3dInertia.movedSimple(translator, inertiaLocal)

  val inertiaSummable: Gen[Pga3dInertiaSummable] =
    inertiaMovedLocal.map(_.toSummable)

  val anyInertia: Gen[Pga3dInertia] =
    Gen.oneOf(
      inertiaSimple(0.1, 10.0, 0.1, 10.0).flatMap(i => Gen.oneOf(i, i.toMovedSimple)),
      inertiaLocal(0.1, 10.0, 0.1, 10.0),
      inertiaMovedSimple,
      inertiaMovedLocal,
    ).flatMap { inertia =>
      Gen.oneOf(
        inertia,
        inertia.toPrecomputed,
        inertia.toInertiaMovedLocal,
        inertia.toFastestRepresentation,
        inertia.toSummable,
      )
    }