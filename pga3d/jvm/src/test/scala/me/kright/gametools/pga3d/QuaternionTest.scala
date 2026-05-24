package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class QuaternionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("rotation is same vectors") {
    val from = Pga3dVector(1, 2, 3).normalizedByNorm
    val to = Pga3dVector(3, 4, 5).normalizedByNorm
    val q = Pga3dQuaternion.rotation(from, to)
    assert((q.sandwich(from) - to).norm < 1e-15)
  }

  test("check for angles near zero") {
    val angles = Seq(0.0, 1e-20, 1e-12, 1e-8, 1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 0.01, 0.1, 1.0)

    val rnd = new Random()
    rnd.setSeed(123)

    def makeQ(): Pga3dQuaternion =
      val q = Pga3dQuaternion(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian())
      if (q.norm < 1e-3) makeQ() else q.normalizedByNorm

    for (isNormalizedInput <- Seq(false, true);
         isNearPi <- Seq(false, true);
         angle <- angles) {
      val from = Pga3dPlaneIdeal(1.0, 0.0, 0.0)

      val to =
        if (isNearPi) Pga3dPlaneIdeal(-Math.cos(angle), Math.sin(angle), 0.0)
        else Pga3dPlaneIdeal(Math.cos(angle), Math.sin(angle), 0.0)

      val errors = new ArrayBuffer[Double]()
      for (i <- 0 until 1000) {
        val q = makeQ()
        val qFrom = q.sandwich(from)
        val qTo = q.sandwich(to)

        val m1 = if (isNormalizedInput) 1.0 else rnd.nextDouble() * 2 + 0.001
        val m2 = if (isNormalizedInput) 1.0 else rnd.nextDouble() * 2 + 0.001

        val real = Pga3dQuaternion.rotation(qFrom * m1, qTo * m2).sandwich(qFrom)
        val err = (real - qTo).norm
        errors += err
      }

      require(errors.max < 2e-8)
      // println(s"${if (isNormalizedInput) "normalized" else "non normalized"} angle = ${if (isNearPi) "Pi - " else ""}${angle}, errors max ${errors.max}, average ${errors.sum / errors.size}")
    }
  }

  test("axisX is equal to sandwich(axisX)") {
    val eps = 1e-15
    forAll(Pga3dGenerators.quaternions, MinSuccessful(100)) { q =>
      assert((q.axisX - q.sandwich(Pga3dVector(1, 0, 0))).norm < eps)
      assert((q.axisY - q.sandwich(Pga3dVector(0, 1, 0))).norm < eps)
      assert((q.axisZ - q.sandwich(Pga3dVector(0, 0, 1))).norm < eps)
    }

    forAll(Pga3dGenerators.anyMotors, MinSuccessful(100)) { m =>
      assert((m.axisX - m.sandwich(Pga3dVector(1, 0, 0))).norm < eps)
      assert((m.axisY - m.sandwich(Pga3dVector(0, 1, 0))).norm < eps)
      assert((m.axisZ - m.sandwich(Pga3dVector(0, 0, 1))).norm < eps)
    }
  }

  test("rotation along axis for zero rotation is zero") {
    assert(Pga3dQuaternion.id.restoreRotationInPlane(Pga3dPlaneIdeal(1, 1, 1)) == 0.0)
    assert(Pga3dQuaternion.id.restoreRotationInPlaneX == 0.0)
    assert(Pga3dQuaternion.id.restoreRotationInPlaneY == 0.0)
    assert(Pga3dQuaternion.id.restoreRotationInPlaneZ == 0.0)
  }

  test("rotation in X") {
    val q = Pga3dQuaternion.rotation(Pga3dVector(0, 0, 1), Pga3dVector(0, 1, 0))
    assert(q.restoreRotationInPlane(Pga3dPlaneIdeal(1, 0, 0)) == 1.5707963267948963)
    assert(q.restoreRotationInPlane(Pga3dPlaneIdeal(-1, 0, 0)) == -1.5707963267948963)
    assert(q.restoreRotationInPlaneX == 1.5707963267948963)
  }

  test("restore rotation with orthogonal err") {
    val err = Pga3dQuaternion.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 0)).log().exp(0.1)
    val q = Pga3dQuaternion.rotation(Pga3dVector(0, 0, 1), Pga3dVector(0, 1, 0))

    val expected = 1.5707963267948963
    assert(q.restoreRotationInPlaneX == expected)
    assert((err geometric q).restoreRotationInPlaneX == expected)
    assert((q geometric err).restoreRotationInPlaneX == expected)
  }
