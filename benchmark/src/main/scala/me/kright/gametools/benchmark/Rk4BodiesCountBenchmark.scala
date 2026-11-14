package me.kright.gametools.benchmark

import me.kright.gametools.physics1d.{Integrable, IntegrableUniformArray, OdeSolverCache, OdeSolverRK4}
import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.physics.*
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Run with: sbt "benchmark/Jmh/run -f1 .*Rk4BodiesCountBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class Rk4BodiesCountBenchmark:
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

  var bodiesArrayWithoutCache: IntegrableUniformArray[Pga3dPhysicsBody] = uninitialized
  var bodiesArray: IntegrableUniformArray[Pga3dPhysicsBody] = uninitialized
  var bodiesArrayCache: OdeSolverCache = uninitialized
  var world: Integrable = uninitialized
  var worldCache: OdeSolverCache = uninitialized

  @Setup
  def setup(): Unit =
    bodiesArrayWithoutCache = IntegrableUniformArray(makeBodies(), Pga3dBodyState.componentsCount)
    bodiesArray = IntegrableUniformArray(makeBodies(), Pga3dBodyState.componentsCount)
    bodiesArrayCache = OdeSolverCache()
    world = Integrable(IntegrableUniformArray(makeBodies(), Pga3dBodyState.componentsCount), BenchmarkOscillatorA(1.0, 0.0))
    worldCache = OdeSolverCache()

  @Benchmark
  def universalSolverOverBodiesArrayWithoutCache(): Unit =
    OdeSolverRK4.step(bodiesArrayWithoutCache, dt, _ => ())

  @Benchmark
  def universalSolverOverBodiesArray(): Unit =
    OdeSolverRK4.step(bodiesArray, dt, _ => (), bodiesArrayCache)

  @Benchmark
  def universalSolverOverBodiesArrayWithOneMoreObject(): Unit =
    OdeSolverRK4.step(world, dt, _ => (), worldCache)
