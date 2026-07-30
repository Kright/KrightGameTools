package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dSphereTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  // no filtering: degenerate triangles must work too
  private val triangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)
  private val spheres: Gen[Pga3dSphere] =
    for {
      center <- Pga3dPhysicsGenerators.pointIn(bounds)
      r <- Gen.oneOf(
        Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0),
        Pga3dVectorMathGenerators.doubleInRange(0.0, 2.0 * halfSize),
      )
    } yield Pga3dSphere(center, r)

  test("intersects(triangle) agrees with the distance") {
    forAll(spheres, triangles, MinSuccessful(2000)) { (sphere, triangle) =>
      assert(sphere.intersects(triangle) == (triangle.distanceTo(sphere.center) <= sphere.r),
        s"sphere = $sphere, triangle = $triangle")
    }
  }

  test("deepestContact is defined iff the sphere intersects the triangle") {
    forAll(spheres, triangles, MinSuccessful(2000)) { (sphere, triangle) =>
      // the only allowed exception: the center exactly on a degenerate triangle has no normal
      val exotic = triangle.area == 0.0 && triangle.distanceSquareTo(sphere.center) == 0.0
      if (!exotic) {
        assert(sphere.deepestContact(triangle).isDefined == sphere.intersects(triangle),
          s"sphere = $sphere, triangle = $triangle")
      }
    }
  }

  test("deepestContact returns a consistent point, normal and depth") {
    forAll(spheres, triangles, MinSuccessful(2000)) { (sphere, triangle) =>
      for (contact <- sphere.deepestContact(triangle)) {
        val scene = 1.0 + triangle.perimeter + sphere.r

        assert(triangle.distanceSquareTo(contact.point) <= 1e-18 * scene * scene,
          s"contact point is not on the triangle: $contact, sphere = $sphere, triangle = $triangle")
        assert((contact.normal.norm - 1.0).abs <= 1e-12,
          s"normal is not unit: $contact")
        assert(contact.depth >= 0.0 && contact.depth <= sphere.r,
          s"depth out of range: $contact, r = ${sphere.r}")

        // the center is reconstructed as point + normal * (r - depth)
        val reconstructed = contact.point + contact.normal * (sphere.r - contact.depth)
        assert((reconstructed - sphere.center).norm <= 1e-9 * scene,
          s"center is not reconstructed: $contact, sphere = $sphere, triangle = $triangle")
      }
    }
  }

  test("deepestContact unit cases") {
    val triangle = Pga3dTriangle(Pga3dPoint(-10, -10, 0), Pga3dPoint(10, -10, 0), Pga3dPoint(0, 10, 0))

    val above = Pga3dSphere(Pga3dPoint(0, 0, 2), r = 3.0)
    val contact = above.deepestContact(triangle).get
    assert((contact.point - Pga3dPoint(0, 0, 0)).norm <= 1e-12)
    assert((contact.normal - Pga3dVector(0, 0, 1)).norm <= 1e-12)
    assert((contact.depth - 1.0).abs <= 1e-12)

    // center exactly on the triangle: the plane normal (winding of this triangle gives +z)
    val onSurface = Pga3dSphere(Pga3dPoint(0, 0, 0), r = 2.0)
    val central = onSurface.deepestContact(triangle).get
    assert((central.point - Pga3dPoint(0, 0, 0)).norm <= 1e-12)
    assert(central.normal.z.abs == 1.0, s"normal = ${central.normal}")
    assert(central.depth == 2.0)

    assert(Pga3dSphere(Pga3dPoint(0, 0, 5), r = 3.0).deepestContact(triangle).isEmpty)

    // center exactly on a degenerate triangle: no normal is defined
    val degenerate = Pga3dTriangle(Pga3dPoint(-1, 0, 0), Pga3dPoint(1, 0, 0), Pga3dPoint(0, 0, 0))
    assert(Pga3dSphere(Pga3dPoint(0, 0, 0), r = 1.0).deepestContact(degenerate).isEmpty)
    // but a center off it works: the contact is with the nearest point of the segment
    val offset = Pga3dSphere(Pga3dPoint(0, 3, 0), r = 4.0).deepestContact(degenerate).get
    assert((offset.point - Pga3dPoint(0, 0, 0)).norm <= 1e-12)
    assert((offset.normal - Pga3dVector(0, 1, 0)).norm <= 1e-12)
    assert((offset.depth - 1.0).abs <= 1e-12)
  }

  test("degenerate spheres: r = 0 touches only when the center is on the triangle") {
    val triangle = Pga3dTriangle(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0), Pga3dPoint(0, 1, 0))
    assert(Pga3dSphere(Pga3dPoint(0.25, 0.25, 0), r = 0.0).intersects(triangle))
    assert(!Pga3dSphere(Pga3dPoint(0.25, 0.25, 0.001), r = 0.0).intersects(triangle))
  }
