package me.kright.gametools.benchmark

import me.kright.gametools.physics1d.{Integrable, IntegrableUniformArray, OdeSolverCache, OdeSolverRK4}
import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.physics.*
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Run with: sbt "benchmark/Jmh/run -f3 .*BodiesArrayDispatchBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class BodiesArrayDispatchBenchmark:
  @Param(Array("2", "10", "100"))
  var bodiesCount: Int = 0

  private val dt = 0.001

  private def makeBodies(): Array[Pga3dPhysicsBody] =
    Array.tabulate(bodiesCount) { i =>
      val motor = Pga3dTranslator.addVector(Pga3dVector(i, 2.0 * i, -i)).toMotor
      Pga3dPhysicsBody(
        Pga3dInertia.moved(motor.reverse, Pga3dInertiaLocal(1.0, 3.0, 2.0, 1.0)),
        motor,
        motor.reverseSandwich(Pga3dBivector(xy = 1.0, yz = 1.0, xz = -1.0)),
      )
    }

  private def threeOtherKinds(): Array[Integrable] = Array(
    BenchmarkSharedLoopsUniformArray[BenchmarkOscillatorA](Array.fill(2)(BenchmarkOscillatorA(1.0, 0.0)), 2),
    BenchmarkSharedLoopsUniformArray[BenchmarkOscillatorB](Array.fill(2)(BenchmarkOscillatorB(1.0, 0.0)), 2),
    BenchmarkSharedLoopsUniformArray[BenchmarkOscillatorC](Array.fill(2)(BenchmarkOscillatorC(1.0, 0.0)), 2),
  )

  var bodiesArray: Integrable = uninitialized
  var uniformArrayWithSharedLoops: Integrable = uninitialized
  var anyArray: Integrable = uninitialized
  var bodiesArrayWithOthers: Integrable = uninitialized
  var uniformArrayWithSharedLoopsAndOthers: Integrable = uninitialized
  var anyArrayWithOthers: Integrable = uninitialized
  var cache: OdeSolverCache = uninitialized

  @Setup
  def setup(): Unit =
    bodiesArray = IntegrableUniformArray(makeBodies(), Pga3dBodyState.componentsCount)
    uniformArrayWithSharedLoops = BenchmarkSharedLoopsUniformArray[Pga3dPhysicsBody](makeBodies(), Pga3dBodyState.componentsCount)
    anyArray = Integrable(makeBodies())
    bodiesArrayWithOthers = Integrable((IntegrableUniformArray(makeBodies(), Pga3dBodyState.componentsCount) +: threeOtherKinds())*)
    uniformArrayWithSharedLoopsAndOthers = Integrable((BenchmarkSharedLoopsUniformArray[Pga3dPhysicsBody](makeBodies(), Pga3dBodyState.componentsCount) +: threeOtherKinds())*)
    anyArrayWithOthers = Integrable((Integrable(makeBodies()) +: threeOtherKinds())*)
    cache = OdeSolverCache()

  @Benchmark
  def stepBodiesArray(): Unit = OdeSolverRK4.step(bodiesArray, dt, _ => (), cache)

  @Benchmark
  def stepUniformArrayWithSharedLoops(): Unit = OdeSolverRK4.step(uniformArrayWithSharedLoops, dt, _ => (), cache)

  @Benchmark
  def stepAnyArray(): Unit = OdeSolverRK4.step(anyArray, dt, _ => (), cache)

  @Benchmark
  def stepBodiesArrayWithThreeOtherKinds(): Unit = OdeSolverRK4.step(bodiesArrayWithOthers, dt, _ => (), cache)

  @Benchmark
  def stepUniformArrayWithSharedLoopsWithThreeOtherKinds(): Unit = OdeSolverRK4.step(uniformArrayWithSharedLoopsAndOthers, dt, _ => (), cache)

  @Benchmark
  def stepAnyArrayWithThreeOtherKinds(): Unit = OdeSolverRK4.step(anyArrayWithOthers, dt, _ => (), cache)
