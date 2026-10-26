package me.kright.gametools.physics1d

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Extreme inputs for every HystereticFriction model: moves of 1e-320 .. 1e300 and degenerate
 * parameters must never produce a NaN or an infinity, never leave the force ball, and a tiny
 * move must change the force by at most the stuck stiffness times the move.
 */
class HystereticFrictionEdgeCaseTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val models: Seq[(String, () => HystereticFriction)] = Seq(
    ("DahlFriction", () => new DahlFriction(maxForce = 100.0, saturationTravel = 1e-4)),
    ("BergFriction", () => new BergFriction(maxForce = 100.0, halfSaturationTravel = 1e-4)),
    ("IwanFriction", () => IwanFriction(IwanElement(4e5, 40.0), IwanElement(2e5, 60.0))),
  )

  private val tiny = Seq(1e-10, 1e-20, 1e-40, 1e-60, 1e-100, 1e-150, 1e-155, 1e-160, 1e-165, 1e-170,
    1e-200, 1e-300, 1e-320, Double.MinPositiveValue)
  private val huge = Seq(1e100, 1e154, 1e200, 1e300, Double.MaxValue)

  /** the models in the virgin state and saturated in both directions (the force at +-maxForce) */
  private def states(make: () => HystereticFriction): Seq[(String, HystereticFriction)] =
    val virgin = make()
    virgin.advance(0.0)
    val saturatedRight = make()
    saturatedRight.advance(-1.0)
    saturatedRight.advance(0.0)
    val saturatedLeft = make()
    saturatedLeft.advance(1.0)
    saturatedLeft.advance(0.0)
    Seq(("virgin", virgin), ("saturated right", saturatedRight), ("saturated left", saturatedLeft))

  test("tiny moves: finite, bounded, and the change is at most the stuck stiffness times the move") {
    for ((name, make) <- models; (state, friction) <- states(make)) {
      val committed = friction.forceAt(0.0)
      for (eps <- tiny; dx <- Seq(eps, -eps)) {
        val force = friction.forceAt(dx)
        assert(!force.isNaN && !force.isInfinite, s"$name $state dx $dx: $force")
        assert(Math.abs(force) <= friction.maxForce * (1 + 1e-12), s"$name $state dx $dx: $force")
        val allowed = 2 * friction.tangentStiffness * eps + 1e-14 * friction.maxForce
        assert(Math.abs(force - committed) <= allowed, s"$name $state dx $dx: $committed -> $force, allowed $allowed")
      }
    }
  }

  test("huge moves saturate exactly against the move") {
    for ((name, make) <- models; (state, friction) <- states(make)) {
      for (eps <- huge; dx <- Seq(eps, -eps)) {
        val force = friction.forceAt(dx)
        assert(Math.abs(force - -friction.maxForce * Math.signum(dx)) <= 1e-12 * friction.maxForce, s"$name $state dx $dx: $force")
      }
    }
  }

  test("committing extreme moves keeps the state finite and bounded") {
    val moves = (tiny ++ huge).flatMap(e => Seq(e, -e))
    forAll(Gen.listOfN(30, Gen.oneOf(moves)), MinSuccessful(200)) { steps =>
      for ((name, make) <- models) {
        val friction = make()
        friction.advance(0.0)
        var sign = 1.0
        for (dx <- steps) {
          // x itself stays finite, but the difference between two huge x of opposite signs overflows
          sign = -sign
          val x = sign * dx
          friction.advance(x)
          val force = friction.forceAt(x)
          assert(!force.isNaN && !force.isInfinite, s"$name at $x: $force")
          assert(Math.abs(force) <= friction.maxForce * (1 + 1e-12), s"$name at $x: $force")
        }
      }
    }
  }

  test("degenerate parameters: zero saturation force, extreme travels and stiffnesses") {
    val degenerate: Seq[(String, HystereticFriction)] = Seq(
      ("Dahl zero force", new DahlFriction(maxForce = 0.0, saturationTravel = 1e-4)),
      ("Dahl tiny travel", new DahlFriction(maxForce = 100.0, saturationTravel = 1e-300)),
      ("Dahl huge travel", new DahlFriction(maxForce = 100.0, saturationTravel = 1e300)),
      ("Berg zero force", new BergFriction(maxForce = 0.0, halfSaturationTravel = 1e-4)),
      ("Berg tiny travel", new BergFriction(maxForce = 100.0, halfSaturationTravel = 1e-300)),
      ("Berg huge travel", new BergFriction(maxForce = 100.0, halfSaturationTravel = 1e300)),
      ("Iwan zero force", IwanFriction(IwanElement(1e6, 0.0))),
      ("Iwan huge stiffness", IwanFriction(IwanElement(1e300, 100.0))),
      ("Iwan tiny stiffness", IwanFriction(IwanElement(1e-300, 100.0))),
      ("Iwan huge force", IwanFriction(IwanElement(1e6, 1e300))),
    )
    val positions = Gen.oneOf((tiny ++ huge ++ Seq(0.0, 1e-3, 1.0)).flatMap(e => Seq(e, -e)))
    forAll(Gen.listOfN(30, positions), MinSuccessful(200)) { path =>
      for ((name, model) <- degenerate) {
        val friction = model.deepCopy()
        friction.advance(0.0)
        for (x <- path) {
          val trial = friction.forceAt(x)
          friction.advance(x)
          val force = friction.forceAt(x)
          assert(trial == force, s"$name at $x: trial $trial vs committed $force")
          assert(!force.isNaN && !force.isInfinite, s"$name at $x: $force")
          assert(Math.abs(force) <= friction.maxForce * (1 + 1e-12), s"$name at $x: $force")
        }
      }
    }
  }
