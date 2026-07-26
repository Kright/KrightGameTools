package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{pointCenter, vector}

object DefObjectMethodsForVector:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        if (cls == vector) {
          code("")
          code.comment(
            s"""classical cross product for a right-handed basis: crossRightHanded(x, y) == z.
               |It is not an operation of geometric algebra and exists for convenience and for adapting code
               |from other libraries. In GA terms it is the join (a v b), an ideal line, read back as a vector
               |by the commutator product with the origin.""".stripMargin)
          code(s"def crossRightHanded(a: ${cls.name}, b: ${cls.name}): ${cls.name} =")
          code.block {
            code(s"${pointCenter.name}.cross(a.antiWedge(b))")
          }
          code("")
          code.comment(
            s"""classical cross product for a left-handed basis: crossLeftHanded(x, y) == -z,
               |the negation of [[crossRightHanded]]. It is not an operation of geometric algebra.""".stripMargin)
          code(s"def crossLeftHanded(a: ${cls.name}, b: ${cls.name}): ${cls.name} =")
          code.block {
            code("crossRightHanded(b, a)")
          }
        }
      }
    }
