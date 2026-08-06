package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.geom.*
import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.openjdk.jmh.annotations.*

import java.util.Random
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Costs of the volume intersection queries against ~1-sized triangles:
 * sphere/capsule intersects and deepestContact, plus the underlying segment-triangle
 * distance. Note none of these have an internal AABB early-out - in a grid scan the
 * caller prefilters with fartherThan / toAABB, so the "miss" numbers here are the cost
 * of a full query on a candidate that survived the prefilter.
 *
 * Scenarios: hit (touching the triangle interior region), miss (a few sizes away),
 * pierce (the capsule axis crosses the triangle - the exact-zero branch).
 *
 * Run with: sbt "benchmark/Jmh/run -f1 .*ShapeIntersectionBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class ShapeIntersectionBenchmark:
  private val size = 2048
  private val mask = size - 1

  var triangles: Array[Pga3dTriangle] = uninitialized
  var spheresHit: Array[Pga3dSphere] = uninitialized
  var spheresMiss: Array[Pga3dSphere] = uninitialized
  var capsulesHit: Array[Pga3dCapsule] = uninitialized
  var capsulesMiss: Array[Pga3dCapsule] = uninitialized
  var capsulesPierce: Array[Pga3dCapsule] = uninitialized
  var otherCapsules: Array[Pga3dCapsule] = uninitialized

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

    def interior(t: Pga3dTriangle): Pga3dPoint = {
      val t1 = rng.nextDouble() * 0.8 + 0.1
      t.getInterpolatedPoint(t1, rng.nextDouble() * (0.9 - t1))
    }

    spheresHit = triangles.map(t => Pga3dSphere(interior(t) + randomVector(0.2), r = 0.4))
    spheresMiss = triangles.map(t => Pga3dSphere(t.center + Pga3dVector(3, 3, 3), r = 0.4))

    capsulesHit = triangles.map { t =>
      val p = interior(t) + randomVector(0.2)
      Pga3dCapsule(p - randomVector(0.5), p + randomVector(0.5), r = 0.4)
    }
    capsulesMiss = triangles.map { t =>
      val p = t.center + Pga3dVector(3, 3, 3)
      Pga3dCapsule(p - randomVector(0.5), p + randomVector(0.5), r = 0.4)
    }
    capsulesPierce = triangles.map { t =>
      val p = interior(t)
      val dir = randomVector(1.0) + Pga3dVector(0, 0, 0.5)
      Pga3dCapsule(p - dir, p + dir, r = 0.4)
    }
    otherCapsules = capsulesHit.map(c => Pga3dCapsule(c.a + randomVector(0.5), c.b + randomVector(0.5), r = 0.3))

  private inline def next(): Int =
    i = (i + 1) & mask
    i

  @Benchmark
  def sphereTriangleHit: Boolean =
    val j = next()
    spheresHit(j).intersects(triangles(j))

  @Benchmark
  def sphereTriangleMiss: Boolean =
    val j = next()
    spheresMiss(j).intersects(triangles(j))

  @Benchmark
  def sphereDeepestContact: Pga3dContact | Null =
    val j = next()
    spheresHit(j).deepestContact(triangles(j))

  @Benchmark
  def capsuleTriangleHit: Boolean =
    val j = next()
    capsulesHit(j).intersects(triangles(j))

  @Benchmark
  def capsuleTriangleMiss: Boolean =
    val j = next()
    capsulesMiss(j).intersects(triangles(j))

  @Benchmark
  def capsuleTrianglePierce: Boolean =
    val j = next()
    capsulesPierce(j).intersects(triangles(j))

  @Benchmark
  def capsuleDeepestContact: Pga3dContact | Null =
    val j = next()
    capsulesHit(j).deepestContact(triangles(j))

  @Benchmark
  def capsuleDeepestContactPierce: Pga3dContact | Null =
    val j = next()
    capsulesPierce(j).deepestContact(triangles(j))

  @Benchmark
  def edgeTriangleDistanceSquare: Double =
    val j = next()
    triangles(j).distanceSquareTo(capsulesHit(j).edge)

  @Benchmark
  def capsuleSphere: Boolean =
    val j = next()
    capsulesHit(j).intersects(spheresHit(j))

  @Benchmark
  def capsuleCapsule: Boolean =
    val j = next()
    capsulesHit(j).intersects(otherCapsules(j))
