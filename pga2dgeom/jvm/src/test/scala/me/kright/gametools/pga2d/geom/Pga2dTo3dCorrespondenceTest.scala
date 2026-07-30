package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint
import me.kright.gametools.pga3d.Pga3dPoint
import me.kright.gametools.pga3d.geom.{Pga3dAABB, Pga3dEdge, Pga3dTriangle}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * 2d geometry is the z=0 special case of 3d: embedding a 2d scene into the z=0 plane
 * must give the same distances and nearest points. With z=0 the dot products are even
 * bit-identical, so the only divergence allowed is the interior branch of getNearestPoint
 * (3d reconstructs the barycentric projection, 2d returns the point itself).
 */
class Pga2dTo3dCorrespondenceTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  // no filtering: the correspondence must hold for degenerate shapes too
  private val triangles: Gen[Pga2dTriangle] = Pga2dPhysicsGenerators.triangleIn(bounds)
  private val edges: Gen[Pga2dEdge] = Pga2dPhysicsGenerators.edgeIn(bounds)
  private val points: Gen[Pga2dPoint] = Pga2dPhysicsGenerators.pointIn(bounds)

  private def to3d(p: Pga2dPoint): Pga3dPoint = Pga3dPoint(p.x, p.y, 0.0)
  private def to3d(t: Pga2dTriangle): Pga3dTriangle = Pga3dTriangle(to3d(t.a), to3d(t.b), to3d(t.c))
  private def to3d(e: Pga2dEdge): Pga3dEdge = Pga3dEdge(to3d(e.a), to3d(e.b))

  private def assertRelEq(actual: Double, expected: Double, context: => String): Unit =
    assert((actual - expected).abs <= 1e-12 * (1.0 + expected.abs),
      s"actual = $actual, expected = $expected, $context")

  test("triangle: getNearestPoint agrees with the 3d embedding") {
    forAll(triangles, points, MinSuccessful(2000)) { (triangle, p) =>
      val nearest2d = triangle.getNearestPoint(p)
      val nearest3d = to3d(triangle).getNearestPoint(to3d(p))

      assert(nearest3d.z == 0.0, s"triangle = $triangle, p = $p, nearest3d = $nearest3d")

      val diff = Math.hypot(nearest3d.x - nearest2d.x, nearest3d.y - nearest2d.y)
      assert(diff <= 1e-12 * (1.0 + triangle.perimeter + (nearest2d - p).norm),
        s"nearest2d = $nearest2d, nearest3d = $nearest3d, triangle = $triangle, p = $p")
    }
  }

  test("triangle: distanceSquareTo agrees with the 3d embedding") {
    forAll(triangles, points, MinSuccessful(2000)) { (triangle, p) =>
      assertRelEq(
        triangle.distanceSquareTo(p),
        to3d(triangle).distanceSquareTo(to3d(p)),
        s"triangle = $triangle, p = $p")
    }
  }

  test("triangle: fartherThan agrees exactly with the 3d embedding") {
    val distances = Gen.oneOf(
      Pga2dVectorMathGenerators.doubleInRange(0.0, 10.0),
      Pga2dVectorMathGenerators.doubleInRange(0.0, 4.0 * halfSize),
    )

    forAll(triangles, points, distances, MinSuccessful(2000)) { (triangle, p, maxDistance) =>
      assert(triangle.fartherThan(p, maxDistance) == to3d(triangle).fartherThan(to3d(p), maxDistance),
        s"triangle = $triangle, p = $p, maxDistance = $maxDistance")
    }
  }

  test("edge: getNearestPoint and distanceSquareTo agree with the 3d embedding") {
    forAll(edges, points, MinSuccessful(2000)) { (edge, p) =>
      val nearest3d = to3d(edge).getNearestPoint(to3d(p))
      assert(nearest3d.z == 0.0, s"edge = $edge, p = $p, nearest3d = $nearest3d")

      val nearest2d = edge.getNearestPoint(p)
      assert(nearest3d.x == nearest2d.x && nearest3d.y == nearest2d.y,
        s"nearest2d = $nearest2d, nearest3d = $nearest3d, edge = $edge, p = $p")

      assertRelEq(edge.distanceSquareTo(p), to3d(edge).distanceSquareTo(to3d(p)), s"edge = $edge, p = $p")
    }
  }

  test("edge-edge: distanceSquareTo agrees with the 3d embedding") {
    forAll(edges, edges, MinSuccessful(2000)) { (e1, e2) =>
      assertRelEq(
        e1.distanceSquareTo(e2),
        to3d(e1).distanceSquareTo(to3d(e2)),
        s"e1 = $e1, e2 = $e2")
    }
  }

  test("aabb-triangle intersects agrees exactly with the 3d embedding") {
    // a flat (z in [0, 0]) 3d box against a z=0 triangle: the extra 3d SAT axes are all
    // vacuous (zero projections against zero radii), so the answers are bit-identical
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), triangles, MinSuccessful(2000)) { (aabb, triangle) =>
      val flatBox = Pga3dAABB(to3d(aabb.min), to3d(aabb.max))
      assert(aabb.intersects(triangle) == flatBox.intersects(to3d(triangle)),
        s"aabb = $aabb, triangle = $triangle")
    }
  }

  test("aabb: distanceSquareTo agrees with the 3d embedding") {
    forAll(triangles, points, MinSuccessful(2000)) { (triangle, p) =>
      val aabb2d = triangle.toAABB
      val aabb3d = Pga3dAABB(to3d(aabb2d.min), to3d(aabb2d.max))
      assertRelEq(aabb2d.distanceSquareTo(p), aabb3d.distanceSquareTo(to3d(p)), s"aabb = $aabb2d, p = $p")
    }
  }
