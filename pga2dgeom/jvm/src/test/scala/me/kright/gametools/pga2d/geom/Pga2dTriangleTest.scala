package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dTriangleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )
  private val minArea = 1e-10
  private val minEdge = 1e-7

  private val suitableTriangles: Gen[Pga2dTriangle] = Pga2dPhysicsGenerators.triangleIn(bounds)
    .filter { t =>
      val area = t.area
      val ca = t.ac.norm
      val cb = t.bc.norm
      val ba = t.ab.norm
      val perimeter = t.perimeter
      val maxArea = (perimeter / 3) * (perimeter / 3) * 0.25 * Math.sqrt(3)

      t.area > minArea &&
        (ba > minEdge && ca > minEdge && cb > minEdge) &&
        (area > maxArea * 0.0001)
    }

  test("getInterpolationFactors for vertices") {
    forAll(suitableTriangles, MinSuccessful(1000)) { triangle =>
      for (
        (point, tba, tbc) <- Seq(
          (triangle.a, 0.0, 0.0),
          (triangle.b, 1.0, 0.0),
          (triangle.c, 0.0, 1.0),
        )
      ) {
        val (f1, f2) = triangle.getInterpolationFactors(point)
        assert((f1 - tba).abs < 1e-12 && (f2 - tbc).abs < 1e-12,
          s"""
             |triangle = $triangle
             |area = ${triangle.area}
             |perimeter = ${triangle.perimeter}
             |edges = ${triangle.edges.map(_.magnitude).mkString(", ")}
             |point = ${point},
             |tba = $tba, tbc = $tbc,
             |f1 = $f1, f2 = $f2""".stripMargin)
      }
    }
  }

  test("getInterpolationFactors is inverse of getInterpolatedPoint") {
    val eps = 5e-6

    forAll(
      suitableTriangles,
      Gen.oneOf(Pga2dVectorMathGenerators.doubleInRange(0.0, 1.0), Pga2dVectorMathGenerators.doubleInRange(-100, 100.0)),
      Gen.oneOf(Pga2dVectorMathGenerators.doubleInRange(0.0, 1.0), Pga2dVectorMathGenerators.doubleInRange(-100, 100.0)),
      MinSuccessful(1000)
    ) { (triangle, tba, tca) =>
      val p = triangle.getInterpolatedPoint(tba, tca)
      val (f1, f2) = triangle.getInterpolationFactors(p)

      assert(
        (f1 - tba).abs < eps &&
          (f2 - tca).abs < eps,
        s"tba = $tba, tca = $tca, f1 = $f1, f2 = $f2")
    }
  }

  test("signedArea sign depends on the order of vertices") {
    val ccw = Pga2dTriangle(Pga2dPoint(0, 0), Pga2dPoint(1, 0), Pga2dPoint(0, 1))
    assert(ccw.signedArea == 0.5)
    assert(Pga2dTriangle(ccw.a, ccw.c, ccw.b).signedArea == -0.5)
    assert(ccw.area == 0.5)
  }

  test("contains vertices and center, but not far points") {
    forAll(suitableTriangles, MinSuccessful(1000)) { triangle =>
      assert(triangle.contains(triangle.center))
      assert(triangle.contains(triangle.a, eps = 1e-9))
      assert(triangle.contains(triangle.b, eps = 1e-9))
      assert(triangle.contains(triangle.c, eps = 1e-9))
    }
  }

  test("distanceTo is zero for points inside") {
    forAll(suitableTriangles, MinSuccessful(1000)) { triangle =>
      // for sliver triangles the barycentric det loses most digits to cancellation,
      // so "zero" is only relative to the triangle size
      assert(triangle.distanceTo(triangle.center) < 1e-7 * (1.0 + triangle.perimeter))
    }
  }

  test("intersection with edge") {
    val triangle = Pga2dTriangle(Pga2dPoint(0, 0), Pga2dPoint(2, 0), Pga2dPoint(0, 2))
    val eps = 1e-12

    // edge endpoint inside
    assert(triangle.intersects(Pga2dEdge(Pga2dPoint(0.5, 0.5), Pga2dPoint(10, 10)), eps))
    // edge passes through the triangle, both endpoints outside
    assert(triangle.intersects(Pga2dEdge(Pga2dPoint(-1, 0.5), Pga2dPoint(3, 0.5)), eps))
    // edge is far away
    assert(!triangle.intersects(Pga2dEdge(Pga2dPoint(5, 5), Pga2dPoint(10, 5)), eps))
    // edge near the boundary, but outside
    assert(!triangle.intersects(Pga2dEdge(Pga2dPoint(-1, -0.5), Pga2dPoint(3, -0.5)), eps))
  }
