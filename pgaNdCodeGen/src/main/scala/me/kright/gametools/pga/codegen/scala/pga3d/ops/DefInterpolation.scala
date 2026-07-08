package me.kright.gametools.pga.codegen.scala.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga3d.Pga3dScalaAlgebra.{motor, rotor}

object DefInterpolation:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      if (cls == rotor || cls == motor) {
        val renorm = if (cls == rotor) "normalizedByNorm" else "renormalized"

        code(
          s"""
             |/**
             | * Spherical linear interpolation along the geodesic from this (t = 0) to b (t = 1),
             | * with constant angular velocity. Computes this * (this.reverse.geometric(b))^t via log/exp.
             | * Assumes both this and b are normalized; the result is normalized.
             | * Near-antipodal inputs (b close to -this) are ill-conditioned: the geodesic is undefined
             | * there and the interpolation direction becomes numerically unstable.
             | */
             |def slerp(b: ${cls.typeName}, t: Double): ${cls.typeName} =
             |  this.geometric(this.reverse.geometric(b).log().exp(t))
             |
             |/**
             | * Normalized linear interpolation: the componentwise lerp this * (1 - t) + b * t, renormalized.
             | * A fast APPROXIMATION of slerp: the angular velocity is not constant, so intermediate values
             | * deviate from the true geodesic. The error is negligible for small angles between this and b
             | * and grows toward the midpoint of large rotations. Assumes both inputs are normalized.
             | */
             |def nlerp(b: ${cls.typeName}, t: Double): ${cls.typeName} =
             |  (this * (1.0 - t) + b * t).${renorm}""".stripMargin)
      }
    }
  }
