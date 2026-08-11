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
      case ("+", elems) => Symbolic.Symbol[String](renderSum(elems.map(toStr)))
      case (name, elems) if elems.size == 2 => Symbolic.Symbol[String](elems.map(toStr).mkString("(", s" $name ", ")"))
      case (name, elems) => Symbolic.Symbol[String](elems.map(toStr).mkString(s"${name}(", ", ", ")"))
    })

  /** a summand with its sign extracted, so a pair starting with a negative term can be
   * rendered as a subtracted group: a + b - c + d becomes (a + b) - (c - d) */
  private case class SignedTerm(negative: Boolean, text: String)

  private def signedTerm(rendered: String): SignedTerm =
    if (rendered.startsWith("-1.0 * ")) SignedTerm(negative = true, rendered.stripPrefix("-1.0 * "))
    else if (rendered.startsWith("-")) SignedTerm(negative = true, rendered.drop(1))
    else SignedTerm(negative = false, rendered)

  /**
   * A sum of 4 or more terms is emitted with the terms grouped in parenthesized pairs, recursively:
   * a + b - c + d + e + f becomes (a + b) - (c - d) + (e + f). The JVM (and a C++ compiler)
   * must not reassociate floating point additions, so a flat sum is evaluated as a sequential
   * left-to-right chain; the pair grouping halves the dependency chain and lets the CPU add
   * the pairs in parallel. A pair starting with a negative term is subtracted as a whole,
   * with both terms negated inside - an exact transformation for floats - so no group starts
   * with a minus sign.
   */
  private def renderSum(rendered: Seq[String]): String =
    renderSumOfSigned(rendered.map(signedTerm))

  private def renderSumOfSigned(terms: Seq[SignedTerm]): String =
    if (terms.size < 4)
      terms.zipWithIndex.map { case (t, index) =>
        if (index == 0) (if (t.negative) s"-${t.text}" else t.text)
        else (if (t.negative) s" - ${t.text}" else s" + ${t.text}")
      }.mkString("(", "", ")")
    else
      renderSumOfSigned(terms.grouped(2).map {
        case Seq(a, b) =>
          val op = if (a.negative == b.negative) " + " else " - "
          SignedTerm(a.negative, s"(${a.text}$op${b.text})")
        case Seq(single) => single
      }.toSeq)

  private def toStr(symbol: Symbolic[Nothing, String]): String =
    val Symbolic.Symbol(result) = symbol: @unchecked
    result
