package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import scala.collection.immutable.ArraySeq

class ProjectionTest extends AnyFunSuiteLike:
  private def x: Pga3dVector = Pga3dVector(1, 0, 0)

  private def y: Pga3dVector = Pga3dVector(0, 1, 0)

  private def z: Pga3dVector = Pga3dVector(0, 0, 1)

  test("project point onto line") {
    val px: Double = 1.0
    val py: Double = 2.0
    val pz: Double = 3.0

    val p = Pga3dPoint(px, py, pz).toProjectivePoint

    val center = Pga3dPoint(10.0, 20.0, 30.0).toProjectivePoint

    val cx = center v x
    val cy = center v y
    val cz = center v z

    assert(p.projectOntoLine(cx) == Pga3dProjectivePoint(px, center.y, center.z, w = 1.0))
    assert(p.projectOntoLine(cy) == Pga3dProjectivePoint(center.x, py, center.z, w = 1.0))
    assert(p.projectOntoLine(cz) == Pga3dProjectivePoint(center.x, center.y, pz, w = 1.0))

    assert(p.projectOntoLine(center v x).w == 1.0)
    assert(p.projectOntoLine(center v -x).w == 1.0)
  }

  test("project point onto plane") {
    val px: Double = 1.0
    val py: Double = 2.0
    val pz: Double = 3.0

    val p = Pga3dPoint(px, py, pz).toProjectivePoint

    val center = Pga3dPoint(10.0, 20.0, 30.0).toProjectivePoint

    val cxy = center v x v y
    val cxz = center v x v z
    val cyz = center v y v z

    assert(p.projectOntoPlane(cxy) == Pga3dProjectivePoint(px, py, center.z, w = 1.0))
    assert(p.projectOntoPlane(cxz) == Pga3dProjectivePoint(px, center.y, pz, w = 1.0))
    assert(p.projectOntoPlane(cyz) == Pga3dProjectivePoint(center.x, py, pz, w = 1.0))

    assert(p.projectOntoPlane(center v x v y).w == 1.0)
    assert(p.projectOntoPlane(center v x v -y).w == 1.0)
  }

  test("project point onto central plane") {
    val projected = Pga3dPoint(3, 4, 5).projectOntoPlane(Pga3dPlaneCentral(0, 0, 1)) // the plane z = 0
    assert(projected.w == 1.0)
    assert(projected.toPointUnsafe == Pga3dPoint(3, 4, 0))
  }

  test("project line onto plane") {
    val px: Double = 1.0
    val py: Double = 2.0
    val pz: Double = 3.0

    val p = Pga3dPoint(px, py, pz).toProjectivePoint

    val center = Pga3dPoint(10.0, 20.0, 30.0).toProjectivePoint

    val cxy = center v x v y
    val cxz = center v x v z
    val cyz = center v y v z

    val p1 = p
    val p2 = p1 + x + y + z

    for (plane <- ArraySeq(cxy, cxz, cyz)) {
      val line = p1 v p2
      val projectedLine = line.projectOntoPlane(plane)
      val lineOnProjectedPoints = p1.projectOntoPlane(plane) v p2.projectOntoPlane(plane)

      assert((lineOnProjectedPoints - projectedLine).norm < 1e-10)
    }
  }
