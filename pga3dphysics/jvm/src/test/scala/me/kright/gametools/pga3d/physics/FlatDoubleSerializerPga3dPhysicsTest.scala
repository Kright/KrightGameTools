package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FlatDoubleSerializerPga3dPhysicsTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  test("check sizes") {
    assert(FlatDoubleSerializer.getSize[Pga3dInertiaLocal] == Pga3dInertiaLocal.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dInertiaSummable] == Pga3dInertiaSummable.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dInertiaSimple] == Pga3dInertiaSimple.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dBodyState] == Pga3dMotor.componentsCount + Pga3dBivector.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dInertiaMovedLocal] == Pga3dMotor.componentsCount + Pga3dInertiaLocal.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dInertiaMovedSimple] == Pga3dTranslator.componentsCount + Pga3dInertiaSimple.componentsCount)
    assert(FlatDoubleSerializer.getSize[Pga3dPoint] == Pga3dPoint.componentsCount)
  }

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

  test("check serialization and deserialization") {
    myCheck(Pga3dInertiaGenerators.inertiaMovedLocal.map(_.localInertia))
    myCheck(Pga3dInertiaGenerators.inertiaMovedLocal.map(_.toSummable))
    myCheck(Pga3dInertiaGenerators.inertiaSimple(minMass = 0.1, maxMass = 10.0, minR = 0.1, maxR = 10.0))
    myCheck(for (motor <- Pga3dGenerators.anyMotors; b <- Pga3dGenerators.bivectors) yield Pga3dBodyState(motor, b))
    myCheck(Pga3dInertiaGenerators.inertiaMovedLocal)
    myCheck(Pga3dInertiaGenerators.inertiaMovedSimple)
  }

object FlatDoubleSerializerPga3dPhysicsTest:
  inline def myCheck[T](gen: Gen[T])(using CanEqual[T, T]): Unit =
    FlatDoubleSerializerPga3dTest.myCheck(gen)
