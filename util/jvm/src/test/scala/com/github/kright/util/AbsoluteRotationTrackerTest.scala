package com.github.kright.util

import com.github.kright.util.AbsoluteRotationTracker.*
import com.github.kright.util.{AbsoluteRotationTracker, MathUtil}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class AbsoluteRotationTrackerTest extends AnyFunSuite with ScalaCheckPropertyChecks:
  private val epsilon = 1e-10

  test("test angle increasing") {
    val angles = (0 to 20).map(i => Math.PI * 2 * i / 10.0)

    val tracker = AbsoluteRotationTracker()

    for (angle <- angles) {
      tracker.update(angle)
      assert(Math.abs(angle - tracker.angle) < epsilon)
      assert(tracker.rotationsOffset < 2 * Math.PI * epsilon && tracker.rotationsOffset >= 0.0)
    }
  }

  test("shift") {
    val eps = 1e-15
    val tracker = AbsoluteRotationTracker(0.234, 0.134)
    tracker.update(1.23)
    tracker.angle = 0.0

    tracker.angle += 2.0
    assert(Math.abs(tracker.angle - 2.0) < eps)

    tracker.angle += 3.0
    assert(Math.abs(tracker.angle - 5.0) < eps)

    tracker.angle -= 7.0
    assert(Math.abs(tracker.angle + 2.0) < eps)
  }

  test("test angle increasing and resetting") {
    val angles = (1 to 20).map(i => Math.PI * 2 * i / 10.0)

    val tracker = AbsoluteRotationTracker()

    for (angle <- angles) {
      tracker.update(angle)
      assert(Math.abs(tracker.angle - Math.PI * 2 / 10.0) < epsilon)
      tracker.angle = 0.0
      assert(Math.abs(tracker.angle) < epsilon)
      assert(tracker.rotationsOffset <= 1.0 + epsilon)
      assert(tracker.rotationsOffset >= 0.0 - epsilon)
    }
  }

  test("update with small changes") {
    val tracker = new AbsoluteRotationTracker()

    tracker.update(math.Pi / 4) // 45 degrees
    assert(math.abs(tracker.angle - math.Pi / 4) < epsilon)

    tracker.update(math.Pi / 2) // 90 degrees
    assert(math.abs(tracker.angle - math.Pi / 2) < epsilon)

    tracker.update(math.Pi / 4) // Back to 45 degrees
    assert(math.abs(tracker.angle - math.Pi / 4) < epsilon)
  }

  test("update crossing zero boundary") {
    val tracker = new AbsoluteRotationTracker()

    // Start at slightly negative
    tracker.update(-0.1)
    assert(math.abs(tracker.angle - (-0.1)) < epsilon)

    // Cross to positive
    tracker.update(0.1)
    assert(math.abs(tracker.angle - 0.1) < epsilon)

    // Cross back to negative
    tracker.update(-0.1)
    assert(math.abs(tracker.angle - (-0.1)) < epsilon)
  }

  test("update with full rotation") {
    val tracker = new AbsoluteRotationTracker()

    // Start at 0
    assert(tracker.angle == 0.0)

    // Update with small increments to complete a full rotation
    val steps = 8
    for (i <- 1 to steps) {
      val angle = -i * (MathUtil.Tau / steps)
      tracker.update(angle)
    }

    // Should be back at 0 but with 1 rotation
    assert(math.abs(tracker.rotations + 1.0) < epsilon)
    assert(math.abs(tracker.rotationsOffset) < epsilon)
    assert(math.abs(tracker.angle + MathUtil.Tau) < epsilon)
  }
