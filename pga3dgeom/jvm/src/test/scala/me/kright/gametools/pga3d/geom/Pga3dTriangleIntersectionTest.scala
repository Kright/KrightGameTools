package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPlane, Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Pga3dTriangle.intersection as it was before the hot-path rework, kept as a correctness
 * reference: allocates two AABBs for the early reject and normalizes the edge direction
 * with a sqrt for the parallelism check.
 */
object LegacyPga3dTriangleIntersection:
  def intersection(triangle: Pga3dTriangle, edge: Pga3dEdge, eps: Double): Option[Pga3dPoint] = {
    if (!triangle.toAABB.intersects(edge.toAABB, expand = eps)) {
      return None
    }

    val normalizedPlane: Pga3dPlane = triangle.normalizedPlane

    val da: Double = normalizedPlane v edge.a
    val db: Double = normalizedPlane v edge.b

    if (da > eps && db > eps) return None
    if (da < -eps && db < -eps) return None

    val eAB: Pga3dVector = edge.normalizedDirection
    val cos = normalizedPlane.x * eAB.x + normalizedPlane.y * eAB.y + normalizedPlane.z * eAB.z

    if (Math.abs(cos) > 0.001) {
      val intersectionPoint = edge.interpolatedPoint(da / (da - db))

      if (edge.contains(intersectionPoint, eps) && triangle.contains(intersectionPoint, eps)) {
        return Option(intersectionPoint)
      } else {
        return None
      }
    }

    val clampedEdge: Pga3dEdge = {
      var t0: Double = 0.0
      var t1: Double = 1.0

      val clampEps = eps * 1.44

      if (da > clampEps) {
        t0 = (da - eps) / (da - db)
      } else if (da < -clampEps) {
        t0 = (da + eps) / (da - db)
      }

      if (db > eps) {
        t1 = 1.0 - (db - eps) / (db - da)
      } else if (db < -clampEps) {
        t1 = 1.0 - (db + eps) / (db - da)
      }

      Pga3dEdge(edge.interpolatedPoint(t0), edge.interpolatedPoint(t1))
    }

    val parallelEps = eps * 2.0

    if (triangle.contains(clampedEdge.a, parallelEps)) return Option(clampedEdge.a)
    if (triangle.contains(clampedEdge.b, parallelEps)) return Option(clampedEdge.b)

    val triangleEdges = triangle.edges

    val pairOfNearestPoints = Pga3dPairOfNearestPoints(triangleEdges(0).getNearestPoints(edge))
    pairOfNearestPoints.update(triangleEdges(1).getNearestPoints(edge))
    pairOfNearestPoints.update(triangleEdges(2).getNearestPoints(edge))

    if (pairOfNearestPoints.distSquare <= parallelEps * parallelEps) {
      Option(pairOfNearestPoints.a)
    } else {
      None
    }
  }


class Pga3dTriangleIntersectionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  // no filtering: degenerate triangles must behave too
  private val triangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)
  private val edges: Gen[Pga3dEdge] = Pga3dPhysicsGenerators.edgeIn(bounds)

  private val epsGen: Gen[Double] = Gen.oneOf(1e-9, 1e-6, 1e-3, 0.1, 10.0)

  private def compare(triangle: Pga3dTriangle, edge: Pga3dEdge, eps: Double): Unit = {
    val actual = triangle.intersection(edge, eps)
    val expected = LegacyPga3dTriangleIntersection.intersection(triangle, edge, eps)

    assert(actual.isDefined == expected.isDefined,
      s"actual = $actual, expected = $expected, triangle = $triangle, edge = $edge, eps = $eps")

    for (a <- actual; e <- expected) {
      assert((a - e).norm <= 1e-9 * (1.0 + triangle.perimeter + edge.magnitude),
        s"actual = $a, expected = $e, triangle = $triangle, edge = $edge, eps = $eps")
    }

    // validity: a returned point must be close to both shapes
    // (the parallel branch legitimately allows up to 2 * eps)
    for (p <- actual) {
      val slack = 2.0 * eps + 1e-9 * (1.0 + triangle.perimeter + edge.magnitude)
      assert(triangle.distanceSquareTo(p) <= slack * slack,
        s"point = $p is too far from the triangle, triangle = $triangle, edge = $edge, eps = $eps")
      assert(edge.distanceSquareTo(p) <= slack * slack,
        s"point = $p is too far from the edge, triangle = $triangle, edge = $edge, eps = $eps")
    }
  }

  test("matches the previous implementation on random triangles and edges") {
    forAll(triangles, edges, epsGen, MinSuccessful(2000)) { (triangle, edge, eps) =>
      compare(triangle, edge, eps)
    }
  }

  test("matches the previous implementation for edges crossing the triangle") {
    val factor = Pga3dVectorMathGenerators.doubleInRange(-0.2, 1.2)
    val directions = Pga3dPhysicsGenerators.vectorIn(
      Pga3dAABB(Pga3dPoint(-1, -1, -1), Pga3dPoint(1, 1, 1)))

    forAll(triangles, factor, factor, directions, epsGen, MinSuccessful(2000)) {
      (triangle, tba, tca, dir, eps) =>
        val onPlane = triangle.getInterpolatedPoint(tba, tca)
        val edge = Pga3dEdge(onPlane - dir * triangle.perimeter, onPlane + dir)
        compare(triangle, edge, eps)
    }
  }

  test("matches the previous implementation for in-plane edges (parallel branch)") {
    val factor = Pga3dVectorMathGenerators.doubleInRange(-1.0, 2.0)

    forAll(triangles, factor, factor, factor, factor, epsGen, MinSuccessful(2000)) {
      (triangle, f1, f2, f3, f4, eps) =>
        val edge = Pga3dEdge(triangle.getInterpolatedPoint(f1, f2), triangle.getInterpolatedPoint(f3, f4))
        compare(triangle, edge, eps)
    }
  }

  test("precomputed-plane overload returns exactly the same result") {
    forAll(triangles, edges, epsGen, MinSuccessful(1000)) { (triangle, edge, eps) =>
      val precomputed = triangle.intersection(edge, triangle.normalizedPlane, eps)
      val direct = triangle.intersection(edge, eps)
      assert(precomputed == direct,
        s"precomputed = $precomputed, direct = $direct, triangle = $triangle, edge = $edge, eps = $eps")
    }
  }

  test("degenerate (collinear) triangle behaves as a segment") {
    // the triangle degenerates to the segment x in [0, 2], its plane is NaN,
    // and the intersection goes through the parallel-branch fallback
    val triangle = Pga3dTriangle(Pga3dPoint(0, 0, 0), Pga3dPoint(2, 0, 0), Pga3dPoint(1, 0, 0))
    val eps = 1e-9

    val crossing = triangle.intersection(Pga3dEdge(Pga3dPoint(1, 0, -1), Pga3dPoint(1, 0, 1)), eps)
    assert(crossing.isDefined && (crossing.get - Pga3dPoint(1, 0, 0)).norm <= 2 * eps, s"crossing = $crossing")

    val missFar = triangle.intersection(Pga3dEdge(Pga3dPoint(5, 0, -1), Pga3dPoint(5, 0, 1)), eps)
    assert(missFar.isEmpty, s"missFar = $missFar")

    val missNear = triangle.intersection(Pga3dEdge(Pga3dPoint(1, 1, -1), Pga3dPoint(1, 1, 1)), eps)
    assert(missNear.isEmpty, s"missNear = $missNear")
  }
