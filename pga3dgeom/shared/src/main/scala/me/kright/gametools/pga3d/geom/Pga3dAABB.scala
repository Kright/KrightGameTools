package me.kright.gametools.pga3d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga3d.{Pga3dPlane, Pga3dPoint, Pga3dTranslator, Pga3dVector}

import scala.annotation.targetName

/**
 * Axis-aligned bounding box
 * [[https://en.wikipedia.org/wiki/Minimum_bounding_box#Axis-aligned_minimum_bounding_box]]
 */
case class Pga3dAABB(min: Pga3dPoint,
                     max: Pga3dPoint) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

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

  def distanceSquareTo(p: Pga3dPoint): Double =
    (clamp(p) - p).normSquare

  def distanceTo(p: Pga3dPoint): Double =
    Math.sqrt(distanceSquareTo(p))

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


  private def intersects1d(min1: Double, max1: Double, min2: Double, max2: Double): Boolean =
    !(min1 > max2 || min2 > max1)


  def intersects(a: Pga3dAABB): Boolean =
    intersects1d(min.x, max.x, a.min.x, a.max.x) &&
      intersects1d(min.y, max.y, a.min.y, a.max.y) &&
      intersects1d(min.z, max.z, a.min.z, a.max.z)

  def intersects(a: Pga3dAABB, expand: Double): Boolean =
    intersects1d(min.x - expand, max.x + expand, a.min.x, a.max.x) &&
      intersects1d(min.y - expand, max.y + expand, a.min.y, a.max.y) &&
      intersects1d(min.z - expand, max.z + expand, a.min.z, a.max.z)

  def intersects(edge: Pga3dEdge): Boolean =
    intersection(edge).isDefined

  /**
   * exact triangle-box overlap test,
   * see [[Pga3dAABB.intersects(aabb:Pga3dAABB,triangle:Pga3dTriangle)]].
   * For a tolerance, expand the box once: `aabb.expand(eps).intersects(triangle)`
   */
  def intersects(triangle: Pga3dTriangle): Boolean =
    Pga3dAABB.intersects(this, triangle)

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

  def apply(capsule: Pga3dCapsule): Pga3dAABB = {
    val r = capsule.r
    val rVector = Pga3dVector(r, r, r)
    Pga3dAABB(
      min = (capsule.a min capsule.b) - rVector,
      max = (capsule.a max capsule.b) + rVector,
    )
  }

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

  private inline def separated(p0: Double, p1: Double, p2: Double, r: Double): Boolean =
    (p0 > r && p1 > r && p2 > r) || (p0 < -r && p1 < -r && p2 < -r)

  private inline def rawSeparated(a: Double, b: Double, c: Double, lo: Double, hi: Double): Boolean =
    (a < lo && b < lo && c < lo) || (a > hi && b > hi && c > hi)

  private inline def insideRaw(p: Pga3dPoint,
                               loX: Double, hiX: Double,
                               loY: Double, hiY: Double,
                               loZ: Double, hiZ: Double): Boolean =
    p.x >= loX && p.x <= hiX && p.y >= loY && p.y <= hiY && p.z >= loZ && p.z <= hiZ

  /**
   * triangle-box overlap via the separating axis theorem (Akenine-Moller, "Fast 3D
   * Triangle-Box Overlap Testing"): 13 axes - 3 box face normals, the triangle plane
   * and 9 cross products of box axes with triangle edges. No allocations, square roots
   * or divisions. Degenerate triangles need no special cases: a zero cross-product axis
   * projects everything to 0 and never separates, so a segment is tested by the
   * known-complete segment-box axis subset and a point by the box axes alone.
   *
   * With NaN anywhere no axis reports separation and the answer degrades to
   * a conservative `true`. For a tolerance, expand the box once:
   * `aabb.expand(eps).intersects(triangle)`
   */
  def intersects(aabb: Pga3dAABB, triangle: Pga3dTriangle): Boolean = {
    // 3 box face normals first, on the raw coordinates: no multiplications and exact
    // comparisons - in a grid scan most triangles are rejected right here
    val loX = aabb.min.x
    val hiX = aabb.max.x
    if (rawSeparated(triangle.a.x, triangle.b.x, triangle.c.x, loX, hiX)) return false

    val loY = aabb.min.y
    val hiY = aabb.max.y
    if (rawSeparated(triangle.a.y, triangle.b.y, triangle.c.y, loY, hiY)) return false

    val loZ = aabb.min.z
    val hiZ = aabb.max.z
    if (rawSeparated(triangle.a.z, triangle.b.z, triangle.c.z, loZ, hiZ)) return false

    // early accept: a vertex inside the box
    if (insideRaw(triangle.a, loX, hiX, loY, hiY, loZ, hiZ)) return true
    if (insideRaw(triangle.b, loX, hiX, loY, hiY, loZ, hiZ)) return true
    if (insideRaw(triangle.c, loX, hiX, loY, hiY, loZ, hiZ)) return true

    // the remaining axes work in the box-center frame
    val hx = (aabb.max.x - aabb.min.x) * 0.5
    val hy = (aabb.max.y - aabb.min.y) * 0.5
    val hz = (aabb.max.z - aabb.min.z) * 0.5
    val cx = (aabb.max.x + aabb.min.x) * 0.5
    val cy = (aabb.max.y + aabb.min.y) * 0.5
    val cz = (aabb.max.z + aabb.min.z) * 0.5

    val v0x = triangle.a.x - cx
    val v0y = triangle.a.y - cy
    val v0z = triangle.a.z - cz
    val v1x = triangle.b.x - cx
    val v1y = triangle.b.y - cy
    val v1z = triangle.b.z - cz
    val v2x = triangle.c.x - cx
    val v2y = triangle.c.y - cy
    val v2z = triangle.c.z - cz

    val f0x = v1x - v0x
    val f0y = v1y - v0y
    val f0z = v1z - v0z
    val f1x = v2x - v1x
    val f1y = v2y - v1y
    val f1z = v2z - v1z
    val f2x = v0x - v2x
    val f2y = v0y - v2y
    val f2z = v0z - v2z

    // 9 cross products of the box axes with the triangle edges:
    // u_x x f = (0, -f.z, f.y), u_y x f = (f.z, 0, -f.x), u_z x f = (-f.y, f.x, 0)
    if (separated(f0y * v0z - f0z * v0y, f0y * v1z - f0z * v1y, f0y * v2z - f0z * v2y, hy * Math.abs(f0z) + hz * Math.abs(f0y))) return false
    if (separated(f0z * v0x - f0x * v0z, f0z * v1x - f0x * v1z, f0z * v2x - f0x * v2z, hx * Math.abs(f0z) + hz * Math.abs(f0x))) return false
    if (separated(f0x * v0y - f0y * v0x, f0x * v1y - f0y * v1x, f0x * v2y - f0y * v2x, hx * Math.abs(f0y) + hy * Math.abs(f0x))) return false

    if (separated(f1y * v0z - f1z * v0y, f1y * v1z - f1z * v1y, f1y * v2z - f1z * v2y, hy * Math.abs(f1z) + hz * Math.abs(f1y))) return false
    if (separated(f1z * v0x - f1x * v0z, f1z * v1x - f1x * v1z, f1z * v2x - f1x * v2z, hx * Math.abs(f1z) + hz * Math.abs(f1x))) return false
    if (separated(f1x * v0y - f1y * v0x, f1x * v1y - f1y * v1x, f1x * v2y - f1y * v2x, hx * Math.abs(f1y) + hy * Math.abs(f1x))) return false

    if (separated(f2y * v0z - f2z * v0y, f2y * v1z - f2z * v1y, f2y * v2z - f2z * v2y, hy * Math.abs(f2z) + hz * Math.abs(f2y))) return false
    if (separated(f2z * v0x - f2x * v0z, f2z * v1x - f2x * v1z, f2z * v2x - f2x * v2z, hx * Math.abs(f2z) + hz * Math.abs(f2x))) return false
    if (separated(f2x * v0y - f2y * v0x, f2x * v1y - f2y * v1x, f2x * v2y - f2y * v2x, hx * Math.abs(f2y) + hy * Math.abs(f2x))) return false

    // the triangle plane: for a (nearly) degenerate triangle the computed normal is pure
    // rounding noise and could fake a separation, so the axis is trusted only when the
    // normal is far above the noise floor (|n|^2 = |f0|^2 * |f1|^2 * sin^2, the threshold
    // is sin > 1e-10); below it the answer is conservative - the other 12 axes are already
    // complete for segments and points
    val nx = f0y * f1z - f0z * f1y
    val ny = f0z * f1x - f0x * f1z
    val nz = f0x * f1y - f0y * f1x
    val n2 = nx * nx + ny * ny + nz * nz
    val f02 = f0x * f0x + f0y * f0y + f0z * f0z
    val f12 = f1x * f1x + f1y * f1y + f1z * f1z
    if (n2 <= 1e-20 * f02 * f12) return true

    val d = nx * v0x + ny * v0y + nz * v0z
    val r = hx * Math.abs(nx) + hy * Math.abs(ny) + hz * Math.abs(nz)
    !(d > r || d < -r)
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
