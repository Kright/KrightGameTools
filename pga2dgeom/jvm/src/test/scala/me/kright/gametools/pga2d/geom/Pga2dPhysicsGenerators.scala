package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}
import org.scalacheck.Gen

object Pga2dPhysicsGenerators:
  def pointIn(box: Pga2dAABB): Gen[Pga2dPoint] =
    for {
      x <- Pga2dVectorMathGenerators.doubleInRange(box.min.x, box.max.x)
      y <- Pga2dVectorMathGenerators.doubleInRange(box.min.y, box.max.y)
    } yield Pga2dPoint(x, y)

  def vectorIn(box: Pga2dAABB): Gen[Pga2dVector] =
    pointIn(box).map(_.toVectorUnsafe)

  def aabbIn(aabb: Pga2dAABB): Gen[Pga2dAABB] =
    for {
      a <- pointIn(aabb)
      b <- pointIn(aabb)
    } yield Pga2dAABB(a).union(b)

  def edgeIn(aabb: Pga2dAABB): Gen[Pga2dEdge] =
    for {
      a <- pointIn(aabb)
      b <- pointIn(aabb)
    } yield Pga2dEdge(a, b)

  def triangleIn(aabb: Pga2dAABB): Gen[Pga2dTriangle] =
    for {
      a <- pointIn(aabb)
      b <- pointIn(aabb)
      c <- pointIn(aabb)
    } yield Pga2dTriangle(a, b, c)
