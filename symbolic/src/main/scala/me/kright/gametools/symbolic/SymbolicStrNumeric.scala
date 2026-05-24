package me.kright.gametools.symbolic

/**
 * to use this: 
 * import scala.math.Numeric.Implicits.infixNumericOps
 * import me.kright.gametools.symbolic.SymbolicStr.{given, *}
 */
class SymbolicStrNumeric extends Numeric[SymbolicStr]:
  override def plus(x: SymbolicStr, y: SymbolicStr): SymbolicStr = SymbolicStr("+", Seq(x, y))

  override def minus(x: SymbolicStr, y: SymbolicStr): SymbolicStr = SymbolicStr("+", Seq(x, SymbolicStr("*", Seq(SymbolicStr(-1.0), y))))

  override def times(x: SymbolicStr, y: SymbolicStr): SymbolicStr = SymbolicStr("*", Seq(x, y))

  override def negate(x: SymbolicStr): SymbolicStr = SymbolicStr(-1.0) * x

  override def fromInt(x: Int): SymbolicStr = SymbolicStr(x.toDouble)

  override def parseString(str: String): Option[SymbolicStr] = Numeric[Double].parseString(str).map(SymbolicStr(_))

  override def toInt(x: SymbolicStr): Int = ???

  override def toLong(x: SymbolicStr): Long = ???

  override def toFloat(x: SymbolicStr): Float = ???

  override def toDouble(x: SymbolicStr): Double = ???

  override def compare(x: SymbolicStr, y: SymbolicStr): Int = ???
