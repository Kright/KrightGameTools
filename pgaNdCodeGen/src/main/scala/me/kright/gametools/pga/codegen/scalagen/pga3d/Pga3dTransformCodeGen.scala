package me.kright.gametools.pga.codegen.scalagen.pga3d

import me.kright.gametools.pga.codegen.scalagen.common.ScalaTransformCodeGen

/**
 * The 3d transform generator (Pga3dProjectiveTransform for normalized = false, Pga3dTransform for
 * normalized = true): the generic [[ScalaTransformCodeGen]] machinery plus the moment (dual) 3x3 block
 * of the bivector sandwich, which does not exist in 2d (there the grade-2 elements are the point family
 * and their translation column is already cached as t).
 */
class Pga3dTransformCodeGen(normalized: Boolean) extends ScalaTransformCodeGen(normalized)(using Pga3dScalaAlgebra):

  override protected def registerMomentBlock(): Unit =
    import Pga3dScalaAlgebra.{bivectorBulk, bivectorWeight}
    // d: the moment block of the bivector sandwich, mixing the bulk part into the weight part
    newGroup()
    val bulkColumns = bivectorBulk.variableFields.map(f => forwardOp.apply(basisInput(f)))
    for ((outF, i) <- bivectorWeight.variableFields.zipWithIndex) {
      addLine(bulkColumns.zipWithIndex.map { (column, j) =>
        val name = s"d$i$j"
        register(name, outputExpr(column, outF))
        name
      })
    }

  override protected def momentBlockDocLines: Seq[String] = Seq(
    " *  - d00..d22: the dual (moment) 3x3 block for bivectors, which mixes the bulk part into the weight part;",
    " *    the reverse block is expressed through the same coefficients.",
  )
