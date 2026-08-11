package me.kright.gametools.pga.codegen.scalagen.pga2d

import me.kright.gametools.pga.codegen.scalagen.common.ScalaTransformCodeGen

/**
 * The 2d transform generator (Pga2dProjectiveTransform for normalized = false, Pga2dTransform for
 * normalized = true). A 2d rotation has a single degree of freedom, so only two rotation entries
 * are stored. In the normalized variant they are literally the cosine and sine of the rotation
 * angle and are named so; in the projective variant they are scaled by motor.normSquare, so the
 * honest matrix-entry names r00 and r01 are kept.
 */
class Pga2dTransformCodeGen(normalized: Boolean) extends ScalaTransformCodeGen(normalized)(using Pga2dScalaAlgebra):

  override protected def rotationFieldName(i: Int, j: Int): String =
    (i, j) match
      case (0, 0) if normalized => "cos"
      case (0, 1) if normalized => "sin"
      case _ => super.rotationFieldName(i, j)

  override protected def rotationDocLines: Seq[String] =
    if (normalized)
      Seq(
        " *  - cos, sin: the cosine and sine of the rotation angle;",
        " *    the full 2x2 rotation matrix is [[cos, sin], [-sin, cos]], and the reverse transformation",
        " *    uses its transpose.",
      )
    else
      Seq(
        " *  - r00, r01: the two independent entries of the 2x2 rotation matrix [[r00, r01], [-r01, r00]],",
        " *    the cosine and sine of the rotation angle scaled by motor.normSquare; the reverse",
        " *    transformation uses the transpose.",
      )
