package me.kright.gametools.symbolic

object SymbolicToPrettyString:
  def apply[S](expr: Symbolic[String, S]): String =
    toStr(toSymbol(expr))
      .replace(" + -1.0 * ", " - ")
      .replace(" + -", " - ")
      .replace("-1.0 * ", "-")

  private def toSymbol[S](expr: Symbolic[String, S]): Symbolic[Nothing, String] =
    expr.flatMap({
      symbolValue => Symbolic.Symbol[String](symbolValue.toString)
    }, {
      case ("*", elems) => Symbolic.Symbol[String](elems.map(toStr).mkString("", " * ", ""))
      case ("+", elems) => Symbolic.Symbol[String](elems.map(toStr).mkString("(", " + ", ")"))
      case (name, elems) if elems.size == 2 => Symbolic.Symbol[String](elems.map(toStr).mkString("(", s" $name ", ")"))
      case (name, elems) => Symbolic.Symbol[String](elems.map(toStr).mkString(s"${name}(", ", ", ")"))
    })

  private def toStr(symbol: Symbolic[Nothing, String]): String =
    @unchecked
    val Symbolic.Symbol(result) = symbol
    result
