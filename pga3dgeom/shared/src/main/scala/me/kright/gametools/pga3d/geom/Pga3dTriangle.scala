package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPlane, Pga3dPoint, Pga3dVector}
import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps

case class Pga3dTriangle(a: Pga3dPoint,
                         b: Pga3dPoint,
                         c: Pga3dPoint) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  override def toString: String =
    s"Pga3dTriangle(a = $a, b = $b, c = $c)"

  def plane: Pga3dPlane =
    a v b v c

  def normalizedPlane: Pga3dPlane =
    plane.normalizedByBulk

  def ab: Pga3dVector = b - a

  def ac: Pga3dVector = c - a

  def bc: Pga3dVector = c - b

  def perimeter: Double =
    ab.norm + bc.norm + ac.norm

  def center: Pga3dPoint =
    Pga3dPoint.mid(a, b, c)

  def area: Double =
    0.5 * ab.antiWedge(ac).norm

  def toAABB: Pga3dAABB =
    Pga3dAABB(this)

  def map(f: Pga3dPoint => Pga3dPoint): Pga3dTriangle =
    Pga3dTriangle(f(a), f(b), f(c))

  def vertices: Array[Pga3dPoint] =
    Array(a, b, c)

  def edges: Array[Pga3dEdge] =
    Array(
      Pga3dEdge(a, b),
      Pga3dEdge(b, c),
      Pga3dEdge(c, a)
    )

  def intersectionWithPlane(edge: Pga3dEdge, eps: Double): Option[Pga3dPoint] =
    Pga3dTriangle.intersectionWithPlane(this, edge, eps)

  def intersection(edge: Pga3dEdge, eps: Double): Option[Pga3dPoint] =
    Pga3dTriangle.intersection(this, edge, eps)

  /** variant with `normalizedPlane` precomputed by the caller (cacheable for static geometry) */
  def intersection(edge: Pga3dEdge, normalizedPlane: Pga3dPlane, eps: Double): Option[Pga3dPoint] =
    Pga3dTriangle.intersection(this, edge, normalizedPlane, eps)

  def intersects(e: Pga3dEdge, eps: Double): Boolean =
    intersection(e, eps).isDefined

  def distanceToPlane(p: Pga3dPoint): Double =
    (getNearestPointOnPlane(p) - p).norm

  def getNearestPointOnPlane(p: Pga3dPoint): Pga3dPoint =
    p.projectOntoPlane(plane).toPoint

  /**
   * closest point of the triangle to p, via Voronoi regions
   * (Christer Ericson, "Real-Time Collision Detection", 5.1.5):
   * no intermediate collections and no square roots
   */
  def getNearestPoint(p: Pga3dPoint): Pga3dPoint = {
    val ab = b - a
    val ac = c - a
    val ap = p - a
    val d1 = ab.antiDotI(ap)
    val d2 = ac.antiDotI(ap)
    if (d1 <= 0.0 && d2 <= 0.0) return a

    val bp = p - b
    val d3 = ab.antiDotI(bp)
    val d4 = ac.antiDotI(bp)
    if (d3 >= 0.0 && d4 <= d3) return b

    // d1 - d3 > 0.0 (and the analogous guards below) protect the divisions against 0/0
    // when the triangle is degenerate; such cases fall through to the longest-edge fallback
    val vc = d1 * d4 - d3 * d2
    if (vc <= 0.0 && d1 >= 0.0 && d3 <= 0.0 && d1 - d3 > 0.0) return a + ab * (d1 / (d1 - d3))

    val cp = p - c
    val d5 = ab.antiDotI(cp)
    val d6 = ac.antiDotI(cp)
    if (d6 >= 0.0 && d5 <= d6) return c

    val vb = d5 * d2 - d1 * d6
    if (vb <= 0.0 && d2 >= 0.0 && d6 <= 0.0 && d2 - d6 > 0.0) return a + ac * (d2 / (d2 - d6))

    val va = d3 * d6 - d5 * d4
    if (va <= 0.0 && d4 - d3 >= 0.0 && d5 - d6 >= 0.0 && (d4 - d3) + (d5 - d6) > 0.0) {
      return b + (c - b) * ((d4 - d3) / ((d4 - d3) + (d5 - d6)))
    }

    // va + vb + vc equals the Gram determinant |ab|^2 * |ac|^2 * sin^2(angle between them),
    // but for a (nearly) degenerate triangle the computed value is rounding noise of either
    // sign, so it is compared against a relative threshold instead of zero.
    // The longest-edge fallback is off by at most the height of the triangle,
    // which is below 1e-6 of the longest edge here
    val denom = va + vb + vc
    val ab2 = d1 - d3 // == ab.normSquare, for free from the dot products above
    val ac2 = d2 - d6 // == ac.normSquare
    if (denom <= 1e-12 * ab2 * ac2) {
      val bc2 = (d4 - d3) + (d5 - d6) // == bc.normSquare
      val longestEdge =
        if (ab2 >= ac2 && ab2 >= bc2) Pga3dEdge(a, b)
        else if (ac2 >= bc2) Pga3dEdge(a, c)
        else Pga3dEdge(b, c)
      return longestEdge.getNearestPoint(p)
    }

    val invDenom = 1.0 / denom
    a + ab * (vb * invDenom) + ac * (vc * invDenom)
  }

  def getInterpolatedPoint(tba: Double, tca: Double): Pga3dPoint =
    a + ab * tba + ac * tca

  def getInterpolationFactors(p: Pga3dPoint): (Double, Double) =
    val ba = this.ab
    val ca = this.ac
    val pa = p - a

    val ba2 = ba.normSquare
    val ca2 = ca.normSquare

    val baDotCa = ba.antiDotI(ca)
    val paDotBa = pa.antiDotI(ba)
    val paDotCa = pa.antiDotI(ca)

    val det = ba2 * ca2 - baDotCa * baDotCa

    if (det >= 1e-40) {
      val detInv = 1.0 / det
      (
        detInv * (ca2 * paDotBa - baDotCa * paDotCa),
        detInv * (-baDotCa * paDotBa + ba2 * paDotCa)
      )
    } else {
      // highly parallel
      val longestEdge = if (ba2 > ca2) Pga3dEdge(a, b) else Pga3dEdge(a, c)
      val t = longestEdge.getInterpolationFactor(p)
      (t, 0.0)
    }


  /**
   * the pair (point on the triangle, point on the edge) minimizing the distance.
   * Candidates in the spirit of Ericson 5.1.10: the edge against the three triangle sides,
   * the edge endpoints against the triangle, and the plane-crossing point; when the edge
   * pierces the triangle interior the distance is exactly zero.
   * Degenerate triangles and zero-length edges are handled by the same candidates
   */
  def getNearestPoints(edge: Pga3dEdge): (Pga3dPoint, Pga3dPoint) = {
    val acc = Pga3dPairOfNearestPoints(getNearestPoint(edge.a), edge.a)
    acc.update(getNearestPoint(edge.b), edge.b)

    // the plane-crossing candidate; NaN distances of a degenerate triangle skip the branch
    val plane = normalizedPlane
    val da: Double = plane v edge.a
    val db: Double = plane v edge.b

    if (da * db < 0.0) {
      val p = edge.interpolatedPoint(da / (da - db))
      val nearestToP = getNearestPoint(p)
      // the interior test goes through the (degeneracy-hardened) nearest point rather than
      // barycentric signs: within the reconstruction noise of the triangle scale is inside
      val threshold = 1e-9 * perimeter
      if ((nearestToP - p).normSquare <= threshold * threshold) {
        // the edge pierces the triangle: the distance is exactly zero
        return (p, p)
      }
      acc.update(nearestToP, p)
    }

    acc.update(Pga3dEdge(a, b).getNearestPoints(edge))
    acc.update(Pga3dEdge(b, c).getNearestPoints(edge))
    acc.update(Pga3dEdge(c, a).getNearestPoints(edge))

    acc.pair
  }

  def distanceSquareTo(edge: Pga3dEdge): Double =
    val (onTriangle, onEdge) = getNearestPoints(edge)
    (onTriangle - onEdge).normSquare

  def distanceTo(edge: Pga3dEdge): Double =
    Math.sqrt(distanceSquareTo(edge))

  def distanceSquareTo(p: Pga3dPoint): Double =
    (getNearestPoint(p) - p).normSquare

  def distanceTo(p: Pga3dPoint): Double =
    Math.sqrt(distanceSquareTo(p))

  /**
   * cheap conservative early reject: true when p is guaranteed to be farther than maxDistance
   * from the triangle. The check is against the triangle's axis-aligned bounding box
   * (a few comparisons, no multiplications and no allocations), so `true` is reliable while
   * `false` only means "possibly within maxDistance". Intended as a prefilter before
   * getNearestPoint when scanning many triangles
   */
  def fartherThan(p: Pga3dPoint, maxDistance: Double): Boolean =
    Pga3dTriangle.outsideAxis(p.x, maxDistance, a.x, b.x, c.x) ||
      Pga3dTriangle.outsideAxis(p.y, maxDistance, a.y, b.y, c.y) ||
      Pga3dTriangle.outsideAxis(p.z, maxDistance, a.z, b.z, c.z)

  def contains(p: Pga3dPoint, eps: Double): Boolean =
    eps >= 0.0 && distanceSquareTo(p) <= eps * eps


object Pga3dTriangle:
  private inline def outsideAxis(p: Double, radius: Double, a: Double, b: Double, c: Double): Boolean = {
    val lo = p - radius
    val hi = p + radius
    (a < lo && b < lo && c < lo) || (a > hi && b > hi && c > hi)
  }

  def intersectionWithPlane(plane: Pga3dTriangle, edge: Pga3dEdge, eps: Double = 1e-9): Option[Pga3dPoint] = {
    val normalizedPlane = plane.normalizedPlane

    val da: Double = normalizedPlane v edge.a
    val db: Double = normalizedPlane v edge.b

    if (da > eps && db > eps) return None // on the same side relative to the plane and far away
    if (da < -eps && db < -eps) return None // on the same side relative to the plane and far away

    val diff = da - db

    if (da * db <= 0.0) { // on different sides to plane
      if (diff > 1e-50 || diff < -1e-50) { // non-parallel to plane
        return Option(Pga3dPoint.interpolate(edge.a, edge.b, t = da / diff))
      } else {
        return Option(edge.center)
      }
    }

    // on the same side relative to the plane

    // Now (-eps < da < eps) or (-eps < db < eps)
    if (Math.abs(da) < Math.abs(db)) {
      return Option(edge.a)
    } else {
      return Option(edge.b)
    }
  }

  /** allocation-free separation test: the AABB of the edge (expanded by eps) vs the triangle's */
  private inline def axisSeparates(e1: Double, e2: Double, a: Double, b: Double, c: Double, expand: Double): Boolean = {
    val lo = Math.min(e1, e2) - expand
    val hi = Math.max(e1, e2) + expand
    (a < lo && b < lo && c < lo) || (a > hi && b > hi && c > hi)
  }

  private def separatedByAABB(triangle: Pga3dTriangle, edge: Pga3dEdge, eps: Double): Boolean =
    axisSeparates(edge.a.x, edge.b.x, triangle.a.x, triangle.b.x, triangle.c.x, eps) ||
      axisSeparates(edge.a.y, edge.b.y, triangle.a.y, triangle.b.y, triangle.c.y, eps) ||
      axisSeparates(edge.a.z, edge.b.z, triangle.a.z, triangle.b.z, triangle.c.z, eps)

  def intersection(triangle: Pga3dTriangle, edge: Pga3dEdge, eps: Double): Option[Pga3dPoint] = {
    if (separatedByAABB(triangle, edge, eps)) {
      // short path when edge and triangle are far away from each other
      return None
    }

    intersectionImpl(triangle, edge, triangle.normalizedPlane, eps)
  }

  /**
   * variant with the triangle's `normalizedPlane` precomputed by the caller: for static
   * geometry the plane can be computed once and stored alongside the triangle, which takes
   * the plane construction and its sqrt off the hot path. Behaves exactly as
   * `intersection(triangle, edge, eps)`
   */
  def intersection(triangle: Pga3dTriangle, edge: Pga3dEdge, normalizedPlane: Pga3dPlane, eps: Double): Option[Pga3dPoint] = {
    if (separatedByAABB(triangle, edge, eps)) return None
    intersectionImpl(triangle, edge, normalizedPlane, eps)
  }

  private def intersectionImpl(triangle: Pga3dTriangle, edge: Pga3dEdge, normalizedPlane: Pga3dPlane, eps: Double): Option[Pga3dPoint] = {
    val da: Double = normalizedPlane v edge.a
    val db: Double = normalizedPlane v edge.b

    if (da > eps && db > eps) return None // edge is far away
    if (da < -eps && db < -eps) return None // edge is far away

    val dir: Pga3dVector = edge.direction
    val dot = normalizedPlane.x * dir.x + normalizedPlane.y * dir.y + normalizedPlane.z * dir.z

    // dot^2 > 1e-6 * |dir|^2 is |cos| > 0.001 without normalizing the direction (no sqrt)
    if (dot * dot > 1e-6 * dir.normSquare) {
      // common case, edge is not parallel to plane
      val intersectionPoint = edge.interpolatedPoint(da / (da - db))

      if (edge.contains(intersectionPoint, eps) && triangle.contains(intersectionPoint, eps)) {
        return Option(intersectionPoint)
      } else {
        return None
      }
    }

    // |cos| <= 0.001 and edge near plane of triangle
    val clampedEdge: Pga3dEdge = {
      var t0: Double = 0.0
      var t1: Double = 1.0

      val clampEps = eps * 1.44
      // clampEps to be sure that (da-db) is not less than eps * 0.44
      
      if (da > clampEps) {
        require(!(db > eps))
        t0 = (da - eps) / (da - db)
      } else if (da < -clampEps) {
        require(!(db < -eps))
        t0 = (da + eps) / (da - db)
      }

      if (db > eps) {
        require(!(da > clampEps))
        t1 = 1.0 - (db - eps) / (db - da)
      } else if (db < -clampEps) {
        require(!(da < -eps))
        t1 = 1.0 - (db + eps) / (db - da)
      }

      Pga3dEdge(edge.interpolatedPoint(t0), edge.interpolatedPoint(t1))
    }

    val parallelEps = eps * 2.0 // allow a bit bigger error to avoid computation errors

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
