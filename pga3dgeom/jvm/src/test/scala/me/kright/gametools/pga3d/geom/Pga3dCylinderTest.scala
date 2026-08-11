package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dPoint, Pga3dTranslator, Pga3dVector}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dCylinderTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  /** points densely sampling the cylinder surface: both cap rims, the cap centers and the side */
  private def surfacePoints(c: Pga3dCylinder): Seq[Pga3dPoint] =
    val axis = (c.b - c.a).normalizedByNorm
    val anyOrtho = if (Math.abs(axis.x) < 0.9) Pga3dVector(1, 0, 0) else Pga3dVector(0, 1, 0)
    val u = Pga3dVector.crossRightHanded(axis, anyOrtho).normalizedByNorm
    val v = Pga3dVector.crossRightHanded(axis, u)
    val points = for {
      angleStep <- 0 until 64
      angle = angleStep * (2.0 * Math.PI / 64)
      radial = (u * Math.cos(angle) + v * Math.sin(angle)) * c.r
      t <- Seq(0.0, 0.25, 0.5, 0.75, 1.0)
    } yield c.a + (c.b - c.a) * t + radial
    points ++ Seq(c.a, c.b)
  
  test("contains correctly identifies points inside the cylinder") {
    val a = Pga3dPoint(0, 0, 0)
    val b = Pga3dPoint(0, 0, 10)
    val r = 2.0
    val cylinder = Pga3dCylinder(a, b, r)
    
    // Points inside
    assert(cylinder.contains(Pga3dPoint(0, 0, 5))) // On the axis
    assert(cylinder.contains(Pga3dPoint(1, 0, 5))) // Inside, off axis
    assert(cylinder.contains(Pga3dPoint(0, 1.9, 5))) // Near the edge  
    
    // Points outside
    assert(!cylinder.contains(Pga3dPoint(0, 0, -1))) // Outside along axis
    assert(!cylinder.contains(Pga3dPoint(0, 0, 11))) // Outside along axis
    assert(!cylinder.contains(Pga3dPoint(2.1, 0, 5))) // Outside radially
    assert(!cylinder.contains(Pga3dPoint(10, 10, 10))) // Far outside
  }
  
  test("intersects correctly identifies edges that intersect the cylinder") {
    val a = Pga3dPoint(0, 0, 0)
    val b = Pga3dPoint(0, 0, 10)
    val r = 2.0
    val cylinder = Pga3dCylinder(a, b, r)
    
    // Edge passing through the cylinder
    val edge1 = Pga3dEdge(Pga3dPoint(-5, 0, 5), Pga3dPoint(5, 0, 5))
    assert(cylinder.intersects(edge1))
    
    // Edge touching the cylinder
    val edge2 = Pga3dEdge(Pga3dPoint(2, 0, 5), Pga3dPoint(5, 0, 5))
    assert(cylinder.intersects(edge2))
    
    // Edge not intersecting the cylinder
    val edge3 = Pga3dEdge(Pga3dPoint(3, 0, 5), Pga3dPoint(5, 0, 5))
    assert(!cylinder.intersects(edge3))
    
    // Edge with one endpoint inside the cylinder
    val edge4 = Pga3dEdge(Pga3dPoint(1, 0, 5), Pga3dPoint(5, 0, 5))
    assert(cylinder.intersects(edge4))
  }

  test("intersects rejects a separated edge whose axis projection partially overlaps the axis range") {
    val cylinder = Pga3dCylinder(Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 10), r = 2.0)

    // crosses the near cap plane far from the axis
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(5, 0, -5), Pga3dPoint(5, 0, 5))))
    // crosses the far cap plane far from the axis
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(5, 0, 5), Pga3dPoint(5, 0, 15))))
    // the near twins of the same edges do intersect
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(1, 0, -5), Pga3dPoint(1, 0, 5))))
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(1, 0, 5), Pga3dPoint(1, 0, 15))))
  }

  test("intersects clamps the edge exactly at the cap planes") {
    val cylinder = Pga3dCylinder(Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 10), r = 2.0)

    // edge.a is beyond the far cap; the edge reaches radius ~1.93 < r exactly at the far cap plane
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(0.3, 0, 12), Pga3dPoint(6, 0, 5))))
    // the mirrored case: edge.b is below the near cap
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(6, 0, 5), Pga3dPoint(0.3, 0, -2))))
    // the same two edges shifted outwards (radius ~4 at the cap plane) do not intersect
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(2.5, 0, 12), Pga3dPoint(8, 0, 5))))
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(8, 0, 5), Pga3dPoint(2.5, 0, -2))))
  }

  test("fromCenter, center, halfAxis and edge round-trip") {
    val cylinder = Pga3dCylinder.fromCenter(Pga3dPoint(1, 2, 3), Pga3dVector(0.5, -1, 2), r = 0.7)
    assert(cylinder.center == Pga3dPoint(1, 2, 3))
    assert(cylinder.halfAxis == Pga3dVector(0.5, -1, 2))
    assert(cylinder.edge == Pga3dEdge(cylinder.a, cylinder.b))
    assert(cylinder.r == 0.7)
  }

  test("expand grows the radius, expandAxis moves the caps outwards") {
    val cylinder = Pga3dCylinder(Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 10), r = 1.0)

    val wider = cylinder.expand(0.5)
    assert(wider.a == cylinder.a && wider.b == cylinder.b && wider.r == 1.5)
    assert(wider.contains(Pga3dPoint(1.4, 0, 5)) && !cylinder.contains(Pga3dPoint(1.4, 0, 5)))

    val longer = cylinder.expandAxis(2.0)
    assert(longer.r == cylinder.r)
    assert((longer.a - Pga3dPoint(0, 0, -2)).norm < 1e-15)
    assert((longer.b - Pga3dPoint(0, 0, 12)).norm < 1e-15)
    assert(longer.contains(Pga3dPoint(0, 0, -1)) && !cylinder.contains(Pga3dPoint(0, 0, -1)))
  }

  test("toAABB is an exact bound of a slanted cylinder") {
    val cylinder = Pga3dCylinder(Pga3dPoint(1, -2, 0.5), Pga3dPoint(4, 3, -1), r = 1.3)
    val aabb = cylinder.toAABB

    val points = surfacePoints(cylinder)
    // the box is exact, so a rim point can land on the face within a rounding error
    val slightlyExpanded = aabb.expand(1e-12)
    for (p <- points) {
      assert(slightlyExpanded.contains(p), s"surface point $p is outside $aabb")
    }
    // the box is tight: every face is touched by the sampled surface (64 angles => ~1e-3 slack)
    val eps = 0.01
    assert(points.exists(_.x < aabb.min.x + eps))
    assert(points.exists(_.y < aabb.min.y + eps))
    assert(points.exists(_.z < aabb.min.z + eps))
    assert(points.exists(_.x > aabb.max.x - eps))
    assert(points.exists(_.y > aabb.max.y - eps))
    assert(points.exists(_.z > aabb.max.z - eps))
  }

  test("toAABB of a degenerate cylinder still bounds the disk") {
    val disk = Pga3dCylinder(Pga3dPoint(1, 2, 3), Pga3dPoint(1, 2, 3), r = 2.0)
    val aabb = disk.toAABB
    assert(aabb.contains(Pga3dPoint(3, 2, 3)))
    assert(aabb.contains(Pga3dPoint(1, 2, 5)))
    assert(!aabb.contains(Pga3dPoint(3.1, 2, 3)))
  }

  test("boundingSphere contains the whole cylinder and is tight at the rims") {
    val cylinder = Pga3dCylinder(Pga3dPoint(1, -2, 0.5), Pga3dPoint(4, 3, -1), r = 1.3)
    val sphere = cylinder.boundingSphere
    for (p <- surfacePoints(cylinder)) {
      assert((p - sphere.center).norm <= sphere.r * (1.0 + 1e-12), s"surface point $p is outside $sphere")
    }
    // a cap rim point lies exactly on the sphere
    val rimDistances = surfacePoints(cylinder).map(p => (p - sphere.center).norm)
    assert(Math.abs(rimDistances.max - sphere.r) < 1e-12)
  }

  test("sandwich moves the cylinder rigidly") {
    val cylinder = Pga3dCylinder(Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 10), r = 2.0)
    val motor = Pga3dTranslator.addVector(Pga3dVector(1, 2, 3)).toMotor
      .geometric(Pga3dBivector(xy = 0.4).exp)
    val moved = motor.sandwich(cylinder)

    assert(moved.r == cylinder.r)
    val testPoints = Seq(
      Pga3dPoint(0, 0, 5), Pga3dPoint(1.9, 0, 5), Pga3dPoint(2.1, 0, 5),
      Pga3dPoint(0, 0, -1), Pga3dPoint(0, 0, 11), Pga3dPoint(0, 1.5, 0.1))
    for (p <- testPoints) {
      assert(moved.contains(motor.sandwich(p).toPointUnsafe) == cylinder.contains(p), s"p = $p")
    }
  }
