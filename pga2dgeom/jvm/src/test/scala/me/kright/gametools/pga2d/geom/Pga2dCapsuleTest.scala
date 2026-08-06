package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dCapsuleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  // no filtering: degenerate triangles and edges must work too
  private val triangles: Gen[Pga2dTriangle] = Pga2dPhysicsGenerators.triangleIn(bounds)
  private val edges: Gen[Pga2dEdge] = Pga2dPhysicsGenerators.edgeIn(bounds)

  private val radii: Gen[Double] = Gen.oneOf(
    Pga2dVectorMathGenerators.doubleInRange(0.0, 1.0),
    Pga2dVectorMathGenerators.doubleInRange(0.0, 2.0 * halfSize),
  )

  private val capsules: Gen[Pga2dCapsule] =
    for {
      a <- Pga2dPhysicsGenerators.pointIn(bounds)
      b <- Pga2dPhysicsGenerators.pointIn(bounds)
      r <- radii
    } yield Pga2dCapsule(a, b, r)

  test("fromCenter and the center/halfAxis accessors are inverse") {
    forAll(
      Pga2dPhysicsGenerators.pointIn(bounds), Pga2dPhysicsGenerators.vectorIn(bounds), radii,
      MinSuccessful(1000)
    ) { (center, halfAxis, r) =>
      val capsule = Pga2dCapsule.fromCenter(center, halfAxis, r)
      val tolerance = 1e-12 * (1.0 + halfSize + halfAxis.norm)

      assert((capsule.center - center).norm <= tolerance, s"capsule = $capsule, center = $center")
      assert((capsule.halfAxis - halfAxis).norm <= tolerance, s"capsule = $capsule, halfAxis = $halfAxis")
      assert(capsule.r == r)
    }
  }

  test("a zero half axis gives the degenerate circle capsule") {
    forAll(Pga2dPhysicsGenerators.pointIn(bounds), radii, MinSuccessful(500)) { (center, r) =>
      val capsule = Pga2dCapsule.fromCenter(center, Pga2dVector(0, 0), r)
      assert(capsule.a == center && capsule.b == center)
      assert(capsule == Pga2dCapsule(Pga2dCircle(center, r)))
    }
  }

  test("toAABB equals the edge AABB expanded by r") {
    forAll(capsules, MinSuccessful(1000)) { capsule =>
      val aabb = capsule.toAABB
      assert(aabb == capsule.edge.toAABB.expand(capsule.r), s"capsule = $capsule")
      assert(aabb.contains(capsule.a) && aabb.contains(capsule.b), s"capsule = $capsule")
    }
  }

  /** an upper bound of the true distance by sampling the edge; within edge.magnitude / n of it */
  private def sampledDistance(triangle: Pga2dTriangle, edge: Pga2dEdge, n: Int): Double =
    (0 to n).map(i => triangle.distanceTo(edge.interpolatedPoint(i.toDouble / n))).min

  test("triangle-edge distance agrees with dense sampling of the edge") {
    val n = 256
    forAll(triangles, edges, MinSuccessful(500)) { (triangle, edge) =>
      val dist = triangle.distanceTo(edge)
      val sampled = sampledDistance(triangle, edge, n)
      val tolerance = 1e-9 * (1.0 + sampled)

      assert(dist <= sampled + tolerance,
        s"dist = $dist > sampled = $sampled, triangle = $triangle, edge = $edge")
      assert(sampled - dist <= edge.magnitude / n + tolerance,
        s"sampled = $sampled is too far above dist = $dist, triangle = $triangle, edge = $edge")
    }
  }

  test("an edge overlapping the triangle has exactly zero distance") {
    // an endpoint strictly inside
    val triangle = Pga2dTriangle(Pga2dPoint(0, 0), Pga2dPoint(10, 0), Pga2dPoint(0, 10))
    assert(triangle.distanceSquareTo(Pga2dEdge(Pga2dPoint(1, 1), Pga2dPoint(20, 20))) == 0.0)
    // both endpoints outside, the edge cuts through
    assert(triangle.distanceSquareTo(Pga2dEdge(Pga2dPoint(-5, 1), Pga2dPoint(20, 1))) == 0.0)

    // property: an edge through an interior point has zero distance
    val factor = Pga2dVectorMathGenerators.doubleInRange(0.05, 0.9)
    forAll(triangles.filter(_.area > 1e-6), factor, factor,
      Pga2dPhysicsGenerators.vectorIn(bounds).filter(_.normSquare > 1e-6), MinSuccessful(1000)
    ) { (t, f1, f2, dir) =>
      val inside = t.getInterpolatedPoint(f1, f2 * (1.0 - f1))
      val edge = Pga2dEdge(inside - dir, inside + dir)
      assert(t.distanceSquareTo(edge) == 0.0, s"triangle = $t, edge = $edge")
    }
  }

  test("zero-radius capsule behaves as its edge") {
    forAll(triangles, edges, MinSuccessful(1000)) { (t, edge) =>
      val capsule = Pga2dCapsule(edge.a, edge.b, 0.0)
      assert(capsule.intersects(t) == (t.distanceSquareTo(edge) == 0.0),
        s"triangle = $t, edge = $edge")
    }
  }

  test("capsule with coinciding a and b behaves as a circle") {
    forAll(Pga2dPhysicsGenerators.pointIn(bounds), radii, triangles, MinSuccessful(1000)) {
      (center, r, triangle) =>
        val capsule = Pga2dCapsule(center, center, r)
        val circle = Pga2dCircle(center, r)
        assert(capsule.intersects(triangle) == circle.intersects(triangle),
          s"center = $center, r = $r, triangle = $triangle")
    }
  }

  test("pairwise queries: symmetry and degenerate reductions") {
    forAll(capsules, capsules, MinSuccessful(1000)) { (c1, c2) =>
      assert(c1.intersects(c2) == c2.intersects(c1), s"c1 = $c1, c2 = $c2")

      val circle = Pga2dCircle(c2.a, c2.r)
      val degenerate = Pga2dCapsule(circle)
      assert(c1.intersects(degenerate) == c1.intersects(circle), s"c1 = $c1, circle = $circle")
      assert(circle.intersects(c1) == c1.intersects(circle), s"c1 = $c1, circle = $circle")

      val circle1 = Pga2dCircle(c1.a, c1.r)
      assert(Pga2dCapsule(circle1).intersects(degenerate) == circle1.intersects(circle),
        s"circle1 = $circle1, circle = $circle")
    }
  }

  test("degenerate triangle behaves as a segment for capsule queries") {
    forAll(
      Pga2dPhysicsGenerators.edgeIn(bounds).filter(_.magnitude > 1e-7),
      Pga2dVectorMathGenerators.doubleInRange(0.0, 1.0),
      edges, MinSuccessful(500)
    ) { (base, t, edge) =>
      val tc = t.max(0.0).min(1.0)
      val triangle = Pga2dTriangle(base.a, base.b, base.interpolatedPoint(tc))

      // the edge-edge reference approximates an extremely short segment (length ratio
      // beyond 1e8) by its center, overestimating by up to half of the shorter segment;
      // the triangle query has more candidates and may legitimately beat it
      val expectedDist = base.distanceTo(edge)
      val actualDist = triangle.distanceTo(edge)
      val tolerance = 1e-9 * (1.0 + expectedDist)
      assert(actualDist <= expectedDist + tolerance,
        s"actual = $actualDist, expected = $expectedDist, triangle = $triangle, edge = $edge")
      assert(actualDist >= expectedDist - 0.5 * Math.min(base.magnitude, edge.magnitude) - tolerance,
        s"actual = $actualDist is too far below expected = $expectedDist, triangle = $triangle, edge = $edge")
    }
  }

  test("triangle-edge distance scales linearly up to 1e+-30") {
    val scales = Gen.oneOf(1e-30, 1e-10, 1e10, 1e30)

    forAll(triangles, edges, scales, MinSuccessful(500)) { (triangle, edge, scale) =>
      def scaled(q: Pga2dPoint): Pga2dPoint = Pga2dPoint(q.x * scale, q.y * scale)

      val dist = triangle.distanceTo(edge)
      val distScaled = triangle.map(scaled).distanceTo(edge.map(scaled)) / scale
      assert((dist - distScaled).abs <= 1e-9 * (1.0 + dist) + 4.0 * halfSize * 1e-15,
        s"dist = $dist, distScaled = $distScaled, scale = $scale, triangle = $triangle, edge = $edge")
    }
  }
