package me.kright.gametools.symbolic

import me.kright.gametools.symbolic.SymbolicStr.{*, given}
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.math.Numeric.Implicits.infixNumericOps


class SymbolicToPrettyStringTest extends AnyFunSuiteLike:
  test("test + -") {
    assert(SymbolicToPrettyString(SymbolicStr("x") + SymbolicStr("y")) == "(x + y)")
    assert(SymbolicToPrettyString(SymbolicStr("x") + SymbolicStr("y") + SymbolicStr("z")) == "((x + y) + z)")
    assert(SymbolicToPrettyString(SymbolicStr("x") - SymbolicStr("y")) == "(x - y)")
    assert(SymbolicToPrettyString(SymbolicStr("x") + SymbolicStr(-2.0) * SymbolicStr("y")) == "(x - 2.0 * y)")
    assert(SymbolicToPrettyString(SymbolicStr(-2.0) * SymbolicStr("x") + SymbolicStr(-2.0) * SymbolicStr("y")) == "(-2.0 * x - 2.0 * y)")
    assert(SymbolicToPrettyString(SymbolicStr(-1.0) * SymbolicStr("x") + SymbolicStr(-2.0) * SymbolicStr("y")) == "(-x - 2.0 * y)")
    assert(SymbolicToPrettyString(SymbolicStr(-1.0) * SymbolicStr("x")) == "-x")
  }
