package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dRayTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val unitBox = Pga2dAABB(Pga2dPoint(0, 0), Pga2dPoint(1, 1))

  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  test("ray through the box hits") {
    val ray = Pga2dRay(Pga2dPoint(-1, -1), Pga2dVector(1, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("ray pointing away from the box misses") {
    val ray = Pga2dRay(Pga2dPoint(-1, -1), Pga2dVector(-1, -1))
    assert(!ray.hasIntersection(unitBox))
  }

  test("ray with origin inside the box hits") {
    val ray = Pga2dRay(Pga2dPoint(0.5, 0.5), Pga2dVector(0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("axis-parallel ray inside the slab hits") {
    val ray = Pga2dRay(Pga2dPoint(0.5, -1), Pga2dVector(0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("axis-parallel ray outside the slab misses") {
    assert(!Pga2dRay(Pga2dPoint(2.0, -1), Pga2dVector(0, 1)).hasIntersection(unitBox))
    assert(!Pga2dRay(Pga2dPoint(-0.5, -1), Pga2dVector(0, 1)).hasIntersection(unitBox))
  }

  test("axis-parallel ray grazing the min side hits (0 * Inf = NaN case)") {
    // origin.x is exactly on the x = min.x side => (min.x - origin.x) * (1 / 0.0) = NaN
    val ray = Pga2dRay(Pga2dPoint(0.0, -1), Pga2dVector(0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("axis-parallel ray grazing the max side hits (0 * Inf = NaN case)") {
    val ray = Pga2dRay(Pga2dPoint(1.0, -1), Pga2dVector(0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("grazing ray with negative zero direction component hits") {
    val ray = Pga2dRay(Pga2dPoint(0.0, -1), Pga2dVector(-0.0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("grazing ray with subnormal direction component hits (reciprocal overflows to Inf)") {
    val ray = Pga2dRay(Pga2dPoint(0.0, -1), Pga2dVector(Double.MinPositiveValue, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("box fully behind the ray misses") {
    val ray = Pga2dRay(Pga2dPoint(0.5, 2), Pga2dVector(0, 1))
    assert(!ray.hasIntersection(unitBox))
  }

  test("ray starting on the near side and pointing into the box hits") {
    val ray = Pga2dRay(Pga2dPoint(0.5, 0.0), Pga2dVector(0, 1))
    assert(ray.hasIntersection(unitBox))
  }

  test("ray starting on the far side and pointing away misses (touch only at t = 0)") {
    val ray = Pga2dRay(Pga2dPoint(0.5, 1.0), Pga2dVector(0, 1))
    assert(!ray.hasIntersection(unitBox))
  }

  test("ray through any point of the aabb hits") {
    forAll(
      Pga2dPhysicsGenerators.aabbIn(bounds),
      Pga2dPhysicsGenerators.pointIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin) =>
      forAll(Pga2dPhysicsGenerators.pointIn(aabb), MinSuccessful(10)) { innerPoint =>
        val direction = innerPoint - rayOrigin
        if (direction.norm > 0) {
          val ray = Pga2dRay(rayOrigin, direction)
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

  test("axis-parallel rays grazing each side hit, rays shifted outside miss") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val c = aabb.center
      val belowY = aabb.min.y - 1.0
      val dirY = Pga2dVector(0, 1)

      for (x <- Seq(aabb.min.x, aabb.max.x, c.x)) {
        assert(Pga2dRay(Pga2dPoint(x, belowY), dirY).hasIntersection(aabb))
      }

      assert(!Pga2dRay(Pga2dPoint(aabb.max.x + 1.0, belowY), dirY).hasIntersection(aabb))
      assert(!Pga2dRay(Pga2dPoint(aabb.min.x - 1.0, belowY), dirY).hasIntersection(aabb))
    }
  }

  test("if hasIntersection is false, no point along the ray is strictly inside the aabb") {
    forAll(
      Pga2dPhysicsGenerators.aabbIn(bounds),
      Pga2dPhysicsGenerators.pointIn(bounds),
      Pga2dPhysicsGenerators.vectorIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin, direction) =>
      whenever(direction.norm > 0) {
        val ray = Pga2dRay(rayOrigin, direction)
        if (!ray.hasIntersection(aabb)) {
          for (i <- 0 to 32) {
            val p = rayOrigin.madd(direction, i * 0.125)
            assert(!aabb.contains(p, expand = -1e-9))
          }
        }
      }
    }
  }

  test("intersectionT returns entry t") {
    assert(Pga2dRay(Pga2dPoint(0.5, -1), Pga2dVector(0, 1)).intersectionT(unitBox) == 1.0)
    assert(Pga2dRay(Pga2dPoint(0.5, -3), Pga2dVector(0, 1)).intersectionT(unitBox) == 3.0)
  }

  test("intersectionT for unnormalized direction is measured in units of direction length") {
    assert(Pga2dRay(Pga2dPoint(0.5, -1), Pga2dVector(0, 2)).intersectionT(unitBox) == 0.5)
    assert(Pga2dRay.normalized(Pga2dPoint(0.5, -1), Pga2dVector(0, 2)).intersectionT(unitBox) == 1.0)
  }

  test("intersectionT is 0 when origin is inside") {
    assert(Pga2dRay(Pga2dPoint(0.5, 0.5), Pga2dVector(0, 1)).intersectionT(unitBox) == 0.0)
  }

  test("intersectionT is +Inf on miss") {
    assert(Pga2dRay(Pga2dPoint(2.0, -1), Pga2dVector(0, 1)).intersectionT(unitBox).isPosInfinity)
    assert(Pga2dRay(Pga2dPoint(0.5, 2), Pga2dVector(0, 1)).intersectionT(unitBox).isPosInfinity)
  }

  test("intersectionT for grazing ray returns entry t (NaN case)") {
    assert(Pga2dRay(Pga2dPoint(0.0, -1), Pga2dVector(0, 1)).intersectionT(unitBox) == 1.0)
  }

  test("intersectionT orders boxes along the ray") {
    val nearBox = Pga2dAABB(Pga2dPoint(0, 2), Pga2dPoint(1, 3))
    val farBox = Pga2dAABB(Pga2dPoint(0, 5), Pga2dPoint(1, 6))
    val ray = Pga2dRay(Pga2dPoint(0.5, 0), Pga2dVector(0, 1))

    assert(ray.intersectionT(nearBox) < ray.intersectionT(farBox))
  }

  test("intersectionT is finite exactly when hasIntersection is true") {
    forAll(
      Pga2dPhysicsGenerators.aabbIn(bounds),
      Pga2dPhysicsGenerators.pointIn(bounds),
      Pga2dPhysicsGenerators.vectorIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin, direction) =>
      whenever(direction.norm > 0) {
        val ray = Pga2dRay(rayOrigin, direction)
        assert(ray.hasIntersection(aabb) == !ray.intersectionT(aabb).isPosInfinity)
      }
    }
  }

  test("entry point at intersectionT lies on the aabb (within eps)") {
    forAll(
      Pga2dPhysicsGenerators.aabbIn(bounds),
      Pga2dPhysicsGenerators.pointIn(bounds),
      Pga2dPhysicsGenerators.vectorIn(bounds),
      MinSuccessful(1000)
    ) { (aabb, rayOrigin, direction) =>
      whenever(direction.norm > 0) {
        val ray = Pga2dRay(rayOrigin, direction)
        val t = ray.intersectionT(aabb)
        if (!t.isPosInfinity) {
          assert(t >= 0.0)
          val entryPoint = rayOrigin.madd(direction, t)
          assert(aabb.contains(entryPoint, expand = 1e-8))
        }
      }
    }
  }
