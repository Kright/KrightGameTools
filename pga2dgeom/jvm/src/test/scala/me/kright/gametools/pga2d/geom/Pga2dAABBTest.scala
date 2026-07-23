package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dTranslator, Pga2dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dAABBTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  test("aabb contains it's vertices and center") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      assert(aabb.contains(aabb.center))

      aabb.vertices.foreach { vertex =>
        assert(aabb.contains(vertex))
      }
    }
  }

  test("aabb contains itself") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      assert(aabb.contains(aabb))
    }
  }

  test("aabb intersection relation is symmetric") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { (aabb, aabb2) =>
      val b1 = aabb.intersects(aabb2)
      val b2 = aabb2.intersects(aabb)
      assert(b1 == b2)
    }
  }

  test("aabb union contains all parts") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { (aabb, aabb2) =>
      val union = aabb.union(aabb2)
      assert(union.contains(aabb))
      assert(union.contains(aabb2))
    }
  }

  test("if contains then intersects") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds).flatMap {
      outer => Pga2dPhysicsGenerators.aabbIn(outer).map(inner => (outer, inner))
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
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val expectedSize = aabb.max - aabb.min
      assert(aabb.size == expectedSize)
    }
  }

  test("halfSize calculation is correct") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val expectedHalfSize = (aabb.max - aabb.min) * 0.5
      assert(aabb.halfSize == expectedHalfSize)
    }
  }

  test("area calculation is correct") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val size = aabb.max - aabb.min
      val expectedArea = size.x * size.y
      assert(aabb.area == expectedArea)
    }
  }

  test("perimeter calculation is correct") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), MinSuccessful(1000)) { aabb =>
      val size = aabb.max - aabb.min
      val expectedPerimeter = 2.0 * (size.x + size.y)
      assert(aabb.perimeter == expectedPerimeter)
    }
  }

  test("clamp keeps points inside the AABB") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), Pga2dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { (aabb, point) =>
      val clampedPoint = aabb.clamp(point)
      assert(aabb.contains(clampedPoint))
    }
  }

  test("clamp doesn't change points already inside the AABB") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds).flatMap { aabb =>
      Pga2dPhysicsGenerators.pointIn(aabb).map(point => (aabb, point))
    }, MinSuccessful(1000)) { (aabb, point) =>
      assert(aabb.contains(point, expand = 1e-12))

      if (aabb.contains(point)) {
        val clampedPoint = aabb.clamp(point)
        assert(clampedPoint == point)
      }
    }
  }

  test("distanceToPoint is zero for points inside or on the boundary") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds).flatMap { aabb =>
      Pga2dPhysicsGenerators.pointIn(aabb).map(point => (aabb, point))
    }, MinSuccessful(1000)) { (aabb, point) =>
      assert(aabb.distanceTo(point) == 0.0)
    }
  }

  test("distanceToPoint is positive for points outside") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), Pga2dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { (aabb, point) =>
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
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), Pga2dVectorMathGenerators.doubleInRange(0.1, 10.0), MinSuccessful(1000)) { (aabb, amount) =>
      val expanded = aabb.expand(amount)

      // Check that the expanded AABB is larger by the expected amount
      assert(expanded.min.x == aabb.min.x - amount)
      assert(expanded.min.y == aabb.min.y - amount)
      assert(expanded.max.x == aabb.max.x + amount)
      assert(expanded.max.y == aabb.max.y + amount)

      // Check that the original AABB is contained in the expanded one
      assert(expanded.contains(aabb))
    }
  }

  test("apply with single point creates AABB with same min and max") {
    forAll(Pga2dPhysicsGenerators.pointIn(bounds), MinSuccessful(1000)) { point =>
      val aabb = Pga2dAABB(point)
      assert(aabb.min == point)
      assert(aabb.max == point)
      assert(aabb.area == 0.0)
      assert(aabb.contains(point))
    }
  }

  test("apply with iterable of points creates AABB containing all points") {
    val pointsGen = Gen.listOfN(10, Pga2dPhysicsGenerators.pointIn(bounds))

    forAll(pointsGen, MinSuccessful(1000)) { points =>
      whenever(points.nonEmpty) {
        val aabb = Pga2dAABB(points)

        // Check that all points are contained in the AABB
        points.foreach { point =>
          assert(aabb.contains(point))
        }

        // Check that the AABB has the correct min and max
        val expectedMin = Pga2dPoint(
          points.map(_.x).min,
          points.map(_.y).min
        )
        val expectedMax = Pga2dPoint(
          points.map(_.x).max,
          points.map(_.y).max
        )

        assert(aabb.min == expectedMin)
        assert(aabb.max == expectedMax)
      }
    }
  }

  test("translator sandwich transforms AABB correctly") {
    val translatorGen = for {
      wx <- Pga2dVectorMathGenerators.doubleInRange(-10, 10)
      wy <- Pga2dVectorMathGenerators.doubleInRange(-10, 10)
    } yield Pga2dTranslator(wx, wy)

    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), translatorGen, MinSuccessful(1000)) { (aabb, translator) =>
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

  test("methods for finding intersection of Pga2dAABB and Pga2dEdge returns result inside AABB") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), Pga2dPhysicsGenerators.edgeIn(bounds), MinSuccessful(10000)) { (aabb, edge) =>
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

  test("methods for intersection of Pga2dAABB and Pga2dEdge and for finding intersection are the same") {
    forAll(Pga2dPhysicsGenerators.aabbIn(bounds), Pga2dPhysicsGenerators.edgeIn(bounds), MinSuccessful(10000)) { (aabb, edge) =>
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

  test("aabb intersection with big triangles") {
    val triangles = Seq(
      Pga2dTriangle(Pga2dPoint(10, 10), Pga2dPoint(10, -10), Pga2dPoint(-10, 10)),
      Pga2dTriangle(Pga2dPoint(-10, -10), Pga2dPoint(10, -10), Pga2dPoint(-10, 10)),
    )

    for (i <- -10 to 10; j <- -10 to 10) {
      val aabb = Pga2dAABB(Pga2dPoint(i, j)).expand(0.5)

      assert(triangles.exists { t => aabb.intersects(t, eps = 1e-12) })
    }
  }

  test("aabb inside triangle intersects it") {
    val triangle = Pga2dTriangle(Pga2dPoint(-10, -10), Pga2dPoint(10, -10), Pga2dPoint(0, 10))
    val aabb = Pga2dAABB(Pga2dPoint(0, 0)).expand(0.5)

    assert(aabb.intersects(triangle, eps = 1e-12))
    assert(aabb.expand(100).intersects(triangle, eps = 1e-12)) // triangle fully inside aabb
    assert(!Pga2dAABB(Pga2dPoint(100, 100)).expand(0.5).intersects(triangle, eps = 1e-12))
  }
