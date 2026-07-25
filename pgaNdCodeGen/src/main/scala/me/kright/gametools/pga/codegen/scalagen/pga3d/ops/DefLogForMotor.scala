package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{bivector, bivectorBulk, bivectorWeight, motor, rotor, translator}
import me.kright.gametools.symbolic.Sym

object DefLogForMotor:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self

      if (cls == motor) {
        val vb = self.grade(2)
        val result = vb * Sym("b") + vb.bulk.dual * Sym("c")

        code(s"\ndef log(): ${bivector.typeName} =")
        code.block {
          code(
            s"""val scalar = s
               |if (s < 0.0) return (-this).log()
               |
               |val lenXYZ2 = xy * xy + xz * xz + yz * yz
               |val lenXYZ = Math.sqrt(lenXYZ2)
               |val angle = Math.atan2(lenXYZ, scalar)
               |
               |// 1 / sin^2 for a normalized motor; (1.0 - scalar * scalar) is the same value,
               |// but cancels catastrophically for small angles (relative error ~eps / angle^2)
               |val a = 1.0 / lenXYZ2
               |
               |// for a normalized motor sin(angle) = lenXYZ, so this is angle / sin(angle)
               |val b = if (Math.abs(angle) > 1e-5) {
               |  angle / lenXYZ
               |} else {
               |  // x/sin(x) = 1 / (sin(x)/x) = 1 / (1 - x^2/6 + x^4/120 - ...);
               |  // substitute v = x^2/6 - x^4/120 + ... into 1/(1 - v) = 1 + v + v^2 + ...:
               |  //   x/sin(x) = 1 + x^2/6 + (1/36 - 1/120)*x^4 + ...
               |  //            = 1 + x^2/6 + 7*x^4/360 + ...
               |  // at x <= 1e-5 the dropped 7*x^4/360 <= 2e-22 relative term is far below 1e-17,
               |  // so the second-order form is exact in double
               |  1.0 + angle * angle / 6.0
               |}
               |
               |// c = a * i * (1 - scalar * b); for a normalized motor scalar = cos(x), lenXYZ = sin(x),
               |// a = 1/sin(x)^2 and b = x/sin(x), so c = i * (1 - cos(x)*b) / sin(x)^2. Step by step:
               |//   cos(x)*b = (1 - x^2/2 + x^4/24 - ...) * (1 + x^2/6 + 7*x^4/360 + ...)
               |//            = 1 + (1/6 - 1/2)*x^2 + (7/360 - 1/12 + 1/24)*x^4 + ...
               |//            = 1 - x^2/3 - x^4/45 - ...
               |//   1 - cos(x)*b = x^2/3 + x^4/45 + ...
               |//   sin(x)^2 = (x - x^3/6 + ...)^2 = x^2 - x^4/3 + ... = x^2 * (1 - x^2/3 + ...)
               |//   1/sin(x)^2 = (1 + x^2/3 + ...) / x^2   (again via 1/(1 - v) = 1 + v + ...)
               |//   c/i = (x^2/3 + x^4/45 + ...) * (1 + x^2/3 + ...) / x^2
               |//       = 1/3 + (1/9 + 1/45)*x^2 + ... = 1/3 + 2*x^2/15 + ...
               |// carrying the x^4 terms through the same steps gives the dropped term 2*x^4/63;
               |// at x <= 1e-5 it is <= 3.2e-22, relatively far below 1e-17, so the second-order
               |// form is exact in double
               |val c = if (Math.abs(angle) > 1e-5) {
               |  a * i * (1.0 - scalar * b)
               |} else {
               |  (1.0 / 3.0 + angle * angle * (2.0 / 15.0)) * i
               |}
               |
               |${bivector.makeConstructor(result)}
               |""".stripMargin)
        }
      }
      if (cls == translator) {
        code(s"\ndef log(): ${bivectorWeight.typeName} =")
        code.block {
          code(bivectorWeight.makeConstructor(self.weight))
        }
      }
      if (cls == rotor) {
        val vb = self.grade(2)
        val result = vb * Sym("b")

        code(s"\ndef log(): ${bivectorBulk.typeName} =")
        code.block {
          code(
            s"""val scalar = s
               |if (s < 0.0) return (-this).log()
               |
               |val lenXYZ = Math.sqrt(xy * xy + xz * xz + yz * yz)
               |val angle = Math.atan2(lenXYZ, scalar)
               |
               |// for a normalized rotor sin(angle) = lenXYZ, so this is angle / sin(angle);
               |// dividing by lenXYZ directly avoids the catastrophic cancellation that the
               |// equivalent sqrt(1.0 - scalar * scalar) form has for small angles
               |val b = if (Math.abs(angle) > 1e-5) {
               |  angle / lenXYZ
               |} else {
               |  // x/sin(x) = 1 / (sin(x)/x) = 1 / (1 - x^2/6 + x^4/120 - ...);
               |  // substitute v = x^2/6 - x^4/120 + ... into 1/(1 - v) = 1 + v + v^2 + ...:
               |  //   x/sin(x) = 1 + x^2/6 + (1/36 - 1/120)*x^4 + ...
               |  //            = 1 + x^2/6 + 7*x^4/360 + ...
               |  // at x <= 1e-5 the dropped 7*x^4/360 <= 2e-22 relative term is far below 1e-17,
               |  // so the second-order form is exact in double
               |  1.0 + angle * angle / 6.0
               |}
               |
               |${bivectorBulk.makeConstructor(result)}
               |""".stripMargin)
        }
      }
    }
  }
