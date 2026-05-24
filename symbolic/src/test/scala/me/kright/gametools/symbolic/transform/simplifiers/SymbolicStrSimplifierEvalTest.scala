package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.transform.{PartialTransform, PartialTransformListener}
import me.kright.gametools.symbolic.{Symbolic, SymbolicStr, SymbolicStrGenerators}
import org.scalacheck.*
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Try


class SymbolicStrSimplifierEvalTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("simplification doesn't change eval result step by step") {
    val simplifier = SymbolicStrSimplifier.simplify()

    forAll(SymbolicStrGenerators.sizedExprGen(8), SymbolicStrGenerators.variableValueMap, MinSuccessful(10_000)) { (expr, variables) =>
      simplifier.withListener(PartialTransformStepByStepChecker(variables)).transform(expr)
    }
  }


private class PartialTransformStepByStepChecker(variables: Map[String, Double]) extends PartialTransformListener[SymbolicStr]:
  override def apply(partialTransform: PartialTransform[SymbolicStr],
                     argument: SymbolicStr,
                     result: Try[Option[SymbolicStr]]): Unit =
    result.get.foreach { simplified =>
      val evalOfOriginal = eval(argument, variables)
      val evalOfSimplified = eval(simplified, variables)
      assert(evalOfOriginal == evalOfSimplified,
        s"wrong transform: ${partialTransform.getClass.getSimpleName}, ${argument} => ${result}")
    }


private def eval(expr: SymbolicStr, variables: Map[String, Double]): Double =
  expr match
    case SymbolicStr.Number(value) => value
    case SymbolicStr.SymbolStr(variableName) => variables(variableName)
    case Symbolic.Func("+", args) => args.map(eval(_, variables)).sum
    case Symbolic.Func("*", args) => args.map(eval(_, variables)).product
