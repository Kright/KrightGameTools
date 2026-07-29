package me.kright.gametools.benchmark

import me.kright.gametools.flatarray.*
import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.geom.Pga3dTriangle
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Nested-struct variant of [[Pga3dVectorRotorBenchmark]]: the element is a `Pga3dTriangle`
 * (3 `Pga3dPoint` = 9 doubles). In `FlatArray` that is 72 B packed inline; boxed it is a
 * triangle object holding 3 references — every field access is a double indirection, and each
 * write-back allocates 4 objects (3 points + the triangle) instead of 1.
 *
 * Three layouts:
 *  1. `FlatArray[Pga3dTriangle]` — flattened doubles, `mapInPlace` mutates in place.
 *  2. `Array[Pga3dTriangle]` "own points": each triangle owns its 3 points, allocated together
 *     with it, so heap order matches iteration order.
 *  3. `Array[Pga3dTriangle]` "mesh": a shared pool of `size / 2` vertices, each triangle
 *     references 3 random vertices from the pool — the realistic mesh layout, where point
 *     reads jump around the pool.
 *
 * Same master-recopy protocol as [[FlatArrayRotorBenchmark]] (see the comment there); the same
 * small-array caveat as in [[Pga3dVectorRotorBenchmark]] applies — treat differences under ~10%
 * as noise.
 * 
 * Results on ryzen 5950
 * [info] Benchmark                                                 (size)  Mode  Cnt    Score    Error  Units
 * [info] Pga3dTriangleRotorBenchmark.arrayMeshReadAccumulate        10000  avgt    5   28.457 ±  0.204  us/op
 * [info] Pga3dTriangleRotorBenchmark.arrayMeshWrite                 10000  avgt    5  203.776 ± 11.075  us/op
 * [info] Pga3dTriangleRotorBenchmark.arrayOwnPointsReadAccumulate   10000  avgt    5   27.459 ±  0.847  us/op
 * [info] Pga3dTriangleRotorBenchmark.arrayOwnPointsWrite            10000  avgt    5  186.989 ± 50.977  us/op
 * [info] Pga3dTriangleRotorBenchmark.flatReadAccumulate             10000  avgt    5  401.836 ± 36.197  us/op
 * [info] Pga3dTriangleRotorBenchmark.flatReadAccumulateV2           10000  avgt    5   16.897 ±  0.467  us/op
 * [info] Pga3dTriangleRotorBenchmark.flatWrite                      10000  avgt    5   39.775 ±  1.606  us/op
 * 
 * Run with, e.g.:
 * sbt "benchmark/Jmh/run -f1 Pga3dTriangleRotor.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class Pga3dTriangleRotorBenchmark:
  @Param(Array("10000"))
  var size: Int = 0

  // a small rotation around the z axis, used to sandwich every triangle vertex
  private val angle = 0.01
  private val rotor = Pga3dRotor(s = math.cos(angle / 2.0), xy = math.sin(angle / 2.0), xz = 0.0, yz = 0.0)

  private inline def rotate(t: Pga3dTriangle): Pga3dTriangle =
    Pga3dTriangle(
      rotor.sandwich(t.a).toPointUnsafe,
      rotor.sandwich(t.b).toPointUnsafe,
      rotor.sandwich(t.c).toPointUnsafe,
    )

  var flatArray: FlatArray[Pga3dTriangle] = uninitialized

  // immutable masters: read-only references defining the layout
  var ownPointsMaster: Array[Pga3dTriangle] = uninitialized
  var meshMaster: Array[Pga3dTriangle] = uninitialized

  // working copies that the benchmarks mutate; reset from the masters before every invocation
  var ownPointsArray: Array[Pga3dTriangle] = uninitialized
  var meshArray: Array[Pga3dTriangle] = uninitialized

  @Setup(Level.Trial)
  def setupTrial(): Unit = {
    val random = new java.util.Random(42)

    def randomPoint(): Pga3dPoint =
      Pga3dPoint(random.nextDouble(), random.nextDouble(), random.nextDouble())

    flatArray = FlatArray[Pga3dTriangle](size)
    for (i <- 0 until size) {
      flatArray(i) = Pga3dTriangle(randomPoint(), randomPoint(), randomPoint())
    }

    // each triangle's points are allocated right before it: heap order matches iteration order
    ownPointsMaster = new Array[Pga3dTriangle](size)
    for (i <- 0 until size) {
      ownPointsMaster(i) = Pga3dTriangle(randomPoint(), randomPoint(), randomPoint())
    }

    // shared vertex pool, each triangle picks 3 random vertices from it (fixed seed):
    // triangle objects are sequential, but their point reads jump around the pool
    val vertexPool = Array.fill(size / 2)(randomPoint())
    meshMaster = new Array[Pga3dTriangle](size)
    for (i <- 0 until size) {
      meshMaster(i) = Pga3dTriangle(
        vertexPool(random.nextInt(vertexPool.length)),
        vertexPool(random.nextInt(vertexPool.length)),
        vertexPool(random.nextInt(vertexPool.length)),
      )
    }

    ownPointsArray = new Array[Pga3dTriangle](size)
    meshArray = new Array[Pga3dTriangle](size)
  }

  @Setup(Level.Invocation)
  def setupInvocation(): Unit = {
    System.arraycopy(ownPointsMaster, 0, ownPointsArray, 0, size)
    System.arraycopy(meshMaster, 0, meshArray, 0, size)
  }

  @Benchmark
  def flatWrite(): FlatArray[Pga3dTriangle] = {
    flatArray.mapInPlace(rotate)
    flatArray
  }

  @Benchmark
  def arrayOwnPointsWrite(bh: Blackhole): Unit = {
    val work = ownPointsArray
    var i = 0
    while (i < work.length) {
      work(i) = rotate(work(i))
      i += 1
    }
    bh.consume(work)
  }
  
  @Benchmark
  def arrayMeshWrite(bh: Blackhole): Unit = {
    val work = meshArray
    var i = 0
    while (i < work.length) {
      work(i) = rotate(work(i))
      i += 1
    }
    bh.consume(work)
  }
  
  @Benchmark
  def flatReadAccumulate(bh: Blackhole): Unit = {
    var sum = 0.0
    // looks like standard loop with lambda prevents escape analysis.
    for (i <- 0 until flatArray.size) {
      val t = rotate(flatArray(i))
      sum += t.a.x + t.b.y + t.c.z
    }
    bh.consume(sum)
  }

  @Benchmark
  def flatReadAccumulateV2(bh: Blackhole): Unit = {
    var sum = 0.0
    flatArray.foreach { triangle =>
      val t = rotate(triangle)
      sum += t.a.x + t.b.y + t.c.z
    }
    bh.consume(sum)
  }

  @Benchmark
  def arrayOwnPointsReadAccumulate(bh: Blackhole): Unit = {
    val master = ownPointsMaster
    var sum = 0.0
    master.foreach { triangle =>
      val t = rotate(triangle)
      sum += t.a.x + t.b.y + t.c.z
    }
    bh.consume(sum)
  }

  @Benchmark
  def arrayMeshReadAccumulate(bh: Blackhole): Unit = {
    val master = meshMaster
    var sum = 0.0
    master.foreach { triangle =>
      val t = rotate(triangle)
      sum += t.a.x + t.b.y + t.c.z
    }
    bh.consume(sum)
  }
