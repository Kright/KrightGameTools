package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Pga2dTriangle.intersection as it was before the allocation-free early reject,
 * kept as a correctness reference.
 */
object LegacyPga2dTriangleIntersection:
  def intersection(triangle: Pga2dTriangle, edge: Pga2dEdge, eps: Double): Option[Pga2dPoint] = {
    if (!triangle.toAABB.intersects(edge.toAABB, expand = eps)) {
      return None
    }

    if (triangle.contains(edge.a, eps)) return Option(edge.a)
    if (triangle.contains(edge.b, eps)) return Option(edge.b)

    val triangleEdges = triangle.edges

    val pairOfNearestPoints = Pga2dPairOfNearestPoints(triangleEdges(0).getNearestPoints(edge))
    pairOfNearestPoints.update(triangleEdges(1).getNearestPoints(edge))
    pairOfNearestPoints.update(triangleEdges(2).getNearestPoints(edge))

    if (pairOfNearestPoints.distSquare <= eps * eps) {
      Option(pairOfNearestPoints.a)
    } else {
      None
    }
  }


class Pga2dTriangleIntersectionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  // no filtering: degenerate triangles must behave too
  private val triangles: Gen[Pga2dTriangle] = Pga2dPhysicsGenerators.triangleIn(bounds)
  private val edges: Gen[Pga2dEdge] = Pga2dPhysicsGenerators.edgeIn(bounds)

  private val epsGen: Gen[Double] = Gen.oneOf(1e-9, 1e-6, 1e-3, 0.1, 10.0)

  test("matches the previous implementation on random triangles and edges") {
    forAll(triangles, edges, epsGen, MinSuccessful(2000)) { (triangle, edge, eps) =>
      val actual = triangle.intersection(edge, eps)
      val expected = LegacyPga2dTriangleIntersection.intersection(triangle, edge, eps)
      assert(actual == expected,
        s"actual = $actual, expected = $expected, triangle = $triangle, edge = $edge, eps = $eps")
    }
  }
