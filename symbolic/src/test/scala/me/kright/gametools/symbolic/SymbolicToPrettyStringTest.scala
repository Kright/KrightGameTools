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

  test("sums of 4+ terms are grouped in pairs to shorten the float addition chain") {
    def flatSum(names: String*): SymbolicStr =
      SymbolicStr("+", names.map(SymbolicStr(_)))

    assert(SymbolicToPrettyString(flatSum("a", "b", "c")) == "(a + b + c)")
    assert(SymbolicToPrettyString(flatSum("a", "b", "c", "d")) == "((a + b) + (c + d))")
    assert(SymbolicToPrettyString(flatSum("a", "b", "c", "d", "e")) == "((a + b) + (c + d) + e)")
    assert(SymbolicToPrettyString(flatSum("a", "b", "c", "d", "e", "f")) == "((a + b) + (c + d) + (e + f))")
    assert(SymbolicToPrettyString(flatSum("a", "b", "c", "d", "e", "f", "g", "h")) ==
      "(((a + b) + (c + d)) + ((e + f) + (g + h)))")
    assert(SymbolicToPrettyString(SymbolicStr("+", Seq(SymbolicStr("a"), SymbolicStr(-1.0) * SymbolicStr("b"), SymbolicStr("c"), SymbolicStr(-1.0) * SymbolicStr("d")))) ==
      "((a - b) + (c - d))")
  }

  test("a pair starting with a negative term is subtracted as a whole") {
    // a + b - c + d becomes (a + b) - (c - d): no group starts with a minus
    assert(SymbolicToPrettyString(SymbolicStr("+", Seq(
      SymbolicStr("a"), SymbolicStr("b"), SymbolicStr(-1.0) * SymbolicStr("c"), SymbolicStr("d")))) ==
      "((a + b) - (c - d))")
    // both terms negative: the pair is subtracted with a plus inside
    assert(SymbolicToPrettyString(SymbolicStr("+", Seq(
      SymbolicStr("a"), SymbolicStr("b"), SymbolicStr(-1.0) * SymbolicStr("c"), SymbolicStr(-2.0) * SymbolicStr("d")))) ==
      "((a + b) - (c + 2.0 * d))")
  }
