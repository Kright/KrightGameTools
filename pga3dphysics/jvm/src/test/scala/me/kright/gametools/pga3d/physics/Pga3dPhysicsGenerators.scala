package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen

object Pga3dPhysicsGenerators:
  def pointIn(min: Pga3dPoint, max: Pga3dPoint): Gen[Pga3dPoint] =
    for {
      x <- Pga3dVectorMathGenerators.doubleInRange(min.x, max.x)
      y <- Pga3dVectorMathGenerators.doubleInRange(min.y, max.y)
      z <- Pga3dVectorMathGenerators.doubleInRange(min.z, max.z)
    } yield Pga3dPoint(x, y, z)

  def vectorIn(min: Pga3dPoint, max: Pga3dPoint): Gen[Pga3dVector] =
    pointIn(min, max).map(_.toVectorUnsafe)

