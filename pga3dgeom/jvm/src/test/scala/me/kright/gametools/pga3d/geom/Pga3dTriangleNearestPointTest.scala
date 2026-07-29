package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Pga3dTriangle.getNearestPoint implementation used up to 0.9.x, kept as a correctness
 * reference: allocates edges and compares distances through sqrt, but is straightforward
 * enough to trust for well-conditioned triangles.
 */
object LegacyPga3dTriangleNearestPoint:
  def getNearestPoint(triangle: Pga3dTriangle, p: Pga3dPoint): Pga3dPoint = {
    val (tba, tca) = triangle.getInterpolationFactors(p)

    val isInside = tba >= 0.0 && tca >= 0.0 && tba + tca <= 1.0

    if (isInside) {
      triangle.getInterpolatedPoint(tba, tca)
    } else {
      triangle.edges.map(e => e.getNearestPoint(p)).minBy(p2 => (p2 - p).norm)
    }
  }


class Pga3dTriangleNearestPointTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  private val minEdge = 1e-7

  /** triangles the legacy implementation handles reliably (not too thin, not too small) */
  private val suitableTriangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)
    .filter { t =>
      val perimeter = t.perimeter
      val maxArea = (perimeter / 3) * (perimeter / 3) * 0.25 * Math.sqrt(3)

      t.area > 1e-10 &&
        (t.ab.norm > minEdge && t.ac.norm > minEdge && t.bc.norm > minEdge) &&
        t.area > maxArea * 0.0001
    }

  private def assertSameDistance(triangle: Pga3dTriangle, p: Pga3dPoint): Unit = {
    val newDist = (triangle.getNearestPoint(p) - p).norm
    val legacyDist = (LegacyPga3dTriangleNearestPoint.getNearestPoint(triangle, p) - p).norm

    assert((newDist - legacyDist).abs <= 1e-9 * (1.0 + legacyDist),
      s"""
         |new = $newDist, legacy = $legacyDist
         |triangle = $triangle
         |area = ${triangle.area}, perimeter = ${triangle.perimeter}
         |point = $p""".stripMargin)
  }

  test("matches the legacy implementation on random triangles and points") {
    forAll(suitableTriangles, Pga3dPhysicsGenerators.pointIn(bounds), MinSuccessful(2000)) { (triangle, p) =>
      assertSameDistance(triangle, p)
    }
  }

  test("matches the legacy implementation for points near the surface") {
    val factor = Pga3dVectorMathGenerators.doubleInRange(-0.3, 1.3)
    val offsetBounds = Pga3dAABB(Pga3dPoint(-0.3, -0.3, -0.3), Pga3dPoint(0.3, 0.3, 0.3))

    forAll(
      suitableTriangles, factor, factor, Pga3dPhysicsGenerators.vectorIn(offsetBounds),
      MinSuccessful(2000)
    ) { (triangle, tba, tca, offset) =>
      val p = triangle.getInterpolatedPoint(tba, tca) + offset * (triangle.perimeter / 3.0)
      assertSameDistance(triangle, p)
    }
  }

  test("points of the triangle itself map to themselves") {
    val factor = Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0)

    forAll(suitableTriangles, factor, factor, MinSuccessful(1000)) { (triangle, f1, f2) =>
      // fold (f1, f2) into the lower-left half of the unit square = valid barycentric range
      val (tba, tca) = if (f1 + f2 <= 1.0) (f1, f2) else (1.0 - f1, 1.0 - f2)
      val p = triangle.getInterpolatedPoint(tba, tca)
      val dist = (triangle.getNearestPoint(p) - p).norm
      assert(dist <= 1e-9 * (1.0 + triangle.perimeter), s"dist = $dist, triangle = $triangle, p = $p")
    }
  }

  /** all seven Voronoi regions get exact answers, at any uniform scale and offset of the scene */
  private def checkRegions(scale: Double, offset: Pga3dVector): Unit = {
    def transformed(x: Double, y: Double, z: Double): Pga3dPoint =
      Pga3dPoint(x * scale, y * scale, z * scale) + offset

    val triangle = Pga3dTriangle(transformed(0, 0, 0), transformed(1, 0, 0), transformed(0, 1, 0))
    val tolerance = 1e-12 * (scale + offset.norm + 1e-300)

    def check(point: Pga3dPoint, expected: Pga3dPoint): Unit = {
      val nearest = triangle.getNearestPoint(point)
      assert((nearest - expected).norm <= tolerance,
        s"scale = $scale, offset = $offset, point = $point, nearest = $nearest, expected = $expected")
    }

    check(transformed(-1, -1, 5), transformed(0, 0, 0)) // vertex a
    check(transformed(2, -1, 0), transformed(1, 0, 0)) // vertex b
    check(transformed(-1, 2, -3), transformed(0, 1, 0)) // vertex c
    check(transformed(0.5, -1, 3), transformed(0.5, 0, 0)) // edge ab
    check(transformed(-2, 0.5, 0), transformed(0, 0.5, 0)) // edge ac
    check(transformed(1, 1, 0), transformed(0.5, 0.5, 0)) // edge bc
    check(transformed(0.25, 0.25, -2), transformed(0.25, 0.25, 0)) // inside
  }

  test("returns exact points for every voronoi region") {
    checkRegions(scale = 1.0, offset = Pga3dVector(0, 0, 0))
  }

  test("voronoi regions survive extreme coordinate magnitudes") {
    // beyond ~1e75 the intermediate products of dot products (~scale^4) overflow;
    // below ~1e-75 they underflow to zero and the answer degrades to the longest-edge fallback
    for (scale <- Seq(1e-70, 1e-30, 1e-10, 1e-3, 1e3, 1e10, 1e30, 1e70)) {
      checkRegions(scale, offset = Pga3dVector(0, 0, 0))
    }
  }

  test("voronoi regions survive a far-away scene") {
    for (
      offset <- Seq(
        Pga3dVector(1e6, -1e6, 1e6),
        Pga3dVector(-1e9, 1e9, 1e9),
      )
    ) {
      checkRegions(scale = 1.0, offset = offset)
      checkRegions(scale = 1e-3, offset = offset)
    }
  }

  test("results scale linearly with the scene up to 1e+-30") {
    val scales = Gen.oneOf(1e-30, 1e-20, 1e-10, 1e-5, 1e5, 1e10, 1e20, 1e30)

    forAll(suitableTriangles, Pga3dPhysicsGenerators.pointIn(bounds), scales, MinSuccessful(1000)) {
      (triangle, p, scale) =>
        def scaled(q: Pga3dPoint): Pga3dPoint = Pga3dPoint(q.x * scale, q.y * scale, q.z * scale)

        val nearest = triangle.getNearestPoint(p)
        val nearestScaled = triangle.map(scaled).getNearestPoint(scaled(p))

        // scaling by a power of ten perturbs the inputs by ~1 ulp, which near a Voronoi region
        // boundary may switch the branch; the answer is continuous across boundaries,
        // so the tolerance stays tight relative to the scene size
        val tolerance = 1e-9 * scale * (1.0 + (nearest - p).norm + triangle.perimeter)

        assert((nearestScaled - scaled(nearest)).norm <= tolerance,
          s"scale = $scale, nearest = $nearest, nearestScaled = $nearestScaled, triangle = $triangle, p = $p")
        assert(((nearestScaled - scaled(p)).norm - (nearest - p).norm * scale).abs <= tolerance,
          s"scale = $scale, dist = ${(nearest - p).norm}, distScaled = ${(nearestScaled - scaled(p)).norm}")
    }
  }

  test("thin triangles stay close to the nearest edge") {
    // flatten a healthy triangle: move c towards its projection onto the line ab,
    // keeping only a `thinness` fraction of the perpendicular component
    val thinnessGen = Gen.oneOf(1e-3, 1e-7, 1e-12, 1e-15, 0.0)

    forAll(suitableTriangles, Pga3dPhysicsGenerators.pointIn(bounds), thinnessGen, MinSuccessful(1000)) {
      (fat, p, thinness) =>
        val ab = fat.ab
        val onLine = fat.a + ab * (ab.antiDotI(fat.c - fat.a) / ab.normSquare)
        val heightVector = fat.c - onLine
        val triangle = Pga3dTriangle(fat.a, fat.b, onLine + heightVector * thinness)
        val height = heightVector.norm * thinness

        val dist = (triangle.getNearestPoint(p) - p).norm
        val minEdgeDist = triangle.edges.map(e => (e.getNearestPoint(p) - p).norm).min

        // every point of the triangle lies within `height` of its edges, so the true distance
        // differs from the edge-based one by at most `height`; the same slack covers
        // the longest-edge fallback for nearly degenerate triangles
        val tolerance = 1e-9 * (1.0 + minEdgeDist)
        assert(dist <= minEdgeDist + height + tolerance,
          s"dist = $dist, minEdgeDist = $minEdgeDist, height = $height, thinness = $thinness, triangle = $triangle, p = $p")
        assert(dist >= minEdgeDist - height - tolerance,
          s"dist = $dist, minEdgeDist = $minEdgeDist, height = $height, thinness = $thinness, triangle = $triangle, p = $p")
    }
  }

  test("degenerate triangles: all vertices coincide") {
    forAll(Pga3dPhysicsGenerators.pointIn(bounds), Pga3dPhysicsGenerators.pointIn(bounds), MinSuccessful(200)) {
      (a, p) =>
        val nearest = Pga3dTriangle(a, a, a).getNearestPoint(p)
        assert((nearest - a).norm == 0.0, s"a = $a, p = $p, nearest = $nearest")
    }
  }

  test("degenerate triangles: two vertices coincide") {
    forAll(
      Pga3dPhysicsGenerators.edgeIn(bounds).filter(_.magnitude > minEdge),
      Pga3dPhysicsGenerators.pointIn(bounds),
      MinSuccessful(500)
    ) { (edge, p) =>
      val expectedDist = (edge.getNearestPoint(p) - p).norm
      val tolerance = 1e-9 * (1.0 + expectedDist)

      for (
        triangle <- Seq(
          Pga3dTriangle(edge.a, edge.a, edge.b),
          Pga3dTriangle(edge.a, edge.b, edge.a),
          Pga3dTriangle(edge.a, edge.b, edge.b),
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
      Pga3dPhysicsGenerators.edgeIn(bounds).filter(_.magnitude > minEdge),
      Pga3dVectorMathGenerators.doubleInRange(-2.0, 2.0),
      Pga3dPhysicsGenerators.pointIn(bounds),
      MinSuccessful(500)
    ) { (edge, t, p) =>
      val triangle = Pga3dTriangle(edge.a, edge.b, edge.interpolatedPoint(t))
      // the triangle degenerates to a segment: the union of its non-empty edges
      // (t may be exactly 0 or 1, making one of the edges a single point)
      val expectedDist = triangle.edges.filter(_.magnitudeSquare > 0.0)
        .map(e => (e.getNearestPoint(p) - p).norm).min
      val dist = (triangle.getNearestPoint(p) - p).norm

      assert((dist - expectedDist).abs <= 1e-9 * (1.0 + expectedDist),
        s"dist = $dist, expected = $expectedDist, t = $t, triangle = $triangle, p = $p")
    }
  }
