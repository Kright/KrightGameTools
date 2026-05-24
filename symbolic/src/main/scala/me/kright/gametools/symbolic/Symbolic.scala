package me.kright.gametools.symbolic


sealed trait Symbolic[+F, +S]:
  def isFunc: Boolean =
    this.isInstanceOf[Symbolic.Func[?, ?]]

  def isSymbol: Boolean =
    this.isInstanceOf[Symbolic.Symbol[?]]


object Symbolic:
  def apply[F, S](f: F, a: Seq[Symbolic[F, S]]): Func[F, S] = Symbolic.Func[F, S](f, a)

  def apply[F, S](a: S): Symbolic[F, S] = Symbolic.Symbol(a)

  case class Symbol[+A](value: A) extends Symbolic[Nothing, A]:
    override def toString: String =
      value.toString

  case class Func[+F, +A](func: F, args: Seq[Symbolic[F, A]]) extends Symbolic[F, A]:
    override def toString: String =
      args.mkString(s"${func}(", ", ", ")")


  extension [F, S](expr: Symbolic[F, S])
    def mapSymbols[S2](f: S => S2): Symbolic[F, S2] =
      expr match
        case Symbol(symbol) => Symbol(f(symbol))
        case Func(func, args) => Func(func, args.map(_.mapSymbols(f)))

    def flatMapSymbols[S2](f: S => Symbolic[F, S2]): Symbolic[F, S2] =
      expr match
        case Symbol(symbol) => f(symbol)
        case Func(func, args) => Func(func, args.map(_.flatMapSymbols(f)))


    def mapFunctions[F2](f: F => F2): Symbolic[F2, S] =
      expr match
        case sa@Symbol(_) => sa
        case Func(func, args) => Func(f(func), args.map(_.mapFunctions(f)))

    def flatMapFunctions[F2](f: (F, Seq[Symbolic[F2, S]]) => Symbolic[F2, S]): Symbolic[F2, S] =
      expr match
        case sa@Symbol(_) => sa
        case Func(func, args) => f(func, args.map(_.flatMapFunctions(f)))


    def map[F2, S2](fSymbol: S => S2, fFunc: F => F2): Symbolic[F2, S2] =
      expr match
        case Symbol(a) => Symbol(fSymbol(a))
        case Func(func, args) => Func(fFunc(func), args.map(_.map(fSymbol, fFunc)))

    def flatMap[F2, S2](fmapSymbol: S => Symbolic[F2, S2],
                        fmapFunc: (F, Seq[Symbolic[F2, S2]]) => Symbolic[F2, S2]): Symbolic[F2, S2] =
      expr match
        case Symbol(a) => fmapSymbol(a)
        case Func(func, args) => fmapFunc(func, args.map(_.flatMap(fmapSymbol, fmapFunc)))


  extension (expr: Symbolic[String, ?])
    def isFuncWithName(name: String): Boolean =
      expr match
        case Symbolic.Func(realName, _) if name == realName => true
        case _ => false