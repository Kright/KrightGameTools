package me.kright.gametools.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * The cost of the wide polynomial windows of the dexp/dexpInv coefficients (see
 * SharedFormulas.dexpSinMinusCos / dexpK2) against the trigonometric alternatives they
 * replace below c = 0.5. The loop body is the per-element cost over an array of angles;
 * divide the score by 1024 for ns per evaluation.
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class TrigVsPolynomialBenchmark:
  private val count = 1024
  private var lens: Array[Double] = uninitialized

  @Setup
  def setup(): Unit =
    lens = Array.tabulate(count)(i => 0.4999 * (i + 1) / count)

  @Benchmark
  def sinDivLen(bh: Blackhole): Unit =
    var i = 0
    while (i < count) {
      val len = lens(i)
      bh.consume(Math.sin(len) / len)
      i += 1
    }

  @Benchmark
  def sinAndCos(bh: Blackhole): Unit =
    var i = 0
    while (i < count) {
      val len = lens(i)
      bh.consume(Math.sin(len) / len)
      bh.consume(Math.cos(len))
      i += 1
    }

  /** the degree-7 polynomial of sinMinusCosDivLen2 */
  @Benchmark
  def polynomialSinMinusCos(bh: Blackhole): Unit =
    var i = 0
    while (i < count) {
      val len2 = lens(i) * lens(i)
      bh.consume(1.0 / 3.0 - len2 * (1.0 / 30.0 - len2 * (1.0 / 840.0 - len2 * (1.0 / 45360.0 - len2 * (1.0 / 3991680.0 - len2 * (1.0 / 518918400.0 - len2 * (1.0 / 93405312000.0 - len2 / 22230464256000.0)))))))
      i += 1
    }

  /** the degree-8 polynomial of k2 */
  @Benchmark
  def polynomialK2(bh: Blackhole): Unit =
    var i = 0
    while (i < count) {
      val len2 = lens(i) * lens(i)
      bh.consume(2.0 / 3.0 - len2 * (2.0 / 15.0 - len2 * (4.0 / 315.0 - len2 * (2.0 / 2835.0 - len2 * (4.0 / 155925.0 - len2 * (4.0 / 6081075.0 - len2 * (8.0 / 638512875.0 - len2 * (2.0 / 10854718875.0 - len2 * (4.0 / 1856156927625.0)))))))))
      i += 1
    }

  /** the same degree-7 sinMinusCosDivLen2 polynomial through Math.fma (vfnmadd on x86) */
  @Benchmark
  def polynomialSinMinusCosFma(bh: Blackhole): Unit =
    var i = 0
    while (i < count) {
      val len2 = lens(i) * lens(i)
      bh.consume(Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, 1.0 / 22230464256000.0, 1.0 / 93405312000.0), 1.0 / 518918400.0), 1.0 / 3991680.0), 1.0 / 45360.0), 1.0 / 840.0), 1.0 / 30.0), 1.0 / 3.0))
      i += 1
    }

  /** the same degree-8 k2 polynomial through Math.fma (vfnmadd on x86) */
  @Benchmark
  def polynomialK2Fma(bh: Blackhole): Unit =
    var i = 0
    while (i < count) {
      val len2 = lens(i) * lens(i)
      bh.consume(Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, Math.fma(-len2, 4.0 / 1856156927625.0, 2.0 / 10854718875.0), 8.0 / 638512875.0), 4.0 / 6081075.0), 4.0 / 155925.0), 2.0 / 2835.0), 4.0 / 315.0), 2.0 / 15.0), 2.0 / 3.0))
      i += 1
    }
