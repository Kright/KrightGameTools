package me.kright.gametools.flatarray

import FlatView.*
import FlatMutableView.*
import org.scalatest.funsuite.AnyFunSuiteLike

import java.lang.management.ManagementFactory

/**
 * The core performance claim of this module: an `inline def` hot-path method that calls the
 * `FlatDoubleSerializer` macros directly (never the virtual typeclass) lets the JVM scalar-replace the
 * temporary `T` instances it constructs, so a hot loop over `FlatArray`s does not allocate.
 *
 * This is a JVM-only, environment-sensitive micro-benchmark-ish test (JIT warmup, GC, thread-allocation
 * counters), so the threshold is deliberately generous for CI stability; it is skipped gracefully if the
 * JVM does not expose `getThreadAllocatedBytes`.
 */
final case class Vec3dAlloc(x: Double, y: Double, z: Double) derives FlatDoubleSerializer, CanEqual

class FlatArrayAllocationTest extends AnyFunSuiteLike:
  test("zipTo hot loop over FlatArray[Vec3dAlloc] allocates ~0 bytes per element once warmed up") {
    val threadMXBean = ManagementFactory.getThreadMXBean
    val sunBeanOpt = threadMXBean match {
      case b: com.sun.management.ThreadMXBean => Some(b)
      case _ => None
    }
    if (sunBeanOpt.isEmpty || !sunBeanOpt.get.isThreadAllocatedMemorySupported) {
      cancel("com.sun.management.ThreadMXBean / getThreadAllocatedBytes is not supported on this JVM")
    }
    val sunBean = sunBeanOpt.get

    val n = 1000
    val a = FlatArray[Vec3dAlloc](n)
    val b = FlatArray[Vec3dAlloc](n)
    val dst = FlatArray[Vec3dAlloc](n)
    for (i <- 0 until n) {
      a(i) = Vec3dAlloc(i.toDouble, i.toDouble + 1, i.toDouble + 2)
      b(i) = Vec3dAlloc(-i.toDouble, i.toDouble * 2, i.toDouble * 3)
    }

    def runOnce(): Unit = {
      a.zipTo(b, dst)((x, y) => Vec3dAlloc(x.x + y.x, x.y + y.y, x.z + y.z))
    }

    // warm up so C2 compiles the loop
    for (_ <- 0 until 20000) {
      runOnce()
    }

    val threadId = Thread.currentThread().threadId()
    val before = sunBean.getThreadAllocatedBytes(threadId)
    if (before < 0) {
      cancel("getThreadAllocatedBytes returned an unsupported/negative value on this JVM")
    }

    val measuredIterations = 20000
    for (_ <- 0 until measuredIterations) {
      runOnce()
    }
    val after = sunBean.getThreadAllocatedBytes(threadId)

    val totalAllocatedBytes = after - before
    val bytesPerIteration = totalAllocatedBytes.toDouble / measuredIterations
    val bytesPerElement = bytesPerIteration / n

    // generous threshold for CI stability: allow up to 4 bytes/element of noise (a fully-allocating
    // implementation would be ~40+ bytes/element for a 3-double case class object plus header)
    val thresholdBytesPerElement = 4.0
    assert(
      bytesPerElement < thresholdBytesPerElement,
      s"expected near-zero allocation, measured ${bytesPerElement} bytes/element " +
        s"(${totalAllocatedBytes} bytes over $measuredIterations iterations of $n elements)")
  }
