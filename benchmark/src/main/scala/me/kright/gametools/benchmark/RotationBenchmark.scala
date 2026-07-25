package me.kright.gametools.benchmark

import me.kright.gametools.pga3d.*
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * Cost of the two Pga3dRotor.rotation branches: the half-angle main branch (dot > -0.9)
 * and the exact-wedge branch around pi (dot <= -0.9, diffOfProducts + asin).
 *
 * Run with: sbt "benchmark/Jmh/run -wi 5 -i 5 -f1 .*RotationBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class RotationBenchmark:
  var from: Pga3dVector = Pga3dVector(3.0 / 13, 4.0 / 13, 12.0 / 13)
  var toMain: Pga3dVector = Pga3dVector(-4.0 / 13, 3.0 / 13, 12.0 / 13) // dot = 0.85
  var toNearPi: Pga3dVector = uninitialized // dot ~ -0.995

  @Setup
  def setup(): Unit =
    val perp = Pga3dVector(from.y, -from.x, 0.0).normalizedByNorm
    toNearPi = -(from * Math.cos(0.1) + perp * Math.sin(0.1))

  @Benchmark
  def mainBranch: Pga3dRotor = Pga3dRotor.rotation(from, toMain)

  @Benchmark
  def exactWedgeBranch: Pga3dRotor = Pga3dRotor.rotation(from, toNearPi)
