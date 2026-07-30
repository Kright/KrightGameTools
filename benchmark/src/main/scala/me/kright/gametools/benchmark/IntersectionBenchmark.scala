package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.geom.{Pga3dEdge, Pga3dTriangle}
import me.kright.gametools.pga3d.{Pga3dPlane, Pga3dPoint, Pga3dVector}
import org.openjdk.jmh.annotations.*

import java.util.Random
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Pga3dTriangle.intersection on a wheel-raycast-like workload: a short (~0.3) vertical edge
 * against ~1-sized triangles. Scenarios:
 *  - hit: the edge crosses the triangle interior;
 *  - missFar: the edge is AABB-separated (the early-reject path - this is what most
 *    triangles of a grid cell see);
 *  - missNear: the AABBs overlap but the crossing point is outside the triangle.
 * "Legacy" is the implementation before the hot-path rework: two AABB allocations for the
 * early reject and a sqrt-normalized direction for the parallelism check.
 * "CachedPlane" also skips the per-call normalizedPlane (with its sqrt) - what a grid with
 * precomputed planes for static geometry would get.
 *
 * Run with: sbt "benchmark/Jmh/run -f1 .*IntersectionBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class IntersectionBenchmark:
  private val size = 2048
  private val mask = size - 1
  private val eps = 1e-9

  var triangles: Array[Pga3dTriangle] = uninitialized
  var planes: Array[Pga3dPlane] = uninitialized
  var hitEdges: Array[Pga3dEdge] = uninitialized
  var missFarEdges: Array[Pga3dEdge] = uninitialized
  var missNearEdges: Array[Pga3dEdge] = uninitialized

  var i: Int = 0

  @Setup
  def setup(): Unit =
    val rng = new Random(42)

    def randomVector(scale: Double): Pga3dVector =
      Pga3dVector(rng.nextDouble() * 2 - 1, rng.nextDouble() * 2 - 1, rng.nextDouble() * 2 - 1) * scale

    triangles = Array.fill(size) {
      val a = Pga3dPoint(rng.nextDouble() * 4 - 2, rng.nextDouble() * 4 - 2, rng.nextDouble() * 4 - 2)
      Pga3dTriangle(a, a + randomVector(1.0), a + randomVector(1.0))
    }
    planes = triangles.map(_.normalizedPlane)

    val halfRay = Pga3dVector(0, 0, 0.15)
    hitEdges = triangles.map { t =>
      val t1 = rng.nextDouble() * 0.8 + 0.1
      val inside = t.getInterpolatedPoint(t1, rng.nextDouble() * (0.9 - t1))
      Pga3dEdge(inside - halfRay, inside + halfRay)
    }
    missFarEdges = triangles.map { t =>
      val p = t.center + Pga3dVector(4.0, 4.0, 4.0)
      Pga3dEdge(p - halfRay, p + halfRay)
    }
    missNearEdges = triangles.map { t =>
      // crosses the triangle's plane beyond the bc side, usually still inside the AABB
      val outside = t.getInterpolatedPoint(0.8, 0.8)
      Pga3dEdge(outside - halfRay, outside + halfRay)
    }

  private inline def next(): Int =
    i = (i + 1) & mask
    i

  @Benchmark
  def hit: Option[Pga3dPoint] =
    val j = next()
    triangles(j).intersection(hitEdges(j), eps)

  @Benchmark
  def hitLegacy: Option[Pga3dPoint] =
    val j = next()
    IntersectionBenchmark.intersectionLegacy(triangles(j), hitEdges(j), eps)

  @Benchmark
  def hitCachedPlane: Option[Pga3dPoint] =
    val j = next()
    triangles(j).intersection(hitEdges(j), planes(j), eps)

  @Benchmark
  def missFar: Option[Pga3dPoint] =
    val j = next()
    triangles(j).intersection(missFarEdges(j), eps)

  @Benchmark
  def missFarLegacy: Option[Pga3dPoint] =
    val j = next()
    IntersectionBenchmark.intersectionLegacy(triangles(j), missFarEdges(j), eps)

  @Benchmark
  def missNear: Option[Pga3dPoint] =
    val j = next()
    triangles(j).intersection(missNearEdges(j), eps)

  @Benchmark
  def missNearLegacy: Option[Pga3dPoint] =
    val j = next()
    IntersectionBenchmark.intersectionLegacy(triangles(j), missNearEdges(j), eps)

  @Benchmark
  def missNearCachedPlane: Option[Pga3dPoint] =
    val j = next()
    triangles(j).intersection(missNearEdges(j), planes(j), eps)


object IntersectionBenchmark:
  /** Pga3dTriangle.intersection before the hot-path rework (only the non-parallel prelude
   * differs from the current code; the parallel branch is not exercised by this data) */
  def intersectionLegacy(triangle: Pga3dTriangle, edge: Pga3dEdge, eps: Double): Option[Pga3dPoint] = {
    if (!triangle.toAABB.intersects(edge.toAABB, expand = eps)) {
      return None
    }

    val normalizedPlane: Pga3dPlane = triangle.normalizedPlane

    val da: Double = normalizedPlane v edge.a
    val db: Double = normalizedPlane v edge.b

    if (da > eps && db > eps) return None
    if (da < -eps && db < -eps) return None

    val eAB: Pga3dVector = edge.normalizedDirection
    val cos = normalizedPlane.x * eAB.x + normalizedPlane.y * eAB.y + normalizedPlane.z * eAB.z

    if (Math.abs(cos) > 0.001) {
      val intersectionPoint = edge.interpolatedPoint(da / (da - db))

      if (edge.contains(intersectionPoint, eps) && triangle.contains(intersectionPoint, eps)) {
        return Option(intersectionPoint)
      } else {
        return None
      }
    }

    // the parallel branch is shared with the current implementation and never hit here
    triangle.intersection(edge, normalizedPlane, eps)
  }
