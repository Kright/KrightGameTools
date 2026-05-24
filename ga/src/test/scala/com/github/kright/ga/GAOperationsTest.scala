package com.github.kright.ga

import com.github.kright.ga.GAGenerator.forAnyGA
import com.github.kright.mathutil.Sign
import org.scalatest.funsuite.AnyFunSuite

class GAOperationsTest extends AnyFunSuite:
  test("dot for basis vectors") {
    forAnyGA {
      for (v <- ga.generators) {
        val vb = BasisBlade(v, ga.signature)
        assert(v.squareSign == ga.operations.multiplication.dot(vb, vb)._2)
      }
    }

    assert(GA.ga3.generators.forall(_.squareSign == Sign.Positive))
    assert(GA.ga2.generators.forall(_.squareSign == Sign.Positive))

    GA.pga3.use {
      assert(ga.generators.map(BasisBlade(_, ga.signature)).map(b => ga.operations.multiplication.dot(b, b)._2.toInt) == IndexedSeq(0, 1, 1, 1))
    }
  }

  test("dot for basis blades") {
    GA.pga3.use {
      assert(GA.pga3.blades.map(b => ga.operations.multiplication.dot(b, b)._2.toInt) == IndexedSeq(
        1,
        0, 1, 1, 1,
        0, 0, 0, -1, -1, -1,
        0, 0, 0, -1,
        0
      ))
    }
  }

  test("associativity") {
    forAnyGA {
      for (v <- ga.generators) {
        val vb = BasisBlade(v, ga.signature)
        assert(v.squareSign == ga.operations.multiplication.dot(vb, vb)._2)
      }
    }

    assert(GA.ga3.generators.forall(_.squareSign == Sign.Positive))
    assert(GA.ga2.generators.forall(_.squareSign == Sign.Positive))
  }

  test("left complement and right complement") {
    forAnyGA {
      for (b <- ga.blades) {
        val v = MultiVector[Double](b)
        assert(v.leftComplement.geometric(v) === MultiVector.pseudoScalar(1.0))
        assert(v.geometric(v.rightComplement) === MultiVector.pseudoScalar(1.0))
      }
    }
  }
