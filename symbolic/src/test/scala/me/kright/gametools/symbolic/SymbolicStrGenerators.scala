package me.kright.gametools.symbolic

import me.kright.gametools.symbolic.SymbolicStr.{*, given}
import org.scalacheck.*
import org.scalacheck.Gen.*

import scala.math.Numeric.Implicits.infixNumericOps


object SymbolicStrGenerators:
  val possibleNames: Seq[String] = Seq("a", "b", "c", "d", "e", "f", "g", "h", "x", "y", "z")
  val variable: Gen[SymbolicStr] = Gen.oneOf(possibleNames).map(SymbolicStr(_))

  val numberValue: Gen[Int] = chooseNum[Int](-100, 100, -1, 0, 1)
  val number: Gen[SymbolicStr] = numberValue.map(v => SymbolicStr(v.toDouble))

  val variableValueMap: Gen[Map[String, Double]] =
    Gen.listOfN(possibleNames.size, numberValue).map { numbers =>
      possibleNames.zip(numbers.map(_.toDouble)).toMap
    }

  val sum: Gen[SymbolicStr] = binop(_ + _)
  val sub: Gen[SymbolicStr] = binop(_ - _)
  val product: Gen[SymbolicStr] = binop(_ * _)

  val unaryMinus: Gen[SymbolicStr] = Gen.sized(d => resize(d - 1, exprGen)).map(v => -v)

  val exprGen: Gen[SymbolicStr] =
    Gen.sized { depth =>
      if (depth <= 1) Gen.oneOf(
        variable,
        number,
      ) else resize(depth - 1, Gen.oneOf(
        variable,
        number,
        sum,
        sub,
        product,
        unaryMinus,
      ))
    }

  def sizedExprGen(size: Int): Gen[SymbolicStr] =
    resize(size, exprGen)

  private def binop(op: (SymbolicStr, SymbolicStr) => SymbolicStr): Gen[SymbolicStr] =
    for {
      left <- Gen.sized(d => resize(d - 1, exprGen))
      right <- Gen.sized(d => resize(d - 1, exprGen))
    } yield op(left, right)
