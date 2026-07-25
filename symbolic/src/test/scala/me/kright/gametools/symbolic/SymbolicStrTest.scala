package me.kright.gametools.symbolic

import me.kright.gametools.symbolic.SymbolicStr.{*, given}
import org.scalatest.funsuite.AnyFunSuiteLike
import scala.collection.immutable.ArraySeq


class SymbolicStrTest extends AnyFunSuiteLike:
  test("apply") {
    assert(SymbolicStr(1.0) == Symbolic.Symbol(1.0))
    assert(SymbolicStr("x") == Symbolic.Symbol("x"))

    assert(SymbolicStr("+", ArraySeq(SymbolicStr("x"), SymbolicStr(1.0))) ==
      Symbolic.Func("+", ArraySeq(Symbolic.Symbol("x"), Symbolic.Symbol(1.0))))
  }

  test("isZero") {
    assert(SymbolicStr.zero.isZero)
    assert(SymbolicStr(0.0).isZero)
    assert(SymbolicStr.zero == SymbolicStr(0.0))

    assert(!SymbolicStr(1.0).isZero)
    assert(!SymbolicStr("x").isZero)
    assert(!SymbolicStr("+", ArraySeq(zero, one)).isZero)
  }

  test("isOne") {
    assert(SymbolicStr.one.isOne)
    assert(SymbolicStr(1.0).isOne)
    assert(SymbolicStr.one == SymbolicStr(1.0))

    assert(!SymbolicStr(0.0).isOne)
    assert(!SymbolicStr("x").isOne)
    assert(!SymbolicStr("+", ArraySeq(one, one)).isOne)
  }

  test("isNumber") {
    assert(SymbolicStr.zero.isNumber)
    assert(SymbolicStr.one.isNumber)
    assert(SymbolicStr(2.0).isNumber)

    assert(!SymbolicStr("x").isNumber)
    assert(!SymbolicStr("+", ArraySeq(one, one)).isNumber)
  }

  test("numeric") {
    import scala.math.Numeric.Implicits.infixNumericOps

    assert(SymbolicStr(1.0) + SymbolicStr("x") == SymbolicStr("+", ArraySeq(SymbolicStr(1.0), SymbolicStr("x"))))
  }