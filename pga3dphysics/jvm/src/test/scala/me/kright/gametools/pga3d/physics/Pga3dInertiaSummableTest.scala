package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.Pga3dGenerators
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dInertiaSummableTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("point mass") {
    forAll(Pga3dGenerators.points, Gen.double.map(_ + 0.1)) { (point, mass) =>
      val summable0 = Pga3dInertiaSummable.point(point, mass)
      val summable1 = Pga3dInertiaSummable.symmetricProduct(point.toProjectivePoint, point * mass)
      assert((summable0 - summable1).norm < 1e-15)
    }
  }

  test("center and mass of point mass") {
    forAll(Pga3dGenerators.points, Gen.double.map(_ + 0.1)) { (point, mass) =>
      val summable = Pga3dInertiaSummable.point(point, mass)
      assert(Math.abs(summable.mass - mass) < 1e-15)
      assert((summable.centerOfMass - point).norm < 1e-15)
      assert((summable.centerOfMassProjective.w - mass) < 1e-15)
    }
  }

  test("summable apply() method same as inertia") {
    forAll(Pga3dInertiaGenerators.inertiaMovedLocal, Pga3dGenerators.bivectors, MinSuccessful(1000)) { (inertia, bivector) =>
      val applied1 = inertia.apply(bivector)
      val applied2 = inertia.toSummable.apply(bivector)
      assert((applied1 - applied2).norm < 1e-12)
    }
  }

  test("summable invert() method inversion of apply()") {
    forAll(Pga3dInertiaGenerators.inertiaSummable, Pga3dGenerators.bivectors, MinSuccessful(1000)) { (inertia, bivector) =>
      val L = inertia.apply(bivector)
      val inv = inertia.invert(L)
      assert((bivector - inv).norm < 1e-7, s"\n${bivector}\n${inv})")
    }
  }
