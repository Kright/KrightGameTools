package me.kright.gametools.symbolic

import org.scalatest.funsuite.AnyFunSuiteLike

class SymbolicStrOrderingTest extends AnyFunSuiteLike:
  test("test order") {
    import SymbolicStrOrdering.given

    val x = SymbolicStr("x")
    val one = SymbolicStr.one
    val func = SymbolicStr("*", Seq(x, x))
    assert(Seq(func, x, one).sorted == Seq(one, x, func))
  }
