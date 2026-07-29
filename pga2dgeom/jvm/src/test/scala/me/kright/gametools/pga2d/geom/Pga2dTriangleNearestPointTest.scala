package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Pga2dTriangle.getNearestPoint implementation used up to 0.9.x, kept as a correctness
 * reference: allocates edges and compares distances through sqrt, but is straightforward
 * enough to trust for well-conditioned triangles.
 */
object LegacyPga2dTriangleNearestPoint:
  def getNearestPoint(triangle: Pga2dTriangle, p: Pga2dPoint): Pga2dPoint = {
    val (tba, tca) = triangle.getInterpolationFactors(p)

    val isInside = tba >= 0.0 && tca >= 0.0 && tba + tca <= 1.0

    if (isInside) {
      triangle.getInterpolatedPoint(tba, tca)
    } else {
      triangle.edges.map(e => e.getNearestPoint(p)).minBy(p2 => (p2 - p).norm)
    }
  }


class Pga2dTriangleNearestPointTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  private val minEdge = 1e-7

  /** triangles the legacy implementation handles reliably (not too thin, not too small) */
  private val suitableTriangles: Gen[Pga2dTriangle] = Pga2dPhysicsGenerators.triangleIn(bounds)
    .filter { t =>
      val perimeter = t.perimeter
      val maxArea = (perimeter / 3) * (perimeter / 3) * 0.25 * Math.sqrt(3)

      t.area > 1e-10 &&
        (t.ab.norm > minEdge && t.ac.norm > minEdge && t.bc.norm > minEdge) &&
        t.area > maxArea * 0.0001
    }

  private def assertSameDistance(triangle: Pga2dTriangle, p: Pga2dPoint): Unit = {
    val newDist = (triangle.getNearestPoint(p) - p).norm
    val legacyDist = (LegacyPga2dTriangleNearestPoint.getNearestPoint(triangle, p) - p).norm

    // the 1e-12 * perimeter term covers the rounding noise of the legacy barycentric
    // reconstruction, which is amplified by ~1/sin^2 of the smallest angle
    // (bounded by the suitableTriangles area filter)
    assert((newDist - legacyDist).abs <= 1e-9 * (1.0 + legacyDist) + 1e-12 * triangle.perimeter,
      s"""
         |new = $newDist, legacy = $legacyDist
         |triangle = $triangle
         |area = ${triangle.area}, perimeter = ${triangle.perimeter}
         |point = $p""".stripMargin)
  }

  test("matches the legacy implementation on random triangles and points") {
    forAll(suitableTriangles, Pga2dPhysicsGenerators.pointIn(bounds), MinSuccessful(2000)) { (triangle, p) =>
      assertSameDistance(triangle, p)
    }
  }

  test("matches the legacy implementation for points near the boundary") {
    val factor = Pga2dVectorMathGenerators.doubleInRange(-0.3, 1.3)
    val offsetBounds = Pga2dAABB(Pga2dPoint(-0.3, -0.3), Pga2dPoint(0.3, 0.3))

    forAll(
      suitableTriangles, factor, factor, Pga2dPhysicsGenerators.vectorIn(offsetBounds),
      MinSuccessful(2000)
    ) { (triangle, tba, tca, offset) =>
      val p = triangle.getInterpolatedPoint(tba, tca) + offset * (triangle.perimeter / 3.0)
      assertSameDistance(triangle, p)
    }
  }

  test("interior points are returned as is") {
    val factor = Pga2dVectorMathGenerators.doubleInRange(0.01, 0.45)

    forAll(suitableTriangles, factor, factor, MinSuccessful(1000)) { (triangle, tba, tca) =>
      val p = triangle.getInterpolatedPoint(tba, tca)
      assert(triangle.getNearestPoint(p) == p, s"triangle = $triangle, p = $p")
    }
  }

  test("points of the triangle itself map to themselves") {
    val factor = Pga2dVectorMathGenerators.doubleInRange(0.0, 1.0)

    forAll(suitableTriangles, factor, factor, MinSuccessful(1000)) { (triangle, f1, f2) =>
      val (tba, tca) = if (f1 + f2 <= 1.0) (f1, f2) else (1.0 - f1, 1.0 - f2)
      val p = triangle.getInterpolatedPoint(tba, tca)
      val dist = (triangle.getNearestPoint(p) - p).norm
      assert(dist <= 1e-9 * (1.0 + triangle.perimeter), s"dist = $dist, triangle = $triangle, p = $p")
    }
  }

  /** all seven Voronoi regions get exact answers, at any uniform scale and offset of the scene */
  private def checkRegions(scale: Double, offset: Pga2dVector): Unit = {
    def transformed(x: Double, y: Double): Pga2dPoint =
      Pga2dPoint(x * scale, y * scale) + offset

    val triangle = Pga2dTriangle(transformed(0, 0), transformed(1, 0), transformed(0, 1))
    val tolerance = 1e-12 * (scale + offset.norm + 1e-300)

    def check(point: Pga2dPoint, expected: Pga2dPoint): Unit = {
      val nearest = triangle.getNearestPoint(point)
      assert((nearest - expected).norm <= tolerance,
        s"scale = $scale, offset = $offset, point = $point, nearest = $nearest, expected = $expected")
    }

    check(transformed(-1, -1), transformed(0, 0)) // vertex a
    check(transformed(2, -1), transformed(1, 0)) // vertex b
    check(transformed(-1, 2), transformed(0, 1)) // vertex c
    check(transformed(0.5, -1), transformed(0.5, 0)) // edge ab
    check(transformed(-2, 0.5), transformed(0, 0.5)) // edge ac
    check(transformed(1, 1), transformed(0.5, 0.5)) // edge bc
    check(transformed(0.25, 0.25), transformed(0.25, 0.25)) // inside: the point itself
  }

  test("returns exact points for every voronoi region") {
    checkRegions(scale = 1.0, offset = Pga2dVector(0, 0))
  }

  test("voronoi regions survive extreme coordinate magnitudes") {
    // beyond ~1e75 the intermediate products of dot products (~scale^4) overflow;
    // below ~1e-75 they underflow to zero and the answer degrades to the longest-edge fallback
    for (scale <- Seq(1e-70, 1e-30, 1e-10, 1e-3, 1e3, 1e10, 1e30, 1e70)) {
      checkRegions(scale, offset = Pga2dVector(0, 0))
    }
  }

  test("voronoi regions survive a far-away scene") {
    for (
      offset <- Seq(
        Pga2dVector(1e6, -1e6),
        Pga2dVector(-1e9, 1e9),
      )
    ) {
      checkRegions(scale = 1.0, offset = offset)
      checkRegions(scale = 1e-3, offset = offset)
    }
  }

  test("results scale linearly with the scene up to 1e+-30") {
    val scales = Gen.oneOf(1e-30, 1e-20, 1e-10, 1e-5, 1e5, 1e10, 1e20, 1e30)

    forAll(suitableTriangles, Pga2dPhysicsGenerators.pointIn(bounds), scales, MinSuccessful(1000)) {
      (triangle, p, scale) =>
        def scaled(q: Pga2dPoint): Pga2dPoint = Pga2dPoint(q.x * scale, q.y * scale)

        val nearest = triangle.getNearestPoint(p)
        val nearestScaled = triangle.map(scaled).getNearestPoint(scaled(p))

        val tolerance = 1e-9 * scale * (1.0 + (nearest - p).norm + triangle.perimeter)

        assert((nearestScaled - scaled(nearest)).norm <= tolerance,
          s"scale = $scale, nearest = $nearest, nearestScaled = $nearestScaled, triangle = $triangle, p = $p")
        assert(((nearestScaled - scaled(p)).norm - (nearest - p).norm * scale).abs <= tolerance,
          s"scale = $scale, dist = ${(nearest - p).norm}, distScaled = ${(nearestScaled - scaled(p)).norm}")
    }
  }

  test("thin triangles stay close to the nearest edge") {
    val thinnessGen = Gen.oneOf(1e-3, 1e-7, 1e-12, 1e-15, 0.0)

    forAll(suitableTriangles, Pga2dPhysicsGenerators.pointIn(bounds), thinnessGen, MinSuccessful(1000)) {
      (fat, p, thinness) =>
        val ab = fat.ab
        val onLine = fat.a + ab * (ab.antiDotI(fat.c - fat.a) / ab.normSquare)
        val heightVector = fat.c - onLine
        val triangle = Pga2dTriangle(fat.a, fat.b, onLine + heightVector * thinness)
        val height = heightVector.norm * thinness

        val dist = (triangle.getNearestPoint(p) - p).norm
        val minEdgeDist = triangle.edges.map(e => (e.getNearestPoint(p) - p).norm).min

        val tolerance = 1e-9 * (1.0 + minEdgeDist)
        assert(dist <= minEdgeDist + height + tolerance,
          s"dist = $dist, minEdgeDist = $minEdgeDist, height = $height, thinness = $thinness, triangle = $triangle, p = $p")
        assert(dist >= minEdgeDist - height - tolerance,
          s"dist = $dist, minEdgeDist = $minEdgeDist, height = $height, thinness = $thinness, triangle = $triangle, p = $p")
    }
  }

  test("degenerate triangles: all vertices coincide") {
    forAll(Pga2dPhysicsGenerators.pointIn(bounds), Pga2dPhysicsGenerators.pointIn(bounds), MinSuccessful(200)) {
      (a, p) =>
        val nearest = Pga2dTriangle(a, a, a).getNearestPoint(p)
        assert((nearest - a).norm == 0.0, s"a = $a, p = $p, nearest = $nearest")
    }
  }

  test("degenerate triangles: two vertices coincide") {
    forAll(
      Pga2dPhysicsGenerators.edgeIn(bounds).filter(_.magnitude > minEdge),
      Pga2dPhysicsGenerators.pointIn(bounds),
      MinSuccessful(500)
    ) { (edge, p) =>
      val expectedDist = (edge.getNearestPoint(p) - p).norm
      val tolerance = 1e-9 * (1.0 + expectedDist)

      for (
        triangle <- Seq(
          Pga2dTriangle(edge.a, edge.a, edge.b),
          Pga2dTriangle(edge.a, edge.b, edge.a),
          Pga2dTriangle(edge.a, edge.b, edge.b),
        )
      ) {
        val dist = (triangle.getNearestPoint(p) - p).norm
        assert((dist - expectedDist).abs <= tolerance,
          s"dist = $dist, expected = $expectedDist, triangle = $triangle, p = $p")
      }
    }
  }

  test("degenerate triangles: collinear vertices") {
    forAll(
      Pga2dPhysicsGenerators.edgeIn(bounds).filter(_.magnitude > minEdge),
      Pga2dVectorMathGenerators.doubleInRange(-2.0, 2.0),
      Pga2dPhysicsGenerators.pointIn(bounds),
      MinSuccessful(500)
    ) { (edge, t, p) =>
      val triangle = Pga2dTriangle(edge.a, edge.b, edge.interpolatedPoint(t))
      // the triangle degenerates to a segment: the union of its non-empty edges
      // (t may be exactly 0 or 1, making one of the edges a single point)
      val expectedDist = triangle.edges.filter(_.magnitudeSquare > 0.0)
        .map(e => (e.getNearestPoint(p) - p).norm).min
      val dist = (triangle.getNearestPoint(p) - p).norm

      assert((dist - expectedDist).abs <= 1e-9 * (1.0 + expectedDist),
        s"dist = $dist, expected = $expectedDist, t = $t, triangle = $triangle, p = $p")
    }
  }
