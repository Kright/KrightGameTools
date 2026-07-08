package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TranslatorTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("translator for vector") {
    val v = Pga2dVector(1, 2)
    val tr = Pga2dTranslator.addVector(v)
    val point = Pga2dProjectivePoint(10, 20, 1.0)

    val b1 = point + v
    val b2 = tr.sandwich(point)
    assert((b1.toMultivector - b2.toMultivector).norm < 2e-16)
  }

  test("translator moves random points by vector") {
    forAll(Pga2dGenerators.points, Pga2dGenerators.vectors, MinSuccessful(1000)) { (p, v) =>
      val moved = Pga2dTranslator.addVector(v).sandwich(p)
      val expected = p + v
      assert((moved.toMultivector - expected.toMultivector).norm < 1e-14)
    }
  }

  test("product of two translators equal to sum of translations") {
    forAll(Pga2dGenerators.vectors, Pga2dGenerators.vectors) { (offset0, offset1) =>
      val asProduct = Pga2dTranslator.addVector(offset0) geometric Pga2dTranslator.addVector(offset1)
      val asSum = Pga2dTranslator.addVector(offset0 + offset1)
      assert((asProduct - asSum).norm < 1e-15)
    }
  }

  test("translator log exp round trip") {
    forAll(Pga2dGenerators.translators) { tr =>
      assert((tr.log().exp() - tr).norm < 1e-15)
    }
  }

  test("vector exp log round trip") {
    forAll(Pga2dGenerators.vectors) { v =>
      assert((v.exp().log() - v).norm < 1e-15)
    }
  }

  test("toMotor is consistent") {
    forAll(Pga2dGenerators.translators, Pga2dGenerators.points) { (tr, p) =>
      assert((tr.toMotor.sandwich(p).toMultivector - tr.sandwich(p).toMultivector).norm < 1e-14)
    }
  }
