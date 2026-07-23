package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dRotor}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dEdgeTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )
  private val minMagnitude = 1e-7

  private val edgeWithMagnitude: Gen[Pga2dEdge] =
    Pga2dPhysicsGenerators.edgeIn(bounds)
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
      Gen.oneOf(Pga2dVectorMathGenerators.doubleInRange(0.0, 1.0), Pga2dVectorMathGenerators.doubleInRange(-100, 100.0)),
      MinSuccessful(1000)
    ) { (edge, t) =>

      val p = edge.interpolatedPoint(t)
      val restoredT = edge.getInterpolationFactor(p)

      // the absolute error of the restored t grows with |t| for tiny edges far from the origin
      assert(Math.abs(t - restoredT) < 1e-5 * Math.max(1.0, t.abs))
    }
  }

  test("getNearestPoint is idempotent") {
    forAll(
      edgeWithMagnitude,
      Pga2dPhysicsGenerators.pointIn(bounds),
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
    val edge = Pga2dEdge(Pga2dPoint(0, 0), Pga2dPoint(1, 0))
    val edge2 = Pga2dEdge(Pga2dPoint(-1, 1), Pga2dPoint(2, 1))

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
    forAll(
      Pga2dVectorMathGenerators.doubleInRange(-Math.PI, Math.PI),
      Pga2dVectorMathGenerators.doubleInRange(-Math.PI, Math.PI),
      MinSuccessful(10_000)
    ) { (a1, a2) =>
      val q1 = Pga2dRotor(s = Math.cos(a1 * 0.5), xy = Math.sin(a1 * 0.5))
      val q2 = Pga2dRotor(s = Math.cos(a2 * 0.5), xy = Math.sin(a2 * 0.5))

      val edge1 = q1.sandwich(Pga2dEdge(Pga2dPoint(0, 0), Pga2dPoint(1, 0)))
      val edge2 = q2.sandwich(Pga2dEdge(Pga2dPoint(-1, 1), Pga2dPoint(2, 1)))

      val (p1, p2) = edge1.getNearestPoints(edge2)
      assert(edge1.contains(p1, eps = 1e-13))
      assert(edge2.contains(p2, eps = 1e-13))

      val (ep1, ep2) = edge1.getNearestPointsBinSearch(edge2)
      assert(edge1.contains(ep1, eps = 1e-13))
      assert(edge2.contains(ep2, eps = 1e-13))

      val dist = (p1 - p2).norm
      val expectedDist = (ep1 - ep2).norm

      // in 2d, unlike 3d, edges often intersect, and near zero the binary search
      // has an absolute positional error, so the tolerance is absolute
      assert(Math.abs(dist - expectedDist) < 1e-12,
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
  }

  test("getNearestPoint for general case in AABB") {
    val halfSize = 1
    val bounds = Pga2dAABB(
      Pga2dPoint(-halfSize, -halfSize),
      Pga2dPoint(halfSize, halfSize)
    )
    val minMagnitude = 1e-7

    val edgeWithMagnitude: Gen[Pga2dEdge] =
      Pga2dPhysicsGenerators.edgeIn(bounds)
        .filter(_.magnitude > minMagnitude)

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

      // in 2d edges are often nearly collinear and crossing at the same time;
      // the closed-form solution is ill-conditioned there (error ~ eps / sin^2(angle)),
      // so the tolerance is much looser than for the 3d sibling of this test
      assert(Math.abs(dist - expectedDist) < 1e-9,
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
  }
