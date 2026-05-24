package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dInertiaSimpleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("Pga3dInertiaSimple and Pga3dInertiaLocal cube are the same") {
    given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-13)

    forAll(
      Pga3dVectorMathGenerators.doubleInRange(0.1, 10.0),
      Pga3dVectorMathGenerators.doubleInRange(0.1, 10.0), MinSuccessful(1000)) { (mass, r) =>

      val simpleInertia = Pga3dInertiaSimple.cube(mass, r)
      val localInertia = Pga3dInertiaLocal.cube(mass, r, r, r)

      assert(localInertia.mryz === simpleInertia.mr2, s"diff = ${localInertia.mryz - simpleInertia.mr2}")
      assert(localInertia.mrxz === simpleInertia.mr2, s"diff = ${localInertia.mrxz - simpleInertia.mr2}")
      assert(localInertia.mrxy === simpleInertia.mr2, s"diff = ${localInertia.mrxy - simpleInertia.mr2}")
    }
  }

  test("Pga3dInertiaSimple and toInertiaLocal are the same") {
    given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-13)

    forAll(Pga3dInertiaGenerators.inertiaSimple(0.1, 1.0, 0.1, 10.0), MinSuccessful(1000)) { simpleInertia =>
      val localInertia = simpleInertia.toInertiaLocal

      assert(localInertia.mryz === simpleInertia.mr2, s"diff = ${localInertia.mryz - simpleInertia.mr2}")
      assert(localInertia.mrxz === simpleInertia.mr2, s"diff = ${localInertia.mrxz - simpleInertia.mr2}")
      assert(localInertia.mrxy === simpleInertia.mr2, s"diff = ${localInertia.mrxy - simpleInertia.mr2}")

      assert((simpleInertia.toSummable - localInertia.toSummable).norm < 1e-13)
    }
  }

  test("Pga3dInertiaSimple toSummable and others consistency") {
    forAll(Pga3dInertiaGenerators.inertiaSimple(0.1, 1.0, 0.1, 10.0), MinSuccessful(1000)) { simpleInertia =>
      val summable = simpleInertia.toSummable
      val summable2 = simpleInertia.toInertiaLocal.toSummable

      assert((summable - summable2).norm < 1e-13)
    }
  }

  test("Pga3dInertiaSimple and Pga3dInertiaLocal apply the same") {
    forAll(
      Pga3dInertiaGenerators.inertiaSimple(0.1, 1.0, 0.1, 10.0),
      Pga3dGenerators.bivectors,
      Pga3dGenerators.bivectors, MinSuccessful(1000)) { (simpleInertia, forque, b) =>

      val localInertia = simpleInertia.toInertiaLocal
      val simpleApply = simpleInertia(b)
      val localApply = localInertia(b)

      val summableInertia = simpleInertia.toSummable

      assert((simpleApply - localApply).norm < 1e-15, s"apply: simpleApply = $simpleApply, localApply = $localApply")

    }
  }

  test("Pga3dInertiaSimple and Pga3dInertiaLocal invert the same") {
    forAll(
      Pga3dInertiaGenerators.inertiaSimple(0.1, 1.0, 0.1, 10.0),
      Pga3dGenerators.bivectors, MinSuccessful(1000)) { (simpleInertia, b) =>

      val localInertia = simpleInertia.toInertiaLocal
      val simpleInvert = simpleInertia.invert(b)
      val localInvert = localInertia.invert(b)

      assert((simpleInvert - localInvert).norm < 1e-12, s"apply: simpleInvert = $simpleInvert, localInvert = $localInvert")
    }
  }

  test("getAcceleration is equal to apply invert") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors, MinSuccessful(1000)) { (forque, b) =>
      val inertia = Pga3dInertiaSimple(mass = 2.0, mr2 = 3.0)

      val a0 = inertia.getAcceleration(b, forque)
      val a1 = inertia.invert(b.cross(inertia(b)) + forque)

      assert((a0 - a1).norm < 1e-15, s"a0 = $a0, a1 = $a1")
    }
  }

  test("representations of simple are the same") {
    forAll(Pga3dInertiaGenerators.inertiaSimple(0.1, 10.0, 0.1, 10.0), MinSuccessful(1000)) { inertia =>
      val representations = Seq[Pga3dInertia](
        inertia.toInertiaLocal,
        inertia.toInertiaMovedLocal,
        inertia.toPrecomputed,
        inertia.toMovedSimple,
        inertia.toFastestRepresentation
      )

      for (repr <- representations) {
        assert((inertia.toSummable - repr.toSummable).norm < 1e-13)
      }
    }
  }

