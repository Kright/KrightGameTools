package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Bit-exact properties of the generated cross: the code generator groups the mirrored
 * summands of the commutator into parenthesized differences (see SortAntisymmetricPairs),
 * which makes the self-commutator exactly zero and the swap of operands an exact negation.
 */
class CrossTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  // exact power-of-two multiples: the mirrored products round to bit-identical doubles
  private val powerOfTwoMultipliers = Seq(1.0, 2.0, 0.5, -4.0)

  test("cross of proportional arguments is exactly zero") {
    forAll(Pga3dGenerators.bivectors) { u =>
      for (t <- powerOfTwoMultipliers) {
        assert(u.cross(u * t) == Pga3dBivector.zero, s"u = $u, t = $t")
      }
    }
    forAll(Pga3dGenerators.anyMotors) { m =>
      for (t <- powerOfTwoMultipliers) {
        assert(m.cross(m * t) == Pga3dBivector.zero, s"m = $m, t = $t")
      }
    }
    forAll(Pga3dGenerators.multivectors) { m =>
      for (t <- powerOfTwoMultipliers) {
        assert(m.cross(m * t) == Pga3dMultivector.zero, s"m = $m, t = $t")
      }
    }
    forAll(Pga3dGenerators.planes) { p =>
      for (t <- powerOfTwoMultipliers) {
        assert(p.cross(p * t) == Pga3dBivector.zero, s"p = $p, t = $t")
      }
    }
  }

  test("cross is exactly antisymmetric for same-class arguments") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors) { (a, b) =>
      assert(a.cross(b) == -b.cross(a), s"a = $a, b = $b")
    }
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.anyMotors) { (a, b) =>
      assert(a.cross(b) == -b.cross(a), s"a = $a, b = $b")
    }
    forAll(Pga3dGenerators.multivectors, Pga3dGenerators.multivectors) { (a, b) =>
      assert(a.cross(b) == -b.cross(a), s"a = $a, b = $b")
    }
    forAll(Pga3dGenerators.planes, Pga3dGenerators.planes) { (a, b) =>
      assert(a.cross(b) == -b.cross(a), s"a = $a, b = $b")
    }
  }
