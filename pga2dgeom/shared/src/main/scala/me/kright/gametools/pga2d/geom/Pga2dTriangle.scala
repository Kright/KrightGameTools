package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}

case class Pga2dTriangle(a: Pga2dPoint,
                         b: Pga2dPoint,
                         c: Pga2dPoint):

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

  def getNearestPoint(p: Pga2dPoint): Pga2dPoint = {
    val (tba, tca) = getInterpolationFactors(p)

    val isInside = tba >= 0.0 && tca >= 0.0 && tba + tca <= 1.0

    if (isInside) {
      getInterpolatedPoint(tba, tca)
    } else {
      edges.map(e => e.getNearestPoint(p)).minBy(p2 => (p2 - p).norm)
    }
  }

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

  def distanceTo(p: Pga2dPoint): Double =
    (getNearestPoint(p) - p).norm

  def contains(p: Pga2dPoint, eps: Double): Boolean =
    distanceTo(p) <= eps


object Pga2dTriangle:
  def intersection(triangle: Pga2dTriangle, edge: Pga2dEdge, eps: Double): Option[Pga2dPoint] = {
    if (!triangle.toAABB.intersects(edge.toAABB, expand = eps)) {
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

    if (pairOfNearestPoints.distSquare <= eps * eps) {
      Option(pairOfNearestPoints.a)
    } else {
      None
    }
  }
