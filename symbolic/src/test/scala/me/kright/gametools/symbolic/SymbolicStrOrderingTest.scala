package me.kright.gametools.symbolic

import org.scalatest.funsuite.AnyFunSuiteLike
import scala.collection.immutable.ArraySeq

class SymbolicStrOrderingTest extends AnyFunSuiteLike:
  test("test order") {
    import SymbolicStrOrdering.given

    val x = SymbolicStr("x")
    val one = SymbolicStr.one
    val func = SymbolicStr("*", ArraySeq(x, x))
    assert(ArraySeq(func, x, one).sorted == ArraySeq(one, x, func))
  }
