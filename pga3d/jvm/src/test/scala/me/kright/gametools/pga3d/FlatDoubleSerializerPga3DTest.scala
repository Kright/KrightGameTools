package me.kright.gametools.pga3d

import me.kright.gametools.pga3d.FlatDoubleSerializerPga3DTest.myCheck
import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

class FlatDoubleSerializerPga3DTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  test("check sizes") {
    assert(FlatDoubleSerializer.getSize[Pga3dBivector] == Pga3dBivector.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dVector] == Pga3dVector.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dProjectivePoint] == Pga3dProjectivePoint.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dRotor] == Pga3dRotor.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dPlane] == Pga3dPlane.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dPlaneIdeal] == Pga3dPlaneIdeal.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dTranslator] == Pga3dTranslator.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dBivectorWeight] == Pga3dBivectorWeight.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dBivectorBulk] == Pga3dBivectorBulk.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dPoint] == Pga3dPoint.componentsCount)
  }

  test("check serialization and deserialization") {
    myCheck(Pga3dGenerators.points)
    myCheck(Pga3dGenerators.bivectors)
    myCheck(Pga3dGenerators.vectors)
    myCheck(Pga3dGenerators.rotors)
    myCheck(Pga3dGenerators.normalizedRotors)
  }

object FlatDoubleSerializerPga3DTest:
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
