package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.geom.Pga3dTriangle
import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.openjdk.jmh.annotations.*

import java.util.Random
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Pga3dTriangle.getNearestPoint: the Voronoi-region implementation (current) vs the legacy
 * one from 0.9.x, on three scenarios of point placement relative to the triangle:
 *  - far: several triangle sizes away, vertex/edge regions dominate (scan of a distant cell);
 *  - near: within ~0.3 of the surface, realistic contact search;
 *  - inside: the point projects inside the triangle.
 *
 * Run with: sbt "benchmark/Jmh/run -f1 .*NearestPointBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class NearestPointBenchmark:
  private val size = 2048
  private val mask = size - 1

  var triangles: Array[Pga3dTriangle] = uninitialized
  var farPoints: Array[Pga3dPoint] = uninitialized
  var nearPoints: Array[Pga3dPoint] = uninitialized
  var insidePoints: Array[Pga3dPoint] = uninitialized

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

    farPoints = triangles.map(t => t.center + randomVector(4.0))
    nearPoints = triangles.map { t =>
      t.getInterpolatedPoint(rng.nextDouble() * 1.6 - 0.3, rng.nextDouble() * 1.6 - 0.3) + randomVector(0.3)
    }
    insidePoints = triangles.map { t =>
      val t1 = rng.nextDouble() * 0.8 + 0.1
      val t2 = rng.nextDouble() * (0.9 - t1)
      t.getInterpolatedPoint(t1, t2) + Pga3dVector(0, 0, rng.nextDouble() * 2 - 1)
    }

  private inline def next(): Int =
    i = (i + 1) & mask
    i

  @Benchmark
  def farVoronoi: Pga3dPoint =
    val j = next()
    triangles(j).getNearestPoint(farPoints(j))

  @Benchmark
  def farLegacy: Pga3dPoint =
    val j = next()
    NearestPointBenchmark.getNearestPointLegacy(triangles(j), farPoints(j))

  @Benchmark
  def nearVoronoi: Pga3dPoint =
    val j = next()
    triangles(j).getNearestPoint(nearPoints(j))

  @Benchmark
  def nearLegacy: Pga3dPoint =
    val j = next()
    NearestPointBenchmark.getNearestPointLegacy(triangles(j), nearPoints(j))

  @Benchmark
  def nearDistance: Double =
    val j = next()
    triangles(j).distanceTo(nearPoints(j))

  @Benchmark
  def nearDistanceSquare: Double =
    val j = next()
    triangles(j).distanceSquareTo(nearPoints(j))

  @Benchmark
  def prefilterFartherThan: Boolean =
    val j = next()
    triangles(j).fartherThan(farPoints(j), 1.0)

  @Benchmark
  def prefilterViaAABB: Boolean =
    val j = next()
    !triangles(j).toAABB.contains(farPoints(j), expand = 1.0)

  @Benchmark
  def insideVoronoi: Pga3dPoint =
    val j = next()
    triangles(j).getNearestPoint(insidePoints(j))

  @Benchmark
  def insideLegacy: Pga3dPoint =
    val j = next()
    NearestPointBenchmark.getNearestPointLegacy(triangles(j), insidePoints(j))


object NearestPointBenchmark:
  /** Pga3dTriangle.getNearestPoint as of 0.9.x */
  def getNearestPointLegacy(triangle: Pga3dTriangle, p: Pga3dPoint): Pga3dPoint = {
    val (tba, tca) = triangle.getInterpolationFactors(p)

    val isInside = tba >= 0.0 && tca >= 0.0 && tba + tca <= 1.0

    if (isInside) {
      triangle.getInterpolatedPoint(tba, tca)
    } else {
      triangle.edges.map(e => e.getNearestPoint(p)).minBy(p2 => (p2 - p).norm)
    }
  }
