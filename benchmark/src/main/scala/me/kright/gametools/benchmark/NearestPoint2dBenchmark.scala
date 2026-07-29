package me.kright.gametools.benchmark

import me.kright.gametools.pga2d.geom.Pga2dTriangle
import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}
import org.openjdk.jmh.annotations.*

import java.util.Random
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Pga2dTriangle.getNearestPoint: the Voronoi-region implementation (current) vs the legacy
 * one from 0.9.x, see NearestPointBenchmark for the 3d counterpart and scenario descriptions.
 *
 * Run with: sbt "benchmark/Jmh/run -f1 .*NearestPoint2dBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class NearestPoint2dBenchmark:
  private val size = 2048
  private val mask = size - 1

  var triangles: Array[Pga2dTriangle] = uninitialized
  var farPoints: Array[Pga2dPoint] = uninitialized
  var nearPoints: Array[Pga2dPoint] = uninitialized
  var insidePoints: Array[Pga2dPoint] = uninitialized

  var i: Int = 0

  @Setup
  def setup(): Unit =
    val rng = new Random(42)

    def randomVector(scale: Double): Pga2dVector =
      Pga2dVector(rng.nextDouble() * 2 - 1, rng.nextDouble() * 2 - 1) * scale

    triangles = Array.fill(size) {
      val a = Pga2dPoint(rng.nextDouble() * 4 - 2, rng.nextDouble() * 4 - 2)
      Pga2dTriangle(a, a + randomVector(1.0), a + randomVector(1.0))
    }

    farPoints = triangles.map(t => t.center + randomVector(4.0))
    nearPoints = triangles.map { t =>
      t.getInterpolatedPoint(rng.nextDouble() * 1.6 - 0.3, rng.nextDouble() * 1.6 - 0.3) + randomVector(0.3)
    }
    insidePoints = triangles.map { t =>
      val t1 = rng.nextDouble() * 0.8 + 0.1
      val t2 = rng.nextDouble() * (0.9 - t1)
      t.getInterpolatedPoint(t1, t2)
    }

  private inline def next(): Int =
    i = (i + 1) & mask
    i

  @Benchmark
  def farVoronoi: Pga2dPoint =
    val j = next()
    triangles(j).getNearestPoint(farPoints(j))

  @Benchmark
  def farLegacy: Pga2dPoint =
    val j = next()
    NearestPoint2dBenchmark.getNearestPointLegacy(triangles(j), farPoints(j))

  @Benchmark
  def nearVoronoi: Pga2dPoint =
    val j = next()
    triangles(j).getNearestPoint(nearPoints(j))

  @Benchmark
  def nearLegacy: Pga2dPoint =
    val j = next()
    NearestPoint2dBenchmark.getNearestPointLegacy(triangles(j), nearPoints(j))

  @Benchmark
  def insideVoronoi: Pga2dPoint =
    val j = next()
    triangles(j).getNearestPoint(insidePoints(j))

  @Benchmark
  def insideLegacy: Pga2dPoint =
    val j = next()
    NearestPoint2dBenchmark.getNearestPointLegacy(triangles(j), insidePoints(j))

  @Benchmark
  def prefilterFartherThan: Boolean =
    val j = next()
    triangles(j).fartherThan(farPoints(j), 1.0)


object NearestPoint2dBenchmark:
  /** Pga2dTriangle.getNearestPoint as of 0.9.x */
  def getNearestPointLegacy(triangle: Pga2dTriangle, p: Pga2dPoint): Pga2dPoint = {
    val (tba, tca) = triangle.getInterpolationFactors(p)

    val isInside = tba >= 0.0 && tca >= 0.0 && tba + tca <= 1.0

    if (isInside) {
      triangle.getInterpolatedPoint(tba, tca)
    } else {
      triangle.edges.map(e => e.getNearestPoint(p)).minBy(p2 => (p2 - p).norm)
    }
  }
