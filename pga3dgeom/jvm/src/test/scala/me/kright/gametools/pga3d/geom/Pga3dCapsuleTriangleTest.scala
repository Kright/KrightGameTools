package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dCapsuleTriangleTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 1000
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  // no filtering: degenerate triangles and edges must work too
  private val triangles: Gen[Pga3dTriangle] = Pga3dPhysicsGenerators.triangleIn(bounds)
  private val edges: Gen[Pga3dEdge] = Pga3dPhysicsGenerators.edgeIn(bounds)

  private val radii: Gen[Double] = Gen.oneOf(
    Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0),
    Pga3dVectorMathGenerators.doubleInRange(0.0, 2.0 * halfSize),
  )

  /** an upper bound of the true distance by sampling the edge; within edge.magnitude / n of it */
  private def sampledDistance(triangle: Pga3dTriangle, edge: Pga3dEdge, n: Int): Double =
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

  test("triangle-edge nearest points lie on their shapes") {
    forAll(triangles, edges, MinSuccessful(1000)) { (triangle, edge) =>
      val (onTriangle, onEdge) = triangle.getNearestPoints(edge)
      val scene = 1.0 + triangle.perimeter + edge.magnitude

      assert(triangle.distanceSquareTo(onTriangle) <= 1e-18 * scene * scene,
        s"onTriangle = $onTriangle is not on the triangle, triangle = $triangle, edge = $edge")
      assert(edge.distanceSquareTo(onEdge) <= 1e-18 * scene * scene,
        s"onEdge = $onEdge is not on the edge, triangle = $triangle, edge = $edge")
    }
  }

  test("an edge piercing the triangle has exactly zero distance") {
    val factor = Pga3dVectorMathGenerators.doubleInRange(0.05, 0.9)

    forAll(triangles.filter(_.area > 1e-6), factor, factor,
      Pga3dPhysicsGenerators.vectorIn(bounds).filter(_.normSquare > 1e-6), MinSuccessful(1000)
    ) { (triangle, f1, f2, dir) =>
      val tba = f1
      val tca = f2 * (1.0 - f1) // strictly inside the triangle
      val inside = triangle.getInterpolatedPoint(tba, tca)
      val edge = Pga3dEdge(inside - dir, inside + dir)

      // the edge crosses the plane at an interior point (unless dir is parallel to the plane)
      val plane = triangle.normalizedPlane
      val cos = plane.x * dir.x + plane.y * dir.y + plane.z * dir.z
      if (cos.abs > 1e-6 * dir.norm) {
        assert(triangle.distanceSquareTo(edge) == 0.0,
          s"triangle = $triangle, edge = $edge")
      }
    }
  }

  test("zero-radius capsule behaves as its edge") {
    // piercing: exact zero distance, so even r = 0 intersects
    val triangle = Pga3dTriangle(Pga3dPoint(-10, -10, 0), Pga3dPoint(10, -10, 0), Pga3dPoint(0, 10, 0))
    assert(Pga3dCapsule(Pga3dPoint(0, 0, -1), Pga3dPoint(0, 0, 1), 0.0).intersects(triangle))
    // an endpoint exactly on the triangle (power-of-two coordinates: exact reconstruction)
    assert(Pga3dCapsule(Pga3dPoint(0.25, 0.25, 0.0), Pga3dPoint(0.25, 0.25, 1.0), 0.0)
      .intersects(Pga3dTriangle(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0), Pga3dPoint(0, 1, 0))))
    // strictly away: no intersection
    assert(!Pga3dCapsule(Pga3dPoint(0, 0, 1), Pga3dPoint(0, 0, 2), 0.0).intersects(triangle))

    // property: intersects(triangle) == (the edge-triangle distance is <= 0)
    forAll(triangles, edges, MinSuccessful(1000)) { (t, edge) =>
      val capsule = Pga3dCapsule(edge.a, edge.b, 0.0)
      assert(capsule.intersects(t) == (t.distanceSquareTo(edge) == 0.0),
        s"triangle = $t, edge = $edge")
    }
  }

  test("capsule with coinciding a and b behaves as a sphere") {
    forAll(Pga3dPhysicsGenerators.pointIn(bounds), radii, triangles, MinSuccessful(1000)) {
      (center, r, triangle) =>
        val capsule = Pga3dCapsule(center, center, r)
        val sphere = Pga3dSphere(center, r)

        assert(capsule.intersects(triangle) == sphere.intersects(triangle),
          s"center = $center, r = $r, triangle = $triangle")

        val capsuleContact = capsule.deepestContact(triangle)
        val sphereContact = sphere.deepestContact(triangle)
        assert(capsuleContact.isDefined == sphereContact.isDefined,
          s"capsule = $capsuleContact, sphere = $sphereContact, center = $center, r = $r, triangle = $triangle")

        for (cc <- capsuleContact; sc <- sphereContact) {
          val tolerance = 1e-12 * (1.0 + triangle.perimeter + r)
          assert((cc.point - sc.point).norm <= tolerance && (cc.normal - sc.normal).norm <= tolerance &&
            (cc.depth - sc.depth).abs <= tolerance,
            s"capsule = $cc, sphere = $sc, center = $center, r = $r, triangle = $triangle")
        }
    }
  }

  test("deepestContact is defined iff the capsule intersects the triangle") {
    forAll(edges, radii, triangles, MinSuccessful(1000)) { (edge, r, triangle) =>
      val capsule = Pga3dCapsule(edge.a, edge.b, r)
      // the only allowed exception: the axis exactly touching a degenerate triangle has no normal
      val exotic = triangle.area == 0.0 && triangle.distanceSquareTo(edge) == 0.0
      if (!exotic) {
        assert(capsule.deepestContact(triangle).isDefined == capsule.intersects(triangle),
          s"capsule = $capsule, triangle = $triangle")
      }
    }
  }

  test("non-piercing deepestContact is consistent: point on triangle, unit normal, axis reconstruction") {
    forAll(edges, radii, triangles, MinSuccessful(1000)) { (edge, r, triangle) =>
      val capsule = Pga3dCapsule(edge.a, edge.b, r)
      for (contact <- capsule.deepestContact(triangle) if contact.depth <= r) {
        val scene = 1.0 + triangle.perimeter + edge.magnitude + r

        assert(triangle.distanceSquareTo(contact.point) <= 1e-18 * scene * scene,
          s"contact point is not on the triangle: $contact, capsule = $capsule, triangle = $triangle")
        assert((contact.normal.norm - 1.0).abs <= 1e-12, s"normal is not unit: $contact")
        assert(contact.depth >= 0.0, s"negative depth: $contact")

        // the nearest axis point is reconstructed as point + normal * (r - depth)
        val onAxis = contact.point + contact.normal * (capsule.r - contact.depth)
        assert(capsule.edge.distanceSquareTo(onAxis) <= 1e-15 * scene * scene,
          s"axis point is not reconstructed: $contact, capsule = $capsule, triangle = $triangle")
      }
    }
  }

  test("piercing deepestContact unit case") {
    val triangle = Pga3dTriangle(Pga3dPoint(-10, -10, 0), Pga3dPoint(10, -10, 0), Pga3dPoint(0, 10, 0))
    // the axis crosses the triangle at the origin; the larger part is above (+z)
    val capsule = Pga3dCapsule(Pga3dPoint(0, 0, -2), Pga3dPoint(0, 0, 3), 0.5)

    val contact = capsule.deepestContact(triangle).get
    assert((contact.point - Pga3dPoint(0, 0, 0)).norm <= 1e-12, s"contact = $contact")
    assert((contact.normal - Pga3dVector(0, 0, 1)).norm <= 1e-12, s"contact = $contact")
    // depth = r + reach below the plane = 0.5 + 2
    assert((contact.depth - 2.5).abs <= 1e-12, s"contact = $contact")

    // flipped: the larger part below - the normal flips
    val flipped = Pga3dCapsule(Pga3dPoint(0, 0, 2), Pga3dPoint(0, 0, -3), 0.5)
    val flippedContact = flipped.deepestContact(triangle).get
    assert((flippedContact.normal - Pga3dVector(0, 0, -1)).norm <= 1e-12, s"contact = $flippedContact")
    assert((flippedContact.depth - 2.5).abs <= 1e-12, s"contact = $flippedContact")
  }

  test("resting capsule unit case") {
    val triangle = Pga3dTriangle(Pga3dPoint(-10, -10, 0), Pga3dPoint(10, -10, 0), Pga3dPoint(0, 10, 0))
    // horizontal axis at height 0.3 over the triangle interior, r = 0.5
    val capsule = Pga3dCapsule(Pga3dPoint(-1, 0, 0.3), Pga3dPoint(1, 0, 0.3), 0.5)

    val contact = capsule.deepestContact(triangle).get
    assert((contact.normal - Pga3dVector(0, 0, 1)).norm <= 1e-12, s"contact = $contact")
    assert((contact.depth - 0.2).abs <= 1e-12, s"contact = $contact")
    assert(contact.point.z.abs <= 1e-12 && contact.point.y.abs <= 1e-12, s"contact = $contact")
  }

  test("degenerate triangle behaves as a segment for capsule queries") {
    forAll(
      Pga3dPhysicsGenerators.edgeIn(bounds).filter(_.magnitude > 1e-7),
      Pga3dVectorMathGenerators.doubleInRange(0.0, 1.0),
      edges, radii, MinSuccessful(500)
    ) { (base, t, edge, r) =>
      val tc = t.max(0.0).min(1.0)
      val triangle = Pga3dTriangle(base.a, base.b, base.interpolatedPoint(tc))
      val capsule = Pga3dCapsule(edge.a, edge.b, r)

      // the edge-edge reference approximates an extremely short segment (length ratio
      // beyond 1e8) by its center, overestimating by up to half of the shorter segment;
      // the triangle query has more candidates and may legitimately beat it
      val expectedDist = base.distanceTo(edge)
      val actualDist = triangle.distanceTo(edge)
      val distTolerance = 1e-9 * (1.0 + expectedDist)
      assert(actualDist <= expectedDist + distTolerance,
        s"actual = $actualDist, expected = $expectedDist, triangle = $triangle, edge = $edge")
      assert(actualDist >= expectedDist - 0.5 * Math.min(base.magnitude, edge.magnitude) - distTolerance,
        s"actual = $actualDist is too far below expected = $expectedDist, triangle = $triangle, edge = $edge")

      val expectedDistSquare = expectedDist * expectedDist
      val rSum = expectedDist
      // consistency of intersects away from the boundary shell
      if ((rSum - r).abs > 1e-6 * (1.0 + rSum)) {
        assert(capsule.intersects(triangle) == (expectedDistSquare <= r * r),
          s"capsule = $capsule, triangle = $triangle")
      }
    }
  }

  test("triangle-edge distance scales linearly up to 1e+-30") {
    val scales = Gen.oneOf(1e-30, 1e-10, 1e10, 1e30)

    forAll(triangles, edges, scales, MinSuccessful(500)) { (triangle, edge, scale) =>
      def scaled(q: Pga3dPoint): Pga3dPoint = Pga3dPoint(q.x * scale, q.y * scale, q.z * scale)

      val dist = triangle.distanceTo(edge)
      val distScaled = triangle.map(scaled).distanceTo(edge.map(scaled)) / scale
      assert((dist - distScaled).abs <= 1e-9 * (1.0 + dist) + 4.0 * halfSize * 1e-15,
        s"dist = $dist, distScaled = $distScaled, scale = $scale, triangle = $triangle, edge = $edge")
    }
  }
