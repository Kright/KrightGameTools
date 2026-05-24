package me.kright.gametools.vector

import com.github.kright.mathutil.MathUtil
import org.scalacheck.Gen

object VectorMathGenerators:
  val gaussian: Gen[Double] = Gen.gaussian(mean = 0, stdDev = 1)
  val double1: Gen[Double] = Gen.double.map(_ * 2.0 - 1.0)

  val angleRadians: Gen[Double] = Gen.double.map(2.0 * Math.PI * _)

  def doubleInRange(min: Double, max: Double): Gen[Double] =
    Gen.oneOf(
      Gen.oneOf(Seq(
        min,
        max,
        0.5 * (min + max),
      )),
      Gen.oneOf(
        Seq(
          1e-1,
          1e-2,
          1e-3,
          1e-4,
          1e-5,
          1e-6,
          1e-8,
          1e-10,
          1e-12,
          1e-15,
          1e-20
        ).flatMap { eps =>
          Seq(
            MathUtil.interpolate(min, max, eps),
            MathUtil.interpolate(min, max, 1.0 - eps),
            MathUtil.interpolate(min, max, 0.5 + eps),
            MathUtil.interpolate(min, max, 0.5 - eps),
          )
        }),
      Gen.double.map { d =>
        d * (max - min) + min
      }
    )

  val vectors2InCube: Gen[Vector2d] =
    for (x <- double1;
         y <- double1)
    yield Vector2d(x, y)

  val zeroVector3: Gen[Vector3d] =
    Gen.const(Vector3d(0.0, 0.0, 0.0))

  val axisVector3: Gen[Vector3d] =
    Gen.oneOf(
      Seq(
        Vector3d(1.0, 0.0, 0.0),
        Vector3d(0.0, 1.0, 0.0),
        Vector3d(0.0, 0.0, 1.0),
      )
    )

  val vectors3InCube: Gen[Vector3d] =
    Gen.oneOf(
      {
        for (
          multiplier <- double1;
          axis <- axisVector3
        ) yield axis * multiplier
      },
      vectorInAABB(Vector3d(-1.0, -1.0, -1.0), Vector3d(1.0, 1.0, 1.0))
    )

  def vectorInAABB(min: Vector3d, max: Vector3d): Gen[Vector3d] =
    for {
      x <- doubleInRange(min.x, max.x)
      y <- doubleInRange(min.y, max.y)
      z <- doubleInRange(min.z, max.z)
    } yield Vector3d(x, y, z)

  val vectors3normalized: Gen[Vector3d] =
    Gen.oneOf(
      axisVector3,
      vectors3InCube.filter(_.mag > 1e-12).map(_.normalized())
    )

  val vectors4InCube: Gen[Vector4d] =
    for (x <- double1;
         y <- double1;
         z <- double1;
         w <- double1)
    yield Vector4d(x, y, z, w)
