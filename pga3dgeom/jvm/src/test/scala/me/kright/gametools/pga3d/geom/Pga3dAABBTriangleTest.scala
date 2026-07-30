package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Pga3dAABB.intersects(triangle) as it was before the SAT rewrite, kept as a correctness
 * reference. Note it is broken for degenerate triangles (the NaN plane fails the plane
 * test), so it is only compared on well-conditioned inputs.
 */
object LegacyPga3dAABBTriangleIntersects:
  def intersects(aabb: Pga3dAABB, triangle: Pga3dTriangle, eps: Double): Boolean = {
    if (!aabb.intersects(triangle.toAABB)) return false // short path for triangles far away
    if (aabb.contains(triangle.a) || aabb.contains(triangle.b) || aabb.contains(triangle.c)) return true

    if (!aabb.intersects(triangle.normalizedPlane)) return false

    if (triangle.edges.exists(e => aabb.intersects(e))) return true
    aabb.edges.exists(e => triangle.intersects(e, eps))
  }


class Pga3dAABBTriangleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  private val aabbs: Gen[Pga3dAABB] = Pga3dPhysicsGenerators.aabbIn(bounds)
  private val triangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)

  // both references (the legacy implementation and intersection(edge) via MinMaxSearcher)
  // are ulp-unstable on zero-thickness boxes, where the SAT answer is exact;
  // flat boxes are covered by the dedicated unit test and the soundness property below
  private val fatAabbs: Gen[Pga3dAABB] = aabbs
    .filter(a => a.size.x > 1e-6 && a.size.y > 1e-6 && a.size.z > 1e-6)

  private val suitableTriangles: Gen[Pga3dTriangle] = triangles
    .filter { t =>
      val perimeter = t.perimeter
      val maxArea = (perimeter / 3) * (perimeter / 3) * 0.25 * Math.sqrt(3)
      t.area > 1e-10 && t.area > maxArea * 0.0001
    }

  /**
   * exactly-touching configurations (which the special-value generators deliberately produce)
   * may be classified differently by implementations that compare rounded quantities;
   * on disagreement, certify that this is a grazing case: a hair-expanded box must intersect
   * and a hair-shrunk one must not
   */
  private def assertAgreesOrGrazes(aabb: Pga3dAABB, triangle: Pga3dTriangle,
                                   actual: Boolean, expected: Boolean, context: => String): Unit =
    if (actual != expected) {
      assert(aabb.expand(1e-6).intersects(triangle), s"real disagreement (expanded box misses): $context")
      assert(!aabb.expand(-1e-6).intersects(triangle), s"real disagreement (shrunk box still hits): $context")
    }

  test("matches the legacy implementation on non-degenerate triangles") {
    forAll(fatAabbs, suitableTriangles, MinSuccessful(2000)) { (aabb, triangle) =>
      val actual = aabb.intersects(triangle)
      val expected = LegacyPga3dAABBTriangleIntersects.intersects(aabb, triangle, eps = 1e-9)
      assertAgreesOrGrazes(aabb, triangle, actual, expected, s"aabb = $aabb, triangle = $triangle")
    }
  }

  test("separation implies every triangle point is outside the box") {
    forAll(aabbs, triangles, MinSuccessful(1000)) { (aabb, triangle) =>
      if (!aabb.intersects(triangle)) {
        val n = 12
        for (i <- 0 to n; j <- 0 to n - i) {
          val p = triangle.getInterpolatedPoint(i.toDouble / n, j.toDouble / n)
          // the small negative expand absorbs the rounding of the barycentric reconstruction
          assert(!aabb.contains(p, expand = -1e-6),
            s"sampled point $p of the triangle is inside, aabb = $aabb, triangle = $triangle")
        }
      }
    }
  }

  test("a triangle with a vertex inside the box intersects") {
    // a vertex strictly inside (25-75% of each extent of a fat box)
    // keeps the case away from grazing ambiguity
    val factor = Pga3dVectorMathGenerators.doubleInRange(0.25, 0.75)

    forAll(fatAabbs, Pga3dPhysicsGenerators.pointIn(bounds), Pga3dPhysicsGenerators.pointIn(bounds),
      factor, factor, factor, MinSuccessful(1000)) { (aabb, p1, p2, fx, fy, fz) =>
      val size = aabb.size
      val inside = aabb.min + Pga3dVector(size.x * fx, size.y * fy, size.z * fz)
      assert(aabb.intersects(Pga3dTriangle(inside, p1, p2)),
        s"aabb = $aabb, inside = $inside, p1 = $p1, p2 = $p2")
    }
  }

  test("a small box pierced by a large triangle intersects") {
    // no triangle vertex is inside and no triangle edge crosses the box:
    // only the box-edge-against-triangle direction detects this
    val aabb = Pga3dAABB(Pga3dPoint(-1, -1, -1), Pga3dPoint(1, 1, 1))
    val triangle = Pga3dTriangle(Pga3dPoint(200, -100, 0), Pga3dPoint(-200, -100, 0), Pga3dPoint(0, 200, 0))
    assert(aabb.intersects(triangle))
    assert(aabb.expand(1e-9).intersects(triangle))
  }

  test("degenerate segment triangles match the exact segment-box test") {
    forAll(fatAabbs, Pga3dPhysicsGenerators.edgeIn(bounds), Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0),
      MinSuccessful(1000)) { (aabb, edge, t) =>
      // scalacheck shrinking may escape the generator range; the third vertex must stay
      // inside [a, b] for the segment-box reference to describe the same shape
      val tc = t.max(0.0).min(1.0)
      val triangle = Pga3dTriangle(edge.a, edge.b, edge.interpolatedPoint(tc))
      val actual = aabb.intersects(triangle)
      val expected = aabb.intersection(edge).isDefined
      assertAgreesOrGrazes(aabb, triangle, actual, expected, s"aabb = $aabb, edge = $edge, t = $tc")
    }
  }

  test("degenerate point triangles match contains") {
    forAll(aabbs, Pga3dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { (aabb, p) =>
      val triangle = Pga3dTriangle(p, p, p)
      assertAgreesOrGrazes(aabb, triangle, aabb.intersects(triangle), aabb.contains(p), s"aabb = $aabb, p = $p")
    }
  }

  test("a flat (zero-thickness) box works, where the legacy reference was unstable") {
    val flat = Pga3dAABB(Pga3dPoint(-1, -1, 0), Pga3dPoint(1, 1, 0))

    // pierces the rectangle at (0.15, 0, 0)
    val piercing = Pga3dTriangle(Pga3dPoint(0, 0, -1), Pga3dPoint(0.3, 0, 1), Pga3dPoint(0, 0.3, 1))
    assert(flat.intersects(piercing))
    // the same triangle shifted beyond the rectangle
    val outside = piercing.map(p => p + Pga3dVector(3.0, 0, 0))
    assert(!flat.intersects(outside))
  }

  test("collinear triangle crossing the box is detected (the legacy implementation missed it)") {
    val aabb = Pga3dAABB(Pga3dPoint(-1, -1, -1), Pga3dPoint(1, 1, 1))
    // all vertices outside, the plane is NaN: the legacy version returned false
    val segment = Pga3dTriangle(Pga3dPoint(-2, 0, 0), Pga3dPoint(2, 0, 0), Pga3dPoint(-2, 0, 0))

    assert(aabb.intersects(segment))
    assert(!LegacyPga3dAABBTriangleIntersects.intersects(aabb, segment, eps = 1e-9))
  }
