package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TranslatorTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("translator for vector") {
    val v = Pga3dVector(1, 2, 3)
    val tr = Pga3dTranslator.addVector(v)
    val point = Pga3dProjectivePoint(10, 20, 30, 1.0)

    val b1 = point + v
    val b2 = tr.sandwich(point)
    assert((b1 - b2).norm < 2e-16)
  }

  test("product of two translators equal to sum of translations") {
    forAll(Pga3dGenerators.vectors, Pga3dGenerators.vectors) { (offset0, offset1) =>
      val asProduct = Pga3dTranslator.addVector(offset0) geometric Pga3dTranslator.addVector(offset1)
      val asSum = Pga3dTranslator.addVector(offset0 + offset1)
      assert((asProduct - asSum).norm < 1e-15)
    }
  }
