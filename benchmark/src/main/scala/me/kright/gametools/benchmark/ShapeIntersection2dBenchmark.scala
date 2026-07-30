package me.kright.gametools.benchmark

import me.kright.gametools.pga2d.geom.*
import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}
import org.openjdk.jmh.annotations.*

import java.util.Random
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * the 2d counterparts of ShapeIntersectionBenchmark: the 2d triangle is filled, so the
 * "hit" cases exercise the interior-endpoint and boundary-crossing exact-zero branches.
 *
 * Run with: sbt "benchmark/Jmh/run -f1 .*ShapeIntersection2dBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class ShapeIntersection2dBenchmark:
  private val size = 2048
  private val mask = size - 1

  var triangles: Array[Pga2dTriangle] = uninitialized
  var circlesHit: Array[Pga2dCircle] = uninitialized
  var capsulesHit: Array[Pga2dCapsule] = uninitialized
  var capsulesMiss: Array[Pga2dCapsule] = uninitialized
  var capsulesCrossing: Array[Pga2dCapsule] = uninitialized

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

    def interior(t: Pga2dTriangle): Pga2dPoint = {
      val t1 = rng.nextDouble() * 0.8 + 0.1
      t.getInterpolatedPoint(t1, rng.nextDouble() * (0.9 - t1))
    }

    circlesHit = triangles.map(t => Pga2dCircle(interior(t) + randomVector(0.2), r = 0.4))
    capsulesHit = triangles.map { t =>
      val p = interior(t) + randomVector(0.2)
      Pga2dCapsule(p - randomVector(0.5), p + randomVector(0.5), r = 0.4)
    }
    capsulesMiss = triangles.map { t =>
      val p = t.center + Pga2dVector(3, 3)
      Pga2dCapsule(p - randomVector(0.5), p + randomVector(0.5), r = 0.4)
    }
    capsulesCrossing = triangles.map { t =>
      // both endpoints far outside, the axis cuts through the interior
      val p = interior(t)
      val dir = randomVector(1.0) + Pga2dVector(2.0, 0)
      Pga2dCapsule(p - dir, p + dir, r = 0.4)
    }

  private inline def next(): Int =
    i = (i + 1) & mask
    i

  @Benchmark
  def circleTriangleHit: Boolean =
    val j = next()
    circlesHit(j).intersects(triangles(j))

  @Benchmark
  def capsuleTriangleHit: Boolean =
    val j = next()
    capsulesHit(j).intersects(triangles(j))

  @Benchmark
  def capsuleTriangleMiss: Boolean =
    val j = next()
    capsulesMiss(j).intersects(triangles(j))

  @Benchmark
  def capsuleTriangleCrossing: Boolean =
    val j = next()
    capsulesCrossing(j).intersects(triangles(j))

  @Benchmark
  def capsuleCapsule: Boolean =
    val j = next()
    capsulesHit(j).intersects(capsulesMiss(j))
