package com.github.kright.ga

import com.github.kright.ga.PGA2.*
import com.github.kright.symbolic.Sym
import com.github.kright.mathutil.EqualityEps
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.math.Numeric.Implicits.infixNumericOps

class PGA2Test extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private given ga: PGA2 = GA.pga2

  private val eps = 1e-12

  private given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)

  private given equalityEps: EqualityEps = EqualityEps(eps)

  test("point is intersection of two lines") {
    val one = Sym.one
    val zero = Sym.zero

    val x = Sym("x")
    val y = Sym("y")

    val px = line(one, zero, -x)
    val py = line(zero, one, -y)

    val p = point(x, y)
    val intersectionPoint = (px ^ py)

    assert(p === intersectionPoint, s"\n${p.toMultilineString} != ${intersectionPoint.toMultilineString}")
  }

  test("lineX mirrors points") {
    val px = Sym("p.x")
    val py = Sym("p.y")

    val p = point(px, py)
    val lineX = line(Sym(1.0), Sym.zero, Sym.zero)

    // note minus sign, pga3 doesn't have it
    // actually in PGA points could be freely multiplied by -1, this will mean the same point
    val mirrored = -lineX.sandwich(p).withoutZeros
    val expected = point(-px, py)

    assert(mirrored == expected, s"\n${mirrored} != ${expected}")
  }

  test("line and line from point to point are equal") {
    // line x * 0 + y * 1 + 0 = 0, so it's a line with y = 0 and any x
    val lx = line(Sym.zero, Sym.one, Sym.zero)

    val pCenter = point(Sym.zero, Sym.zero)
    val pX = point(Sym.one, Sym.zero)

    assert(lx == (pCenter v pX))
  }
