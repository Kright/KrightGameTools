package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.physics.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * apply, invert and getAcceleration across every inertia representation, warm caches (the lazy
 * inverse blocks of the summable are built in setup). Each representation runs on its own
 * natural instance - the code path cost does not depend on the values. One invocation
 * processes `size` random bivectors; divide the score by `size` for the per-call cost.
 *
 * Results on ryzen 5950 (size = 1000, so the score is ns per call):
 * [info] InertiaBenchmark.localApply                 1000  avgt    3   1.153 ± 0.144  us/op
 * [info] InertiaBenchmark.localInvert                1000  avgt    3   1.145 ± 0.029  us/op
 * [info] InertiaBenchmark.localAcceleration          1000  avgt    3   4.063 ± 8.581  us/op
 * [info] InertiaBenchmark.simpleApply                1000  avgt    3   1.394 ± 5.461  us/op
 * [info] InertiaBenchmark.simpleInvert               1000  avgt    3   1.422 ± 2.369  us/op
 * [info] InertiaBenchmark.simpleAcceleration         1000  avgt    3   2.488 ± 0.360  us/op
 * [info] InertiaBenchmark.movedSimpleApply           1000  avgt    3   4.130 ± 0.081  us/op
 * [info] InertiaBenchmark.movedSimpleInvert          1000  avgt    3   4.183 ± 0.444  us/op
 * [info] InertiaBenchmark.movedSimpleAcceleration    1000  avgt    3   6.345 ± 0.169  us/op
 * [info] InertiaBenchmark.summableApply              1000  avgt    3   4.901 ± 6.270  us/op
 * [info] InertiaBenchmark.summableInvert             1000  avgt    3   5.027 ± 0.495  us/op
 * [info] InertiaBenchmark.summableAcceleration       1000  avgt    3  14.670 ± 6.401  us/op
 * [info] InertiaBenchmark.movedLocalApply            1000  avgt    3  10.278 ± 1.599  us/op
 * [info] InertiaBenchmark.movedLocalInvert           1000  avgt    3  10.078 ± 0.354  us/op
 * [info] InertiaBenchmark.movedLocalAcceleration     1000  avgt    3  16.716 ± 0.525  us/op
 *
 * The stable findings: the diagonal forms (local, simple) are the cheapest as expected;
 * the summable is the fastest general (rotated + translated) representation on every method -
 * 2x the moved-local route; the translator-only movedSimple sits in between. The acceleration
 * costs apply + cross + invert on top of the trait default for everyone but simple, which has
 * a closed form. The removed Pga3dInertiaPrecomputed measured 14.9 ns for both apply and
 * invert (6x6 matrices) - slower than every representation here, which is why it was removed.
 *
 * Run with: sbt "benchmark/Jmh/run -wi 3 -i 3 -f1 InertiaBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class InertiaBenchmark:
  @Param(Array("1000"))
  var size: Int = 0

  private var simple: Pga3dInertiaSimple = uninitialized
  private var local: Pga3dInertiaLocal = uninitialized
  private var movedSimple: Pga3dInertiaMovedSimple = uninitialized
  private var movedLocal: Pga3dInertiaMovedLocal = uninitialized
  private var summable: Pga3dInertiaSummable = uninitialized
  private var velocities: Array[Pga3dBivector] = uninitialized
  private var forques: Array[Pga3dBivector] = uninitialized

  @Setup(Level.Trial)
  def setup(): Unit = {
    val random = new java.util.Random(42)
    def rnd(): Double = random.nextDouble() * 2.0 - 1.0

    simple = Pga3dInertiaSimple(2.0, 0.7)
    local = Pga3dInertiaLocal(2.0, 0.5, 0.7, 0.9)
    movedSimple = Pga3dInertia.movedSimple(
      Pga3dTranslator.addVector(Pga3dVector(rnd(), rnd(), rnd())), simple)
    movedLocal = Pga3dInertiaMovedLocal(
      Pga3dTranslator.addVector(Pga3dVector(rnd(), rnd(), rnd())).toMotor
        .geometric(Pga3dBivector(xy = rnd(), xz = rnd(), yz = rnd()).exp),
      local)
    summable = movedLocal.toSummable
    // warm the lazy inverse blocks so the benchmarks measure the steady state
    summable.inverse

    velocities = Array.fill(size)(Pga3dBivector(rnd(), rnd(), rnd(), rnd(), rnd(), rnd()))
    forques = Array.fill(size)(Pga3dBivector(rnd(), rnd(), rnd(), rnd(), rnd(), rnd()))
  }

  private inline def loop(inline f: Pga3dBivector => Pga3dBivector, bh: Blackhole): Unit = {
    val arr = velocities
    var sum = 0.0
    var i = 0
    while (i < arr.length) {
      val r = f(arr(i))
      sum += r.wx + r.wy + r.wz + r.xy + r.xz + r.yz
      i += 1
    }
    bh.consume(sum)
  }

  private inline def loopAccel(inertia: Pga3dInertia, bh: Blackhole): Unit = {
    val vs = velocities
    val fs = forques
    var sum = 0.0
    var i = 0
    while (i < vs.length) {
      val r = inertia.getAcceleration(vs(i), fs(i))
      sum += r.wx + r.wy + r.wz + r.xy + r.xz + r.yz
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def simpleApply(bh: Blackhole): Unit = loop(simple(_), bh)

  @Benchmark
  def simpleInvert(bh: Blackhole): Unit = loop(simple.invert(_), bh)

  @Benchmark
  def simpleAcceleration(bh: Blackhole): Unit = loopAccel(simple, bh)

  @Benchmark
  def localApply(bh: Blackhole): Unit = loop(local(_), bh)

  @Benchmark
  def localInvert(bh: Blackhole): Unit = loop(local.invert(_), bh)

  @Benchmark
  def localAcceleration(bh: Blackhole): Unit = loopAccel(local, bh)

  @Benchmark
  def movedSimpleApply(bh: Blackhole): Unit = loop(movedSimple(_), bh)

  @Benchmark
  def movedSimpleInvert(bh: Blackhole): Unit = loop(movedSimple.invert(_), bh)

  @Benchmark
  def movedSimpleAcceleration(bh: Blackhole): Unit = loopAccel(movedSimple, bh)

  @Benchmark
  def movedLocalApply(bh: Blackhole): Unit = loop(movedLocal(_), bh)

  @Benchmark
  def movedLocalInvert(bh: Blackhole): Unit = loop(movedLocal.invert(_), bh)

  @Benchmark
  def movedLocalAcceleration(bh: Blackhole): Unit = loopAccel(movedLocal, bh)

  @Benchmark
  def summableApply(bh: Blackhole): Unit = loop(summable(_), bh)

  @Benchmark
  def summableInvert(bh: Blackhole): Unit = loop(summable.invert(_), bh)

  @Benchmark
  def summableAcceleration(bh: Blackhole): Unit = loopAccel(summable, bh)
