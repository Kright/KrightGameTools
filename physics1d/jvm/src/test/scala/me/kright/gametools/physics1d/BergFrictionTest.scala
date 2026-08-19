package me.kright.gametools.physics1d

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BergFrictionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val maxForce = 100.0
  private val halfTravel = 1e-4

  /** friction with the starting coordinate already latched at x = 0 */
  private def makeFriction() =
    val friction = new BergFriction(maxForce, halfTravel)
    friction.advance(0.0)
    friction

  test("the first advance only latches the coordinate") {
    val friction = new BergFriction(maxForce, halfTravel)
    assert(friction.forceAt(123.0) == 0.0) // no committed coordinate yet
    friction.advance(123.0)
    assert(friction.forceAt(123.0) == 0.0)
    friction.advance(123.0 + halfTravel)
    assert(friction.forceAt(123.0 + halfTravel) < 0)
  }

  test("the force resists the motion: lengthening gives negative force") {
    val friction = makeFriction()
    friction.advance(0.001)
    assert(friction.forceAt(0.001) < 0)

    friction.advance(-0.001)
    assert(friction.forceAt(-0.001) > 0)
  }

  test("halfSaturationTravel is exactly the travel to maxForce / 2 from zero force") {
    val friction = makeFriction()
    assert(friction.forceAt(halfTravel) == -maxForce / 2)
    assert(friction.forceAt(-halfTravel) == maxForce / 2)
  }

  test("saturates towards -maxForce when lengthening and +maxForce when shortening") {
    val friction = makeFriction()
    for (i <- 1 to 1000) friction.advance(i * halfTravel)
    // hyperbolic tail: at u = 1000 * x2 the force is maxForce * u / (x2 + u)
    assert(Math.abs(friction.forceAt(1000 * halfTravel) - (-maxForce)) <= 2e-3 * maxForce)

    for (i <- 1 to 2000) friction.advance((1000 - i) * halfTravel)
    assert(Math.abs(friction.forceAt(-1000 * halfTravel) - maxForce) <= 2e-3 * maxForce)
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
    // start from a non-trivial state: a reversal anchors the branch at 2e-4
    oneStep.advance(3e-4)
    oneStep.advance(2e-4)
    substeps.advance(3e-4)
    substeps.advance(2e-4)

    val dx = -5e-4
    val n = 100
    oneStep.advance(2e-4 + dx)
    for (i <- 1 to n) substeps.advance(2e-4 + dx * i / n)

    val end = 2e-4 + dx
    assert(Math.abs(oneStep.forceAt(end) - substeps.forceAt(end)) <= 1e-12 * maxForce)
  }

  test("forceAt is pure and consistent with advance, including trial reversals") {
    val friction = makeFriction()
    friction.advance(3e-4)
    val committed = friction.forceAt(3e-4)

    val forward = friction.forceAt(5e-4)
    val backward = friction.forceAt(1e-4) // trial reversal: a virtual branch, must not mutate
    assert(backward > committed)
    assert(friction.forceAt(3e-4) == committed)
    assert(friction.forceAt(5e-4) == forward)
    assert(friction.forceAt(1e-4) == backward)

    friction.advance(5e-4)
    assert(friction.forceAt(5e-4) == forward) // the committed value is exactly the stage evaluation
  }

  test("every branch starts with the slope tangentStiffness, even after saturation") {
    val friction = makeFriction()
    assert(friction.tangentStiffness == maxForce / halfTravel)

    // from the zero state
    val dx = 1e-9
    assert(Math.abs(friction.forceAt(dx) - (-friction.tangentStiffness * dx)) <= 1e-4 * friction.tangentStiffness * dx)

    // Berg signature: after full saturation the unloading branch starts with the SAME slope
    // (Dahl would start with twice that)
    for (i <- 1 to 1000) friction.advance(i * halfTravel)
    val saturated = friction.forceAt(1000 * halfTravel)
    val unloaded = friction.forceAt(1000 * halfTravel - dx)
    assert(Math.abs((unloaded - saturated) / dx - friction.tangentStiffness) <= 2e-2 * friction.tangentStiffness)
  }

  test("the unloading branch from saturation crosses zero after 2 * halfSaturationTravel") {
    val friction = makeFriction()
    for (i <- 1 to 10000) friction.advance(i * halfTravel) // saturate: F ~ -maxForce
    val top = 10000 * halfTravel
    friction.advance(top - 2 * halfTravel)
    // from Fs = -maxForce: c = 2 * x2, F = -maxForce + (2x2 / 4x2) * 2 * maxForce = 0
    assert(Math.abs(friction.forceAt(top - 2 * halfTravel)) <= 1e-3 * maxForce)
  }

  test("advancing to the same coordinate keeps the state unchanged") {
    val friction = makeFriction()
    friction.advance(3e-4)
    val before = friction.forceAt(3e-4)
    friction.advance(3e-4)
    assert(friction.forceAt(3e-4) == before)
  }

  test("deepCopy is independent and copies the reversal anchor") {
    val friction = makeFriction()
    friction.advance(5e-4)
    friction.advance(3e-4) // reversal: anchor at 5e-4
    val copy = friction.deepCopy()
    assert(copy.forceAt(3e-4) == friction.forceAt(3e-4))
    assert(copy.forceAt(1e-4) == friction.forceAt(1e-4)) // same anchor => same branch

    friction.advance(1e-4)
    assert(copy.forceAt(3e-4) != friction.forceAt(1e-4))

    copy.advance(4e-4)
    assert(copy.forceAt(4e-4) < 0 && friction.forceAt(1e-4) > 0)
  }

  test("closed cycle dissipates energy close to the ideal 4 * maxForce * amplitude loop") {
    val friction = makeFriction()
    val amplitude = 0.01 // = 100 * halfTravel
    val stepsPerPeriod = 1000

    def position(i: Int): Double = amplitude * Math.sin(2 * Math.PI * i / stepsPerPeriod)

    // first period: transient to reach the steady loop
    for (i <- 1 to stepsPerPeriod) friction.advance(position(i))

    var dissipated = 0.0
    for (i <- 1 to stepsPerPeriod) {
      val prev = position(stepsPerPeriod + i - 1)
      val next = position(stepsPerPeriod + i)
      val forceBefore = friction.forceAt(prev)
      friction.advance(next)
      dissipated -= 0.5 * (forceBefore + friction.forceAt(next)) * (next - prev)
    }

    // analytic steady loop area: 2 * maxForce * (2A - 4 * x2 * ln(1 + A / x2)) ~ 0.9 of ideal
    val idealLoop = 4 * maxForce * amplitude
    assert(dissipated > 0.5 * idealLoop && dissipated < 1.1 * idealLoop)
  }

  test("holds a preload below maxForce without ratcheting through") {
    // frozen-force splitting: the viscous partner is mandatory, as for Dahl
    val friction = makeFriction()
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
    val friction = makeFriction()
    val preload = 50.0
    val mass = 1.0
    val dt = 5e-5 // omega = sqrt(tangentStiffness / mass) = 1000, omega * dt = 0.05

    var r = 0.0
    var v = 0.0
    val steps = 40000 // 2 seconds

    for (_ <- 1 to steps) {
      val force = preload + friction.forceAt(r)
      v += force / mass * dt
      r += v * dt
      friction.advance(r)
    }

    // oscillates around the equilibrium u = x2 (where the branch balances the preload)
    // instead of creeping away; the analytic turning point is ~2.5 * x2
    assert(Math.abs(r) < 20 * halfTravel, s"crept away: r = $r")
  }
