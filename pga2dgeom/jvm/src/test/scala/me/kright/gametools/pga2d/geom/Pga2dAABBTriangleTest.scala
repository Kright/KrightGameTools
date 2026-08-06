package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Pga2dAABB.intersects(triangle, eps) as it was before the SAT rewrite, kept as a
 * correctness reference (the eps parameter was unused in it).
 */
object LegacyPga2dAABBTriangleIntersects:
  def intersects(aabb: Pga2dAABB, triangle: Pga2dTriangle): Boolean = {
    if (!aabb.intersects(triangle.toAABB)) return false
    if (aabb.contains(triangle.a) || aabb.contains(triangle.b) || aabb.contains(triangle.c)) return true
    if (triangle.contains(aabb.center)) return true // when AABB is fully inside the triangle

    triangle.edges.exists(e => aabb.intersects(e))
  }


class Pga2dAABBTriangleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  private val aabbs: Gen[Pga2dAABB] = Pga2dPhysicsGenerators.aabbIn(bounds)
  private val triangles: Gen[Pga2dTriangle] = Pga2dPhysicsGenerators.triangleIn(bounds)

  // the references (the legacy implementation and intersection(edge) via MinMaxSearcher)
  // are ulp-unstable on zero-thickness boxes, where the SAT answer is exact;
  // flat boxes are covered by the soundness property and the flat-box unit test
  private val fatAabbs: Gen[Pga2dAABB] = aabbs
    .filter(a => a.size.x > 1e-6 && a.size.y > 1e-6)

  private val suitableTriangles: Gen[Pga2dTriangle] = triangles
    .filter { t =>
      val perimeter = t.perimeter
      val maxArea = (perimeter / 3) * (perimeter / 3) * 0.25 * Math.sqrt(3)
      t.area > 1e-10 && t.area > maxArea * 0.0001
    }

  /** see Pga3dAABBTriangleTest: grazing configurations may round either way */
  private def assertAgreesOrGrazes(aabb: Pga2dAABB, triangle: Pga2dTriangle,
                                   actual: Boolean, expected: Boolean, context: => String): Unit =
    if (actual != expected) {
      assert(aabb.expand(1e-6).intersects(triangle), s"real disagreement (expanded box misses): $context")
      assert(!aabb.expand(-1e-6).intersects(triangle), s"real disagreement (shrunk box still hits): $context")
    }

  test("matches the legacy implementation on non-degenerate triangles") {
    forAll(fatAabbs, suitableTriangles, MinSuccessful(2000)) { (aabb, triangle) =>
      val actual = aabb.intersects(triangle)
      val expected = LegacyPga2dAABBTriangleIntersects.intersects(aabb, triangle)
      assertAgreesOrGrazes(aabb, triangle, actual, expected, s"aabb = $aabb, triangle = $triangle")
    }
  }

  test("separation implies every triangle point is outside the box") {
    forAll(aabbs, triangles, MinSuccessful(1000)) { (aabb, triangle) =>
      if (!aabb.intersects(triangle)) {
        val n = 12
        for (i <- 0 to n; j <- 0 to n - i) {
          val p = triangle.getInterpolatedPoint(i.toDouble / n, j.toDouble / n)
          assert(!aabb.contains(p, expand = -1e-6),
            s"sampled point $p of the triangle is inside, aabb = $aabb, triangle = $triangle")
        }
      }
    }
  }

  test("a box fully inside the triangle intersects") {
    val triangle = Pga2dTriangle(Pga2dPoint(-100, -100), Pga2dPoint(100, -100), Pga2dPoint(0, 100))
    val aabb = Pga2dAABB(Pga2dPoint(-1, -1), Pga2dPoint(1, 1))
    assert(aabb.intersects(triangle))
    assert(aabb.expand(1e-9).intersects(triangle))
  }

  test("degenerate segment triangles match the exact segment-box test") {
    forAll(fatAabbs, Pga2dPhysicsGenerators.edgeIn(bounds), Pga2dVectorMathGenerators.doubleInRange(0.0, 1.0),
      MinSuccessful(1000)) { (aabb, edge, t) =>
      // scalacheck shrinking may escape the generator range; the third vertex must stay
      // inside [a, b] for the segment-box reference to describe the same shape
      val tc = t.max(0.0).min(1.0)
      val triangle = Pga2dTriangle(edge.a, edge.b, edge.interpolatedPoint(tc))
      val actual = aabb.intersects(triangle)
      val expected = aabb.intersection(edge) ne null
      assertAgreesOrGrazes(aabb, triangle, actual, expected, s"aabb = $aabb, edge = $edge, t = $tc")
    }
  }

  test("a flat (zero-height) box works, where the legacy reference was unstable") {
    val flat = Pga2dAABB(Pga2dPoint(-1, 0), Pga2dPoint(1, 0))

    // crosses the segment y = 0, x in [-1, 1] at (0.15, 0)
    val piercing = Pga2dTriangle(Pga2dPoint(0, -1), Pga2dPoint(0.3, 1), Pga2dPoint(0, 1))
    assert(flat.intersects(piercing))
    val outside = piercing.map(p => p + me.kright.gametools.pga2d.Pga2dVector(3.0, 0))
    assert(!flat.intersects(outside))
  }

  test("degenerate point triangles match contains") {
    forAll(aabbs, Pga2dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { (aabb, p) =>
      val triangle = Pga2dTriangle(p, p, p)
      assertAgreesOrGrazes(aabb, triangle, aabb.intersects(triangle), aabb.contains(p), s"aabb = $aabb, p = $p")
    }
  }
