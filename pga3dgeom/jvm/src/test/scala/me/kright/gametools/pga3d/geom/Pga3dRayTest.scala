package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dRayTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val unitBox = Pga3dAABB(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 1, 1))

  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  test("ray through the box hits") {
    val ray = Pga3dRay(Pga3dPoint(-1, -1, -1), Pga3dVector(1, 1, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("ray pointing away from the box misses") {
    val ray = Pga3dRay(Pga3dPoint(-1, -1, -1), Pga3dVector(-1, -1, -1))
    assert(!ray.hasIntersection(unitBox))
  }

  test("ray with origin inside the box hits") {
    val ray = Pga3dRay(Pga3dPoint(0.5, 0.5, 0.5), Pga3dVector(0, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("axis-parallel ray inside the slab hits") {
    val ray = Pga3dRay(Pga3dPoint(0.5, 0.5, -1), Pga3dVector(0, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("axis-parallel ray outside the slab misses") {
    assert(!Pga3dRay(Pga3dPoint(2.0, 0.5, -1), Pga3dVector(0, 0, 1)).hasIntersection(unitBox))
    assert(!Pga3dRay(Pga3dPoint(-0.5, 0.5, -1), Pga3dVector(0, 0, 1)).hasIntersection(unitBox))
  }

  test("axis-parallel ray grazing the min face hits (0 * Inf = NaN case)") {
    // origin.x is exactly on the x = min.x face => (min.x - origin.x) * (1 / 0.0) = NaN
    val ray = Pga3dRay(Pga3dPoint(0.0, 0.5, -1), Pga3dVector(0, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("axis-parallel ray grazing the max face hits (0 * Inf = NaN case)") {
    val ray = Pga3dRay(Pga3dPoint(1.0, 0.5, -1), Pga3dVector(0, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("axis-parallel ray grazing along a box edge hits (NaN on two axes)") {
    val ray = Pga3dRay(Pga3dPoint(0.0, 0.0, -1), Pga3dVector(0, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("grazing ray with negative zero direction component hits") {
    val ray = Pga3dRay(Pga3dPoint(0.0, 0.5, -1), Pga3dVector(-0.0, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("grazing ray with subnormal direction component hits (reciprocal overflows to Inf)") {
    val ray = Pga3dRay(Pga3dPoint(0.0, 0.5, -1), Pga3dVector(Double.MinPositiveValue, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("box fully behind the ray misses") {
    val ray = Pga3dRay(Pga3dPoint(0.5, 0.5, 2), Pga3dVector(0, 0, 1))
    assert(!ray.hasIntersection(unitBox))
  }

  test("ray starting on the near face and pointing into the box hits") {
    val ray = Pga3dRay(Pga3dPoint(0.5, 0.5, 0.0), Pga3dVector(0, 0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("ray starting on the far face and pointing away misses (touch only at t = 0)") {
    val ray = Pga3dRay(Pga3dPoint(0.5, 0.5, 1.0), Pga3dVector(0, 0, 1))
    assert(!ray.hasIntersection(unitBox))
  }

  test("ray through any point of the aabb hits") {
    forAll(
      Pga3dPhysicsGenerators.aabbIn(bounds),
      Pga3dPhysicsGenerators.pointIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin) =>
      forAll(Pga3dPhysicsGenerators.pointIn(aabb), MinSuccessful(10)) { innerPoint =>
        val direction = innerPoint - rayOrigin
        if (direction.norm > 0) {
          val ray = Pga3dRay(rayOrigin, direction)
          if (aabb.contains(innerPoint, expand = -1e-6)) {
            assert(ray.hasIntersection(aabb))
          } else {
            // innerPoint is on (or within 1e-6 of) the boundary: an exact touch is not
            // guaranteed to survive rounding, so check against a slightly expanded box
            assert(ray.hasIntersection(aabb.expand(1e-6)))
          }
        }
      }
    }
  }

  test("axis-parallel rays grazing each face hit, rays shifted outside miss") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val c = aabb.center
      val belowZ = aabb.min.z - 1.0
      val dirZ = Pga3dVector(0, 0, 1)

      for (x <- Seq(aabb.min.x, aabb.max.x); y <- Seq(aabb.min.y, aabb.max.y, c.y)) {
        assert(Pga3dRay(Pga3dPoint(x, y, belowZ), dirZ).hasIntersection(aabb))
      }
      for (y <- Seq(aabb.min.y, aabb.max.y)) {
        assert(Pga3dRay(Pga3dPoint(c.x, y, belowZ), dirZ).hasIntersection(aabb))
      }

      assert(!Pga3dRay(Pga3dPoint(aabb.max.x + 1.0, c.y, belowZ), dirZ).hasIntersection(aabb))
      assert(!Pga3dRay(Pga3dPoint(aabb.min.x - 1.0, c.y, belowZ), dirZ).hasIntersection(aabb))
      assert(!Pga3dRay(Pga3dPoint(c.x, aabb.max.y + 1.0, belowZ), dirZ).hasIntersection(aabb))
    }
  }

  test("intersectionT returns entry t") {
    assert(Pga3dRay(Pga3dPoint(0.5, 0.5, -1), Pga3dVector(0, 0, 1)).intersectionT(unitBox) == 1.0)
    assert(Pga3dRay(Pga3dPoint(0.5, 0.5, -3), Pga3dVector(0, 0, 1)).intersectionT(unitBox) == 3.0)
  }

  test("intersectionT for unnormalized direction is measured in units of direction length") {
    assert(Pga3dRay(Pga3dPoint(0.5, 0.5, -1), Pga3dVector(0, 0, 2)).intersectionT(unitBox) == 0.5)
    assert(Pga3dRay.normalized(Pga3dPoint(0.5, 0.5, -1), Pga3dVector(0, 0, 2)).intersectionT(unitBox) == 1.0)
  }

  test("intersectionT is 0 when origin is inside") {
    assert(Pga3dRay(Pga3dPoint(0.5, 0.5, 0.5), Pga3dVector(0, 0, 1)).intersectionT(unitBox) == 0.0)
  }

  test("intersectionT is +Inf on miss") {
    assert(Pga3dRay(Pga3dPoint(2.0, 0.5, -1), Pga3dVector(0, 0, 1)).intersectionT(unitBox).isPosInfinity)
    assert(Pga3dRay(Pga3dPoint(0.5, 0.5, 2), Pga3dVector(0, 0, 1)).intersectionT(unitBox).isPosInfinity)
  }

  test("intersectionT for grazing ray returns entry t (NaN case)") {
    assert(Pga3dRay(Pga3dPoint(0.0, 0.5, -1), Pga3dVector(0, 0, 1)).intersectionT(unitBox) == 1.0)
  }

  test("intersectionT orders boxes along the ray") {
    val nearBox = Pga3dAABB(Pga3dPoint(0, 0, 2), Pga3dPoint(1, 1, 3))
    val farBox = Pga3dAABB(Pga3dPoint(0, 0, 5), Pga3dPoint(1, 1, 6))
    val ray = Pga3dRay(Pga3dPoint(0.5, 0.5, 0), Pga3dVector(0, 0, 1))

    assert(ray.intersectionT(nearBox) < ray.intersectionT(farBox))
  }

  test("intersectionT is finite exactly when hasIntersection is true") {
    forAll(
      Pga3dPhysicsGenerators.aabbIn(bounds),
      Pga3dPhysicsGenerators.pointIn(bounds),
      Pga3dPhysicsGenerators.vectorIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin, direction) =>
      whenever(direction.norm > 0) {
        val ray = Pga3dRay(rayOrigin, direction)
        assert(ray.hasIntersection(aabb) == !ray.intersectionT(aabb).isPosInfinity)
      }
    }
  }

  test("entry point at intersectionT lies on the aabb (within eps)") {
    forAll(
      Pga3dPhysicsGenerators.aabbIn(bounds),
      Pga3dPhysicsGenerators.pointIn(bounds),
      Pga3dPhysicsGenerators.vectorIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin, direction) =>
      whenever(direction.norm > 0) {
        val ray = Pga3dRay(rayOrigin, direction)
        val t = ray.intersectionT(aabb)
        if (!t.isPosInfinity) {
          assert(t >= 0.0)
          val entryPoint = rayOrigin.madd(direction, t)
          assert(aabb.contains(entryPoint, expand = 1e-8))
        }
      }
    }
  }

  test("if hasIntersection is false, no point along the ray is strictly inside the aabb") {
    forAll(
      Pga3dPhysicsGenerators.aabbIn(bounds),
      Pga3dPhysicsGenerators.pointIn(bounds),
      Pga3dPhysicsGenerators.vectorIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin, direction) =>
      whenever(direction.norm > 0) {
        val ray = Pga3dRay(rayOrigin, direction)
        if (!ray.hasIntersection(aabb)) {
          for (i <- 0 to 32) {
            val p = rayOrigin.madd(direction, i * 0.125)
            assert(!aabb.contains(p, expand = -1e-9))
          }
        }
      }
    }
  }
