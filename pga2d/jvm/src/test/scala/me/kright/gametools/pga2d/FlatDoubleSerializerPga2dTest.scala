package me.kright.gametools.pga2d

import me.kright.gametools.pga2d.FlatDoubleSerializerPga2dTest.myCheck
import me.kright.gametools.mathutil.FlatDoubleSerializer
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

class FlatDoubleSerializerPga2dTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  test("check sizes") {
    assert(FlatDoubleSerializer.getSize[Pga2dMultivector] == Pga2dMultivector.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dMotor] == Pga2dMotor.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dLine] == Pga2dLine.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dLineIdeal] == Pga2dLineIdeal.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dProjectivePoint] == Pga2dProjectivePoint.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dPoint] == Pga2dPoint.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dVector] == Pga2dVector.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dRotor] == Pga2dRotor.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dTranslator] == Pga2dTranslator.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dProjectiveTranslator] == Pga2dProjectiveTranslator.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga2dPseudoScalar] == Pga2dPseudoScalar.componentsCount)
  }

  test("check serialization and deserialization") {
    myCheck(Pga2dGenerators.points)
    myCheck(Pga2dGenerators.vectors)
    myCheck(Pga2dGenerators.rotors)
    myCheck(Pga2dGenerators.normalizedRotors)
    myCheck(Pga2dGenerators.lines)
    myCheck(Pga2dGenerators.lineIdeals)
    myCheck(Pga2dGenerators.anyMotors)
    myCheck(Pga2dGenerators.projectivePoints)
  }

object FlatDoubleSerializerPga2dTest:
  inline def myCheck[T](gen: Gen[T])(using CanEqual[T, T]): Unit = {
    forAll(gen, gen) { (a, b) =>
      val size = FlatDoubleSerializer.getSize[T]
      val arr = new Array[Double](size * 2)
      FlatDoubleSerializer.write(a, arr, offset = 0)
      FlatDoubleSerializer.write(b, arr, offset = size)
      val ar = FlatDoubleSerializer.read[T](arr, offset = 0)
      val br = FlatDoubleSerializer.read[T](arr, offset = size)
      assert(a == ar)
      assert(b == br)
    }
  }
