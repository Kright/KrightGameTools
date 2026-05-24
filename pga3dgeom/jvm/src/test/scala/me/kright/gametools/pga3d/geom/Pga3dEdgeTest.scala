package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dBivectorBulk, Pga3dPoint}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dEdgeTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )
  private val minMagnitude = 1e-7

  private val edgeWithMagnitude: Gen[Pga3dEdge] =
    Pga3dPhysicsGenerators.edgeIn(bounds)
      .filter(_.magnitude > minMagnitude)

  test("interpolation unit test") {
    forAll(edgeWithMagnitude, MinSuccessful(1000)) { edge =>
      assert(edge.interpolatedPoint(0.0) == edge.a)
      assert(edge.interpolatedPoint(1.0) == edge.b)
      assert(edge.interpolatedPoint(0.5) == edge.center)

      assert(edge.getInterpolationFactor(edge.a) == 0.0)
      assert(edge.getInterpolationFactor(edge.b) == 1.0)
      assert(Math.abs(edge.getInterpolationFactor(edge.center) - 0.5) < 1e-6)
    }
  }

  test("getInterpolationFactor is inverse of interpolation") {
    forAll(
      edgeWithMagnitude,
      Gen.oneOf(Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0), Pga3dVectorMathGenerators.doubleInRange(-100, 100.0)),
      MinSuccessful(1000)
    ) { (edge, t) =>

      val p = edge.interpolatedPoint(t)
      val restoredT = edge.getInterpolationFactor(p)

      assert(Math.abs(t - restoredT) < 1e-5)
    }
  }

  test("getNearestPoint is idempotent") {
    forAll(
      edgeWithMagnitude,
      Pga3dPhysicsGenerators.pointIn(bounds),
      MinSuccessful(1000)
    ) { (edge, point) =>
      val nearestPoint = edge.getNearestPoint(point)
      val nearestPoint2 = edge.getNearestPoint(nearestPoint)
      assert((nearestPoint - nearestPoint2).norm < 1e-12)
    }
  }

  test("getNearestPoint for interpolated") {
    forAll(
      edgeWithMagnitude,
    ) { edge =>
      assert(edge.getNearestPoint(edge.a) == edge.a)
      assert(edge.getNearestPoint(edge.interpolatedPoint(-0.1)) == edge.a)
      assert(edge.getNearestPoint(edge.b) == edge.b)
      assert(edge.getNearestPoint(edge.interpolatedPoint(1.1)) == edge.b)
    }
  }

  test("unit getNearestPoint for parallel edges") {
    val edge = Pga3dEdge(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0))
    val edge2 = Pga3dEdge(Pga3dPoint(-1, 1, 0), Pga3dPoint(2, 1, 0))

    for ((p1, p2) <- Seq(
      edge.getNearestPoints(edge2),
      edge.getNearestPointsBinSearch(edge2)
    )) {
      assert(edge.contains(p1, eps = 1e-12))
      assert(edge2.contains(p2, eps = 1e-12))

      assert(Math.abs((p1 - p2).norm - 1.0) < 1e-12)
    }
  }

  test("getNearestPoint for general case") {
    var maxError: Double = 0.0

    forAll(
      Pga3dVectorMathGenerators.doubleInRange(-Math.PI, Math.PI),
      Pga3dVectorMathGenerators.doubleInRange(-Math.PI, Math.PI),
      MinSuccessful(100_000)
    ) { (a1, a2) =>
      val q1 = Pga3dBivectorBulk(xy = 1.0).exp(a1)
      val q2 = Pga3dBivectorBulk(xz = 1.0).exp(a2)

      val edge1 = q1.sandwich(Pga3dEdge(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0)))
      val edge2 = q2.sandwich(Pga3dEdge(Pga3dPoint(-1, 1, 0), Pga3dPoint(2, 1, 0)))

      val (p1, p2) = edge1.getNearestPoints(edge2)
      assert(edge1.contains(p1, eps = 1e-14))
      assert(edge2.contains(p2, eps = 1e-14))

      val (ep1, ep2) = edge1.getNearestPointsBinSearch(edge2)
      assert(edge1.contains(ep1, eps = 1e-14))
      assert(edge2.contains(ep2, eps = 1e-14))

      val dist = (p1 - p2).norm
      val expectedDist = (ep1 - ep2).norm
      maxError = Math.max(maxError, Math.abs(dist - expectedDist))

      assert(Math.abs(dist - expectedDist) < 1e-14,
        s"""
           |angles = $a1, $a2
           |
           |expected:
           |t1 = ${edge1.getInterpolationFactor(ep1)}
           |t2 = ${edge1.getInterpolationFactor(ep2)}
           |result:
           |t1 = ${edge1.getInterpolationFactor(p1)}
           |t2 = ${edge2.getInterpolationFactor(p2)}
           |
           |dist = $dist
           |edist = $expectedDist
           |difference = ${Math.abs(dist - expectedDist)}
           |
           |
           |""".stripMargin)
    }

//    println(s"maxError = $maxError")
  }

  test("getNearestPoint for general case in AABB") {
    val halfSize = 1
    val bounds = Pga3dAABB(
      Pga3dPoint(-halfSize, -halfSize, -halfSize),
      Pga3dPoint(halfSize, halfSize, halfSize)
    )
    val minMagnitude = 1e-7

    val edgeWithMagnitude: Gen[Pga3dEdge] =
      Pga3dPhysicsGenerators.edgeIn(bounds)
        .filter(_.magnitude > minMagnitude)

    var maxError: Double = 0.0

    forAll(
      edgeWithMagnitude,
      edgeWithMagnitude,
      MinSuccessful(10_000)
    ) { (edge1, edge2) =>
      val (p1, p2) = edge1.getNearestPoints(edge2)
      assert(edge1.contains(p1, eps = 1e-12))
      assert(edge2.contains(p2, eps = 1e-12))

      val (ep1, ep2) = edge1.getNearestPointsBinSearch(edge2)
      assert(edge1.contains(ep1, eps = 1e-12))
      assert(edge2.contains(ep2, eps = 1e-12))

      val dist = (p1 - p2).norm
      val expectedDist = (ep1 - ep2).norm

      maxError = Math.max(maxError, Math.abs(dist - expectedDist))

      assert(Math.abs(dist - expectedDist) < 1e-12,
        s"""
           |edge1 = ${edge1}
           |edge2 = ${edge2}
           |
           |expected:
           |t1 = ${edge1.getInterpolationFactor(ep1)}
           |t2 = ${edge1.getInterpolationFactor(ep2)}
           |result:
           |t1 = ${edge1.getInterpolationFactor(p1)}
           |t2 = ${edge2.getInterpolationFactor(p2)}
           |
           |dist = $dist
           |edist = $expectedDist
           |difference = ${Math.abs(dist - expectedDist)}
           |
           |""".stripMargin)
    }

//    println(s"maxError = $maxError")
  }
