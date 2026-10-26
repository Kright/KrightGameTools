package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dRotor, Pga3dVector}
import me.kright.gametools.physics1d.{DahlFriction, HystereticFriction, IwanElement, IwanFriction}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * The Pga3dHystereticFriction contract, checked identically for every model together with its
 * 1d counterpart: any new model added under the trait should be listed here and pass unchanged.
 */
class Pga3dHystereticFrictionContractTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val models: Seq[(String, () => Pga3dHystereticFriction, () => HystereticFriction)] = Seq(
    ("Dahl",
      () => new Pga3dDahlFriction(maxForce = 100.0, saturationTravel = 1e-4),
      () => new DahlFriction(maxForce = 100.0, saturationTravel = 1e-4)),
    ("Iwan",
      () => Pga3dIwanFriction(IwanElement(4e5, 40.0), IwanElement(2e5, 60.0)),
      () => IwanFriction(IwanElement(4e5, 40.0), IwanElement(2e5, 60.0))),
  )

  private val origin = Pga3dPoint(0.0, 0.0, 0.0)
  private val bound = 0.01
  private val points: Gen[Pga3dPoint] =
    Pga3dPhysicsGenerators.pointIn(Pga3dPoint(-bound, -bound, -bound), Pga3dPoint(bound, bound, bound))
  private val paths: Gen[List[Pga3dPoint]] = Gen.listOf(points)
  private val scalars: Gen[List[Double]] = Gen.listOf(Gen.choose(-bound, bound))
  private val directions: Gen[Pga3dVector] =
    Pga3dPhysicsGenerators.vectorIn(Pga3dPoint(-1, -1, -1), Pga3dPoint(1, 1, 1))
      .filter(_.norm > 0.1).map(_.normalizedByNorm)

  test("the first advance only latches the point") {
    for ((name, make, _) <- models) {
      val friction = make()
      val p = Pga3dPoint(1.0, 2.0, 3.0)
      assert(friction.forceAt(p) == Pga3dVector.zero, name)
      friction.advance(p)
      assert(friction.forceAt(p) == Pga3dVector.zero, name)
      val q = p + Pga3dVector(0.0, 1e-5, 0.0)
      friction.advance(q)
      val f = friction.forceAt(q)
      assert(f.y < 0 && f.x == 0.0 && f.z == 0.0, name)
    }
  }

  test("back and forth along one line is equivalent to the 1d model, trial points included") {
    forAll(directions, scalars, MinSuccessful(300)) { (u, positions) =>
      for ((name, make3d, make1d) <- models) {
        val friction3d = make3d()
        val friction1d = make1d()
        friction3d.advance(origin)
        friction1d.advance(0.0)
        for (s <- positions) {
          val trial = s + 3e-5 // a trial evaluation before the commit must agree and must not mutate
          assert((friction3d.forceAt(origin + u * trial) - u * friction1d.forceAt(trial)).norm <= 1e-12 * friction3d.maxForce, name)
          friction3d.advance(origin + u * s)
          friction1d.advance(s)
          val f3 = friction3d.forceAt(origin + u * s)
          val f1 = friction1d.forceAt(s)
          assert((f3 - u * f1).norm <= 1e-12 * friction3d.maxForce, s"$name: 3d $f3 vs 1d $f1 along $u at $s")
        }
        assert(friction3d.maxForce == friction1d.maxForce && friction3d.tangentStiffness == friction1d.tangentStiffness, name)
      }
    }
  }

  test("rate-independence: many substeps of one straight segment equal one big step") {
    forAll(directions, directions, directions, MinSuccessful(100)) { (u1, u2, u3) =>
      for ((name, make, _) <- models) {
        val oneStep = make()
        val substeps = make()
        val a = origin + u1 * 6e-5
        val b = a + u2 * 1e-4
        for (f <- Seq(oneStep, substeps)) { f.advance(origin); f.advance(a); f.advance(b) }

        val dx = u3 * 4e-4
        val n = 100
        val end = b + dx
        oneStep.advance(end)
        for (i <- 1 to n) substeps.advance(b + dx * (i.toDouble / n))
        assert((oneStep.forceAt(end) - substeps.forceAt(end)).norm <= 1e-9 * oneStep.maxForce, name)
      }
    }
  }

  test("rotation invariance: the rotated path gives the rotated force") {
    forAll(directions, directions, paths, MinSuccessful(100)) { (from, to, positions) =>
      val rotor = Pga3dRotor.rotation(from, to)
      for ((name, make, _) <- models) {
        val friction = make()
        val rotated = make()
        friction.advance(origin)
        rotated.advance(origin)
        for (p <- positions) {
          val q = rotor.sandwich(p).toPointUnsafe
          friction.advance(p)
          rotated.advance(q)
          assert((rotated.forceAt(q) - rotor.sandwich(friction.forceAt(p))).norm <= 1e-9 * friction.maxForce, name)
        }
      }
    }
  }

  test("odd symmetry: the mirrored path gives exactly the negated force") {
    forAll(paths, MinSuccessful(300)) { positions =>
      for ((name, make, _) <- models) {
        val friction = make()
        val mirrored = make()
        friction.advance(origin)
        mirrored.advance(origin)
        for (p <- positions) {
          val m = origin - (p - origin)
          friction.advance(p)
          mirrored.advance(m)
          assert(mirrored.forceAt(m) == -friction.forceAt(p), name)
        }
      }
    }
  }

  test("force stays bounded by maxForce on any path") {
    forAll(paths, MinSuccessful(300)) { positions =>
      for ((name, make, _) <- models) {
        val friction = make()
        friction.advance(origin)
        for (p <- positions) {
          friction.advance(p)
          assert(friction.forceAt(p).norm <= friction.maxForce * (1 + 1e-12), name)
        }
      }
    }
  }

  test("forceAt is pure and consistent with advance, including trial turns") {
    for ((name, make, _) <- models) {
      val friction = make()
      friction.advance(origin)
      val a = Pga3dPoint(1e-4, 0.0, 0.0)
      friction.advance(a)
      val committed = friction.forceAt(a)
      val side = Pga3dPoint(1e-4, 1e-4, 0.0)
      val back = Pga3dPoint(0.5e-4, 0.0, 0.0)
      val sideways = friction.forceAt(side)
      val backward = friction.forceAt(back)
      assert(backward.x > committed.x && sideways.y < 0.0, name)
      assert(friction.forceAt(a) == committed && friction.forceAt(side) == sideways && friction.forceAt(back) == backward, name)
      friction.advance(side)
      assert(friction.forceAt(side) == sideways, s"$name: the committed value is exactly the stage evaluation")
      friction.advance(side)
      assert(friction.forceAt(side) == sideways, s"$name: advancing to the same point keeps the state")
    }
  }

  test("deepCopy is independent") {
    for ((name, make, _) <- models) {
      val friction = make()
      friction.advance(origin)
      val a = Pga3dPoint(1e-4, 0.0, 0.0)
      friction.advance(a)
      val copy = friction.deepCopy()
      assert(copy.forceAt(a) == friction.forceAt(a), name)
      val b = Pga3dPoint(0.0, 1e-3, 0.0)
      friction.advance(b)
      assert(copy.forceAt(a) != friction.forceAt(b), name)
      copy.advance(Pga3dPoint(-1e-3, 0.0, 0.0))
      assert(copy.forceAt(Pga3dPoint(-1e-3, 0.0, 0.0)).x > 0 && friction.forceAt(b).y < 0, name)
    }
  }

  test("extreme step lengths: tiny moves change the force by at most the stuck stiffness, huge moves saturate") {
    val u = Pga3dVector(0.3, -0.5, 0.8).normalizedByNorm
    val w = Pga3dVector(-0.7, 0.2, 0.4).normalizedByNorm // the direction of the saturating history
    val tiny = Seq(1e-10, 1e-20, 1e-40, 1e-60, 1e-100, 1e-150, 1e-155, 1e-160, 1e-165, 1e-170, 1e-200, 1e-300, 1e-320)
    val huge = Seq(1e100, 1e154, 1e200, 1e300)
    for ((name, make, _) <- models) {
      // from the virgin state and from a state saturated along w (the force on its sphere)
      val virgin = make()
      virgin.advance(origin)
      val saturated = make()
      saturated.advance(origin - w * 1.0)
      saturated.advance(origin)
      for ((friction, state) <- Seq((virgin, "virgin"), (saturated, "saturated"))) {
        val committed = friction.forceAt(origin)
        for (eps <- tiny) {
          val force = friction.forceAt(origin + u * eps)
          val change = (force - committed).norm
          assert(!force.x.isNaN && !force.y.isNaN && !force.z.isNaN, s"$name $state eps $eps: $force")
          assert(force.norm <= friction.maxForce * (1 + 1e-12), s"$name $state eps $eps: $force")
          // the noise floor: a saturated Iwan element is re-projected on its sphere, ~10 ulp per component
          val allowed = 2 * friction.tangentStiffness * eps + 1e-14 * friction.maxForce
          assert(change <= allowed, s"$name $state eps $eps: the force changed by $change, allowed $allowed: $committed -> $force")
        }
        for (eps <- huge) {
          val force = friction.forceAt(origin + u * eps)
          assert((force - u * -friction.maxForce).norm <= 1e-12 * friction.maxForce, s"$name $state eps $eps: $force")
        }
      }
    }
  }

  private val extremeLengths = Seq(1e-10, 1e-20, 1e-60, 1e-100, 1e-160, 1e-170, 1e-300, 1e-320, Double.MinPositiveValue,
    1e100, 1e154, 1e200, 1e300, Double.MaxValue)
  private val extremeMoves: Gen[Pga3dVector] =
    for (eps <- Gen.oneOf(extremeLengths); u <- directions) yield u * eps

  private def assertFiniteAndBounded(name: String, friction: Pga3dHystereticFriction, p: Pga3dPoint): Unit =
    val force = friction.forceAt(p)
    assert(!force.x.isNaN && !force.y.isNaN && !force.z.isNaN, s"$name at $p: $force")
    assert(!force.x.isInfinite && !force.y.isInfinite && !force.z.isInfinite, s"$name at $p: $force")
    // the norm of a 1e300 force overflows, so the bound is checked on the force scaled by maxForce (unless it is 0)
    assert(force.norm <= friction.maxForce * (1 + 1e-12) || (force / friction.maxForce).norm <= 1 + 1e-12, s"$name at $p: $force")

  test("committing extreme moves in random directions keeps the state finite and bounded") {
    forAll(Gen.listOfN(30, extremeMoves), MinSuccessful(200)) { moves =>
      for ((name, make, _) <- models) {
        val friction = make()
        friction.advance(origin)
        var sign = 1.0
        for (move <- moves) {
          // the point stays finite, but the difference between two huge points of opposite signs overflows
          sign = -sign
          val at = origin + move * sign
          friction.advance(at)
          assertFiniteAndBounded(name, friction, at)
        }
      }
    }
  }

  test("degenerate parameters: zero saturation force, extreme travels and stiffnesses") {
    val degenerate: Seq[(String, () => Pga3dHystereticFriction)] = Seq(
      ("Dahl zero force", () => new Pga3dDahlFriction(maxForce = 0.0, saturationTravel = 1e-4)),
      ("Dahl tiny travel", () => new Pga3dDahlFriction(maxForce = 100.0, saturationTravel = 1e-300)),
      ("Dahl huge travel", () => new Pga3dDahlFriction(maxForce = 100.0, saturationTravel = 1e300)),
      ("Iwan zero force", () => Pga3dIwanFriction(IwanElement(1e6, 0.0))),
      ("Iwan huge stiffness", () => Pga3dIwanFriction(IwanElement(1e300, 100.0))),
      ("Iwan tiny stiffness", () => Pga3dIwanFriction(IwanElement(1e-300, 100.0))),
      ("Iwan huge force", () => Pga3dIwanFriction(IwanElement(1e6, 1e300))),
    )
    val moves = Gen.oneOf(extremeMoves, points.map(_ - origin), Gen.const(Pga3dVector.zero))
    forAll(Gen.listOfN(30, moves), MinSuccessful(200)) { path =>
      for ((name, make) <- degenerate) {
        val friction = make()
        friction.advance(origin)
        for (move <- path) {
          val p = origin + move
          val trial = friction.forceAt(p)
          friction.advance(p)
          assert(trial == friction.forceAt(p), s"$name at $p: trial $trial vs committed ${friction.forceAt(p)}")
          assertFiniteAndBounded(name, friction, p)
        }
      }
    }
  }
