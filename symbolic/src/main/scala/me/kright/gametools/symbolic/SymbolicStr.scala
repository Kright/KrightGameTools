package me.kright.gametools.symbolic

type SymbolicStr = Symbolic[SymbolicStr.F, SymbolicStr.S]


object SymbolicStr:
  type F = String
  type Number = Double
  type S = Double | String

  val zero: Symbolic.Symbol[Double | String] = Symbolic.Symbol(0.0)
  val one: Symbolic.Symbol[Double | String] = Symbolic.Symbol(1.0)
  val minusOne: Symbolic.Symbol[Double | String] = Symbolic.Symbol(-1.0)

  def apply(func: String, args: Seq[SymbolicStr]): SymbolicStr =
    Symbolic.Func[String, Double | String](func, args)

  def apply(symbol: Double | String): SymbolicStr =
    Symbolic.Symbol(symbol)


  extension (expr: SymbolicStr)
    def isZero: Boolean =
      expr match
        case Symbolic.Symbol(0.0) => true
        case _ => false

    def isOne: Boolean =
      expr match
        case Symbolic.Symbol(1.0) => true
        case _ => false

    def isNumber: Boolean =
      expr match
        case Number(_) => true
        case _ => false

    def size: Int =
      expr match
        case Symbolic.Symbol(_) => 1
        case Symbolic.Func(func, args) => args.map(f => f.size).sum

  given ordering: Ordering[SymbolicStr] = SymbolicStrOrdering.ordering

  /**
   * to use this: 
   * import scala.math.Numeric.Implicits.infixNumericOps
   * import me.kright.gametools.symbolic.SymbolicStr.{given, *}
   */
  given symbolicStrNumeric: Numeric[SymbolicStr] = SymbolicStrNumeric()

  object Number:
    def apply(v: Double): SymbolicStr = SymbolicStr(v)

    def unapply(expr: SymbolicStr): Option[Double] =
      expr match
        case Symbolic.Symbol(value) =>
          value match
            case v: Double => Option(v)
            case _ => None
        case _ => None

  object SymbolStr:
    def apply(v: String): SymbolicStr = SymbolicStr(v)

    def unapply(expr: SymbolicStr): Option[String] =
      expr match
        case Symbolic.Symbol(value) =>
          value match
            case s: String => Option(s)
            case _ => None
        case _ => None
