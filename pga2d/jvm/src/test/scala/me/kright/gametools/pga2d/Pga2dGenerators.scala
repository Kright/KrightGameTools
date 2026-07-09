package me.kright.gametools.pga2d

import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalacheck.Gen

object Pga2dGenerators:
  private def makeGenT[T](elemsCount: Int, factory: (Array[Double], Int) => T): Gen[T] =
    Gen.containerOfN[Array, Double](elemsCount, double1)
      .map(arr => factory(arr, 0))

  val double1: Gen[Double] = Gen.double.map(_ * 2.0 - 1.0)

  val rotors: Gen[Pga2dRotor] =
    Gen.oneOf(
      Gen.oneOf(
        Seq(
          Pga2dRotor.id,
          -Pga2dRotor.id,
          Pga2dRotor(),
        )
      ),
      makeGenT(2, FlatDoubleSerializer.read[Pga2dRotor])
    )

  val normalizedRotors: Gen[Pga2dRotor] =
    rotors.filter(_.norm > 1e-40).map(_.normalizedByNorm)

  val points: Gen[Pga2dPoint] =
    makeGenT(2, FlatDoubleSerializer.read[Pga2dPoint])

  val vectors: Gen[Pga2dVector] =
    Gen.oneOf(
      Gen.oneOf(
        Pga2dVector.zero,
        Pga2dVector(1, 0),
        Pga2dVector(0, 1),
      ),
      makeGenT(2, FlatDoubleSerializer.read[Pga2dVector])
    )

  val projectivePoints: Gen[Pga2dProjectivePoint] =
    makeGenT(3, FlatDoubleSerializer.read[Pga2dProjectivePoint])

  val lines: Gen[Pga2dLine] =
    makeGenT(3, FlatDoubleSerializer.read[Pga2dLine])

  val lineIdeals: Gen[Pga2dLineIdeal] =
    Gen.oneOf(
      Gen.oneOf(
        Pga2dLineIdeal(1, 0),
        Pga2dLineIdeal(0, 1),
        Pga2dLineIdeal(1, 1),
      ),
      makeGenT(2, FlatDoubleSerializer.read[Pga2dLineIdeal])
    )

  val translators: Gen[Pga2dTranslator] =
    vectors.map(Pga2dTranslator.addVector)

  val normalizedMotors: Gen[Pga2dMotor] =
    for (
      v <- vectors;
      r <- normalizedRotors
    ) yield Pga2dTranslator.addVector(v).geometric(r)

  val anyMotors: Gen[Pga2dMotor] =
    makeGenT(4, FlatDoubleSerializer.read[Pga2dMotor])

  val multivectors: Gen[Pga2dMultivector] =
    makeGenT(8, FlatDoubleSerializer.read[Pga2dMultivector])
