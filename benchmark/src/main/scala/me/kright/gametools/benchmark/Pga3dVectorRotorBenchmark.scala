package me.kright.gametools.benchmark

import me.kright.gametools.flatarray.*
import me.kright.gametools.pga3d.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * The small-array (cache-resident) sibling of [[FlatArrayRotorBenchmark]]: applying a unit
 * `Pga3dRotor` sandwich to every element of a `FlatArray[Pga3dVector]` vs. a boxed
 * `Array[Pga3dVector]`. At the default 10k elements even the boxed layout fits in L2, so this
 * measures layout/allocation cost without the GC pressure that dominates at 500k+.
 *
 * Same master-recopy protocol as [[FlatArrayRotorBenchmark]] (see the comment there). A pass over
 * 10k elements is tens of microseconds, so the per-invocation arraycopy (~80 KB of references) is
 * a larger fraction of a benchmark cycle than at 500k+ — it stays outside the timed region, but
 * treat differences under ~10% as noise.
 *
 * Results on ryzen 5950
 * [info] Benchmark                                          (size)  Mode  Cnt   Score    Error  Units
 * [info] Pga3dVectorRotorBenchmark.arraySeqReadAccumulate    10000  avgt    5  16.345 ±  0.145  us/op
 * [info] Pga3dVectorRotorBenchmark.arraySeqWrite             10000  avgt    5  71.896 ± 15.176  us/op
 * [info] Pga3dVectorRotorBenchmark.arrayShufReadAccumulate   10000  avgt    5  28.489 ±  0.747  us/op
 * [info] Pga3dVectorRotorBenchmark.arrayShufWrite            10000  avgt    5  70.935 ± 15.697  us/op
 * [info] Pga3dVectorRotorBenchmark.flatReadAccumulate        10000  avgt    5  17.462 ±  0.411  us/op
 * [info] Pga3dVectorRotorBenchmark.flatWrite                 10000  avgt    5  11.985 ±  0.332  us/op
 *
 * Run with, e.g.:
 * sbt "benchmark/Jmh/run -wi 5 -i 5 -f1 Pga3dVectorRotor.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class Pga3dVectorRotorBenchmark:
  @Param(Array("10000"))
  var size: Int = 0

  // a small rotation around the z axis, used to sandwich every vector
  private val angle = 0.01
  private val rotor = Pga3dRotor(s = math.cos(angle / 2.0), xy = math.sin(angle / 2.0), xz = 0.0, yz = 0.0)

  var flatArray: FlatArray[Pga3dVector] = uninitialized

  // immutable masters: read-only references defining the layout (sequential vs. shuffled heap order)
  var sequentialMaster: Array[Pga3dVector] = uninitialized
  var shuffledMaster: Array[Pga3dVector] = uninitialized

  // working copies that the benchmarks mutate; reset from the masters before every invocation
  var sequentialArray: Array[Pga3dVector] = uninitialized
  var shuffledArray: Array[Pga3dVector] = uninitialized

  @Setup(Level.Trial)
  def setupTrial(): Unit = {
    val random = new java.util.Random(42)

    flatArray = FlatArray[Pga3dVector](size)
    for (i <- 0 until size) {
      flatArray(i) = Pga3dVector(random.nextDouble(), random.nextDouble(), random.nextDouble())
    }

    sequentialMaster = new Array[Pga3dVector](size)
    for (i <- 0 until size) {
      sequentialMaster(i) = Pga3dVector(random.nextDouble(), random.nextDouble(), random.nextDouble())
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

    sequentialArray = new Array[Pga3dVector](size)
    shuffledArray = new Array[Pga3dVector](size)
  }

  @Setup(Level.Invocation)
  def setupInvocation(): Unit = {
    System.arraycopy(sequentialMaster, 0, sequentialArray, 0, size)
    System.arraycopy(shuffledMaster, 0, shuffledArray, 0, size)
  }

  @Benchmark
  def flatWrite(): FlatArray[Pga3dVector] = {
    flatArray.mapInPlace(v => rotor.sandwich(v))
    flatArray
  }

  @Benchmark
  def arraySeqWrite(bh: Blackhole): Unit = {
    val work = sequentialArray
    var i = 0
    while (i < work.length) {
      work(i) = rotor.sandwich(work(i))
      i += 1
    }
    bh.consume(work)
  }

  @Benchmark
  def arrayShufWrite(bh: Blackhole): Unit = {
    val work = shuffledArray
    var i = 0
    while (i < work.length) {
      work(i) = rotor.sandwich(work(i))
      i += 1
    }
    bh.consume(work)
  }

  @Benchmark
  def flatReadAccumulate(bh: Blackhole): Unit = {
    var sum = 0.0
    flatArray.foreach { vec =>
      val v = rotor.sandwich(vec)
      sum += v.x + v.y + v.z
    }
    bh.consume(sum)
  }

  @Benchmark
  def arraySeqReadAccumulate(bh: Blackhole): Unit = {
    val master = sequentialMaster
    var sum = 0.0
    var i = 0
    while (i < master.length) {
      val v = rotor.sandwich(master(i))
      sum += v.x + v.y + v.z
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
      val v = rotor.sandwich(master(i))
      sum += v.x + v.y + v.z
      i += 1
    }
    bh.consume(sum)
  }
