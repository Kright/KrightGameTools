package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.ScalaMultivectorSubClass.{point, pointCenter, projectivePoint, vector}
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, MultivectorUnaryOp}

object DefConstAndDualFields:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        for ((f, v) <- cls.constantFields) {
          code(s"inline val ${f.name} = ${v}")
        }

        if (Seq(vector, projectivePoint, point, pointCenter).contains(cls)) {
          cls.self.values.foreach { (b, sym) =>
            val fName = s"${pga3.representation(b)}"
            code("")
            code(s"inline def $fName: Double = ${sym}")
          }
        }
      }
    }
