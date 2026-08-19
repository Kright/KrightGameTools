package me.kright.gametools.physics1d

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class IwanFrictionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  // the reference bushing pair: a stiff "micro" element and a soft "macro" element
  private val micro = IwanElement(stiffness = 4e6, breakForce = 150.0) // breaks at 3.75e-5
  private val macro_ = IwanElement(stiffness = 1e6, breakForce = 250.0) // breaks at 2.5e-4
  private val maxForce = micro.breakForce + macro_.breakForce // 400

  /** friction with the starting coordinate already latched at x = 0 */
  private def makeFriction() =
    val friction = IwanFriction(micro, macro_)
    friction.advance(0.0)
    friction

  private def virginForce(x: Double): Double =
    -(Math.min(micro.stiffness * x, micro.breakForce) + Math.min(macro_.stiffness * x, macro_.breakForce))

  test("constructor validation") {
    assertThrows[IllegalArgumentException](IwanFriction())
    assertThrows[IllegalArgumentException](IwanElement(stiffness = 0.0, breakForce = 10.0))
    assertThrows[IllegalArgumentException](IwanElement(stiffness = -1.0, breakForce = 10.0))
    assertThrows[IllegalArgumentException](IwanElement(stiffness = 1.0, breakForce = -1.0))
  }

  test("the first advance only latches the coordinate") {
    val friction = IwanFriction(micro, macro_)
    assert(friction.forceAt(123.0) == 0.0) // no committed coordinate yet
    friction.advance(123.0)
    assert(friction.forceAt(123.0) == 0.0)
    friction.advance(123.0 + 1e-5)
    assert(friction.forceAt(123.0 + 1e-5) < 0)
  }

  test("the force resists the motion: lengthening gives negative force") {
    val friction = makeFriction()
    friction.advance(0.001)
    assert(friction.forceAt(0.001) < 0)

    friction.advance(-0.001)
    assert(friction.forceAt(-0.001) > 0)
  }

  test("the virgin curve is piecewise linear with knees at the break deformations") {
    val friction = makeFriction()
    // before the first knee (3.75e-5), between the knees, and past the second knee (2.5e-4)
    for (x <- Seq(1e-5, 3.75e-5, 1e-4, 2.5e-4, 1e-3, 1e-2)) {
      assert(Math.abs(friction.forceAt(x) - virginForce(x)) <= 1e-12 * maxForce, s"at x = $x")
      assert(Math.abs(friction.forceAt(-x) + virginForce(x)) <= 1e-12 * maxForce, s"at x = -$x")
    }
    // spot values: -(4e6 + 1e6) * 1e-5 = -50; -(150 + 1e6 * 1e-4) = -250
    assert(friction.forceAt(1e-5) == -50.0)
    assert(friction.forceAt(1e-4) == -250.0)
  }

  test("saturates exactly at +-maxForce once every element slips") {
    val friction = makeFriction()
    friction.advance(1e-3) // > all break deformations
    assert(friction.forceAt(1e-3) == -maxForce)
    friction.advance(-1e-3)
    assert(friction.forceAt(-1e-3) == maxForce)
  }

  test("force stays bounded by maxForce for any move sequence") {
    forAll(Gen.listOf(Gen.choose(-0.01, 0.01))) { positions =>
      val friction = makeFriction()
      for (x <- positions) {
        friction.advance(x)
        assert(Math.abs(friction.forceAt(x)) <= maxForce * (1 + 1e-12))
      }
    }
  }

  test("rate-independence: many substeps of one direction equal one big step") {
    val oneStep = makeFriction()
    val substeps = makeFriction()
    // start from a non-trivial state with a reversal in the history
    for (f <- Seq(oneStep, substeps)) { f.advance(6e-5); f.advance(2e-5) }

    val dx = 4e-4
    val n = 100
    oneStep.advance(2e-5 + dx)
    for (i <- 1 to n) substeps.advance(2e-5 + dx * i / n)

    val end = 2e-5 + dx
    assert(Math.abs(oneStep.forceAt(end) - substeps.forceAt(end)) <= 1e-9 * maxForce)
  }

  test("forceAt is pure and consistent with advance, including trial reversals") {
    val friction = makeFriction()
    friction.advance(1e-4)
    val committed = friction.forceAt(1e-4)

    val forward = friction.forceAt(2e-4)
    val backward = friction.forceAt(0.5e-4) // trial reversal, must not mutate
    assert(backward > committed)
    assert(friction.forceAt(1e-4) == committed)
    assert(friction.forceAt(2e-4) == forward)
    assert(friction.forceAt(0.5e-4) == backward)

    friction.advance(2e-4)
    assert(friction.forceAt(2e-4) == forward) // the committed value is exactly the stage evaluation
  }

  test("tangent stiffness: sum of all stiffnesses while stuck, drops as elements slip") {
    val friction = makeFriction()
    assert(friction.tangentStiffness == micro.stiffness + macro_.stiffness)

    // from the zero state a tiny move engages all springs
    val dx = 1e-9
    assert(Math.abs(friction.forceAt(dx) - (-friction.tangentStiffness * dx)) <= 1e-9 * friction.tangentStiffness * dx)

    // between the knees only the macro element is still stuck
    friction.advance(5e-5) // micro (3.75e-5) already slipping
    val slope = (friction.forceAt(6e-5) - friction.forceAt(5e-5)) / 1e-5
    assert(Math.abs(slope - (-macro_.stiffness)) <= 1e-9 * macro_.stiffness)
  }

  test("Masing rule: the unloading branch is the doubled virgin curve") {
    val friction = makeFriction()
    val top = 1e-2
    friction.advance(top) // deep saturation: force = -maxForce
    assert(friction.forceAt(top) == -maxForce)

    for (u <- Seq(1e-5, 5e-5, 3e-4, 7e-4)) {
      val expected = -maxForce - 2.0 * virginForce(u / 2)
      assert(Math.abs(friction.forceAt(top - u) - expected) <= 1e-9 * maxForce, s"at u = $u")
    }
    // spot values derived by hand: u = 5e-5 -> -150, u = 3e-4 -> +200
    assert(Math.abs(friction.forceAt(top - 5e-5) - (-150.0)) <= 1e-9 * maxForce)
    assert(Math.abs(friction.forceAt(top - 3e-4) - 200.0) <= 1e-9 * maxForce)
  }

  test("advancing to the same coordinate keeps the state unchanged") {
    val friction = makeFriction()
    friction.advance(1e-4)
    val before = friction.forceAt(1e-4)
    friction.advance(1e-4)
    assert(friction.forceAt(1e-4) == before)
  }

  test("deepCopy is independent (the element state array is cloned)") {
    val friction = makeFriction()
    friction.advance(1e-4)
    val copy = friction.deepCopy()
    assert(copy.forceAt(1e-4) == friction.forceAt(1e-4))

    friction.advance(1e-3)
    assert(copy.forceAt(1e-4) != friction.forceAt(1e-3))

    copy.advance(-1e-3)
    assert(copy.forceAt(-1e-3) > 0 && friction.forceAt(1e-3) < 0)
  }

  test("steady cycle dissipation matches the per-element analytic formula") {
    for (amplitude <- Seq(4e-4, 1e-3)) {
      val friction = makeFriction()
      val stepsPerPeriod = 1000

      def position(i: Int): Double = amplitude * Math.sin(2 * Math.PI * i / stepsPerPeriod)

      // first period: transient from the virgin state to the steady loop
      for (i <- 1 to stepsPerPeriod) friction.advance(position(i))

      var dissipated = 0.0
      for (i <- 1 to stepsPerPeriod) {
        val prev = position(stepsPerPeriod + i - 1)
        val next = position(stepsPerPeriod + i)
        val forceBefore = friction.forceAt(prev)
        friction.advance(next)
        dissipated -= 0.5 * (forceBefore + friction.forceAt(next)) * (next - prev)
      }

      // W = sum(4 * F_i * (A - F_i / k_i)) over the slipping elements; the trapezoid sum
      // is exact on the linear segments, only the steps containing a knee deviate
      val expected = friction.elements.map(_.dissipationPerCycle(amplitude)).sum
      assert(Math.abs(dissipated - expected) <= 0.01 * expected, s"amplitude $amplitude: $dissipated vs $expected")
    }
  }

  test("holds a preload below maxForce without ratcheting through") {
    // frozen-force splitting: the viscous partner is mandatory, as for Dahl and Berg
    val friction = IwanFriction(IwanElement(5e5, 40.0), IwanElement(5e5, 60.0))
    friction.advance(0.0)
    def viscous(v: Double): Double = -500.0 * v
    val preload = 50.0
    val mass = 1.0
    val dt = 5e-4

    var r = 0.0
    var v = 0.0
    var rHalfTime = 0.0
    val steps = 4000 // 2 seconds

    for (i <- 1 to steps) {
      val force = preload + viscous(v) + friction.forceAt(r) // r is the committed coordinate here
      v += force / mass * dt
      r += v * dt
      friction.advance(r)
      if (i == steps / 2) rHalfTime = r
    }

    assert(Math.abs(r) < 5e-3, s"the body crept through the friction: r = $r")
    assert(Math.abs(r - rHalfTime) < 2e-4, s"late creep: ${r - rHalfTime} over the second half")
  }

  test("stage-consistent forceAt holds the preload with no viscous partner at all") {
    val friction = IwanFriction(IwanElement(5e5, 40.0), IwanElement(5e5, 60.0))
    friction.advance(0.0)
    val preload = 50.0
    val mass = 1.0
    val dt = 5e-5 // omega = sqrt(1e6 / mass) = 1000, omega * dt = 0.05

    var r = 0.0
    var v = 0.0
    val steps = 40000 // 2 seconds

    for (_ <- 1 to steps) {
      val force = preload + friction.forceAt(r)
      v += force / mass * dt
      r += v * dt
      friction.advance(r)
    }

    // the elastic equilibrium is at r = 5e-5 (before the first knee at 8e-5); the body
    // oscillates around it, losing energy whenever the peak slips the first element
    assert(Math.abs(r) < 3e-4, s"crept away: r = $r")
  }
