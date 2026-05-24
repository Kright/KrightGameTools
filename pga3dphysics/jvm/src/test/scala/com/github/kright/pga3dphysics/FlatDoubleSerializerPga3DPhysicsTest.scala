package com.github.kright.pga3dphysics

import com.github.kright.pga3d.*
import com.github.kright.mathutil.FlatDoubleSerializer
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FlatDoubleSerializerPga3DPhysicsTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  test("check sizes") {
    assert(FlatDoubleSerializer.getSize[Pga3dInertiaLocal] == Pga3dInertiaLocal.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dInertiaSummable] == Pga3dInertiaSummable.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dPoint] == Pga3dPoint.componentsCount)
  }

  inline def myCheck[T](gen: Gen[T]): Unit = {
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

  test("check serialization and deserialization") {
    myCheck(Pga3dInertiaGenerators.inertiaMovedLocal.map(_.localInertia))
    myCheck(Pga3dInertiaGenerators.inertiaMovedLocal.map(_.toSummable))
  }

object FlatDoubleSerializerPga3DPhysicsTest:
  inline def myCheck[T](gen: Gen[T]): Unit =
    FlatDoubleSerializerPga3DTest.myCheck(gen)
