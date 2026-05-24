package me.kright.gametools.ga

import me.kright.gametools.ga.PGA3.point
import me.kright.gametools.vector.VectorMathGenerators
import org.scalacheck.Gen

object PGA3Generator:
  def planeGen(using ga: PGA3): Gen[MultiVector[Double]] =
    for (
      normal <- VectorMathGenerators.vectors3normalized;
      d <- VectorMathGenerators.double1
    ) yield PGA3.plane(normal.x, normal.y, normal.z, d)

  def pointGen(using ga: PGA3): Gen[MultiVector[Double]] =
    VectorMathGenerators.vectors3InCube.map { p =>
      PGA3.point(p.x, p.y, p.z)
    }
