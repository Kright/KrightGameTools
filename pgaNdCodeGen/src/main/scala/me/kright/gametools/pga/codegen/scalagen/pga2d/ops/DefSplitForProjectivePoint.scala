package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra.{projectivePoint, vector}

/**
 * The 2d sibling of the 3d [[me.kright.gametools.pga.codegen.scalagen.pga3d.ops.DefBivectorSplit]]:
 * decomposes a motor generator into a commuting (rotation, translation) pair. There are no screw
 * motions in 2d, so the split is all-or-nothing: a generator with w != 0 is entirely a rotation
 * around the point (x/w, y/w) - no nonzero translation commutes with it - and a generator with
 * w == 0 is entirely a translation (an ideal point, i.e. a vector).
 */
object DefSplitForProjectivePoint:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    if (cls == projectivePoint) {
      GeneratedCode { code =>
        code("")
        code("/** the commuting (rotation, translation) decomposition of a motor generator, the 2d sibling")
        code(" * of Pga3dBivector.split: this == first + second, exp == first.exp.geometric(second.exp).")
        code(" * There are no screw motions in 2d, so the split is all-or-nothing: w != 0 is a pure")
        code(" * rotation around the point (x/w, y/w) with a zero shift, w == 0 (up to 1e-100) is a pure")
        code(" * translation. */")
        code(s"def split: (${projectivePoint.name}, ${vector.name}) =")
        code.block {
          code("if (Math.abs(w) < 1e-100) {")
          code.block {
            code(s"return (${projectivePoint.name}(0.0, 0.0, w), ${vector.name}(x, y))")
          }
          code("}")
          code("")
          code(s"(this, ${vector.name}(0.0, 0.0))")
        }
      }
    } else None
  }
