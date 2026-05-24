package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}

object DefRenormalizedForMotor:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>

      val self = cls.self

      if (cls == ScalaMultivectorSubClass.motor) {
        code(
          s"""
             |/**
             | * see [[https://arxiv.org/abs/2206.07496]], page 14
             | * and [[https://https://bivector.net/PGAdyn.pdf.net/PGAdyn.pdf]], page 42
             | */
             |def renormalized: Pga3dMotor =
             |  val a2 = 1.0 / (s * s + xy * xy + xz * xz + yz * yz)
             |  val a = Math.sqrt(a2)
             |  val b = (s * i - wx * yz + wy * xz - wz * xy) * a * a2
             |  Pga3dMotor(
             |    s = a * s,
             |    wx = a * wx + b * yz,
             |    wy = a * wy - b * xz,
             |    wz = a * wz + b * xy,
             |    xy = a * xy,
             |    xz = a * xz,
             |    yz = a * yz,
             |    i = a * i - b * s,
             |  )""".stripMargin)
      }
    }
  }

