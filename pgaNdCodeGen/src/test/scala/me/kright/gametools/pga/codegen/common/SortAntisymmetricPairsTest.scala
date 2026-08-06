package me.kright.gametools.pga.codegen.common

import me.kright.gametools.symbolic.Sym
import me.kright.gametools.symbolic.Sym.given_Numeric_Sym.mkNumericOps
import org.scalatest.funsuite.AnyFunSuiteLike

/**
 * Pins the contract of [[SortAntisymmetricPairs]]: mirror pairs become parenthesized
 * differences, pairs go before unpaired terms, everything else is left alone. The expected
 * strings also pin the canonical sort order of the symbolic library: if that order ever
 * changes, this test fails loudly instead of the generated code drifting silently.
 */
class SortAntisymmetricPairsTest extends AnyFunSuiteLike:

  private def apply(sym: Sym): String =
    sym.map(SortAntisymmetricPairs).toString

  test("groups the mirror pairs of a commutator component") {
    // the wx component of Pga3dBivector.cross(r: Pga3dBivector)
    val expr = Sym("r.wy") * Sym("xy") + Sym("r.wz") * Sym("xz") - Sym("r.xy") * Sym("wy") - Sym("r.xz") * Sym("wz")

    // the canonical sort interleaves the pairs...
    assert(expr.toString == "(r.wy * xy + r.wz * xz - r.xy * wy - r.xz * wz)")
    // ...and the transform reunites them, each pair parenthesized
    assert(apply(expr) == "((r.wy * xy - r.xy * wy) + (r.wz * xz - r.xz * wz))")
  }

  test("unpaired terms go after the pairs") {
    val expr = Sym("k1") + Sym("r.wy") * Sym("xy") - Sym("r.xy") * Sym("wy")
    assert(apply(expr) == "((r.wy * xy - r.xy * wy) + k1)")
  }

  test("works for the C++ operand naming as well") {
    val expr = Sym("a.x") * Sym("b.y") - Sym("a.y") * Sym("b.x") + Sym("k1")
    assert(apply(expr) == "((a.x * b.y - a.y * b.x) + k1)")
  }

  test("a two-term sum is left unchanged") {
    val expr = Sym("r.wy") * Sym("xy") - Sym("r.xy") * Sym("wy")
    assert(apply(expr) == expr.toString)
  }

  test("factors of the same operand are not treated as a pair") {
    val expr = Sym("r.wy") * Sym("r.xy") - Sym("r.wz") * Sym("r.xz") + Sym("k1")
    assert(apply(expr) == expr.toString)
  }

  test("a pair requires the opposite coefficient") {
    // same swapped names, but the same sign: not a mirror, nothing to group
    val expr = Sym("r.wy") * Sym("xy") + Sym("r.xy") * Sym("wy") + Sym("k1")
    assert(apply(expr) == expr.toString)
  }
