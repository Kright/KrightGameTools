package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra.{projectivePoint, vector}

/**
 * dexp and dexpInv: the differential of exp and its inverse, needed to integrate ODEs on the
 * motor group. See the derivation comment in [[SharedFormulas]]. In 2d every grade-2 element u
 * has bulk ^ weight = 0, so u * u = -bulkNormSquare exactly: the dual angle of 3d degenerates
 * to the real angle and only the real coefficients remain (like the 3d pure-bulk case).
 * The commutator of two grade-2 elements is always ideal (a vector): the rotational part of
 * se(2) is one-dimensional and commutes.
 */
object DefDexpForBivector:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val prefix = Pga2dScalaAlgebra.typeNamePrefix
      val b = projectivePoint.makeSymbolic("b")

      def render(template: String): Unit =
        code(FormulaTemplate.renderScala(template, prefix))

      if (cls == projectivePoint) {
        val ub = vector.makeSymbolic("ub")
        val uub = vector.makeSymbolic("uub")

        code(
          s"""
             |/**
             | * The differential of exp: maps the direction b of change of this grade-2 element u to the
             | * resulting velocity of u.exp, expressed as a grade-2 element (the left trivialization):
             | *   (u + b * h).exp == (u.dexp(b) * h).exp.geometric(u.exp) + O(h^2)
             | * In SE(2)/robotics terms this is the left Jacobian of exp applied to b;
             | * the right Jacobian (for the motor.geometric(u.exp) update convention) is (-u).dexp(b).
             | * The closed form (x is `cross`, the angle c = bulkNorm is real in 2d: u * u = -c^2 exactly):
             | *   dexp(u, b) = b + (sin(c)^2/c^2) * (u x b) + ((2c - sin(2c))/(2c^3)) * (u x (u x b))
             | * Degenerate cases are exact and NaN-free: ${projectivePoint.typeName}.zero.dexp(b) == b,
             | * and for an ideal u (w == 0) the series terminates at b + u.cross(b).
             | * The inverse is dexpInv: u.dexpInv(u.dexp(b)) == b.
             | */
             |def dexp(b: ${projectivePoint.typeName}): ${projectivePoint.typeName} =""".stripMargin)
        code.block {
          render(SharedFormulas.expSinDivLen("bulkNorm"))
          code("")
          render(SharedFormulas.dexpK1)
          code("")
          render(SharedFormulas.dexpK2)
          code("")
          code("val ub = this.cross(b)")
          code("val uub = this.cross(ub)")
          code("")
          code(projectivePoint.makeConstructor(SharedFormulas.dexpBulkResult(b, ub, uub)))
        }

        code(
          s"""
             |/**
             | * The inverse of the differential of exp: u.dexpInv(u.dexp(b)) == b. Maps the velocity of
             | * u.exp (as a grade-2 element, left trivialization) back to the rate of change of u itself -
             | * the workhorse of Lie-group ODE integrators, which integrate in the flat grade-2 space and
             | * return to the group with one exp, keeping the motor normalized by construction.
             | * The closed form (x is `cross`, the angle c = bulkNorm is real in 2d):
             | *   dexpInv(u, b) = b - (u x b) + ((1 - c*cot(c))/c^2) * (u x (u x b))
             | * The coefficient of (u x b) is exactly -1: the odd Bernoulli numbers beyond B1 vanish.
             | * Singular at bulkNorm == pi, where exp stops being injective. Degenerate cases are exact
             | * and NaN-free: ${projectivePoint.typeName}.zero.dexpInv(b) == b, an ideal u (w == 0)
             | * gives b - u.cross(b).
             | */
             |def dexpInv(b: ${projectivePoint.typeName}): ${projectivePoint.typeName} =""".stripMargin)
        code.block {
          render(SharedFormulas.expSinDivLen("bulkNorm"))
          code("")
          render(SharedFormulas.expSinMinusCos)
          code("")
          render(SharedFormulas.dexpInvK3)
          code("")
          code("val ub = this.cross(b)")
          code("val uub = this.cross(ub)")
          code("")
          code(projectivePoint.makeConstructor(SharedFormulas.dexpInvBulkResult(b, ub, uub)))
        }
      }

      if (cls == vector) {
        val ub = vector.makeSymbolic("ub")

        code(
          s"""
             |/**
             | * The differential of exp at an ideal (pure-translation) u. The commutator operator is
             | * nilpotent here (u x (u x b) == 0), so the series terminates and the two-term form is exact:
             | *   dexp(u, b) = b + u.cross(b)
             | * See ${projectivePoint.typeName}.dexp for the general form.
             | */
             |def dexp(b: ${projectivePoint.typeName}): ${projectivePoint.typeName} =""".stripMargin)
        code.block {
          code("val ub = this.cross(b)")
          code(projectivePoint.makeConstructor(SharedFormulas.dexpWeightResult(b, ub)))
        }

        code(
          s"""
             |/**
             | * The inverse of the differential of exp at an ideal (pure-translation) u; exact,
             | * no trigonometry:
             | *   dexpInv(u, b) = b - u.cross(b)
             | * See ${projectivePoint.typeName}.dexpInv for the general form.
             | */
             |def dexpInv(b: ${projectivePoint.typeName}): ${projectivePoint.typeName} =""".stripMargin)
        code.block {
          code("val ub = this.cross(b)")
          code(projectivePoint.makeConstructor(SharedFormulas.dexpInvWeightResult(b, ub)))
        }
      }
    }
  }
