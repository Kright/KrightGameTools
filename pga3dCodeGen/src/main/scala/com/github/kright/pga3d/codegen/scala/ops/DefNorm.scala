package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.{MultiVector, PGA3}
import com.github.kright.pga3d.codegen.*
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, GeneratedValue, MultivectorUnaryOp}
import com.github.kright.symbolic.Sym

object DefNorm:
  def apply(normSquareName: String,
            normName: String,
            normVecName: String,
            normSquare: MultiVector[Sym] => MultiVector[Sym])(using pga3: PGA3): MultivectorUnaryOp =
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
