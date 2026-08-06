package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{bivector, bivectorBulk, bivectorWeight}

/**
 * dexp and dexpInv: the differential of exp and its inverse, needed to integrate ODEs on the
 * motor group (RKMK4 and friends). See the derivation comment in [[SharedFormulas]].
 */
object DefDexpForBivector:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self
      val prefix = Pga3dScalaAlgebra.typeNamePrefix
      val b = bivector.makeSymbolic("b")

      def render(template: String): Unit =
        code(FormulaTemplate.renderScala(template, prefix))

      if (cls == bivector) {
        val ub = bivector.makeSymbolic("ub")
        val uub = bivector.makeSymbolic("uub")

        code(
          s"""
             |/**
             | * The differential of exp: maps the direction b of change of this bivector u to the resulting
             | * velocity of u.exp, expressed as a bivector (the left trivialization):
             | *   (u + b * h).exp == (u.dexp(b) * h).exp.geometric(u.exp) + O(h^2)
             | * In SE(3)/robotics terms this is the left Jacobian of exp applied to b;
             | * the right Jacobian (for the motor.geometric(u.exp) update convention) is (-u).dexp(b).
             | * The closed form (x is `cross`, c is the dual angle of u, c^2 = -u.geometric(u)):
             | *   dexp(u, b) = b + (sin(c)^2/c^2) * (u x b) + ((2c - sin(2c))/(2c^3)) * (u x (u x b))
             | * Degenerate cases are exact and NaN-free: ${bivector.typeName}.zero.dexp(b) == b, and for a
             | * pure-weight u the series terminates at b + u.cross(b).
             | * The inverse is dexpInv: u.dexpInv(u.dexp(b)) == b.
             | */
             |def dexp(b: ${bivector.typeName}): ${bivector.typeName} =""".stripMargin)
        code.block {
          render(SharedFormulas.dexpSinDivLen)
          code("")
          render(SharedFormulas.dexpSinMinusCos)
          code("")
          render(SharedFormulas.dexpPseudoScalarP(self))
          code("")
          render(SharedFormulas.dexpK1)
          code("")
          render(SharedFormulas.dexpK1Dual)
          code("")
          render(SharedFormulas.dexpK2)
          code("")
          render(SharedFormulas.dexpK2Dual)
          code("")
          code("val ub = this.cross(b)")
          code("val uub = this.cross(ub)")
          code("")
          code(bivector.makeConstructor(SharedFormulas.dexpResult(b, ub, uub)))
        }

        code(
          s"""
             |/**
             | * The inverse of the differential of exp: u.dexpInv(u.dexp(b)) == b. Maps the velocity of
             | * u.exp (as a bivector, left trivialization) back to the rate of change of u itself - the
             | * workhorse of Lie-group ODE integrators (RKMK4), which integrate in the flat bivector space
             | * and return to the group with one exp, keeping the motor normalized by construction.
             | * The closed form (x is `cross`, c is the dual angle of u, c^2 = -u.geometric(u)):
             | *   dexpInv(u, b) = b - (u x b) + ((1 - c*cot(c))/c^2) * (u x (u x b))
             | * The coefficient of (u x b) is exactly -1: the odd Bernoulli numbers beyond B1 vanish.
             | * Singular at bulkNorm == pi, where exp stops being injective. Degenerate cases are exact
             | * and NaN-free: ${bivector.typeName}.zero.dexpInv(b) == b, pure-weight u gives b - u.cross(b).
             | */
             |def dexpInv(b: ${bivector.typeName}): ${bivector.typeName} =""".stripMargin)
        code.block {
          render(SharedFormulas.dexpSinDivLen)
          code("")
          render(SharedFormulas.dexpSinMinusCos)
          code("")
          render(SharedFormulas.dexpPseudoScalarP(self))
          code("")
          render(SharedFormulas.dexpInvK3)
          code("")
          render(SharedFormulas.dexpInvK3Dual)
          code("")
          code("val ub = this.cross(b)")
          code("val uub = this.cross(ub)")
          code("")
          code(bivector.makeConstructor(SharedFormulas.dexpInvResult(b, ub, uub)))
        }
      }

      if (cls == bivectorBulk) {
        val ub = bivector.makeSymbolic("ub")
        val uub = bivector.makeSymbolic("uub")

        code(
          s"""
             |/**
             | * The differential of exp at a pure-bulk u (the classical so(3) case, Rodrigues-style
             | * coefficients): (u + b * h).exp == (u.dexp(b) * h).exp.geometric(u.exp) + O(h^2).
             | * See ${bivector.typeName}.dexp for the general form; here the angle c = bulkNorm is real,
             | * so the dual (weight) corrections of the coefficients vanish.
             | */
             |def dexp(b: ${bivector.typeName}): ${bivector.typeName} =""".stripMargin)
        code.block {
          render(SharedFormulas.dexpSinDivLen)
          code("")
          render(SharedFormulas.dexpK1)
          code("")
          render(SharedFormulas.dexpK2)
          code("")
          code("val ub = this.cross(b)")
          code("val uub = this.cross(ub)")
          code("")
          code(bivector.makeConstructor(SharedFormulas.dexpBulkResult(b, ub, uub)))
        }

        code(
          s"""
             |/**
             | * The inverse of the differential of exp at a pure-bulk u (the classical so(3) case):
             | * u.dexpInv(u.dexp(b)) == b. See ${bivector.typeName}.dexpInv for the general form;
             | * here the angle c = bulkNorm is real, so the dual (weight) correction vanishes.
             | * Singular at bulkNorm == pi, where exp stops being injective.
             | */
             |def dexpInv(b: ${bivector.typeName}): ${bivector.typeName} =""".stripMargin)
        code.block {
          render(SharedFormulas.dexpSinDivLen)
          code("")
          render(SharedFormulas.dexpSinMinusCos)
          code("")
          render(SharedFormulas.dexpInvK3)
          code("")
          code("val ub = this.cross(b)")
          code("val uub = this.cross(ub)")
          code("")
          code(bivector.makeConstructor(SharedFormulas.dexpInvBulkResult(b, ub, uub)))
        }
      }

      if (cls == bivectorWeight) {
        val ub = bivectorWeight.makeSymbolic("ub")

        code(
          s"""
             |/**
             | * The differential of exp at a pure-weight (ideal) u. The commutator operator is nilpotent
             | * here (u x (u x b) == 0), so the series terminates and the two-term form is exact:
             | *   dexp(u, b) = b + u.cross(b)
             | * See ${bivector.typeName}.dexp for the general form.
             | */
             |def dexp(b: ${bivector.typeName}): ${bivector.typeName} =""".stripMargin)
        code.block {
          code("val ub = this.cross(b)")
          code(bivector.makeConstructor(SharedFormulas.dexpWeightResult(b, ub)))
        }

        code(
          s"""
             |/**
             | * The inverse of the differential of exp at a pure-weight (ideal) u; exact, no trigonometry:
             | *   dexpInv(u, b) = b - u.cross(b)
             | * See ${bivector.typeName}.dexpInv for the general form.
             | */
             |def dexpInv(b: ${bivector.typeName}): ${bivector.typeName} =""".stripMargin)
        code.block {
          code("val ub = this.cross(b)")
          code(bivector.makeConstructor(SharedFormulas.dexpInvWeightResult(b, ub)))
        }
      }
    }
  }
