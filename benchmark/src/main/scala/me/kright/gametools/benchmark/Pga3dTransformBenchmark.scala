package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * motor.sandwich(x) vs. the cached Pga3dProjectiveTransform.sandwich(x) vs. the normalized
 * Pga3dTransform.sandwich(x) for the common argument classes, plus the cost of building each
 * transform from a motor. One benchmark invocation applies the same transformation to `size`
 * random arguments and accumulates all the result components, so nothing gets
 * dead-code-eliminated; divide the score by `size` for the per-application cost.
 *
 * Both transforms pay one conversion per motor (the creation benchmarks) and then apply a
 * precomputed matrix, while motor.sandwich recomputes the coefficient products on every call -
 * so a transform wins when the same motor is applied more than a few times. The normalized
 * Pga3dTransform additionally drops the Study-number corrections (normSquare / normSquareI)
 * from the formulas and narrows the result types: sandwich of a Pga3dPoint is a Pga3dPoint,
 * not a Pga3dProjectivePoint.
 *
 * Results on ryzen 5950 (-wi 3 -i 3, size = 1000, so the score is ns per one application):
 * [info] Benchmark                                          (size)  Mode  Cnt   Score     Error  Units
 * [info] Pga3dTransformBenchmark.bivectorViaMotor             1000  avgt    3  17.611 ±   1.005  us/op
 * [info] Pga3dTransformBenchmark.bivectorViaProjective        1000  avgt    3   5.606 ±   0.096  us/op
 * [info] Pga3dTransformBenchmark.bivectorViaTransform         1000  avgt    3   5.605 ±   0.217  us/op
 * [info] Pga3dTransformBenchmark.creationFromNormalized       1000  avgt    3  18.492 ±   0.960  us/op
 * [info] Pga3dTransformBenchmark.creationProjective           1000  avgt    3  21.090 ±   1.496  us/op
 * [info] Pga3dTransformBenchmark.creationRenormalizing        1000  avgt    3  34.101 ±  14.653  us/op
 * [info] Pga3dTransformBenchmark.motorViaMotor                1000  avgt    3  24.460 ± 110.312  us/op
 * [info] Pga3dTransformBenchmark.motorViaProjective           1000  avgt    3   6.212 ±   0.127  us/op
 * [info] Pga3dTransformBenchmark.motorViaTransform            1000  avgt    3   5.901 ±   0.521  us/op
 * [info] Pga3dTransformBenchmark.planeViaMotor                1000  avgt    3  11.086 ±  36.249  us/op
 * [info] Pga3dTransformBenchmark.planeViaProjective           1000  avgt    3   1.927 ±   0.067  us/op
 * [info] Pga3dTransformBenchmark.planeViaTransform            1000  avgt    3   1.977 ±   0.102  us/op
 * [info] Pga3dTransformBenchmark.pointViaMotor                1000  avgt    3   9.345 ±   0.999  us/op
 * [info] Pga3dTransformBenchmark.pointViaProjective           1000  avgt    3   1.673 ±   0.629  us/op
 * [info] Pga3dTransformBenchmark.pointViaProjectiveToPoint    1000  avgt    3   1.769 ±   0.314  us/op
 * [info] Pga3dTransformBenchmark.pointViaTransform            1000  avgt    3   1.524 ±   0.052  us/op
 * [info] Pga3dTransformBenchmark.vectorViaMotor               1000  avgt    3   2.002 ±   3.972  us/op
 * [info] Pga3dTransformBenchmark.vectorViaProjective          1000  avgt    3   1.847 ±   0.009  us/op
 * [info] Pga3dTransformBenchmark.vectorViaTransform           1000  avgt    3   1.848 ±   0.118  us/op
 *
 * Both transforms beat motor.sandwich 3-6x on everything but the (JIT-hoistable) vector. Between
 * the two transforms the shared blocks (bivector, vector, plane) are identical, the normalized one
 * is ~10% faster for points (1.52 vs 1.67, or vs 1.77 with the unitizing toPoint the projective
 * caller actually needs) and ~5% for motors (one dropped normSquare row). Building: fromNormalized
 * is the cheapest (no Study-number rows), the renormalizing apply pays the sqrt + a new motor on
 * top - build with fromNormalized inside solvers that keep motors normalized.
 *
 * Run with, e.g.:
 * sbt "benchmark/Jmh/run -wi 5 -i 5 -f1 Pga3dTransformBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class Pga3dTransformBenchmark:
  @Param(Array("1000"))
  var size: Int = 0

  private var motor: Pga3dMotor = uninitialized
  private var projective: Pga3dProjectiveTransform = uninitialized
  private var transform: Pga3dTransform = uninitialized

  private var motors: Array[Pga3dMotor] = uninitialized
  private var bivectors: Array[Pga3dBivector] = uninitialized
  private var vectors: Array[Pga3dVector] = uninitialized
  private var points: Array[Pga3dPoint] = uninitialized
  private var planes: Array[Pga3dPlane] = uninitialized

  @Setup(Level.Trial)
  def setup(): Unit = {
    val random = new java.util.Random(42)
    def rnd(): Double = random.nextDouble() * 2.0 - 1.0
    def rndMotor(): Pga3dMotor = Pga3dBivector(rnd(), rnd(), rnd(), rnd(), rnd(), rnd()).exp

    motor = rndMotor()
    projective = Pga3dProjectiveTransform(motor)
    transform = Pga3dTransform.fromNormalized(motor)

    motors = Array.fill(size)(rndMotor())
    bivectors = Array.fill(size)(Pga3dBivector(rnd(), rnd(), rnd(), rnd(), rnd(), rnd()))
    vectors = Array.fill(size)(Pga3dVector(rnd(), rnd(), rnd()))
    points = Array.fill(size)(Pga3dPoint(rnd(), rnd(), rnd()))
    planes = Array.fill(size)(Pga3dPlane(rnd(), rnd(), rnd(), rnd()))
  }

  @Benchmark
  def creationProjective(bh: Blackhole): Unit = {
    val arr = motors
    var i = 0
    while (i < arr.length) {
      bh.consume(Pga3dProjectiveTransform(arr(i)))
      i += 1
    }
  }

  @Benchmark
  def creationRenormalizing(bh: Blackhole): Unit = {
    val arr = motors
    var i = 0
    while (i < arr.length) {
      bh.consume(Pga3dTransform(arr(i)))
      i += 1
    }
  }

  @Benchmark
  def creationFromNormalized(bh: Blackhole): Unit = {
    val arr = motors
    var i = 0
    while (i < arr.length) {
      bh.consume(Pga3dTransform.fromNormalized(arr(i)))
      i += 1
    }
  }

  @Benchmark
  def bivectorViaMotor(bh: Blackhole): Unit = {
    val arr = bivectors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val b = motor.sandwich(arr(i))
      sum += b.wx + b.wy + b.wz + b.xy + b.xz + b.yz
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def bivectorViaProjective(bh: Blackhole): Unit = {
    val arr = bivectors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val b = projective.sandwich(arr(i))
      sum += b.wx + b.wy + b.wz + b.xy + b.xz + b.yz
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def bivectorViaTransform(bh: Blackhole): Unit = {
    val arr = bivectors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val b = transform.sandwich(arr(i))
      sum += b.wx + b.wy + b.wz + b.xy + b.xz + b.yz
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def vectorViaMotor(bh: Blackhole): Unit = {
    val arr = vectors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val v = motor.sandwich(arr(i))
      sum += v.x + v.y + v.z
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def vectorViaProjective(bh: Blackhole): Unit = {
    val arr = vectors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val v = projective.sandwich(arr(i))
      sum += v.x + v.y + v.z
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def vectorViaTransform(bh: Blackhole): Unit = {
    val arr = vectors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val v = transform.sandwich(arr(i))
      sum += v.x + v.y + v.z
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def pointViaMotor(bh: Blackhole): Unit = {
    val arr = points
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val p = motor.sandwich(arr(i))
      sum += p.x + p.y + p.z + p.w
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def pointViaProjective(bh: Blackhole): Unit = {
    val arr = points
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val p = projective.sandwich(arr(i))
      sum += p.x + p.y + p.z + p.w
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def pointViaTransform(bh: Blackhole): Unit = {
    val arr = points
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val p = transform.sandwich(arr(i))
      sum += p.x + p.y + p.z
      i += 1
    }
    bh.consume(sum)
  }

  /** the projective result unitized back to an euclidean point - what user code actually pays
   * when it needs a Pga3dPoint from the projective transform */
  @Benchmark
  def pointViaProjectiveToPoint(bh: Blackhole): Unit = {
    val arr = points
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val p = projective.sandwich(arr(i)).toPoint
      sum += p.x + p.y + p.z
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def planeViaMotor(bh: Blackhole): Unit = {
    val arr = planes
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val p = motor.sandwich(arr(i))
      sum += p.x + p.y + p.z + p.w
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def planeViaProjective(bh: Blackhole): Unit = {
    val arr = planes
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val p = projective.sandwich(arr(i))
      sum += p.x + p.y + p.z + p.w
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def planeViaTransform(bh: Blackhole): Unit = {
    val arr = planes
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val p = transform.sandwich(arr(i))
      sum += p.x + p.y + p.z + p.w
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def motorViaMotor(bh: Blackhole): Unit = {
    val arr = motors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val m = motor.sandwich(arr(i))
      sum += m.s + m.wx + m.wy + m.wz + m.xy + m.xz + m.yz + m.i
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def motorViaProjective(bh: Blackhole): Unit = {
    val arr = motors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val m = projective.sandwich(arr(i))
      sum += m.s + m.wx + m.wy + m.wz + m.xy + m.xz + m.yz + m.i
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def motorViaTransform(bh: Blackhole): Unit = {
    val arr = motors
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val m = transform.sandwich(arr(i))
      sum += m.s + m.wx + m.wy + m.wz + m.xy + m.xz + m.yz + m.i
      i += 1
    }
    bh.consume(sum)
  }
