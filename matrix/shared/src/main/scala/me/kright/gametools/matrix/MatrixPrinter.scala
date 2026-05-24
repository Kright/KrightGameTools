package me.kright.gametools.matrix

import scala.annotation.targetName

case class MatrixPrinter(elemToStr: Double => String,
                         elemSep: String,
                         rowSep: String,
                         useBrackets: Boolean) {

  @targetName("invoke")
  def apply(m: Matrix): String =
    val openBr = if (useBrackets) "[" else ""
    val closeBr = if (useBrackets) "]" else ""

    (0 until m.h).map { y =>
      (0 until m.w).map { x =>
        elemToStr(m(y, x))
      }.mkString(openBr, elemSep, closeBr)
    }.mkString(openBr, rowSep, closeBr)
}

object MatrixPrinter:
  val oneLinePrinter: MatrixPrinter =
    MatrixPrinter(elemToStr = _.toString, elemSep = ", ", rowSep = ", ", useBrackets = true)

  val squarePrinter5f: MatrixPrinter =
    MatrixPrinter(elemToStr = "%5.5f".format(_), elemSep = " ", rowSep = "\n", useBrackets = false)