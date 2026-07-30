package me.kright.gametools.pga2d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga2d.{Pga2dLine, Pga2dPoint, Pga2dTranslator, Pga2dVector}

import scala.annotation.targetName

/**
 * Axis-aligned bounding box
 * [[https://en.wikipedia.org/wiki/Minimum_bounding_box#Axis-aligned_minimum_bounding_box]]
 */
case class Pga2dAABB(min: Pga2dPoint,
                     max: Pga2dPoint) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

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

  def distanceSquareTo(p: Pga2dPoint): Double =
    (clamp(p) - p).normSquare

  def distanceTo(p: Pga2dPoint): Double =
    Math.sqrt(distanceSquareTo(p))

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

  /**
   * exact triangle-box overlap test,
   * see [[Pga2dAABB.intersects(aabb:Pga2dAABB,triangle:Pga2dTriangle)]].
   * For a tolerance, expand the box once: `aabb.expand(eps).intersects(triangle)`
   */
  def intersects(triangle: Pga2dTriangle): Boolean =
    Pga2dAABB.intersects(this, triangle)

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

  def apply(capsule: Pga2dCapsule): Pga2dAABB = {
    val r = capsule.r
    val rVector = Pga2dVector(r, r)
    Pga2dAABB(
      min = (capsule.a min capsule.b) - rVector,
      max = (capsule.a max capsule.b) + rVector,
    )
  }

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

  private inline def separated(p0: Double, p1: Double, p2: Double, r: Double): Boolean =
    (p0 > r && p1 > r && p2 > r) || (p0 < -r && p1 < -r && p2 < -r)

  private inline def rawSeparated(a: Double, b: Double, c: Double, lo: Double, hi: Double): Boolean =
    (a < lo && b < lo && c < lo) || (a > hi && b > hi && c > hi)

  /**
   * triangle-box overlap via the separating axis theorem: 5 axes - 2 box face normals
   * and 3 triangle edge normals. No allocations, square roots or divisions.
   * Degenerate triangles need no special cases: a zero edge normal never separates,
   * so a segment or a point is tested correctly.
   *
   * With NaN anywhere no axis reports separation and the answer degrades to
   * a conservative `true`. For a tolerance, expand the box once:
   * `aabb.expand(eps).intersects(triangle)`
   */
  def intersects(aabb: Pga2dAABB, triangle: Pga2dTriangle): Boolean = {
    // 2 box face normals first, on the raw coordinates: no multiplications and exact
    // comparisons - in a grid scan most triangles are rejected right here
    val loX = aabb.min.x
    val hiX = aabb.max.x
    if (rawSeparated(triangle.a.x, triangle.b.x, triangle.c.x, loX, hiX)) return false

    val loY = aabb.min.y
    val hiY = aabb.max.y
    if (rawSeparated(triangle.a.y, triangle.b.y, triangle.c.y, loY, hiY)) return false

    // early accept: a vertex inside the box
    if (triangle.a.x >= loX && triangle.a.x <= hiX && triangle.a.y >= loY && triangle.a.y <= hiY) return true
    if (triangle.b.x >= loX && triangle.b.x <= hiX && triangle.b.y >= loY && triangle.b.y <= hiY) return true
    if (triangle.c.x >= loX && triangle.c.x <= hiX && triangle.c.y >= loY && triangle.c.y <= hiY) return true

    // the remaining axes work in the box-center frame
    val hx = (aabb.max.x - aabb.min.x) * 0.5
    val hy = (aabb.max.y - aabb.min.y) * 0.5
    val cx = (aabb.max.x + aabb.min.x) * 0.5
    val cy = (aabb.max.y + aabb.min.y) * 0.5

    val v0x = triangle.a.x - cx
    val v0y = triangle.a.y - cy
    val v1x = triangle.b.x - cx
    val v1y = triangle.b.y - cy
    val v2x = triangle.c.x - cx
    val v2y = triangle.c.y - cy

    val f0x = v1x - v0x
    val f0y = v1y - v0y
    val f1x = v2x - v1x
    val f1y = v2y - v1y
    val f2x = v0x - v2x
    val f2y = v0y - v2y

    // 3 triangle edge normals (-f.y, f.x)
    if (separated(f0x * v0y - f0y * v0x, f0x * v1y - f0y * v1x, f0x * v2y - f0y * v2x, hx * Math.abs(f0y) + hy * Math.abs(f0x))) return false
    if (separated(f1x * v0y - f1y * v0x, f1x * v1y - f1y * v1x, f1x * v2y - f1y * v2x, hx * Math.abs(f1y) + hy * Math.abs(f1x))) return false
    if (separated(f2x * v0y - f2y * v0x, f2x * v1y - f2y * v1x, f2x * v2y - f2y * v2x, hx * Math.abs(f2y) + hy * Math.abs(f2x))) return false

    true
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
