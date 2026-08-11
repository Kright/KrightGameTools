package me.kright.gametools.pga2d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.pga2d.FlatDoubleSerializerPga2dTest.myCheck
import me.kright.gametools.pga2d.Pga2dPoint
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FlatDoubleSerializerPga2dGeomTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private val box = Pga2dAABB(Pga2dPoint(-10.0, -10.0), Pga2dPoint(10.0, 10.0))

  test("check sizes") {
    assert(FlatDoubleSerializer.getSize[Pga2dEdge] == 4)
    assert(FlatDoubleSerializer.getSize[Pga2dTriangle] == 6)
    assert(FlatDoubleSerializer.getSize[Pga2dAABB] == 4)
    assert(FlatDoubleSerializer.getSize[Pga2dCircle] == 3)
    assert(FlatDoubleSerializer.getSize[Pga2dRay] == 4)
    assert(FlatDoubleSerializer.getSize[Pga2dCapsule] == 5)
  }

  test("check serialization and deserialization") {
    val radii = Gen.choose(0.1, 10.0)

    myCheck(Pga2dPhysicsGenerators.edgeIn(box))
    myCheck(Pga2dPhysicsGenerators.triangleIn(box))
    myCheck(Pga2dPhysicsGenerators.aabbIn(box))
    myCheck(for (center <- Pga2dPhysicsGenerators.pointIn(box); r <- radii) yield Pga2dCircle(center, r))
    myCheck(for (origin <- Pga2dPhysicsGenerators.pointIn(box); direction <- Pga2dPhysicsGenerators.vectorIn(box)) yield Pga2dRay(origin, direction))
    myCheck(for (a <- Pga2dPhysicsGenerators.pointIn(box); b <- Pga2dPhysicsGenerators.pointIn(box); r <- radii) yield Pga2dCapsule(a, b, r))
  }
