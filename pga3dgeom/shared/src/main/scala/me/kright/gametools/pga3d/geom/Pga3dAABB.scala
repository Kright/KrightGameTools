package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPlane, Pga3dPoint, Pga3dTranslator, Pga3dVector}

import scala.annotation.targetName

/**
 * Axis-aligned bounding box
 * [[https://en.wikipedia.org/wiki/Minimum_bounding_box#Axis-aligned_minimum_bounding_box]]
 */
case class Pga3dAABB(min: Pga3dPoint,
                     max: Pga3dPoint):

  override def toString: String =
    s"Pga3dAABB(min = $min, max = $max)"

  def size: Pga3dVector =
    max - min

  def halfSize: Pga3dVector =
    size * 0.5

  def volume: Double =
    val s = size
    s.x * s.y * s.z

  def surfaceArea: Double =
    val s = size
    2.0 * (s.x * s.y + s.y * s.z + s.z * s.x)

  def center: Pga3dPoint =
    Pga3dPoint.mid(min, max)

  def vertices: Array[Pga3dPoint] =
    Array(
      Pga3dPoint(min.x, min.y, min.z),
      Pga3dPoint(min.x, min.y, max.z),
      Pga3dPoint(min.x, max.y, min.z),
      Pga3dPoint(min.x, max.y, max.z),
      Pga3dPoint(max.x, min.y, min.z),
      Pga3dPoint(max.x, min.y, max.z),
      Pga3dPoint(max.x, max.y, min.z),
      Pga3dPoint(max.x, max.y, max.z),
    )

  def edges: Array[Pga3dEdge] =
    val v = vertices
    Array(
      Pga3dEdge(v(0), v(1)),
      Pga3dEdge(v(0), v(2)),
      Pga3dEdge(v(0), v(4)),
      Pga3dEdge(v(1), v(3)),
      Pga3dEdge(v(1), v(5)),
      Pga3dEdge(v(2), v(3)),
      Pga3dEdge(v(2), v(6)),
      Pga3dEdge(v(3), v(7)),
      Pga3dEdge(v(4), v(5)),
      Pga3dEdge(v(4), v(6)),
      Pga3dEdge(v(5), v(7)),
      Pga3dEdge(v(6), v(7)),
    )

  def clamp(p: Pga3dPoint): Pga3dPoint =
    p.max(min).min(max)

  def distanceTo(p: Pga3dPoint): Double =
    val clampedPoint = clamp(p)
    (clampedPoint - p).norm

  def union(a: Pga3dAABB): Pga3dAABB =
    Pga3dAABB(
      min = this.min min a.min,
      max = this.max max a.max,
    )

  def union(p: Pga3dPoint): Pga3dAABB = {
    Pga3dAABB(
      min = this.min min p,
      max = this.max max p,
    )
  }

  def union(p: Pga3dEdge): Pga3dAABB =
    union(p.a).union(p.b)

  def union(p: Pga3dTriangle): Pga3dAABB =
    union(p.a).union(p.b).union(p.c)


  def expand(amount: Double): Pga3dAABB =
    Pga3dAABB(
      min - Pga3dVector(amount, amount, amount),
      max + Pga3dVector(amount, amount, amount)
    )

  def expand(v: Pga3dVector): Pga3dAABB =
    Pga3dAABB(
      min - v,
      max + v
    )

  def contains(p: Pga3dPoint): Boolean =
    (p.x >= min.x && p.x <= max.x) &&
      (p.y >= min.y && p.y <= max.y) &&
      (p.z >= min.z && p.z <= max.z)

  def contains(p: Pga3dPoint, expand: Double): Boolean =
    (p.x >= min.x - expand) && (p.x <= max.x + expand) &&
      (p.y >= min.y - expand) && (p.y <= max.y + expand) &&
      (p.z >= min.z - expand) && (p.z <= max.z + expand)

  def contains(p: Pga3dEdge): Boolean =
    contains(p.a) && contains(p.b)

  def contains(p: Pga3dEdge, expand: Double): Boolean =
    contains(p.a, expand) && contains(p.b, expand)

  def contains(p: Pga3dTriangle): Boolean =
    contains(p.a) && contains(p.b) && contains(p.c)

  def contains(p: Pga3dTriangle, expand: Double): Boolean =
    contains(p.a, expand) && contains(p.b, expand) && contains(p.c, expand)

  def contains(a: Pga3dAABB): Boolean =
    (min.x <= a.min.x && max.x >= a.max.x) &&
      (min.y <= a.min.y && max.y >= a.max.y) &&
      (min.z <= a.min.z && max.z >= a.max.z)

  def contains(a: Pga3dAABB, expand: Double): Boolean =
    (min.x - expand <= a.min.x && max.x + expand >= a.max.x) &&
      (min.y - expand <= a.min.y && max.y + expand >= a.max.y) &&
      (min.z - expand <= a.min.z && max.z + expand >= a.max.z)


  private def hasIntersection1d(min1: Double, max1: Double, min2: Double, max2: Double): Boolean =
    !(min1 > max2 || min2 > max1)


  def intersects(a: Pga3dAABB): Boolean =
    hasIntersection1d(min.x, max.x, a.min.x, a.max.x) &&
      hasIntersection1d(min.y, max.y, a.min.y, a.max.y) &&
      hasIntersection1d(min.z, max.z, a.min.z, a.max.z)

  def intersects(a: Pga3dAABB, expand: Double): Boolean =
    hasIntersection1d(min.x - expand, max.x + expand, a.min.x, a.max.x) &&
      hasIntersection1d(min.y - expand, max.y + expand, a.min.y, a.max.y) &&
      hasIntersection1d(min.z - expand, max.z + expand, a.min.z, a.max.z)

  def intersects(edge: Pga3dEdge): Boolean =
    intersection(edge).isDefined

  def intersects(triangle: Pga3dTriangle, eps: Double): Boolean = {
    if (!intersects(triangle.toAABB)) return false // short path for triangles far away
    if (contains(triangle.a) || contains(triangle.b) || contains(triangle.c)) return true // when vertex inside AABB

    if (!intersects(triangle.normalizedPlane)) return false

    // currently code below is not much efficient, but correctness and code size are more important
    if (triangle.edges.exists(this.intersects)) return true
    edges.exists(e => triangle.intersects(e, eps))
  }

  def intersection(edge: Pga3dEdge): Option[Pga3dEdge] =
    Pga3dAABB.intersection(this, edge)

  /** @param plane : normalized plane */
  def intersects(plane: Pga3dPlane): Boolean =
    Pga3dAABB.intersects(this, plane)


object Pga3dAABB:
  def apply(point: Pga3dPoint): Pga3dAABB =
    new Pga3dAABB(point, point)

  def apply(edge: Pga3dEdge): Pga3dAABB =
    Pga3dAABB(
      min = edge.a min edge.b,
      max = edge.a max edge.b,
    )

  def apply(t: Pga3dTriangle): Pga3dAABB =
    Pga3dAABB(
      min = (t.a min t.b) min t.c,
      max = (t.a max t.b) max t.c,
    )

  def apply(sphere: Pga3dSphere): Pga3dAABB = {
    val center = sphere.center
    val r = sphere.r
    Pga3dAABB(
      center - Pga3dVector(r, r, r),
      center + Pga3dVector(r, r, r),
    )
  }

  @targetName("unionPoints")
  def apply(t: Iterable[Pga3dPoint]): Pga3dAABB =
    var result = Pga3dAABB(t.head)
    for (p <- t) {
      result = result.union(p)
    }
    result

  @targetName("unionEdges")
  def apply(t: Iterable[Pga3dEdge]): Pga3dAABB =
    var result = Pga3dAABB(t.head)
    for (p <- t) {
      result = result.union(p)
    }
    result

  @targetName("unionTriangles")
  def apply(t: Iterable[Pga3dTriangle]): Pga3dAABB =
    var result = Pga3dAABB(t.head)
    for (p <- t) {
      result = result.union(p)
    }
    result

  extension (translator: Pga3dTranslator)
    def sandwich(aabb: Pga3dAABB): Pga3dAABB =
      Pga3dAABB(
        min = translator.sandwich(aabb.min),
        max = translator.sandwich(aabb.max),
      )


  /** @param plane : normalized plane */
  def intersects(aabb: Pga3dAABB, plane: Pga3dPlane): Boolean = {
    var alongNormMaxX: Double = 0
    var alongNormMinX: Double = 0
    var alongNormMaxY: Double = 0
    var alongNormMinY: Double = 0
    var alongNormMaxZ: Double = 0
    var alongNormMinZ: Double = 0

    if (plane.x >= 0) {
      alongNormMaxX = aabb.max.x
      alongNormMinX = aabb.min.x
    } else {
      alongNormMaxX = aabb.min.x
      alongNormMinX = aabb.max.x
    }

    if (plane.y >= 0) {
      alongNormMaxY = aabb.max.y
      alongNormMinY = aabb.min.y
    } else {
      alongNormMaxY = aabb.min.y
      alongNormMinY = aabb.max.y
    }

    if (plane.z >= 0) {
      alongNormMaxZ = aabb.max.z
      alongNormMinZ = aabb.min.z
    } else {
      alongNormMaxZ = aabb.min.z
      alongNormMinZ = aabb.max.z
    }

    val maxDistance = alongNormMaxX * plane.x + alongNormMaxY * plane.y + alongNormMaxZ * plane.z + plane.w
    val minDistance = alongNormMinX * plane.x + alongNormMinY * plane.y + alongNormMinZ * plane.z + plane.w

    maxDistance >= 0 && minDistance <= 0
  }

  def intersection(aabb: Pga3dAABB, edge: Pga3dEdge): Option[Pga3dEdge] =
    if (aabb.contains(edge.a) && aabb.contains(edge.b)) return Some(edge)

    val searcher = new MinMaxSearcher()

    searcher.updateMinMaxT(edge.a.x, edge.b.x, aabb.min.x, aabb.max.x)
    searcher.updateMinMaxT(edge.a.y, edge.b.y, aabb.min.y, aabb.max.y)
    searcher.updateMinMaxT(edge.a.z, edge.b.z, aabb.min.z, aabb.max.z)

    if (searcher.isSolutionExist) {
      Option(Pga3dEdge(edge.interpolatedPoint(searcher.lowerBound), edge.interpolatedPoint(searcher.upperBound)))
    } else None


private class MinMaxSearcher:
  var lowerBound = 0.0
  var upperBound = 1.0

  def isSolutionExist: Boolean =
    lowerBound <= upperBound

  def updateMinMaxT(edgeA: Double, edgeB: Double, min: Double, max: Double): Unit =
    if (!isSolutionExist) return

    if (edgeA <= edgeB) {
      updateMinMaxT(edgeA, edgeB, min, max, edgeNotReversed = true)
    } else {
      updateMinMaxT(edgeB, edgeA, min, max, edgeNotReversed = false)
    }

  private def updateLower(v: Double): Unit =
    lowerBound = Math.max(lowerBound, v)

  private def updateUpper(v: Double): Unit =
    upperBound = Math.min(upperBound, v)

  private def updateMinMaxT(edgeMin: Double, edgeMax: Double, min: Double, max: Double, edgeNotReversed: Boolean): Unit =
    if (edgeMin > max || edgeMax < min) {
      // no solution
      upperBound = -1
      return
    }

    val dist = edgeMax - edgeMin

    if (dist <= 1e-50) {
      // start and end coordinates are nearly identical and one of it is inside
      return
    }

    if (edgeMin < min) {
      val newT = (min - edgeMin) / dist

      if (edgeNotReversed) {
        updateLower(newT)
      } else {
        updateUpper(1.0 - newT)
      }
    }

    if (max < edgeMax) {
      val newT = (edgeMax - max) / dist

      if (edgeNotReversed) {
        updateUpper(1.0 - newT)
      } else {
        updateLower(newT)
      }
    }
