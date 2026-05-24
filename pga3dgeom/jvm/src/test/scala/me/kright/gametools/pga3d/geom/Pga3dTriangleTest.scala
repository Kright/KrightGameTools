package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dTriangleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )
  private val minArea = 1e-10
  private val minEdge = 1e-7

  private val suitableTriangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)
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
      Gen.oneOf(Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0), Pga3dVectorMathGenerators.doubleInRange(-100, 100.0)),
      Gen.oneOf(Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0), Pga3dVectorMathGenerators.doubleInRange(-100, 100.0)),
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