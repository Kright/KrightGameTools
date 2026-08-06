package me.kright.gametools.pga2d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}

case class Pga2dTriangle(a: Pga2dPoint,
                         b: Pga2dPoint,
                         c: Pga2dPoint) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  override def toString: String =
    s"Pga2dTriangle(a = $a, b = $b, c = $c)"

  def ab: Pga2dVector = b - a

  def ac: Pga2dVector = c - a

  def bc: Pga2dVector = c - b

  def perimeter: Double =
    ab.norm + bc.norm + ac.norm

  def center: Pga2dPoint =
    Pga2dPoint.mid(a, b, c)

  /** positive for counter-clockwise order of vertices, negative for clockwise */
  def signedArea: Double =
    val u = ab
    val v = ac
    0.5 * (u.x * v.y - u.y * v.x)

  def area: Double =
    signedArea.abs

  def toAABB: Pga2dAABB =
    Pga2dAABB(this)

  def map(f: Pga2dPoint => Pga2dPoint): Pga2dTriangle =
    Pga2dTriangle(f(a), f(b), f(c))

  def vertices: Array[Pga2dPoint] =
    Array(a, b, c)

  def edges: Array[Pga2dEdge] =
    Array(
      Pga2dEdge(a, b),
      Pga2dEdge(b, c),
      Pga2dEdge(c, a)
    )

  def intersection(edge: Pga2dEdge, eps: Double): Option[Pga2dPoint] =
    Pga2dTriangle.intersection(this, edge, eps)

  def intersects(e: Pga2dEdge, eps: Double): Boolean =
    intersection(e, eps).isDefined

  /**
   * closest point of the triangle to p, via Voronoi regions
   * (Christer Ericson, "Real-Time Collision Detection", 5.1.5):
   * no intermediate collections and no square roots.
   * Unlike the 3d case, a point inside a 2d triangle is its own nearest point
   */
  def getNearestPoint(p: Pga2dPoint): Pga2dPoint = {
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
        if (ab2 >= ac2 && ab2 >= bc2) Pga2dEdge(a, b)
        else if (ac2 >= bc2) Pga2dEdge(a, c)
        else Pga2dEdge(b, c)
      return longestEdge.getNearestPoint(p)
    }

    // all outside regions are rejected: p lies inside the triangle, and in 2d it is
    // its own projection (the 3d code reconstructs the barycentric projection here)
    p
  }

  /**
   * cheap conservative early reject: true when p is guaranteed to be farther than maxDistance
   * from the triangle. The check is against the triangle's axis-aligned bounding box
   * (a few comparisons, no multiplications and no allocations), so `true` is reliable while
   * `false` only means "possibly within maxDistance". Intended as a prefilter before
   * getNearestPoint when scanning many triangles
   */
  def fartherThan(p: Pga2dPoint, maxDistance: Double): Boolean =
    Pga2dTriangle.outsideAxis(p.x, maxDistance, a.x, b.x, c.x) ||
      Pga2dTriangle.outsideAxis(p.y, maxDistance, a.y, b.y, c.y)

  def getInterpolatedPoint(tba: Double, tca: Double): Pga2dPoint =
    a + ab * tba + ac * tca

  def getInterpolationFactors(p: Pga2dPoint): (Double, Double) =
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
      val longestEdge = if (ba2 > ca2) Pga2dEdge(a, b) else Pga2dEdge(a, c)
      val t = longestEdge.getInterpolationFactor(p)
      (t, 0.0)
    }


  /** unlike the 3d triangle, a 2d triangle has an interior, and points inside it have zero distance */
  def contains(p: Pga2dPoint): Boolean =
    val (tba, tca) = getInterpolationFactors(p)
    tba >= 0.0 && tca >= 0.0 && tba + tca <= 1.0

  /**
   * the pair (point on the triangle, point on the edge) minimizing the distance.
   * The 2d triangle is filled: an edge endpoint inside it or a proper crossing of the
   * boundary yields an exactly zero distance
   */
  def getNearestPoints(edge: Pga2dEdge): (Pga2dPoint, Pga2dPoint) = {
    // an interior endpoint is returned as is by the 2d getNearestPoint: exactly zero
    val nearestToA = getNearestPoint(edge.a)
    if (nearestToA == edge.a) return (edge.a, edge.a)
    val nearestToB = getNearestPoint(edge.b)
    if (nearestToB == edge.b) return (edge.b, edge.b)

    // a proper crossing of a triangle side: exactly zero at the crossing point
    val crossingAB = Pga2dTriangle.properCrossing(a, b, edge)
    if (!crossingAB.isNaN) {
      val p = edge.interpolatedPoint(crossingAB)
      return (p, p)
    }
    val crossingBC = Pga2dTriangle.properCrossing(b, c, edge)
    if (!crossingBC.isNaN) {
      val p = edge.interpolatedPoint(crossingBC)
      return (p, p)
    }
    val crossingCA = Pga2dTriangle.properCrossing(c, a, edge)
    if (!crossingCA.isNaN) {
      val p = edge.interpolatedPoint(crossingCA)
      return (p, p)
    }

    val acc = Pga2dPairOfNearestPoints(nearestToA, edge.a)
    acc.update(nearestToB, edge.b)
    acc.update(Pga2dEdge(a, b).getNearestPoints(edge))
    acc.update(Pga2dEdge(b, c).getNearestPoints(edge))
    acc.update(Pga2dEdge(c, a).getNearestPoints(edge))
    acc.pair
  }

  def distanceSquareTo(edge: Pga2dEdge): Double =
    val (onTriangle, onEdge) = getNearestPoints(edge)
    (onTriangle - onEdge).normSquare

  def distanceTo(edge: Pga2dEdge): Double =
    Math.sqrt(distanceSquareTo(edge))

  def distanceSquareTo(p: Pga2dPoint): Double =
    (getNearestPoint(p) - p).normSquare

  def distanceTo(p: Pga2dPoint): Double =
    Math.sqrt(distanceSquareTo(p))

  def contains(p: Pga2dPoint, eps: Double): Boolean =
    eps >= 0.0 && distanceSquareTo(p) <= eps * eps


object Pga2dTriangle:
  /** > 0 when a -> b -> c turns counter-clockwise */
  private inline def orient(a: Pga2dPoint, b: Pga2dPoint, c: Pga2dPoint): Double =
    (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

  /**
   * the interpolation factor along `edge` where it properly crosses the segment [p, q],
   * or NaN when the segments do not properly cross (touching or collinear cases are left
   * to the distance candidates, which classify them within rounding noise anyway)
   */
  private def properCrossing(p: Pga2dPoint, q: Pga2dPoint, edge: Pga2dEdge): Double = {
    val o1 = orient(p, q, edge.a)
    val o2 = orient(p, q, edge.b)
    if (!(o1 * o2 < 0.0)) return Double.NaN

    val o3 = orient(edge.a, edge.b, p)
    val o4 = orient(edge.a, edge.b, q)
    if (!(o3 * o4 < 0.0)) return Double.NaN

    o1 / (o1 - o2)
  }

  private inline def outsideAxis(p: Double, radius: Double, a: Double, b: Double, c: Double): Boolean = {
    val lo = p - radius
    val hi = p + radius
    (a < lo && b < lo && c < lo) || (a > hi && b > hi && c > hi)
  }

  /** allocation-free separation test: the AABB of the edge (expanded by eps) vs the triangle's */
  private inline def axisSeparates(e1: Double, e2: Double, a: Double, b: Double, c: Double, expand: Double): Boolean = {
    val lo = Math.min(e1, e2) - expand
    val hi = Math.max(e1, e2) + expand
    (a < lo && b < lo && c < lo) || (a > hi && b > hi && c > hi)
  }

  def intersection(triangle: Pga2dTriangle, edge: Pga2dEdge, eps: Double): Option[Pga2dPoint] = {
    if (axisSeparates(edge.a.x, edge.b.x, triangle.a.x, triangle.b.x, triangle.c.x, eps) ||
      axisSeparates(edge.a.y, edge.b.y, triangle.a.y, triangle.b.y, triangle.c.y, eps)) {
      // short path when edge and triangle are far away from each other
      return None
    }

    if (triangle.contains(edge.a, eps)) return Option(edge.a)
    if (triangle.contains(edge.b, eps)) return Option(edge.b)

    // both endpoints are outside: the edge intersects the triangle
    // only if it crosses (or comes eps-close to) the triangle boundary
    val triangleEdges = triangle.edges

    val pairOfNearestPoints = Pga2dPairOfNearestPoints(triangleEdges(0).getNearestPoints(edge))
    pairOfNearestPoints.update(triangleEdges(1).getNearestPoints(edge))
    pairOfNearestPoints.update(triangleEdges(2).getNearestPoints(edge))

    if (pairOfNearestPoints.distanceSquare <= eps * eps) {
      Option(pairOfNearestPoints.a)
    } else {
      None
    }
  }
