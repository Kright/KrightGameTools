package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPlane, Pga3dPoint, Pga3dVector}
import me.kright.gametools.flatarray.FlatDoubleSerializer

case class Pga3dTriangle(a: Pga3dPoint,
                         b: Pga3dPoint,
                         c: Pga3dPoint):

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

  def intersects(e: Pga3dEdge, eps: Double): Boolean =
    intersection(e, eps).isDefined

  def distanceToPlane(p: Pga3dPoint): Double =
    (getNearestPointOnPlane(p) - p).norm

  def getNearestPointOnPlane(p: Pga3dPoint): Pga3dPoint =
    p.projectOntoPlane(plane).toPoint

  def getNearestPoint(p: Pga3dPoint): Pga3dPoint = {
    val (tba, tca) = getInterpolationFactors(p)

    val isInside = tba >= 0.0 && tca >= 0.0 && tba + tca <= 1.0

    if (isInside) {
      getInterpolatedPoint(tba, tca)
    } else {
      edges.map(e => e.getNearestPoint(p)).minBy(p2 => (p2 - p).norm)
    }
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


  def distanceTo(p: Pga3dPoint): Double =
    (getNearestPoint(p) - p).norm

  def contains(p: Pga3dPoint, eps: Double): Boolean =
    distanceTo(p) <= eps


object Pga3dTriangle:
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

  def intersection(triangle: Pga3dTriangle, edge: Pga3dEdge, eps: Double): Option[Pga3dPoint] = {
    if (!triangle.toAABB.intersects(edge.toAABB, expand = eps)) {
      // short path when edge and triangle are far away from each other
      return None
    }

    val normalizedPlane: Pga3dPlane = triangle.normalizedPlane

    val da: Double = normalizedPlane v edge.a
    val db: Double = normalizedPlane v edge.b

    if (da > eps && db > eps) return None // edge is far away
    if (da < -eps && db < -eps) return None // edge is far away

    val eAB: Pga3dVector = edge.normalizedDirection
    val cos = normalizedPlane.x * eAB.x + normalizedPlane.y * eAB.y + normalizedPlane.z * eAB.z

    if (Math.abs(cos) > 0.001) {
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
