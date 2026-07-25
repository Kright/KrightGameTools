package me.kright.gametools.pga3d

import me.kright.gametools.matrix.Matrix
import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalacheck.Gen

import scala.collection.immutable.ArraySeq

object Pga3dGenerators:
  private def makeGenT[T](elemsCount: Int, factory: (Array[Double], Int) => T): Gen[T] =
    Gen.containerOfN[Array, Double](elemsCount, double1)
      .map(arr => factory(arr, 0))

  val double1: Gen[Double] = Gen.double.map(_ * 2.0 - 1.0)

  val bivectorProbes: Seq[Pga3dBivector] = Seq(
    Pga3dBivector(1, 0, 0, 0, 0, 0),
    Pga3dBivector(0, 1, 0, 0, 0, 0),
    Pga3dBivector(0, 0, 1, 0, 0, 0),
    Pga3dBivector(0, 0, 0, 1, 0, 0),
    Pga3dBivector(0, 0, 0, 0, 1, 0),
    Pga3dBivector(0, 0, 0, 0, 0, 1),
  )

  val bivectors: Gen[Pga3dBivector] =
    Gen.oneOf(
      Gen.oneOf(bivectorProbes :+ Pga3dBivector.zero),
      makeGenT(6, FlatDoubleSerializer.read[Pga3dBivector])
    )

  val bivectorBulks: Gen[Pga3dBivectorBulk] =
    Gen.oneOf(
      Gen.oneOf(
        ArraySeq(
          Pga3dBivectorBulk.zero,
          Pga3dBivectorBulk(1, 0, 0),
          Pga3dBivectorBulk(0, 1, 0),
          Pga3dBivectorBulk(0, 0, 1),
        )
      ),
      makeGenT(3, FlatDoubleSerializer.read[Pga3dBivectorBulk]),
    )

  val bivectorWeight: Gen[Pga3dBivectorWeight] =
    Gen.oneOf(
      Gen.oneOf(
        ArraySeq(
          Pga3dBivectorWeight.zero,
          Pga3dBivectorWeight(1, 0, 0),
          Pga3dBivectorWeight(0, 1, 0),
          Pga3dBivectorWeight(0, 0, 1),
        )
      ),
      makeGenT(3, FlatDoubleSerializer.read[Pga3dBivectorWeight]),
    )

  val rotors: Gen[Pga3dRotor] =
    Gen.oneOf(
      Gen.oneOf(
        ArraySeq(
          Pga3dRotor.id,
          -Pga3dRotor.id,
          Pga3dRotor()
        )
      ),
      makeGenT(4, FlatDoubleSerializer.read[Pga3dRotor])
    )

  val normalizedRotors: Gen[Pga3dRotor] =
    rotors.filter(_.norm > 1e-40).map(_.normalizedByNorm)

  val points: Gen[Pga3dPoint] =
    makeGenT(3, FlatDoubleSerializer.read[Pga3dPoint])

  val vectors: Gen[Pga3dVector] =
    Gen.oneOf(
      Gen.oneOf(
        Pga3dVector.zero,
        Pga3dVector(1, 0, 0),
        Pga3dVector(0, 1, 0),
        Pga3dVector(0, 0, 1),
      ),
      makeGenT(3, FlatDoubleSerializer.read[Pga3dVector])
    )

  val translators: Gen[Pga3dTranslator] =
    vectors.map(Pga3dTranslator.addVector)

  val normalizedMotors: Gen[Pga3dMotor] =
    for (
      v <- vectors;
      q <- normalizedRotors
    ) yield Pga3dTranslator.addVector(v).geometric(q)

  val anyMotors: Gen[Pga3dMotor] =
    makeGenT(8, FlatDoubleSerializer.read[Pga3dMotor])

  val planes: Gen[Pga3dPlane] =
    makeGenT(4, FlatDoubleSerializer.read[Pga3dPlane])

  val planeIdeals: Gen[Pga3dPlaneIdeal] =
    Gen.oneOf(
      Gen.oneOf(
        Pga3dPlaneIdeal(1, 0, 0),
        Pga3dPlaneIdeal(0, 1, 0),
        Pga3dPlaneIdeal(0, 0, 1),
      ),
      makeGenT(3, FlatDoubleSerializer.read[Pga3dPlaneIdeal])
    )

  val projectivePoints: Gen[Pga3dProjectivePoint] =
    makeGenT(4, FlatDoubleSerializer.read[Pga3dProjectivePoint])

  val multivectors: Gen[Pga3dMultivector] =
    makeGenT(16, FlatDoubleSerializer.read[Pga3dMultivector])

  def matrices(h: Int, w: Int): Gen[Matrix] =
    Gen.containerOfN[Array, Double](h * w, double1).map(arr => Matrix.fromValues(h, w)(arr *))
