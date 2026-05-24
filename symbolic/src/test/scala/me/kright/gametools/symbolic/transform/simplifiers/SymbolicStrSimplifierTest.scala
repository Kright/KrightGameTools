package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.SymbolicStr.{*, given}
import me.kright.gametools.symbolic.{Sym, Symbolic, SymbolicStr}
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.math.Numeric.Implicits.infixNumericOps

class SymbolicStrSimplifierTest extends AnyFunSuiteLike:
  val simplifier = SymbolicStrSimplifier.simplify()

  val simplifierWithSort = simplifier.combine(SymbolicStrSimplifier.sortArgs())

  private def assertSimplified(expr: SymbolicStr, expected: SymbolicStr): Unit =
    val logger = LoggingListener()
    val simplified = simplifier.withListener(logger).transform(expr)
    assert(simplified == expected, s"${logger.log}")

  private def assertSimplified(expr: Sym, expected: Sym): Unit =
    assertSimplified(expr.symbol, expected.symbol)

  test("don't touch primitives") {
    val primitives = Seq(SymbolicStr("x"), SymbolicStr(1.0), SymbolicStr(0.0))

    for (p <- primitives) {
      assertSimplified(p, p)
    }
  }

  test("fold constants") {
    assertSimplified(SymbolicStr(1.0) + SymbolicStr(3.0), SymbolicStr(4.0))
    assertSimplified(SymbolicStr(1.0) - SymbolicStr(3.0), SymbolicStr(-2.0))
    assertSimplified(SymbolicStr(1.0) - SymbolicStr(3.0) - SymbolicStr(5.0), SymbolicStr(-7.0))
  }

  test("product with zero") {
    assertSimplified(SymbolicStr(0.0) * SymbolicStr(3.0), SymbolicStr(0.0))
    assertSimplified(SymbolicStr("x") * SymbolicStr(0.0), SymbolicStr(0.0))
    assertSimplified(SymbolicStr("x") * (SymbolicStr(1.0) - SymbolicStr(1.0)), SymbolicStr(0.0))
  }

  test("product with one") {
    assertSimplified(SymbolicStr(0.5) * SymbolicStr("x") * SymbolicStr(2.0), SymbolicStr("x"))
  }

  test("CombineElemsInSum") {
    val x = SymbolicStr("x")
    val y = SymbolicStr("y")

    assertSimplified(SymbolicStr(1.0) + SymbolicStr(0.0), SymbolicStr(1.0))
    assertSimplified(SymbolicStr(1.0) + SymbolicStr(2.0), SymbolicStr(3.0))
    assertSimplified(x + x, SymbolicStr(2.0) * x)
    assertSimplified(x + SymbolicStr.zero, x)
    assertSimplified(x - x, SymbolicStr(0.0))
    assertSimplified(x * y - y * x, SymbolicStr(0.0))
    assertSimplified(x * y - y * x * SymbolicStr(0.5), SymbolicStr("*", Seq(SymbolicStr(0.5), x, y)))
    assertSimplified(SymbolicStr(1.0) + SymbolicStr(2.0) + x + y + x - y, SymbolicStr(3.0) + SymbolicStr(2.0) * x)
  }

  test("Constant should be first in product") {
    val x = SymbolicStr("x")
    assertSimplified(x * SymbolicStr(-1.0), SymbolicStr(-1.0) * x)
  }

  test("--x") {
    val x = SymbolicStr("x")
    val mX = -x
    val mmX = -mX
    assertSimplified(mmX, x)
  }

  test("- (-x/w)") {
    val x = Sym(SymbolicStr("x"))
    val w = Sym(SymbolicStr("w"))

    assertSimplified(-((-x) / w), x / w)
  }
