package me.kright.gametools.pga.codegen.scalagen.pga2d

import me.kright.gametools.pga.codegen.scalagen.common.ScalaTransformCodeGen

/**
 * The 2d Pga2dTransform generator. A 2d rotation has a single degree of freedom, so only two
 * rotation entries are stored, and they are literally the cosine and sine of the rotation angle -
 * named cos and sin instead of r00 and r01.
 */
class Pga2dTransformCodeGen extends ScalaTransformCodeGen(using Pga2dScalaAlgebra):

  override protected def rotationFieldName(i: Int, j: Int): String =
    (i, j) match
      case (0, 0) => "cos"
      case (0, 1) => "sin"
      case _ => super.rotationFieldName(i, j)

  override protected def rotationDocLines: Seq[String] = Seq(
    " *  - cos, sin: the cosine and sine of the rotation angle (scaled by motor.normSquare for non-normalized",
    " *    motors); the full 2x2 rotation matrix is [[cos, sin], [-sin, cos]], and the reverse transformation",
    " *    uses its transpose.",
  )
