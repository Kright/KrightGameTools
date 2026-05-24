package me.kright.gametools.ga

import org.scalactic.Equality

object GaEquality:
  def makeEquality(eps: Double): Equality[MultiVector[Double]] = new Equality[MultiVector[Double]]:
    override def areEqual(left: MultiVector[Double], right: Any): Boolean =
      right match
        case rightV: MultiVector[Double] => math.abs(left.getSqrDist(rightV)) < eps
        case _ => false
