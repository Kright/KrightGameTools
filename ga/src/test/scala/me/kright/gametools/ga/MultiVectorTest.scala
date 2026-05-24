package me.kright.gametools.ga

import me.kright.gametools.ga.GAGenerator.*
import com.github.kright.symbolic.Sym
import org.scalacheck.Gen
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import scala.math.Numeric.Implicits.infixNumericOps

class MultiVectorTest extends AnyFunSuite:
  private implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-9)
  private implicit val vectorEquality: Equality[MultiVector[Double]] = GaEquality.makeEquality(1e-9)

  def checkAssociativityForBasisBlades(op: (MultiVector[Double], MultiVector[Double]) => MultiVector[Double]): Unit =
    forAnyGA {
      forAll(bladesGen(1), bladesGen(1), bladesGen(1)) { (a, b, c) =>
        val left = op(op(a, b), c)
        val right = op(a, op(b, c))
        assert(left === right)
      }
    }

  def checkDistributivityForMultivectors(op: (MultiVector[Double], MultiVector[Double]) => MultiVector[Double]): Unit =
    forAnyGA {
      forAll(multivectorsGen, multivectorsGen, multivectorsGen) { (a, b, c) =>
        assert(op(a + b, c) === op(a, c) + op(b, c))
        assert(op(c, a + b) === op(c, a) + op(c, b))
      }
    }

  def checkAssociativityForMultivectors(op: (MultiVector[Double], MultiVector[Double]) => MultiVector[Double]): Unit =
    forAnyGA {
      forAll(multivectorsGen, multivectorsGen, multivectorsGen) { (a, b, c) =>
        assert(op(op(a, b), c) === op(a, op(b, c)))
      }
    }

  test("geometric product distributivity for multivectors") {
    checkDistributivityForMultivectors(_ geometric _)
  }

  test("dot product distributivity for multivectors") {
    checkDistributivityForMultivectors(_ dot _)
  }

  test("wedge product distributivity for multivectors") {
    checkDistributivityForMultivectors(_ wedge _)
  }

  test("geometric product associativity for multivectors") {
    checkAssociativityForMultivectors(_ geometric _)
  }

  test("geometric antiproduct associativity for multivectors") {
    checkAssociativityForMultivectors(_ antiGeometric _)
  }

  test("wedge product associativity for blades") {
    checkAssociativityForBasisBlades(_ wedge _)
  }

  test("wedge product with two same vectors is zero") {
    forAnyGA {
      val zero = MultiVector.zero[Double]
      forAll(bladesGen(1), multivectorsGen) { (a, m) =>
        assert((a ∧ a) === zero)
        assert((a ∧ (m ∧ a)) === zero)
        assert(((a ∧ m) ∧ a) === zero)
      }
    }
  }

  test("geometric product preserves 1-blade length") {
    forAnyGA {
      if (ga.signature.zeros == 0) {
        forAll(bladesGen(1), bladesGen(1), bladesGen(1)) { (a, b, c) =>
          val ma = a.magnitude
          val mb = b.magnitude
          val mab = (a ⟑ b).magnitude
          assert(mab === ma * mb, s"wrong dist $ma * $mb = ${ma * mb} != $mab, basis = $ga")
        }
      }
    }
  }

  test("geometric product preserves length for a product of blades") {
    forAnyGA {
      if (ga.signature.zeros == 0) {
        forAll(Gen.containerOfN[Seq, MultiVector[Double]](4, bladesGen(1))) { blades =>
          val mags = blades.map(_.magnitude)
          val totalMag = blades.reduce(_ ⟑ _).magnitude
          assert(mags.product === totalMag)
        }
      }
    }
  }

  test("wedge product is antisymmetric for vectors") {
    forAnyGA {
      forAll(bladesGen(1), bladesGen(1)) { (a, b) =>
        assert(a ∧ b === -(b ∧ a))
      }
    }
  }

  test("dot product is symmetric for vectors") {
    forAnyGA {
      forAll(bladesGen(1), bladesGen(1)) { (a, b) =>
        assert(a ⋅ b === b ⋅ a)
      }
    }
  }

  test("geometric product for vectors is a sum of wedge and dot ") {
    forAnyGA {
      forAll(bladesGen(1), bladesGen(1)) { (a, b) =>
        val w = a ∧ b
        val d = a ⋅ b
        val g = a ⟑ b

        assert(g === w + d,
          s"""g = $g
             |w = $w
             |d = $d
             |w + d = ${w + d}
             |""".stripMargin)
      }
    }
  }

  test("geometric antiproduct for psuedovectors is sum of wedge antiproduct and dot antiproduct") {
    forAnyGA {
      forAll(bladesGen(ga.signature.generatorsCount - 1), bladesGen(ga.signature.generatorsCount - 1)) { (a, b) =>
        val w = a ∨ b
        val d = a ◦ b
        val g = a ⟇ b

        assert(g === w + d,
          s"""g = $g
             |w = $w
             |d = $d
             |w + d = ${w + d}
             |""".stripMargin)
      }
    }
  }

  test("geometric antiproduct corresponds to geometric product") {
    forAnyGA {
      forAll(multivectorsGen, multivectorsGen) { (a, b) =>
        assert(a.geometric(b).dual === a.dual.antiGeometric(b.dual))
        assert(a.dual.geometric(b.dual) === a.antiGeometric(b).dual)
      }
    }
  }

  test("wedge antiproduct corresponds to wedge product") {
    forAnyGA {
      forAll(multivectorsGen, multivectorsGen) { (a, b) =>
        assert(a.wedge(b).dual === a.dual.antiWedge(b.dual))
      }
    }
  }

  test("dot antiproduct corresponds to dot product") {
    forAnyGA {
      forAll(multivectorsGen, multivectorsGen) { (a, b) =>
        assert(a.dot(b).dual === a.dual.antiDot(b.dual))
      }
    }
  }

  test("pseudoScalar commutativity and anticommutativity") {
    forAnyGA {
      val i = MultiVector[Double](ga.pseudoScalarBlade)
      forAll(bladesGen(1)) { a =>
        val isEven = ga.signature.generatorsCount % 2 == 0
        if (isEven) {
          assert((a ⟑ i) === -(i ⟑ a))
        } else {
          assert((a ⟑ i) === (i ⟑ a))
        }
      }
    }
  }

  test("exterior and interior product duality") {
    forAnyGA {
      val i = MultiVector[Double](ga.pseudoScalarBlade)

      forAll(bladesGen(1), bladesGen(1)) { (a, b) =>
        val isEven = ga.signature.generatorsCount % 2 == 0
        if (isEven) {
          assert(a ∧ (i ⟑ b) === -(a ⋅ b) ⟑ i)
        } else {
          assert(a ∧ (i ⟑ b) === (a ⋅ b) ⟑ i)
        }
      }
    }
  }

  test("bulk and weight sum is a vector itself") {
    forAnyGA {
      forAll(multivectorsGen) { v =>
        assert((v.bulk + v.weight) === v)
      }
    }
  }

  test("sandwich products") {
    forAnyGA {
      forAll(multivectorsGen, multivectorsGen, multivectorsGen) { (a, b, mid) =>
        assert(a ⟑ mid ⟑ a.reverse === a.sandwich(mid))
        assert(a ⟑ b ⟑ mid ⟑ b.reverse ⟑ a.reverse === a.geometric(b).sandwich(mid))

        assert(a ⟇ mid ⟇ a.antiReverse === a.antiSandwich(mid))
        assert(a ⟇ b ⟇ mid ⟇ b.antiReverse ⟇ a.antiReverse === a.antiGeometric(b).antiSandwich(mid))
      }
    }
  }

  test("my project") {
    GA.pga3.use {

      val p = Sym.point("p")

      val planeZ = PGA3.plane(
        Sym.zero,
        Sym.zero,
        Sym.one,
        -Sym.one
      )

      val centerPoint =
        PGA3.point(Sym.zero, Sym.zero, Sym.zero)

      //      println(((centerPoint v p) ^ planeZ).toMultilineString)
      //      println(((centerPoint v p) ^ planeZ).dual.toMultilineString)
    }
  }
