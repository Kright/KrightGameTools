package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Bit-exact properties of the generated cross: the code generator groups the mirrored
 * summands of the commutator into parenthesized differences (see SortAntisymmetricPairs),
 * which makes the self-commutator exactly zero and the swap of operands an exact negation.
 * The 2d sibling of the pga3d CrossTest.
 */
class CrossTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  // exact power-of-two multiples: the mirrored products round to bit-identical doubles
  private val powerOfTwoMultipliers = Seq(1.0, 2.0, 0.5, -4.0)

  test("cross of proportional arguments is exactly zero") {
    forAll(Pga2dGenerators.projectivePoints) { u =>
      for (t <- powerOfTwoMultipliers) {
        assert(u.cross(u * t) == Pga2dVector.zero, s"u = $u, t = $t")
      }
    }
    forAll(Pga2dGenerators.multivectors) { m =>
      for (t <- powerOfTwoMultipliers) {
        assert(m.cross(m * t) == Pga2dMultivector.zero, s"m = $m, t = $t")
      }
    }
  }

  test("cross is exactly antisymmetric for same-class arguments") {
    forAll(Pga2dGenerators.projectivePoints, Pga2dGenerators.projectivePoints) { (a, b) =>
      assert(a.cross(b) == -b.cross(a), s"a = $a, b = $b")
    }
    forAll(Pga2dGenerators.multivectors, Pga2dGenerators.multivectors) { (a, b) =>
      assert(a.cross(b) == -b.cross(a), s"a = $a, b = $b")
    }
  }
