package me.kright.gametools.pga.codegen.scalagen.common.ops

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, GeneratedValue, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefNorm:
  def apply(normSquareName: String,
            normName: String,
            normVecName: String,
            normSquare: MultiVector[Sym] => MultiVector[Sym])(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, s) =>
      val squareValue = normSquare(s)
      val isConstantOne = squareValue.values.toSeq match
        case Seq((blade, value)) => blade.grade == 0 && value == Sym.one
        case _ => false
      if (isConstantOne) {
        // constant-norm classes (e.g. a point with its fixed unit blade): fold to constants
        // and keep the class type instead of widening through the division
        GeneratedCode { code =>
          code(s"\ndef $normSquareName: Double = 1.0")
          code(s"\ndef $normName: Double = 1.0")
          code(s"\ndef $normVecName: ${cls.typeName} = this")
        }
      } else {
        GeneratedValue(cls, normSquareName, squareValue).flatMap { lines =>
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
    }
