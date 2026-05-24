package me.kright.gametools.ga

import me.kright.gametools.ga
import com.github.kright.mathutil.Sign

import scala.math.Numeric.Implicits.infixNumericOps

class GARepresentation(params: GARepresentationConfig,
                       operations: GAOperations,
                       val generators: IndexedSeq[Generator],
                       val blades: IndexedSeq[BasisBlade]):

  def apply(v: Generator): String =
    params.namePrefix + params.generatorNames(v.number)

  def apply(blade: BasisBlade): String =
    if (blade.bits == 0 && params.overrideScalar.isDefined) return params.overrideScalar.get
    if (blade.bits == params.signature.bitMask && params.overridePseudoScalar.isDefined) return params.overridePseudoScalar.get
    params.namePrefix + blade.generators.map(v => params.generatorNames(v.number)).mkString("")

  protected def withoutPrefix(str: String, prefix: String): String =
    if (params.namePrefix.isEmpty) {
      str
    } else {
      require(str.startsWith(params.namePrefix))
      str.substring(params.namePrefix.length)
    }

  def basisBladeWithSign(str: String): BasisBladeWithSign =
    if (params.overrideScalar.contains(str)) return BasisBladeWithSign(BasisBlade.scalar(params.signature))
    if (params.overridePseudoScalar.contains(str)) return BasisBladeWithSign(BasisBlade.pseudoScalar(params.signature))

    var result = BasisBlade.scalar(params.signature)
    withoutPrefix(str, params.namePrefix).map { char =>
      val i = params.char2Index(char)
      BasisBladeWithSign(BasisBlade(Generator(i, params.signature), params.signature))
    }.fold(BasisBladeWithSign(BasisBlade.scalar(params.signature)))(operations.multiplication.geometric(_, _))

  def apply(v: BasisBladeWithSign): String =
    v.sign match
      case Sign.Positive => apply(v.basisBlade)
      case Sign.Negative => s"-${apply(v.basisBlade)}"
      case Sign.Zero => "0"

  protected def multiVector2str[T](vec: MultiVector[T], start: String, separator: String, end: String): String =
    if (vec.values.isEmpty) return "MultiVector()"
    val text = blades.flatMap(b => vec.values.get(b).map(v => s"${'\"' + apply(b) + '\"'} -> ${v}")).mkString(start, separator, end)
    s"MultiVector$text"

  def apply[T](v: MultiVector[T]): String = multiVector2str(v, "(", ", ", ")")

  def toMultilineString[T](v: MultiVector[T]): String = multiVector2str(v, "(\n", "\n", "\n)")

  def showUnaryOp(singleOp: UnaryOp, bladesOrder: IndexedSeq[BasisBlade] = blades): String =
    bladesOrder.map(b => s"${b} -> ${apply(b)}").mkString("[", ", ", "]")

  def showBinaryOp(binaryOp: BinaryOp, bladesOrder: IndexedSeq[BasisBlade] = blades, setMaxLen: Int = 0): String =
    val strings: IndexedSeq[IndexedSeq[String]] =
      bladesOrder.map { left =>
        bladesOrder.map { right =>
          apply(binaryOp(left, right))
        }
      }

    val maxLen = math.max(setMaxLen, strings.view.flatten.map(_.length).max)
    val padded = strings.map(_.map(s => (s" " * (maxLen - s.length)) + s))
    padded.map(_.mkString("|")).mkString("\n")


  private def swapTwoLastSymbols(str: String): String =
    require(str.length >= 2)
    str.take(str.length - 2) + str.takeRight(2).reverse

  private def makeTrickyBlades(blades: Seq[BasisBlade], getSign: (BasisBlade, BasisBlade) => Sign): Seq[(BasisBladeWithSign, String)] =
    val signed = blades.take(blades.size / 2).map(BasisBladeWithSign(_)) ++
      blades.take(blades.size / 2).reverseIterator.zip(blades.takeRight(blades.size / 2)).map { (left, right) =>
        BasisBladeWithSign(right, getSign(left, right))
      }
    signed.map { b =>
      val s = apply(b.basisBlade)
      val reorderedString = if (b.sign == Sign.Negative) swapTwoLastSymbols(s) else s
      b -> reorderedString
    }

  val trickyBlades: Seq[(BasisBladeWithSign, String)] =
    makeTrickyBlades(blades, (left, right) => operations.multiplication.geometric(left, right).sign)

  def toMultilineString[T: Numeric](vec: MultiVector[T], elemToString: T => String): String =
    if (vec.values.isEmpty) return "MultiVector()"
    val text = trickyBlades.flatMap { (b, bladeName) =>
      vec.values.get(b.basisBlade).map { v =>
        s"$bladeName -> ${elemToString(b.sign.toNumeric[T] * v)}"
      }
    }.mkString("(\n", ",\n", "\n)")
    s"MultiVector$text"