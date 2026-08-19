package me.kright.gametools.physics1d

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * The HystereticFriction contract, checked identically for every model: any new model
 * added under the trait should be listed here and pass unchanged. The monotonicity and
 * odd-symmetry properties are the sign-error catchers: almost any flipped sign in a
 * branch breaks one of them on a random path.
 */
class HystereticFrictionContractTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val models: Seq[(String, () => HystereticFriction)] = Seq(
    ("DahlFriction", () => new DahlFriction(maxForce = 100.0, saturationTravel = 1e-4)),
    ("BergFriction", () => new BergFriction(maxForce = 100.0, halfSaturationTravel = 1e-4)),
    ("IwanFriction", () => IwanFriction(IwanElement(4e5, 40.0), IwanElement(2e5, 60.0))),
  )

  private val positionLists = Gen.listOf(Gen.choose(-0.01, 0.01))

  test("the first advance only latches the coordinate") {
    for ((name, make) <- models) {
      val friction = make()
      assert(friction.forceAt(123.0) == 0.0, name)
      friction.advance(123.0)
      assert(friction.forceAt(123.0) == 0.0, name)
      friction.advance(123.0 + 1e-5)
      assert(friction.forceAt(123.0 + 1e-5) < 0, name)
    }
  }

  test("moving right never increases the force, moving left never decreases it") {
    forAll(positionLists) { positions =>
      for ((name, make) <- models) {
        val friction = make()
        friction.advance(0.0)
        var x = 0.0
        var force = friction.forceAt(0.0)
        for (next <- positions) {
          friction.advance(next)
          val nextForce = friction.forceAt(next)
          if (next > x) assert(nextForce <= force, s"$name: force grew on a move right")
          if (next < x) assert(nextForce >= force, s"$name: force fell on a move left")
          if (next == x) assert(nextForce == force, name)
          x = next
          force = nextForce
        }
      }
    }
  }

  test("odd symmetry: the mirrored path gives exactly the negated force") {
    forAll(positionLists) { positions =>
      for ((name, make) <- models) {
        val friction = make()
        val mirrored = make()
        friction.advance(0.0)
        mirrored.advance(0.0)
        for (x <- positions) {
          friction.advance(x)
          mirrored.advance(-x)
          assert(mirrored.forceAt(-x) == -friction.forceAt(x), name)
        }
      }
    }
  }

  test("force stays bounded by maxForce on any path") {
    forAll(positionLists) { positions =>
      for ((name, make) <- models) {
        val friction = make()
        friction.advance(0.0)
        for (x <- positions) {
          friction.advance(x)
          assert(Math.abs(friction.forceAt(x)) <= friction.maxForce * (1 + 1e-12), name)
        }
      }
    }
  }

  test("forceAt is pure and advance commits exactly its value, forward and on a reversal") {
    for ((name, make) <- models) {
      val friction = make()
      friction.advance(0.0)
      friction.advance(5e-4) // moving right

      val forward = friction.forceAt(8e-4)
      val reversal = friction.forceAt(2e-4)
      assert(friction.forceAt(8e-4) == forward, name) // probing did not mutate the state
      assert(friction.forceAt(2e-4) == reversal, name)
      assert(friction.forceAt(5e-4) == friction.forceAt(5e-4), name)

      friction.advance(2e-4) // the reversal commit must equal the stage probe
      assert(friction.forceAt(2e-4) == reversal, name)

      val continued = friction.forceAt(0.0) // continuing the new (leftward) branch
      friction.advance(0.0)
      assert(friction.forceAt(0.0) == continued, name)
    }
  }

  test("rate-independence: substeps of one direction equal one big step") {
    for ((name, make) <- models) {
      val oneStep = make()
      val substeps = make()
      for (f <- Seq(oneStep, substeps)) { f.advance(0.0); f.advance(5e-4); f.advance(2e-4) }

      val end = 2e-4 - 6e-4
      val n = 100
      oneStep.advance(end)
      for (i <- 1 to n) substeps.advance(2e-4 - 6e-4 * i / n)

      assert(Math.abs(oneStep.forceAt(end) - substeps.forceAt(end)) <= 1e-9 * oneStep.maxForce, name)
    }
  }

  test("deepCopy is independent in both directions") {
    for ((name, make) <- models) {
      val friction = make()
      friction.advance(0.0)
      friction.advance(1e-4)
      val committed = friction.forceAt(1e-4)

      val copy = friction.deepCopy()
      assert(copy.forceAt(1e-4) == committed, name)

      friction.advance(1e-2)
      assert(copy.forceAt(1e-4) == committed, name) // the original moved, the copy did not

      copy.advance(-1e-2)
      assert(copy.forceAt(-1e-2) > 0 && friction.forceAt(1e-2) < 0, name)
    }
  }
