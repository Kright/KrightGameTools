package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dLine, Pga2dPoint, Pga2dTranslator, Pga2dVector}

import scala.annotation.targetName

/**
 * Axis-aligned bounding box
 * [[https://en.wikipedia.org/wiki/Minimum_bounding_box#Axis-aligned_minimum_bounding_box]]
 */
case class Pga2dAABB(min: Pga2dPoint,
                     max: Pga2dPoint):

  override def toString: String =
    s"Pga2dAABB(min = $min, max = $max)"

  def size: Pga2dVector =
    max - min

  def halfSize: Pga2dVector =
    size * 0.5

  def area: Double =
    val s = size
    s.x * s.y

  def perimeter: Double =
    val s = size
    2.0 * (s.x + s.y)

  def center: Pga2dPoint =
    Pga2dPoint.mid(min, max)

  def vertices: Array[Pga2dPoint] =
    Array(
      Pga2dPoint(min.x, min.y),
      Pga2dPoint(min.x, max.y),
      Pga2dPoint(max.x, min.y),
      Pga2dPoint(max.x, max.y),
    )

  def edges: Array[Pga2dEdge] =
    val v = vertices
    Array(
      Pga2dEdge(v(0), v(1)),
      Pga2dEdge(v(0), v(2)),
      Pga2dEdge(v(1), v(3)),
      Pga2dEdge(v(2), v(3)),
    )

  def clamp(p: Pga2dPoint): Pga2dPoint =
    p.max(min).min(max)

  def distanceTo(p: Pga2dPoint): Double =
    val clampedPoint = clamp(p)
    (clampedPoint - p).norm

  def union(a: Pga2dAABB): Pga2dAABB =
    Pga2dAABB(
      min = this.min min a.min,
      max = this.max max a.max,
    )

  def union(p: Pga2dPoint): Pga2dAABB = {
    Pga2dAABB(
      min = this.min min p,
      max = this.max max p,
    )
  }

  def union(p: Pga2dEdge): Pga2dAABB =
    union(p.a).union(p.b)

  def union(p: Pga2dTriangle): Pga2dAABB =
    union(p.a).union(p.b).union(p.c)


  def expand(amount: Double): Pga2dAABB =
    Pga2dAABB(
      min - Pga2dVector(amount, amount),
      max + Pga2dVector(amount, amount)
    )

  def expand(v: Pga2dVector): Pga2dAABB =
    Pga2dAABB(
      min - v,
      max + v
    )

  def contains(p: Pga2dPoint): Boolean =
    (p.x >= min.x && p.x <= max.x) &&
      (p.y >= min.y && p.y <= max.y)

  def contains(p: Pga2dPoint, expand: Double): Boolean =
    (p.x >= min.x - expand) && (p.x <= max.x + expand) &&
      (p.y >= min.y - expand) && (p.y <= max.y + expand)

  def contains(p: Pga2dEdge): Boolean =
    contains(p.a) && contains(p.b)

  def contains(p: Pga2dEdge, expand: Double): Boolean =
    contains(p.a, expand) && contains(p.b, expand)

  def contains(p: Pga2dTriangle): Boolean =
    contains(p.a) && contains(p.b) && contains(p.c)

  def contains(p: Pga2dTriangle, expand: Double): Boolean =
    contains(p.a, expand) && contains(p.b, expand) && contains(p.c, expand)

  def contains(a: Pga2dAABB): Boolean =
    (min.x <= a.min.x && max.x >= a.max.x) &&
      (min.y <= a.min.y && max.y >= a.max.y)

  def contains(a: Pga2dAABB, expand: Double): Boolean =
    (min.x - expand <= a.min.x && max.x + expand >= a.max.x) &&
      (min.y - expand <= a.min.y && max.y + expand >= a.max.y)


  private def hasIntersection1d(min1: Double, max1: Double, min2: Double, max2: Double): Boolean =
    !(min1 > max2 || min2 > max1)


  def intersects(a: Pga2dAABB): Boolean =
    hasIntersection1d(min.x, max.x, a.min.x, a.max.x) &&
      hasIntersection1d(min.y, max.y, a.min.y, a.max.y)

  def intersects(a: Pga2dAABB, expand: Double): Boolean =
    hasIntersection1d(min.x - expand, max.x + expand, a.min.x, a.max.x) &&
      hasIntersection1d(min.y - expand, max.y + expand, a.min.y, a.max.y)

  def intersects(edge: Pga2dEdge): Boolean =
    intersection(edge).isDefined

  def intersects(triangle: Pga2dTriangle, eps: Double): Boolean = {
    if (!intersects(triangle.toAABB)) return false // short path for triangles far away
    if (contains(triangle.a) || contains(triangle.b) || contains(triangle.c)) return true // when vertex inside AABB
    if (triangle.contains(center)) return true // when AABB is fully inside the triangle

    // currently code below is not much efficient, but correctness and code size are more important
    triangle.edges.exists(this.intersects)
  }

  def intersection(edge: Pga2dEdge): Option[Pga2dEdge] =
    Pga2dAABB.intersection(this, edge)

  /** @param line : normalized line */
  def intersects(line: Pga2dLine): Boolean =
    Pga2dAABB.intersects(this, line)


object Pga2dAABB:
  def apply(point: Pga2dPoint): Pga2dAABB =
    new Pga2dAABB(point, point)

  def apply(edge: Pga2dEdge): Pga2dAABB =
    Pga2dAABB(
      min = edge.a min edge.b,
      max = edge.a max edge.b,
    )

  def apply(t: Pga2dTriangle): Pga2dAABB =
    Pga2dAABB(
      min = (t.a min t.b) min t.c,
      max = (t.a max t.b) max t.c,
    )

  def apply(circle: Pga2dCircle): Pga2dAABB = {
    val center = circle.center
    val r = circle.r
    Pga2dAABB(
      center - Pga2dVector(r, r),
      center + Pga2dVector(r, r),
    )
  }

  @targetName("unionPoints")
  def apply(t: Iterable[Pga2dPoint]): Pga2dAABB =
    var result = Pga2dAABB(t.head)
    for (p <- t) {
      result = result.union(p)
    }
    result

  @targetName("unionEdges")
  def apply(t: Iterable[Pga2dEdge]): Pga2dAABB =
    var result = Pga2dAABB(t.head)
    for (p <- t) {
      result = result.union(p)
    }
    result

  @targetName("unionTriangles")
  def apply(t: Iterable[Pga2dTriangle]): Pga2dAABB =
    var result = Pga2dAABB(t.head)
    for (p <- t) {
      result = result.union(p)
    }
    result

  extension (translator: Pga2dTranslator)
    def sandwich(aabb: Pga2dAABB): Pga2dAABB =
      Pga2dAABB(
        min = translator.sandwich(aabb.min),
        max = translator.sandwich(aabb.max),
      )


  /** @param line : normalized line */
  def intersects(aabb: Pga2dAABB, line: Pga2dLine): Boolean = {
    var alongNormMaxX: Double = 0
    var alongNormMinX: Double = 0
    var alongNormMaxY: Double = 0
    var alongNormMinY: Double = 0

    if (line.x >= 0) {
      alongNormMaxX = aabb.max.x
      alongNormMinX = aabb.min.x
    } else {
      alongNormMaxX = aabb.min.x
      alongNormMinX = aabb.max.x
    }

    if (line.y >= 0) {
      alongNormMaxY = aabb.max.y
      alongNormMinY = aabb.min.y
    } else {
      alongNormMaxY = aabb.min.y
      alongNormMinY = aabb.max.y
    }

    val maxDistance = alongNormMaxX * line.x + alongNormMaxY * line.y + line.w
    val minDistance = alongNormMinX * line.x + alongNormMinY * line.y + line.w

    maxDistance >= 0 && minDistance <= 0
  }

  def intersection(aabb: Pga2dAABB, edge: Pga2dEdge): Option[Pga2dEdge] =
    if (aabb.contains(edge.a) && aabb.contains(edge.b)) return Some(edge)

    val searcher = new MinMaxSearcher()

    searcher.updateMinMaxT(edge.a.x, edge.b.x, aabb.min.x, aabb.max.x)
    searcher.updateMinMaxT(edge.a.y, edge.b.y, aabb.min.y, aabb.max.y)

    if (searcher.isSolutionExist) {
      Option(Pga2dEdge(edge.interpolatedPoint(searcher.lowerBound), edge.interpolatedPoint(searcher.upperBound)))
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
