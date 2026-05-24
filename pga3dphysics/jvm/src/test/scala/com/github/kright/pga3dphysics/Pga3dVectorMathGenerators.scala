package com.github.kright.pga3dphysics

import me.kright.gametools.mathutil.MathUtil
import org.scalacheck.Gen

object Pga3dVectorMathGenerators:
  val gaussian: Gen[Double] = Gen.gaussian(mean = 0, stdDev = 1)
  val double01: Gen[Double] = Gen.double
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
