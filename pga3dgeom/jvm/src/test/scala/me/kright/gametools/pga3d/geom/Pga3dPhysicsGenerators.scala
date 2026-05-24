package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen

object Pga3dPhysicsGenerators:
  def pointIn(box: Pga3dAABB): Gen[Pga3dPoint] =
    for {
      x <- Pga3dVectorMathGenerators.doubleInRange(box.min.x, box.max.x)
      y <- Pga3dVectorMathGenerators.doubleInRange(box.min.y, box.max.y)
      z <- Pga3dVectorMathGenerators.doubleInRange(box.min.z, box.max.z)
    } yield Pga3dPoint(x, y, z)

  def vectorIn(box: Pga3dAABB): Gen[Pga3dVector] =
    pointIn(box).map(_.toVectorUnsafe)

  def aabbIn(aabb: Pga3dAABB): Gen[Pga3dAABB] =
    for {
      a <- pointIn(aabb)
      b <- pointIn(aabb)
    } yield Pga3dAABB(a).union(b)

  def edgeIn(aabb: Pga3dAABB): Gen[Pga3dEdge] =
    for {
      a <- pointIn(aabb)
      b <- pointIn(aabb)
    } yield Pga3dEdge(a, b)

  def triangleIn(aabb: Pga3dAABB): Gen[Pga3dTriangle] =
    for {
      a <- pointIn(aabb)
      b <- pointIn(aabb)
      c <- pointIn(aabb)
    } yield Pga3dTriangle(a, b, c)
