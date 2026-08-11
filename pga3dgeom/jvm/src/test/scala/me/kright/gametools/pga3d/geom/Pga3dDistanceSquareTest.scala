package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dDistanceSquareTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  // no filtering: degenerate shapes must work too
  private val triangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)
  private val edges: Gen[Pga3dEdge] = Pga3dPhysicsGenerators.edgeIn(bounds)
  private val points: Gen[Pga3dPoint] = Pga3dPhysicsGenerators.pointIn(bounds)

  private val epsGen: Gen[Double] = Gen.oneOf(
    Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0),
    Pga3dVectorMathGenerators.doubleInRange(0.0, 2.0 * halfSize),
  )

  private def assertRelEq(actual: Double, expected: Double, context: => String): Unit =
    assert((actual - expected).abs <= 1e-9 * (1.0 + expected.abs),
      s"actual = $actual, expected = $expected, $context")

  /**
   * compares a scaled-scene distanceSquare against the plain-scene one, in the distance domain.
   * Scaling by a non-power-of-two rounds every input coordinate by ~1 ulp of the scene size;
   * for nearly-touching shapes (which the generators deliberately produce) that legitimately
   * shifts the true distance by up to ~1e-12 absolute, hence the absolute term of the tolerance
   */
  private def assertScaledDistanceSquare(dsqScaled: Double, dsqPlain: Double, scale: Double, context: => String): Unit =
    val dScaled = Math.sqrt(dsqScaled) / scale
    val dPlain = Math.sqrt(dsqPlain)
    assert((dScaled - dPlain).abs <= 1e-9 * (1.0 + dPlain) + 4.0 * halfSize * 1e-15,
      s"dScaled = $dScaled, dPlain = $dPlain, scale = $scale, $context")

  test("triangle: contains via squares agrees with the sqrt distance") {
    forAll(triangles, points, epsGen, MinSuccessful(2000)) { (triangle, p, eps) =>
      assert(triangle.contains(p, eps) == (triangle.distanceTo(p) <= eps),
        s"triangle = $triangle, p = $p, eps = $eps")
    }
  }

  test("edge: contains via squares agrees with the sqrt distance") {
    forAll(edges, points, epsGen, MinSuccessful(2000)) { (edge, p, eps) =>
      assert(edge.contains(p, eps) == (edge.distanceTo(p) <= eps),
        s"edge = $edge, p = $p, eps = $eps")
    }
  }

  test("edge-edge: intersects via squares agrees with the sqrt distance") {
    forAll(edges, edges, epsGen, MinSuccessful(2000)) { (e1, e2, eps) =>
      assert(e1.intersects(e2, eps) == (e1.distanceTo(e2) <= eps),
        s"e1 = $e1, e2 = $e2, eps = $eps")
    }
  }

  test("edge-edge: crossing and skew segments") {
    // crossing segments touch: zero distance, intersects with any eps >= 0
    val e1 = Pga3dEdge(Pga3dPoint(-1, 0, 0), Pga3dPoint(1, 0, 0))
    val e2 = Pga3dEdge(Pga3dPoint(0, -1, 0), Pga3dPoint(0, 1, 0))
    assert(e1.intersects(e2, 0.0))

    // the same pair pulled apart along z: skew segments at distance 0.5
    val skew = Pga3dEdge(Pga3dPoint(0, -1, 0.5), Pga3dPoint(0, 1, 0.5))
    assert(!e1.intersects(skew, 0.4))
    assert(e1.intersects(skew, 0.5))
  }

  test("contains with negative or NaN eps is always false") {
    val triangle = Pga3dTriangle(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0), Pga3dPoint(0, 1, 0))
    val edge = Pga3dEdge(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0))

    // the point is exactly on the shape: with squares an unguarded eps * eps would turn
    // a negative eps into a false positive
    assert(!triangle.contains(triangle.a, -1.0))
    assert(!triangle.contains(triangle.a, Double.NaN))
    assert(!edge.contains(edge.a, -1.0))
    assert(!edge.contains(edge.a, Double.NaN))
    assert(!edge.intersects(edge, -1.0))
    assert(!edge.intersects(edge, Double.NaN))
  }

  test("distanceSquare scales quadratically with the scene up to 1e+-30") {
    val scales = Gen.oneOf(1e-30, 1e-10, 1e10, 1e30)

    forAll(triangles, edges, points, scales, MinSuccessful(1000)) { (triangle, edge, p, scale) =>
      def scaled(q: Pga3dPoint): Pga3dPoint = Pga3dPoint(q.x * scale, q.y * scale, q.z * scale)

      assertScaledDistanceSquare(
        triangle.map(scaled).distanceSquareTo(scaled(p)),
        triangle.distanceSquareTo(p), scale,
        s"triangle = $triangle, p = $p")
      assertScaledDistanceSquare(
        edge.map(scaled).distanceSquareTo(scaled(p)),
        edge.distanceSquareTo(p), scale,
        s"edge = $edge, p = $p")
      assertScaledDistanceSquare(
        Pga3dAABB(triangle.map(scaled)).distanceSquareTo(scaled(p)),
        triangle.toAABB.distanceSquareTo(p), scale,
        s"aabb of $triangle, p = $p")
    }
  }

  test("edge-edge: distanceSquare agrees with the pair of nearest points at any scale") {
    val scales = Gen.oneOf(1e-30, 1.0, 1e30)

    forAll(edges, edges, scales, MinSuccessful(1000)) { (e1, e2, scale) =>
      def scaled(q: Pga3dPoint): Pga3dPoint = Pga3dPoint(q.x * scale, q.y * scale, q.z * scale)

      assertScaledDistanceSquare(
        e1.map(scaled).distanceSquareTo(e2.map(scaled)),
        e1.distanceSquareTo(e2), scale,
        s"e1 = $e1, e2 = $e2")
    }
  }

  test("zero-length edges behave as points") {
    forAll(points, points, MinSuccessful(500)) { (a, p) =>
      val pointEdge = Pga3dEdge(a, a)
      assert(pointEdge.getNearestPoint(p) == a)
      assertRelEq(pointEdge.distanceSquareTo(p), (p - a).normSquare, s"a = $a, p = $p")
    }
  }

  test("edge-edge distance for degenerate pairs") {
    forAll(points, points, edges, MinSuccessful(500)) { (a, b, edge) =>
      val pointA = Pga3dEdge(a, a)
      val pointB = Pga3dEdge(b, b)

      assertRelEq(pointA.distanceSquareTo(pointB), (a - b).normSquare, s"a = $a, b = $b")
      assertRelEq(pointA.distanceSquareTo(edge), edge.distanceSquareTo(a), s"a = $a, edge = $edge")
      assertRelEq(edge.distanceSquareTo(pointA), edge.distanceSquareTo(a), s"a = $a, edge = $edge")
    }
  }

  test("degenerate triangles measure distance to their point or segment") {
    forAll(points, points, MinSuccessful(500)) { (a, p) =>
      assertRelEq(Pga3dTriangle(a, a, a).distanceSquareTo(p), (p - a).normSquare, s"a = $a, p = $p")
    }

    forAll(edges, points, MinSuccessful(500)) { (edge, p) =>
      assertRelEq(
        Pga3dTriangle(edge.a, edge.b, edge.a).distanceSquareTo(p),
        edge.distanceSquareTo(p),
        s"edge = $edge, p = $p")
    }
  }
