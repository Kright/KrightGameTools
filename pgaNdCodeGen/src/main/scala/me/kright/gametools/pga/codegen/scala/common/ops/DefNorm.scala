package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, GeneratedValue, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefNorm:
  def apply(normSquareName: String,
            normName: String,
            normVecName: String,
            normSquare: MultiVector[Sym] => MultiVector[Sym])(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, s) =>
      GeneratedValue(cls, normSquareName, normSquare(s)).flatMap { lines =>
        GeneratedCode { code =>
          code(lines)

          code(s"\ndef $normName: Double =")
          code.block {
            code(s"Math.sqrt($normSquareName)")
          }

          code(s"\ndef $normVecName =")
          code.block {
            code(s"this / $normName")
          }
        }
      }
    }
