package me.kright.gametools.symbolic

object SymbolicStrOrdering:
  /** number is less than string */
  private given symbolOrdering: Ordering[SymbolicStr.S] with
    override def compare(left: SymbolicStr.S, right: SymbolicStr.S): Int =
      left match
        case leftS: String =>
          right match
            case rightS: String => summon[Ordering[String]].compare(leftS, rightS)
            case rightD: Double => 1
        case leftD: Double =>
          right match
            case rightS: String => -1
            case rightD: Double => summon[Ordering[Double]].compare(leftD, rightD)


  /** compare by func arg length, after that by func name and after that by args */
  private given funcOrdering: Ordering[Symbolic.Func[SymbolicStr.F, SymbolicStr.S]] with
    private val orderingByLengthOrFuncName: Ordering[Symbolic.Func[SymbolicStr.F, SymbolicStr.S]] =
      Ordering.by(f => (f.args.length, f.func))

    override def compare(left: Symbolic.Func[SymbolicStr.F, SymbolicStr.S],
                         right: Symbolic.Func[SymbolicStr.F, SymbolicStr.S]): Int =
      val simpleOrderingResult = orderingByLengthOrFuncName.compare(left, right)
      if (simpleOrderingResult != 0) return simpleOrderingResult

      left.args.zip(right.args).view.map { (l, r) => ordering.compare(l, r) }.find(_ != 0).getOrElse(0)

  given ordering: Ordering[SymbolicStr] with
    override def compare(left: SymbolicStr, right: SymbolicStr): Int =
      left match
        case Symbolic.Symbol(leftValue) =>
          right match
            case Symbolic.Symbol(rightValue) => symbolOrdering.compare(leftValue, rightValue)
            case Symbolic.Func(_, _) => -1
        case leftF@Symbolic.Func(_, _) =>
          right match
            case Symbolic.Symbol(_) => 1
            case rightF@Symbolic.Func(_, _) => funcOrdering.compare(leftF, rightF)
