package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefConstAndDualFields:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, _) =>
      GeneratedCode { code =>
        for ((f, v) <- cls.constantFields) {
          code(s"inline val ${f.name} = ${v}")
        }

        if (Seq(algebra.vector, algebra.projectivePoint, algebra.point, algebra.pointCenter).contains(cls)) {
          cls.self.values.foreach { (b, sym) =>
            val fName = s"${algebra.pga.representation(b)}"
            code("")
            code(s"inline def $fName: Double = ${sym}")
          }
        }
      }
    }
