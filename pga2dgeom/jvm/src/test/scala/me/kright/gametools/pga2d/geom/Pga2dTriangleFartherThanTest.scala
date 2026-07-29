package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dTriangleFartherThanTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  // no filtering: fartherThan has to behave for degenerate triangles too
  private val triangles: Gen[Pga2dTriangle] = Pga2dPhysicsGenerators.triangleIn(bounds)

  private val distances: Gen[Double] = Gen.oneOf(
    Pga2dVectorMathGenerators.doubleInRange(0.0, 10.0),
    Pga2dVectorMathGenerators.doubleInRange(0.0, 4.0 * halfSize),
  )

  test("unit cases") {
    val triangle = Pga2dTriangle(Pga2dPoint(0, 0), Pga2dPoint(1, 0), Pga2dPoint(0, 1))

    assert(triangle.fartherThan(Pga2dPoint(0.5, -2.5), maxDistance = 1.0))
    assert(triangle.fartherThan(Pga2dPoint(3.0, 0), maxDistance = 1.0))
    assert(!triangle.fartherThan(Pga2dPoint(0.5, 0.25), maxDistance = 1.0))
    // the aabb bound is conservative: near the corner it keeps a point which is actually farther
    assert(!triangle.fartherThan(Pga2dPoint(1.9, 1.9), maxDistance = 1.0))
  }

  test("agrees with the expanded AABB of the triangle") {
    forAll(triangles, Pga2dPhysicsGenerators.pointIn(bounds), distances, MinSuccessful(2000)) {
      (triangle, p, maxDistance) =>
        assert(triangle.fartherThan(p, maxDistance) == !triangle.toAABB.contains(p, expand = maxDistance),
          s"triangle = $triangle, p = $p, maxDistance = $maxDistance")
    }
  }

  test("never rejects a point actually within maxDistance") {
    forAll(triangles, Pga2dPhysicsGenerators.pointIn(bounds), distances, MinSuccessful(2000)) {
      (triangle, p, maxDistance) =>
        if (triangle.fartherThan(p, maxDistance)) {
          val dist = (triangle.getNearestPoint(p) - p).norm
          assert(dist >= maxDistance * (1.0 - 1e-12),
            s"dist = $dist, maxDistance = $maxDistance, triangle = $triangle, p = $p")
        }
    }
  }

  test("is scale-invariant up to 1e+-30") {
    val scales = Gen.oneOf(1e-30, 1e-10, 1e10, 1e30)

    forAll(triangles, Pga2dPhysicsGenerators.pointIn(bounds), distances, scales, MinSuccessful(1000)) {
      (triangle, p, maxDistance, scale) =>
        def scaled(q: Pga2dPoint): Pga2dPoint = Pga2dPoint(q.x * scale, q.y * scale)

        assert(
          triangle.map(scaled).fartherThan(scaled(p), maxDistance * scale) == triangle.fartherThan(p, maxDistance),
          s"triangle = $triangle, p = $p, maxDistance = $maxDistance, scale = $scale")
    }
  }

  test("special values never reject unreliably") {
    val triangle = Pga2dTriangle(Pga2dPoint(0, 0), Pga2dPoint(1, 0), Pga2dPoint(0, 1))

    assert(!triangle.fartherThan(Pga2dPoint(Double.NaN, 0), maxDistance = 1.0))
    assert(!triangle.fartherThan(Pga2dPoint(100, 100), maxDistance = Double.NaN))
    assert(!triangle.fartherThan(Pga2dPoint(1e300, 0), maxDistance = Double.PositiveInfinity))
    assert(triangle.fartherThan(Pga2dPoint(0.5, -5.0), maxDistance = -1.0))
  }
