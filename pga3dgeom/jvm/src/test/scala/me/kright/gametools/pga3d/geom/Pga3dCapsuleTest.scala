package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dBivectorBulk, Pga3dPoint, Pga3dTranslator, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dCapsuleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  private val radii: Gen[Double] = Gen.oneOf(
    Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0),
    Pga3dVectorMathGenerators.doubleInRange(0.0, 100.0),
  )

  private val capsules: Gen[Pga3dCapsule] =
    for {
      a <- Pga3dPhysicsGenerators.pointIn(bounds)
      b <- Pga3dPhysicsGenerators.pointIn(bounds)
      r <- radii
    } yield Pga3dCapsule(a, b, r)

  test("fromCenter and the center/halfAxis accessors are inverse") {
    forAll(
      Pga3dPhysicsGenerators.pointIn(bounds), Pga3dPhysicsGenerators.vectorIn(bounds), radii,
      MinSuccessful(1000)
    ) { (center, halfAxis, r) =>
      val capsule = Pga3dCapsule.fromCenter(center, halfAxis, r)
      val tolerance = 1e-12 * (1.0 + center.toVectorUnsafe.norm + halfAxis.norm)

      assert((capsule.center - center).norm <= tolerance, s"capsule = $capsule, center = $center")
      assert((capsule.halfAxis - halfAxis).norm <= tolerance, s"capsule = $capsule, halfAxis = $halfAxis")
      assert(capsule.r == r)
    }

    forAll(capsules, MinSuccessful(1000)) { capsule =>
      val roundTrip = Pga3dCapsule.fromCenter(capsule.center, capsule.halfAxis, capsule.r)
      val tolerance = 1e-12 * (1.0 + halfSize)
      assert((roundTrip.a - capsule.a).norm <= tolerance && (roundTrip.b - capsule.b).norm <= tolerance,
        s"roundTrip = $roundTrip, capsule = $capsule")
    }
  }

  test("a zero half axis gives the degenerate sphere capsule") {
    forAll(Pga3dPhysicsGenerators.pointIn(bounds), radii, MinSuccessful(500)) { (center, r) =>
      val capsule = Pga3dCapsule.fromCenter(center, Pga3dVector(0, 0, 0), r)
      assert(capsule.a == center && capsule.b == center)
      assert(capsule == Pga3dCapsule(Pga3dSphere(center, r)))
      assert(capsule.halfAxis == Pga3dVector(0, 0, 0))
    }
  }

  test("toAABB equals the edge AABB expanded by r") {
    forAll(capsules, MinSuccessful(1000)) { capsule =>
      val aabb = capsule.toAABB
      assert(aabb == capsule.edge.toAABB.expand(capsule.r), s"capsule = $capsule")
      assert(aabb.contains(capsule.a) && aabb.contains(capsule.b), s"capsule = $capsule")
    }
  }

  test("sandwich transforms the hemisphere centers and keeps the radius") {
    import Pga3dCapsule.sandwich

    val rotor = Pga3dBivectorBulk(xy = 1.0).exp(0.3)
    val translator = Pga3dTranslator(1.0, -2.0, 3.0)

    forAll(capsules, MinSuccessful(500)) { capsule =>
      val rotated = rotor.sandwich(capsule)
      assert(rotated.a == rotor.sandwich(capsule.a).toPointUnsafe)
      assert(rotated.b == rotor.sandwich(capsule.b).toPointUnsafe)
      assert(rotated.r == capsule.r)

      val translated = translator.sandwich(capsule)
      assert(translated.a == translator.sandwich(capsule.a))
      assert(translated.r == capsule.r)
    }
  }

  test("pairwise queries: symmetry and degenerate reductions") {
    forAll(capsules, capsules, MinSuccessful(1000)) { (c1, c2) =>
      // capsule-capsule is symmetric
      assert(c1.intersects(c2) == c2.intersects(c1), s"c1 = $c1, c2 = $c2")

      // a degenerate second capsule is the sphere query, in both directions
      val sphere = Pga3dSphere(c2.a, c2.r)
      val degenerate = Pga3dCapsule(sphere)
      assert(c1.intersects(degenerate) == c1.intersects(sphere), s"c1 = $c1, sphere = $sphere")
      assert(sphere.intersects(c1) == c1.intersects(sphere), s"c1 = $c1, sphere = $sphere")

      // both degenerate: plain sphere-sphere
      val sphere1 = Pga3dSphere(c1.a, c1.r)
      assert(Pga3dCapsule(sphere1).intersects(degenerate) == sphere1.intersects(sphere),
        s"sphere1 = $sphere1, sphere = $sphere")
    }
  }

  test("expand changes only the radius") {
    val capsule = Pga3dCapsule(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0), 0.5)
    assert(capsule.expand(0.25) == Pga3dCapsule(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0), 0.75))
  }
