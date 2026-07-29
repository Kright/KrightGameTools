package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dTriangleFartherThanTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  // no filtering: fartherThan has to behave for degenerate triangles too
  private val triangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)

  private val distances: Gen[Double] = Gen.oneOf(
    Pga3dVectorMathGenerators.doubleInRange(0.0, 10.0),
    Pga3dVectorMathGenerators.doubleInRange(0.0, 4.0 * halfSize),
  )

  test("unit cases") {
    val triangle = Pga3dTriangle(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0), Pga3dPoint(0, 1, 0))

    assert(triangle.fartherThan(Pga3dPoint(0.5, 0.5, 2.0), maxDistance = 1.0))
    assert(triangle.fartherThan(Pga3dPoint(3.0, 0, 0), maxDistance = 1.0))
    assert(!triangle.fartherThan(Pga3dPoint(0.5, 0.25, 0.5), maxDistance = 1.0))
    // the aabb bound is conservative: near the corner it keeps a point which is actually farther
    assert(!triangle.fartherThan(Pga3dPoint(1.9, 1.9, 0), maxDistance = 1.0))
  }

  test("agrees with the expanded AABB of the triangle") {
    forAll(triangles, Pga3dPhysicsGenerators.pointIn(bounds), distances, MinSuccessful(2000)) {
      (triangle, p, maxDistance) =>
        assert(triangle.fartherThan(p, maxDistance) == !triangle.toAABB.contains(p, expand = maxDistance),
          s"triangle = $triangle, p = $p, maxDistance = $maxDistance")
    }
  }

  test("never rejects a point actually within maxDistance") {
    forAll(triangles, Pga3dPhysicsGenerators.pointIn(bounds), distances, MinSuccessful(2000)) {
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

    forAll(triangles, Pga3dPhysicsGenerators.pointIn(bounds), distances, scales, MinSuccessful(1000)) {
      (triangle, p, maxDistance, scale) =>
        def scaled(q: Pga3dPoint): Pga3dPoint = Pga3dPoint(q.x * scale, q.y * scale, q.z * scale)

        assert(
          triangle.map(scaled).fartherThan(scaled(p), maxDistance * scale) == triangle.fartherThan(p, maxDistance),
          s"triangle = $triangle, p = $p, maxDistance = $maxDistance, scale = $scale")
    }
  }

  test("special values never reject unreliably") {
    val triangle = Pga3dTriangle(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0), Pga3dPoint(0, 1, 0))

    // NaN anywhere must not produce a (false) rejection
    assert(!triangle.fartherThan(Pga3dPoint(Double.NaN, 0, 0), maxDistance = 1.0))
    assert(!triangle.fartherThan(Pga3dPoint(100, 100, 100), maxDistance = Double.NaN))
    // infinite maxDistance keeps everything
    assert(!triangle.fartherThan(Pga3dPoint(1e300, 0, 0), maxDistance = Double.PositiveInfinity))
    // negative maxDistance: any answer is sound ("farther than a negative distance" is always
    // true), just check it does not throw and stays consistent with the expanded AABB
    assert(triangle.fartherThan(Pga3dPoint(0.5, 0.25, -5.0), maxDistance = -1.0))
  }
