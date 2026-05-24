package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dPoint, Pga3dVector}

/**
 *
 * @param a - point on cylinder axis and cylinder side
 * @param b - point on cylinder axis on another size
 * @param r - radius of cylinder
 */
case class Pga3dCylinder(a: Pga3dPoint,
                         b: Pga3dPoint,
                         r: Double):

  def ab: Pga3dVector =
    b - a

  def line: Pga3dBivector =
    a v b

  def center: Pga3dPoint =
    Pga3dPoint.mid(a, b)

  def abNormalized: Pga3dVector =
    ab.normalizedByNorm

  def halfAb: Pga3dVector =
    ab * 0.5

  def expand(expand: Double): Pga3dCylinder =
    expandAB(expand).expandR(expand)

  def expandAB(expand: Double): Pga3dCylinder =
    val expandVector = abNormalized * expand
    Pga3dCylinder(
      a = a - expandVector,
      b = b + expandVector,
      r = r
    )

  def expandR(expand: Double): Pga3dCylinder =
    Pga3dCylinder(a, b, r + expand)

  def contains(point: Pga3dPoint): Boolean =
    val t = Pga3dEdge.getInterpolationFactor(a, b, point)
    if (t < 0.0 || t > 1.0) return false

    val pointOnAxis = point.projectOntoLine(line).toPoint
    (pointOnAxis - point).norm <= r

  def contains(edge: Pga3dEdge): Boolean =
    contains(edge.a) && contains(edge.b)

  def boundingSphere: Pga3dSphere =
    Pga3dSphere(center, Math.sqrt(r * r + halfAb.normSquare))

  def intersects(edge: Pga3dEdge): Boolean = {
    val ta = Pga3dEdge.getInterpolationFactor(a, b, edge.a)
    val tb = Pga3dEdge.getInterpolationFactor(a, b, edge.b)

    val taIsInside = 0.0 <= ta && ta <= 1.0
    val tbIsInside = 0.0 <= tb && tb <= 1.0

    if (taIsInside && edge.a.projectOntoLine(line).toPoint.distanceTo(edge.a) <= r) return true
    if (tbIsInside && edge.b.projectOntoLine(line).toPoint.distanceTo(edge.b) <= r) return true

    if (taIsInside && tbIsInside) {
      return Pga3dEdge(a, b).distanceTo(edge) <= r
    }

    if (ta < 0.0 && tb < 0.0) return false
    if (ta > 1.0 && tb > 1.0) return false

    val edgeClampedTa =
      if (ta < 0.0) edge.interpolatedPoint(-ta / (tb - ta))
      else if (ta > 1.0) edge.interpolatedPoint((1.0 - tb) / (ta - tb))
      else edge.a

    val edgeClampedTb =
      if (tb < 0.0) edge.interpolatedPoint(-tb / (ta - tb))
      else if (tb > 1.0) edge.interpolatedPoint((1.0 - ta) / (tb - ta))
      else edge.b

    Pga3dEdge(edgeClampedTa, edgeClampedTb).distanceTo(edge) <= r
  }
