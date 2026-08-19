package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.physics1d.SpringElasticity
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dForqueTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("force and torque") {
    val torque = (Pga3dPoint(0, 1, 0) v Pga3dVector(1, 0, 0)) + (Pga3dPoint(0, -1, 0) v Pga3dVector(-1, 0, 0))
    val force = (Pga3dPoint(0, 1, 0) v Pga3dVector(1, 0, 0)) + (Pga3dPoint(0, -1, 0) v Pga3dVector(1, 0, 0))

    val t2 = Pga3dForque.force(Pga3dPoint(0, 1, 0), Pga3dVector(1, 0, 0)) + Pga3dForque.force(Pga3dPoint(0, -1, 0), Pga3dVector(-1, 0, 0))
    val f2 = Pga3dForque.force(Pga3dPoint(0, 1, 0), Pga3dVector(1, 0, 0)) + Pga3dForque.force(Pga3dPoint(0, -1, 0), Pga3dVector(1, 0, 0))

    assert(force == Pga3dBivector(0, 0, 0, 0, 0, 2))
    assert(f2 == Pga3dBivector(0, 0, 0, 0, 0, 2))

    assert(torque == Pga3dBivector(0, 0, -2, 0, 0, 0))
    assert(t2 == Pga3dBivector(0, 0, -2, 0, 0, 0))
  }

  test("make force and extract linear part back") {
    forAll(Pga3dGenerators.points, Pga3dGenerators.points, Pga3dGenerators.vectors, Pga3dGenerators.vectors) { (p1, p2, v1, v2) =>
      val forque1 = Pga3dForque.force(p1, v1)
      val forque2 = Pga3dForque.force(p2, v2)
      val forqueSum = forque1 + forque2
      val linearPart = Pga3dForque.getLinearForce(forqueSum)

      val dist = (v1 + v2 - linearPart).norm
      assert(dist <= 1e-14)
    }
  }

  test("make torque") {
    assert(Pga3dForque.torque(Pga3dVector(1, 0, 0)) == Pga3dBivector(1, 0, 0, 0, 0, 0))
    assert(Pga3dForque.torque(Pga3dVector(0, 1, 0)) == Pga3dBivector(0, 1, 0, 0, 0, 0))
    assert(Pga3dForque.torque(Pga3dVector(0, 0, 1)) == Pga3dBivector(0, 0, 1, 0, 0, 0))
  }

  test("pure torque translation affect nothing") {
    forAll(Pga3dGenerators.vectors, Pga3dGenerators.vectors) { (shift, t) =>
      val torque = Pga3dForque.torque(t)
      val shifted = Pga3dTranslator.addVector(shift).sandwich(torque)
      assert(shifted == torque)
    }
  }

  test("force translation") {
    forAll(Pga3dGenerators.points, Pga3dGenerators.vectors, Pga3dGenerators.vectors) { (point, f, shift) =>
      val shifted = Pga3dTranslator.addVector(shift).sandwich(Pga3dForque.force(point, f))
      val shifted2 = Pga3dForque.force(point + shift, f)
      assert((shifted - shifted2).norm < 1e-15)
    }
  }

  test("force center") {
    forAll(Pga3dGenerators.points, Pga3dGenerators.vectors) { (point, f) =>
      val force = Pga3dForque.force(point, f)
      val linearForce = Pga3dForque.getLinearForce(force)
      val p = Pga3dForque.getCenter(force)
      val force2 = Pga3dForque.force(Pga3dForque.getCenter(force), f)
      assert((force2 - force).norm < 1e-15)
    }
  }

  test("force and torque extraction for parallel case") {
    val f = Pga3dVector(3.1, 0, 0)
    val t = Pga3dVector(12, 0, 0)
    val center = Pga3dPoint(0, 1, 2)

    val force = Pga3dForque.force(center, f)
    val torque = Pga3dForque.torque(t)
    val sum = force + torque

    val extractedForce = Pga3dForque.getLinearForce(sum)
    val extractedTorque = Pga3dForque.getTorqueAroundCenter(sum)
    val extractedCenter = Pga3dForque.getCenter(sum)

    assert(extractedForce == f)
    assert(extractedTorque == t)
    assert(extractedCenter == center)
  }

  test("torque around center for force couples") {
    forAll(Pga3dGenerators.points, Pga3dGenerators.vectors, Pga3dGenerators.vectors) { (center, f, dir) =>
      val perp = dir - f * ((dir antiDotI f) / f.normSquare)
      whenever(f.norm > 1e-2 && perp.norm > 1e-2) {
        val d = perp.normalizedByNorm

        // couple: f at (center + d), -f at (center - d) is the pure moment 2 * (d v f)
        val couplePerp = Pga3dForque.force(center + d, f) + Pga3dForque.force(center - d, -f)
        assert((couplePerp - (d v f) * 2.0).norm <= 1e-12)
        assert(Pga3dForque.getLinearForce(couplePerp).norm <= 1e-12)
        assert((Pga3dForque.torque(Pga3dForque.getTorqueAroundCenter(couplePerp)) - couplePerp).norm <= 1e-12)

        // g = d rotated 90 degrees around the axis along f, so the moment 2 * (d v g) is parallel to f
        val fHat = f.normalizedByNorm
        val g = (Pga3dPointCenter v fHat).exp(Math.PI / 4).sandwich(d)
        val couplePar = Pga3dForque.force(center + d, g) + Pga3dForque.force(center - d, -g)

        val sum = Pga3dForque.force(center, f) + couplePerp + couplePar

        assert((Pga3dForque.getLinearForce(sum) - f).norm <= 1e-12)
        assert((Pga3dForque.torque(Pga3dForque.getTorqueAroundCenter(sum)) - couplePar).norm <= 1e-12)

        val reconstructed = Pga3dForque.force(Pga3dForque.getCenter(sum), Pga3dForque.getLinearForce(sum))
          + Pga3dForque.torque(Pga3dForque.getTorqueAroundCenter(sum))
        assert((reconstructed - sum).norm <= 1e-12)
      }
    }
  }

  test("forque of spring") {
    val a = Pga3dPoint(0, 0, 0)
    val b = Pga3dPoint(0, 0, 1)
    val k = 7.7
    assert(Pga3dForque.spring(a, b, k, springLength = 1.0) == Pga3dBivector(0, 0, 0, 0, 0, 0))
    assert(Pga3dForque.spring(a, b, k, springLength = 0.5) == Pga3dForque.force(a, Pga3dVector(0, 0, 0.5 * k)))
    assert(Pga3dForque.spring(a, b, k, springLength = 2.0) == Pga3dForque.force(a, Pga3dVector(0, 0, -1.0 * k)))
  }

  test("forque of spring when the poinst are the same") {
    val a = Pga3dPoint(0, 0, 0)
    assert(Pga3dForque.spring(a, a, k = 10, springLength = 1.0) == Pga3dBivector(0, 0, 0, 0, 0, 0))
  }

  test("SpringElasticity.linear(k) spring gives the same forque as the plain-k spring") {
    forAll(Pga3dGenerators.points, Pga3dGenerators.points, Gen.choose(0.1, 100.0), Gen.choose(0.0, 2.0)) {
      (a, b, k, springLength) =>
        val expected = Pga3dForque.spring(a, b, k, springLength)
        val actual = Pga3dForque.spring(a, b, SpringElasticity.linear(k), springLength)
        assert((actual - expected).norm <= 1e-12 * (1.0 + expected.norm))
    }
  }

  test("spring forque with elasticity: zero at rest length, pulls together when stretched") {
    val a = Pga3dPoint(0, 0, 0)
    val b = Pga3dPoint(0, 0, 1)
    val elastic = SpringElasticity(softK = 10.0, softZoneTravel = 0.5, stiffK = 1000.0)

    assert(Pga3dForque.spring(a, b, elastic, springLength = 1.0).norm == 0.0)

    // the scalar force is a generalized force along r, so the pull on `a` towards `b` is its negation
    val stretched = Pga3dForque.spring(a, b, elastic, springLength = 0.5)
    val expectedStretched = Pga3dForque.force(a, Pga3dVector(0, 0, -elastic.force(0.5)))
    assert((stretched - expectedStretched).norm <= 1e-12 * expectedStretched.norm)
    assert(elastic.force(0.5) < 0) // stretched: acts to decrease r, pulls a towards b

    val compressed = Pga3dForque.spring(a, b, elastic, springLength = 2.0)
    val expectedCompressed = Pga3dForque.force(a, Pga3dVector(0, 0, -elastic.force(-1.0)))
    assert((compressed - expectedCompressed).norm <= 1e-12 * expectedCompressed.norm)
    assert(elastic.force(-1.0) > 0) // compressed: acts to increase r, pushes a away from b
  }

  test("spring forque with elasticity when the points are the same") {
    val a = Pga3dPoint(1, 2, 3)
    val elastic = SpringElasticity(softK = 10.0, softZoneTravel = 0.5, stiffK = 1000.0)
    assert(Pga3dForque.spring(a, a, elastic, springLength = 1.0).norm == 0.0)
  }
