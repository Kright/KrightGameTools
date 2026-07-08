package me.kright.gametools.pga.codegen.scala.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga2d.Pga2dScalaAlgebra.motor

object DefRenormalizedForMotor:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>

      val self = cls.self

      if (cls == motor) {
        code(
          s"""
             |/**
             | * 2D motors satisfy m * reverse(m) = scalar exactly, so renormalization is a uniform scale
             | */
             |def renormalized: Pga2dMotor =
             |  val a = 1.0 / Math.sqrt(s * s + xy * xy)
             |  Pga2dMotor(
             |    s = a * s,
             |    wx = a * wx,
             |    wy = a * wy,
             |    xy = a * xy,
             |  )""".stripMargin)
      }
    }
  }
