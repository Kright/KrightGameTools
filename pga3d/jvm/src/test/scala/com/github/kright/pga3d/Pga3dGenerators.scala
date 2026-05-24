package com.github.kright.pga3d

import com.github.kright.matrix.Matrix
import com.github.kright.mathutil.FlatDoubleSerializer
import org.scalacheck.Gen

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
        Seq(
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
        Seq(
          Pga3dBivectorWeight.zero,
          Pga3dBivectorWeight(1, 0, 0),
          Pga3dBivectorWeight(0, 1, 0),
          Pga3dBivectorWeight(0, 0, 1),
        )
      ),
      makeGenT(3, FlatDoubleSerializer.read[Pga3dBivectorWeight]),
    )

  val quaternions: Gen[Pga3dQuaternion] =
    Gen.oneOf(
      Gen.oneOf(
        Seq(
          Pga3dQuaternion.id,
          -Pga3dQuaternion.id,
          Pga3dQuaternion()
        )
      ),
      makeGenT(4, FlatDoubleSerializer.read[Pga3dQuaternion])
    )

  val normalizedQuaternions: Gen[Pga3dQuaternion] =
    quaternions.filter(_.norm > 1e-40).map(_.normalizedByNorm)

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
      q <- normalizedQuaternions
    ) yield Pga3dTranslator.addVector(v).geometric(q)

  val anyMotors: Gen[Pga3dMotor] =
    makeGenT(8, FlatDoubleSerializer.read[Pga3dMotor])

  def matrices(h: Int, w: Int): Gen[Matrix] =
    Gen.containerOfN[Array, Double](h * w, double1).map(arr => Matrix.fromValues(h, w)(arr *))
