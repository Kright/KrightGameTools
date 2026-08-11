package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * motor.sandwich(x) vs. the cached Pga3dTransform.sandwich(x) for the common argument classes,
 * plus the cost of building the transform from a motor. One benchmark invocation applies the same
 * transformation to `size` random arguments and accumulates all the result components, so nothing
 * gets dead-code-eliminated; divide the score by `size` for the per-application cost.
 *
 * The transform pays one conversion per motor (transformCreation) and then applies a precomputed
 * matrix, while motor.sandwich recomputes the coefficient products on every call - so the transform
 * wins when the same motor is applied more than a few times.
 *
 * Results on ryzen 5950 (size = 1000, so the score is ns per one application):
 * [info] Benchmark                                     (size)  Mode  Cnt   Score   Error  Units
 * [info] Pga3dTransformBenchmark.bivectorViaMotor        1000  avgt    5  17.821 ± 0.214  us/op
 * [info] Pga3dTransformBenchmark.bivectorViaTransform    1000  avgt    5   6.238 ± 0.104  us/op
 * [info] Pga3dTransformBenchmark.motorViaMotor           1000  avgt    5  20.662 ± 0.748  us/op
 * [info] Pga3dTransformBenchmark.motorViaTransform       1000  avgt    5   5.992 ± 0.124  us/op
 * [info] Pga3dTransformBenchmark.planeViaMotor           1000  avgt    5   9.676 ± 0.112  us/op
 * [info] Pga3dTransformBenchmark.planeViaTransform       1000  avgt    5   1.905 ± 0.040  us/op
 * [info] Pga3dTransformBenchmark.pointViaMotor           1000  avgt    5   9.379 ± 0.152  us/op
 * [info] Pga3dTransformBenchmark.pointViaTransform       1000  avgt    5   1.767 ± 0.032  us/op
 * [info] Pga3dTransformBenchmark.transformCreation       1000  avgt    5  23.239 ± 0.967  us/op
 * [info] Pga3dTransformBenchmark.vectorViaMotor          1000  avgt    5   1.615 ± 0.005  us/op
 * [info] Pga3dTransformBenchmark.vectorViaTransform      1000  avgt    5   1.854 ± 0.010  us/op
 *
 * The transform is ~3x faster for bivectors and motors and ~5x for points and planes; building it
 * costs about one motor-bivector sandwich, so it breaks even from the second application. The one
 * exception is the vector: motor.sandwich(vector) uses so few coefficient products that the JIT
 * hoists them out of this benchmark's loop (the motor is loop-invariant), matching the cached
 * transform; outside such a tight loop the transform is not worse.
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
    transform = Pga3dTransform(motor)

    motors = Array.fill(size)(rndMotor())
    bivectors = Array.fill(size)(Pga3dBivector(rnd(), rnd(), rnd(), rnd(), rnd(), rnd()))
    vectors = Array.fill(size)(Pga3dVector(rnd(), rnd(), rnd()))
    points = Array.fill(size)(Pga3dPoint(rnd(), rnd(), rnd()))
    planes = Array.fill(size)(Pga3dPlane(rnd(), rnd(), rnd(), rnd()))
  }

  @Benchmark
  def transformCreation(bh: Blackhole): Unit = {
    val arr = motors
    var i = 0
    while (i < arr.length) {
      bh.consume(Pga3dTransform(arr(i)))
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
  def pointViaTransform(bh: Blackhole): Unit = {
    val arr = points
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
