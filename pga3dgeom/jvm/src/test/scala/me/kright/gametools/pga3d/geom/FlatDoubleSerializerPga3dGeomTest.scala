package me.kright.gametools.pga3d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.pga3d.FlatDoubleSerializerPga3dTest.myCheck
import me.kright.gametools.pga3d.Pga3dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FlatDoubleSerializerPga3dGeomTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private val box = Pga3dAABB(Pga3dPoint(-10.0, -10.0, -10.0), Pga3dPoint(10.0, 10.0, 10.0))

  test("check sizes") {
    assert(FlatDoubleSerializer.getSize[Pga3dEdge] == 6)
    assert(FlatDoubleSerializer.getSize[Pga3dTriangle] == 9)
    assert(FlatDoubleSerializer.getSize[Pga3dAABB] == 6)
    assert(FlatDoubleSerializer.getSize[Pga3dSphere] == 4)
    assert(FlatDoubleSerializer.getSize[Pga3dCylinder] == 7)
    assert(FlatDoubleSerializer.getSize[Pga3dRay] == 9)
  }

  test("check serialization and deserialization") {
    val radii = Gen.choose(0.1, 10.0)

    myCheck(Pga3dPhysicsGenerators.edgeIn(box))
    myCheck(Pga3dPhysicsGenerators.triangleIn(box))
    myCheck(Pga3dPhysicsGenerators.aabbIn(box))
    myCheck(for (center <- Pga3dPhysicsGenerators.pointIn(box); r <- radii) yield Pga3dSphere(center, r))
    myCheck(for (a <- Pga3dPhysicsGenerators.pointIn(box); b <- Pga3dPhysicsGenerators.pointIn(box); r <- radii) yield Pga3dCylinder(a, b, r))
    myCheck(for (origin <- Pga3dPhysicsGenerators.pointIn(box); direction <- Pga3dPhysicsGenerators.vectorIn(box)) yield Pga3dRay(origin, direction))
  }
