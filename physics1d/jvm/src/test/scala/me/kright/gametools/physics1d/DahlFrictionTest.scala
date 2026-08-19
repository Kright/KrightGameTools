package me.kright.gametools.physics1d

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DahlFrictionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val maxForce = 100.0
  private val saturationTravel = 1e-4

  /** friction with the starting coordinate already latched at x = 0 */
  private def makeFriction() =
    val friction = new DahlFriction(maxForce, saturationTravel)
    friction.advance(0.0)
    friction

  test("the first advance only latches the coordinate") {
    val friction = new DahlFriction(maxForce, saturationTravel)
    assert(friction.forceAt(123.0) == 0.0) // no committed coordinate yet
    friction.advance(123.0)
    assert(friction.forceAt(123.0) == 0.0)
    friction.advance(123.0 + saturationTravel)
    assert(friction.forceAt(123.0 + saturationTravel) < 0)
  }

  test("the force resists the motion: lengthening gives negative force") {
    val friction = makeFriction()
    friction.advance(0.001)
    assert(friction.forceAt(0.001) < 0)

    friction.advance(-0.001)
    assert(friction.forceAt(-0.001) > 0)
  }

  test("saturates to -maxForce when lengthening and +maxForce when shortening") {
    val friction = makeFriction()
    for (i <- 1 to 1000) friction.advance(i * saturationTravel)
    assert(Math.abs(friction.forceAt(1000 * saturationTravel) - (-maxForce)) <= 1e-9 * maxForce)

    for (i <- 1 to 2000) friction.advance((1000 - i) * saturationTravel)
    assert(Math.abs(friction.forceAt(-1000 * saturationTravel) - maxForce) <= 1e-9 * maxForce)
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

  test("rate-independence: many substeps equal one big step") {
    val oneStep = makeFriction()
    val substeps = makeFriction()
    // start from a non-trivial state
    oneStep.advance(2e-4)
    substeps.advance(2e-4)

    val dx = 5e-4
    val n = 100
    oneStep.advance(2e-4 + dx)
    for (i <- 1 to n) substeps.advance(2e-4 + dx * i / n)

    val end = 2e-4 + dx
    assert(Math.abs(oneStep.forceAt(end) - substeps.forceAt(end)) <= 1e-9 * maxForce)
  }

  test("forceAt is pure and consistent with advance") {
    val friction = makeFriction()
    friction.advance(3e-4)
    val committed = friction.forceAt(3e-4)

    val probe = friction.forceAt(5e-4)
    assert(friction.forceAt(3e-4) == committed) // probing did not mutate the state
    assert(friction.forceAt(5e-4) == probe) // repeated evaluation gives the same value

    friction.advance(5e-4)
    assert(friction.forceAt(5e-4) == probe) // the committed value is exactly the stage evaluation
  }

  test("forceAt continues the same branch across a monotone step split") {
    // evaluating from (x0, F0) directly and re-latching midway give the same curve
    val direct = makeFriction()
    val relatched = makeFriction()
    relatched.advance(2e-4)

    val x = 7e-4
    assert(Math.abs(direct.forceAt(x) - relatched.forceAt(x)) <= 1e-12 * maxForce)
  }

  test("tangent stiffness of the stuck state is maxForce / saturationTravel") {
    val friction = makeFriction()
    assert(friction.tangentStiffness == maxForce / saturationTravel)

    // from zero force a tiny move gives dF ~ -tangentStiffness * dx
    val dx = 1e-9
    val expected = -friction.tangentStiffness * dx
    assert(Math.abs(friction.forceAt(dx) - expected) <= 1e-4 * Math.abs(expected))
  }

  test("advancing to the same coordinate keeps the state unchanged") {
    val friction = makeFriction()
    friction.advance(3e-4)
    val before = friction.forceAt(3e-4)
    friction.advance(3e-4)
    assert(friction.forceAt(3e-4) == before)
  }

  test("deepCopy is independent") {
    val friction = makeFriction()
    friction.advance(3e-4)
    val copy = friction.deepCopy()
    assert(copy.forceAt(3e-4) == friction.forceAt(3e-4))

    friction.advance(3e-4 + 1e-3)
    assert(copy.forceAt(3e-4) != friction.forceAt(3e-4 + 1e-3))

    copy.advance(3e-4 - 1e-3)
    assert(copy.forceAt(3e-4 - 1e-3) > 0 && friction.forceAt(3e-4 + 1e-3) < 0)
  }

  test("closed cycle dissipates energy close to the ideal 4 * maxForce * amplitude loop") {
    val friction = makeFriction()
    val amplitude = 0.01 // >> saturationTravel
    val stepsPerPeriod = 1000

    def position(i: Int): Double = amplitude * Math.sin(2 * Math.PI * i / stepsPerPeriod)

    // first period: transient to reach the steady loop
    for (i <- 1 to stepsPerPeriod) friction.advance(position(i))

    // second period: the friction force does negative work, dissipating energy
    var dissipated = 0.0
    for (i <- 1 to stepsPerPeriod) {
      val prev = position(stepsPerPeriod + i - 1)
      val next = position(stepsPerPeriod + i)
      val forceBefore = friction.forceAt(prev)
      friction.advance(next)
      dissipated -= 0.5 * (forceBefore + friction.forceAt(next)) * (next - prev)
    }

    val idealLoop = 4 * maxForce * amplitude
    assert(dissipated > 0.5 * idealLoop && dissipated < 1.1 * idealLoop)
  }

  test("holds a preload below maxForce without ratcheting through") {
    // 1d body on symplectic Euler with the force frozen over the step (forceAt at the
    // committed coordinate): constant pull of 50 N against maxForce = 100 N.
    // The viscous partner is mandatory here: without it the undamped internal stiffness
    // maxForce / saturationTravel ratchets the body through the friction (flipping the
    // viscous sign to +c*v makes r grow as exp(c/m * t) - the sign canary).
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
    // reading forceAt at the current trial coordinate removes the one-step force lag,
    // so the body finds the true equilibrium inside the loop even without damping
    val friction = makeFriction()
    val preload = 50.0
    val mass = 1.0
    val dt = 5e-5 // omega = sqrt(2 * tangentStiffness / mass) ~ 1414, omega * dt ~ 0.07

    var r = 0.0
    var v = 0.0
    val steps = 40000 // 2 seconds

    for (_ <- 1 to steps) {
      val force = preload + friction.forceAt(r)
      v += force / mass * dt
      r += v * dt
      friction.advance(r)
    }

    // undamped: the body oscillates around the equilibrium instead of creeping away;
    // the equilibrium displacement is where forceAt balances the preload, well inside
    // a few saturationTravel
    assert(Math.abs(r) < 20 * saturationTravel, s"crept away: r = $r")
  }
