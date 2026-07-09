package me.kright.gametools.benchmark

import me.kright.gametools.flatarray.*
import me.kright.gametools.flatarray.FlatMutableView.*
import me.kright.gametools.pga3d.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Reproduces the write-back comparison from `flatarray/performance.md`: applying a unit `Pga3dRotor`
 * sandwich to every element of a `FlatArray[Pga3dPoint]` (mutates doubles in place, zero allocation)
 * vs. a boxed `Array[Pga3dPoint]` in sequential (allocation order matches iteration order) and
 * shuffled (references permuted, scattered heap access) layouts.
 *
 * Run with, e.g.:
 * sbt "benchmark/Jmh/run -wi 5 -i 5 -f1 -p size=500000 .*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class FlatArrayRotorBenchmark:
  @Param(Array("500000", "2000000"))
  var size: Int = 0

  // a small rotation around the z axis, used to sandwich every point
  private val angle = 0.01
  private val rotor = Pga3dRotor(s = math.cos(angle / 2.0), xy = math.sin(angle / 2.0), xz = 0.0, yz = 0.0)

  var flatArray: FlatArray[Pga3dPoint] = uninitialized

  // immutable masters: read-only references defining the layout (sequential vs. shuffled heap order)
  var sequentialMaster: Array[Pga3dPoint] = uninitialized
  var shuffledMaster: Array[Pga3dPoint] = uninitialized

  // working copies that the benchmarks mutate; reset from the masters before every invocation
  var sequentiaArray: Array[Pga3dPoint] = uninitialized
  var shuffledArray: Array[Pga3dPoint] = uninitialized

  @Setup(Level.Trial)
  def setupTrial(): Unit = {
    val random = new java.util.Random(42)

    flatArray = FlatArray[Pga3dPoint](size)
    for (i <- 0 until size) {
      flatArray(i) = Pga3dPoint(random.nextDouble(), random.nextDouble(), random.nextDouble())
    }

    sequentialMaster = new Array[Pga3dPoint](size)
    for (i <- 0 until size) {
      sequentialMaster(i) = Pga3dPoint(random.nextDouble(), random.nextDouble(), random.nextDouble())
    }

    // same objects as sequentialMaster, references permuted with a fixed seed so iteration
    // hits random heap addresses (the objects were allocated in sequentialMaster's iteration order)
    shuffledMaster = sequentialMaster.clone()
    val shuffleRandom = new java.util.Random(1234)
    var i = shuffledMaster.length - 1
    while (i >= 1) {
      val j = shuffleRandom.nextInt(i + 1)
      val tmp = shuffledMaster(i)
      shuffledMaster(i) = shuffledMaster(j)
      shuffledMaster(j) = tmp
      i -= 1
    }

    sequentiaArray = new Array[Pga3dPoint](size)
    shuffledArray = new Array[Pga3dPoint](size)
  }

  // The master-recopy protocol: `arr(i) = f(arr(i))` allocates a fresh immutable Pga3dPoint per element
  // (an inherent, honest cost of the boxed write-back, not something to optimize away). After one pass
  // over a working array, it would hold freshly TLAB-allocated objects in iteration order -- the
  // shuffled layout would silently become sequential starting from the second invocation. To keep both
  // layouts honest across invocations, the working arrays are reset from the immutable masters via
  // System.arraycopy before every invocation: reads then always hit the master's layout, and writes
  // allocate throwaway objects discarded on the next reset. `FlatArray` needs no such reset -- it
  // mutates doubles in place and allocates nothing.
  //
  // JMH warns against @Setup(Level.Invocation) for very short benchmarks (the setup can dwarf the
  // measured work), but a single pass over 500k+ elements is millisecond-scale, so two
  // System.arraycopy calls per invocation are negligible overhead here.
  @Setup(Level.Invocation)
  def setupInvocation(): Unit = {
    System.arraycopy(sequentialMaster, 0, sequentiaArray, 0, size)
    System.arraycopy(shuffledMaster, 0, shuffledArray, 0, size)
  }

  @Benchmark
  def flatWrite(): FlatArray[Pga3dPoint] = {
    flatArray.mapInPlace(p => rotor.sandwich(p).toPointUnsafe)
    flatArray
  }

  @Benchmark
  def arraySeqWrite(bh: Blackhole): Unit = {
    val work = sequentiaArray
    var i = 0
    while (i < work.length) {
      work(i) = rotor.sandwich(work(i)).toPointUnsafe
      i += 1
    }
    bh.consume(work)
  }

  @Benchmark
  def arrayShufWrite(bh: Blackhole): Unit = {
    val work = shuffledArray
    var i = 0
    while (i < work.length) {
      work(i) = rotor.sandwich(work(i)).toPointUnsafe
      i += 1
    }
    bh.consume(work)
  }

  @Benchmark
  def flatReadAccumulate(bh: Blackhole): Unit = {
    var sum = 0.0
    for (i <- 0 until flatArray.size) {
      val p = rotor.sandwich(flatArray(i)).toPointUnsafe
      sum += p.x + p.y + p.z
    }
    bh.consume(sum)
  }

  @Benchmark
  def arraySeqReadAccumulate(bh: Blackhole): Unit = {
    val master = sequentialMaster
    var sum = 0.0
    var i = 0
    while (i < master.length) {
      val p = rotor.sandwich(master(i)).toPointUnsafe
      sum += p.x + p.y + p.z
      i += 1
    }
    bh.consume(sum)
  }

  @Benchmark
  def arrayShufReadAccumulate(bh: Blackhole): Unit = {
    val master = shuffledMaster
    var sum = 0.0
    var i = 0
    while (i < master.length) {
      val p = rotor.sandwich(master(i)).toPointUnsafe
      sum += p.x + p.y + p.z
      i += 1
    }
    bh.consume(sum)
  }
