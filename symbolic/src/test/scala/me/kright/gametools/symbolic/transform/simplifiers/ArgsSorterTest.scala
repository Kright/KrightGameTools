package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.SymbolicStr
import me.kright.gametools.symbolic.SymbolicStr.given
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.math.Numeric.Implicits.infixNumericOps

class ArgsSorterTest extends AnyFunSuiteLike:
  test("check order") {
    val argsSorted = SymbolicStrSimplifier.sortArgs()
    assert(argsSorted.transform(SymbolicStr("x") + SymbolicStr(2.0)) == SymbolicStr(2.0) + SymbolicStr("x"))
  }
