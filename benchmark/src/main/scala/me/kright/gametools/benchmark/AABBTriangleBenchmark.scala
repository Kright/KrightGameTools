package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.geom.{Pga3dAABB, Pga3dTriangle}
import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.openjdk.jmh.annotations.*

import java.util.Random
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Pga3dAABB.intersects(triangle): the SAT rewrite (current) vs the pre-rewrite
 * implementation. Scenarios:
 *  - overlap: a triangle vertex is inside the box (both implementations exit early);
 *  - farMiss: bounding boxes are separated (early reject in both);
 *  - hardMiss: the triangle plane crosses the box, the bounding boxes overlap, but the
 *    triangle itself lies diagonally beyond a corner - the legacy version fell through
 *    to 3 edge-vs-box and up to 12 box-edge-vs-triangle checks with allocations.
 *
 * Run with: sbt "benchmark/Jmh/run -f1 .*AABBTriangleBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class AABBTriangleBenchmark:
  private val size = 2048
  private val mask = size - 1

  var boxes: Array[Pga3dAABB] = uninitialized
  var overlapTriangles: Array[Pga3dTriangle] = uninitialized
  var farMissTriangles: Array[Pga3dTriangle] = uninitialized
  var hardMissTriangles: Array[Pga3dTriangle] = uninitialized

  var i: Int = 0

  @Setup
  def setup(): Unit =
    val rng = new Random(42)

    def randomVector(scale: Double): Pga3dVector =
      Pga3dVector(rng.nextDouble() * 2 - 1, rng.nextDouble() * 2 - 1, rng.nextDouble() * 2 - 1) * scale

    boxes = Array.fill(size) {
      val center = Pga3dPoint(rng.nextDouble() * 4 - 2, rng.nextDouble() * 4 - 2, rng.nextDouble() * 4 - 2)
      val h = Pga3dVector(0.3 + rng.nextDouble() * 0.7, 0.3 + rng.nextDouble() * 0.7, 0.3 + rng.nextDouble() * 0.7)
      Pga3dAABB(center - h, center + h)
    }

    overlapTriangles = boxes.map { box =>
      val a = box.center + randomVector(0.2)
      Pga3dTriangle(a, a + randomVector(1.0), a + randomVector(1.0))
    }
    farMissTriangles = boxes.map { box =>
      val a = box.center + Pga3dVector(5.0, 5.0, 5.0) + randomVector(0.2)
      Pga3dTriangle(a, a + randomVector(1.0), a + randomVector(1.0))
    }
    hardMissTriangles = boxes.map { box =>
      // in the z = center.z plane (crosses the box), placed diagonally just beyond
      // the (+x, +y) corner: the bounding boxes overlap, but the diagonal separates
      val corner = box.max
      val z = box.center.z
      Pga3dTriangle(
        Pga3dPoint(corner.x + 0.9, corner.y - 0.5, z),
        Pga3dPoint(corner.x - 0.5, corner.y + 0.9, z),
        Pga3dPoint(corner.x + 0.9, corner.y + 0.9, z),
      )
    }

  private inline def next(): Int =
    i = (i + 1) & mask
    i

  @Benchmark
  def overlap: Boolean =
    val j = next()
    boxes(j).intersects(overlapTriangles(j))

  @Benchmark
  def overlapLegacy: Boolean =
    val j = next()
    AABBTriangleBenchmark.intersectsLegacy(boxes(j), overlapTriangles(j), 1e-9)

  @Benchmark
  def farMiss: Boolean =
    val j = next()
    boxes(j).intersects(farMissTriangles(j))

  @Benchmark
  def farMissLegacy: Boolean =
    val j = next()
    AABBTriangleBenchmark.intersectsLegacy(boxes(j), farMissTriangles(j), 1e-9)

  @Benchmark
  def hardMiss: Boolean =
    val j = next()
    boxes(j).intersects(hardMissTriangles(j))

  @Benchmark
  def hardMissLegacy: Boolean =
    val j = next()
    AABBTriangleBenchmark.intersectsLegacy(boxes(j), hardMissTriangles(j), 1e-9)


object AABBTriangleBenchmark:
  /** Pga3dAABB.intersects(triangle, eps) before the SAT rewrite */
  def intersectsLegacy(aabb: Pga3dAABB, triangle: Pga3dTriangle, eps: Double): Boolean = {
    if (!aabb.intersects(triangle.toAABB)) return false
    if (aabb.contains(triangle.a) || aabb.contains(triangle.b) || aabb.contains(triangle.c)) return true

    if (!aabb.intersects(triangle.normalizedPlane)) return false

    if (triangle.edges.exists(e => aabb.intersects(e))) return true
    aabb.edges.exists(e => triangle.intersects(e, eps))
  }
