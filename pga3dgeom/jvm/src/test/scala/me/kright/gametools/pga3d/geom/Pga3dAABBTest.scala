package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dTranslator, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dAABBTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  test("aabb contains it's vertices and center") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      assert(aabb.contains(aabb.center))

      aabb.vertices.foreach { vertex =>
        assert(aabb.contains(vertex))
      }
    }
  }

  test("aabb contains itself") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      assert(aabb.contains(aabb))
    }
  }

  test("aabb intersection relation is symmetric") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { (aabb, aabb2) =>
      val b1 = aabb.intersects(aabb2)
      val b2 = aabb2.intersects(aabb)
      assert(b1 == b2)
    }
  }

  test("aabb union contains all parts") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { (aabb, aabb2) =>
      val union = aabb.union(aabb2)
      assert(union.contains(aabb))
      assert(union.contains(aabb2))
    }
  }

  test("if contains then intersects") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds).flatMap {
      outer => Pga3dPhysicsGenerators.aabbIn(outer).map(inner => (outer, inner))
    }, MinSuccessful(1000)) { (outer, inner) =>
      assert(outer.contains(inner, expand = 1e-12))

      assert(outer.intersects(inner, expand = 1e-12))
      assert(inner.intersects(outer, expand = 1e-12))

      assert(outer.contains(inner, expand = 0.0) == outer.contains(inner))

      if (outer.contains(inner)) {
        assert(outer.intersects(inner))
        assert(inner.intersects(outer))
      }
    }
  }

  test("size calculation is correct") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val expectedSize = aabb.max - aabb.min
      assert(aabb.size == expectedSize)
    }
  }

  test("halfSize calculation is correct") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val expectedHalfSize = (aabb.max - aabb.min) * 0.5
      assert(aabb.halfSize == expectedHalfSize)
    }
  }

  test("volume calculation is correct") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val size = aabb.max - aabb.min
      val expectedVolume = size.x * size.y * size.z
      assert(aabb.volume == expectedVolume)
    }
  }

  test("surfaceArea calculation is correct") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val size = aabb.max - aabb.min
      val expectedArea = 2.0 * (size.x * size.y + size.y * size.z + size.z * size.x)
      assert(aabb.surfaceArea == expectedArea)
    }
  }

  test("clamp keeps points inside the AABB") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), Pga3dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { (aabb, point) =>
      val clampedPoint = aabb.clamp(point)
      assert(aabb.contains(clampedPoint))
    }
  }

  test("clamp doesn't change points already inside the AABB") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds).flatMap { aabb =>
      Pga3dPhysicsGenerators.pointIn(aabb).map(point => (aabb, point))
    }, MinSuccessful(1000)) { (aabb, point) =>
      assert(aabb.contains(point, expand = 1e-12))

      if (aabb.contains(point)) {
        val clampedPoint = aabb.clamp(point)
        assert(clampedPoint == point)
      }
    }
  }

  test("distanceToPoint is zero for points inside or on the boundary") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds).flatMap { aabb =>
      Pga3dPhysicsGenerators.pointIn(aabb).map(point => (aabb, point))
    }, MinSuccessful(1000)) { (aabb, point) =>
      assert(aabb.distanceTo(point) == 0.0)
    }
  }

  test("distanceToPoint is positive for points outside") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), Pga3dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { (aabb, point) =>
      if (!aabb.contains(point)) {
        val distance = aabb.distanceTo(point)
        assert(distance > 0.0)

        // The distance should be the distance to the closest point on the AABB
        val clampedPoint = aabb.clamp(point)
        val expectedDistance = (clampedPoint - point).norm
        assert(distance == expectedDistance)
      }
    }
  }

  test("expand increases the size by the specified amount in all directions") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), Pga3dVectorMathGenerators.doubleInRange(0.1, 10.0), MinSuccessful(1000)) { (aabb, amount) =>
      val expanded = aabb.expand(amount)

      // Check that the expanded AABB is larger by the expected amount
      assert(expanded.min.x == aabb.min.x - amount)
      assert(expanded.min.y == aabb.min.y - amount)
      assert(expanded.min.z == aabb.min.z - amount)
      assert(expanded.max.x == aabb.max.x + amount)
      assert(expanded.max.y == aabb.max.y + amount)
      assert(expanded.max.z == aabb.max.z + amount)

      // Check that the original AABB is contained in the expanded one
      assert(expanded.contains(aabb))
    }
  }

  test("apply with single point creates AABB with same min and max") {
    forAll(Pga3dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { point =>
      val aabb = Pga3dAABB(point)
      assert(aabb.min == point)
      assert(aabb.max == point)
      assert(aabb.volume == 0.0)
      assert(aabb.contains(point))
    }
  }

  test("apply with iterable of points creates AABB containing all points") {
    val pointsGen = Gen.listOfN(10, Pga3dPhysicsGenerators.pointIn(bounds))

    forAll(pointsGen, MinSuccessful(1000)) { points =>
      whenever(points.nonEmpty) {
        val aabb = Pga3dAABB(points)

        // Check that all points are contained in the AABB
        points.foreach { point =>
          assert(aabb.contains(point))
        }

        // Check that the AABB has the correct min and max
        val expectedMin = Pga3dPoint(
          points.map(_.x).min,
          points.map(_.y).min,
          points.map(_.z).min
        )
        val expectedMax = Pga3dPoint(
          points.map(_.x).max,
          points.map(_.y).max,
          points.map(_.z).max
        )

        assert(aabb.min == expectedMin)
        assert(aabb.max == expectedMax)
      }
    }
  }

  test("translator sandwich transforms AABB correctly") {
    val translatorGen = for {
      x <- Pga3dVectorMathGenerators.doubleInRange(-10, 10)
      y <- Pga3dVectorMathGenerators.doubleInRange(-10, 10)
      z <- Pga3dVectorMathGenerators.doubleInRange(-10, 10)
    } yield Pga3dTranslator(x, y, z)

    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), translatorGen, MinSuccessful(1000)) { (aabb, translator) =>
      val transformed = translator.sandwich(aabb)

      val expectedMin = translator.sandwich(aabb.min)
      val expectedMax = translator.sandwich(aabb.max)

      assert(transformed.min == expectedMin)
      assert(transformed.max == expectedMax)

      // Check that all transformed vertices are contained in the transformed AABB
      aabb.vertices.foreach { vertex =>
        val transformedVertex = translator.sandwich(vertex)
        assert(transformed.contains(transformedVertex))
      }
    }
  }

  test("methods for finding intersection of Pga3dAABB and Pga3dEdge returns result inside AABB") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), Pga3dPhysicsGenerators.edgeIn(bounds), MinSuccessful(10000)) { (aabb, edge) =>
      aabb.intersection(edge) match
        case Some(intersection) =>
          assert(aabb.contains(intersection, expand = 1e-10),
            s"""
               |distance to a = ${aabb.distanceTo(intersection.a)}
               |distance to b = ${aabb.distanceTo(intersection.b)}
               |AABB = ${aabb},
               |edge = ${edge},
               |intersection = ${intersection}""".stripMargin)
        case None => ()
    }
  }

  test("methods for intersection of Pga3dAABB and Pga3dEdge and for finding intersection are the same") {
    forAll(Pga3dPhysicsGenerators.aabbIn(bounds), Pga3dPhysicsGenerators.edgeIn(bounds), MinSuccessful(10000)) { (aabb, edge) =>
      val intersection = aabb.intersection(edge)
      val intersects = aabb.intersects(edge)

      assert(intersects == intersection.isDefined, s"\nintersection = ${intersection},\nintersects = $intersects")

      if (aabb.contains(edge.a) || aabb.contains(edge.b)) {
        assert(intersects)
      }

      if (aabb.contains(edge.a) && aabb.contains(edge.b)) {
        assert(intersection.get == edge, s"\nintersection = $intersection, \nedge = $edge, \naabb = $aabb")
      }
    }
  }

  test("aabb intersecion with big triangles") {
    val triangles = Seq(
      Pga3dTriangle(Pga3dPoint(10, 0, 10), Pga3dPoint(10, 0, -10), Pga3dPoint(-10, 0, 10)),
      Pga3dTriangle(Pga3dPoint(-10, 0, -10), Pga3dPoint(10, 0, -10), Pga3dPoint(-10, 0, 10)),
    )

    for (i <- -10 to 10; j <- -10 to 10) {
      val aabb = Pga3dAABB(Pga3dPoint(i, 0.0, j)).expand(0.5)

      assert(triangles.exists { t => aabb.intersects(t, eps = 1e-12) })
    }
  }

  test("check") {
    val triangle = Pga3dTriangle(a = Pga3dPoint(x = -10.0, y = 0.0, z = -10.0), b = Pga3dPoint(x = 10.0, y = 0.0, z = 10.0), c = Pga3dPoint(x = 10.0, y = 0.0, z = -10.0))
    val volume = Pga3dAABB(min = Pga3dPoint(x = -5.2727272727272725, y = -1.0, z = -9.09090909090909), max = Pga3dPoint(x = -2.3636363636363633, y = 1.0, z = -6.181818181818181))
    volume.intersects(triangle, eps = 1e-15)
  }

  test("check triangle intersection") {
    val edge = Pga3dEdge(a = Pga3dPoint(x = -5.2727272727272725, y = -1.0, z = -9.09090909090909), b = Pga3dPoint(x = -5.2727272727272725, y = 1.0, z = -9.09090909090909))
    val triangle = Pga3dTriangle(a = Pga3dPoint(x = -10.0, y = 0.0, z = -10.0), b = Pga3dPoint(x = 10.0, y = 0.0, z = 10.0), c = Pga3dPoint(x = 10.0, y = 0.0, z = -10.0))
    assert(triangle.intersects(edge, eps = 1e-15))
  }
