package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint
import org.scalatest.funsuite.AnyFunSuiteLike

class Pga2dAccumulatorsTest extends AnyFunSuiteLike:

  test("Pga2dNearestPoint.update returns true exactly when it improves") {
    val acc = new Pga2dNearestPoint(Pga2dPoint(0, 0))

    assert(acc.update(Pga2dPoint(10, 0)))
    assert(acc.update(Pga2dPoint(0, 5)))
    assert(!acc.update(Pga2dPoint(7, 0)))
    assert(!acc.update(Pga2dPoint(5, 0))) // equal distance: rejected, first stays
    assert(acc.update(Pga2dPoint(1, 0)))

    assert(acc.nearestPoint.nn == Pga2dPoint(1, 0))
    assert(acc.distanceSquare == 1.0)
  }

  test("Pga2dNearestPoint: NaN safety in both directions") {
    val acc = new Pga2dNearestPoint(Pga2dPoint(0, 0), Pga2dPoint(Double.NaN, 0))
    assert(acc.distanceSquare.isNaN)

    assert(acc.update(Pga2dPoint(3, 0))) // heals
    assert(acc.distanceSquare == 9.0)
    assert(!acc.update(Pga2dPoint(Double.NaN, 0))) // never poisons back
    assert(acc.distanceSquare == 9.0)
  }

  test("Pga2dPairOfNearestPoints.update returns true exactly when it improves") {
    val acc = new Pga2dPairOfNearestPoints(Pga2dPoint(0, 0), Pga2dPoint(10, 0))

    assert(acc.update(Pga2dPoint(0, 0), Pga2dPoint(5, 0)))
    assert(!acc.update(Pga2dPoint(0, 0), Pga2dPoint(0, 8)))
    assert(!acc.update((Pga2dPoint(0, 0), Pga2dPoint(0, 5)))) // equal: rejected
    assert(acc.update((Pga2dPoint(0, 0), Pga2dPoint(0, 2))))

    assert(acc.pair == (Pga2dPoint(0, 0), Pga2dPoint(0, 2)))
    assert(acc.distanceSquare == 4.0)
  }

  test("Pga2dPairOfNearestPoints: NaN safety in both directions") {
    val poisoned = new Pga2dPairOfNearestPoints(Pga2dPoint(Double.NaN, 0), Pga2dPoint(0, 0))
    assert(poisoned.distanceSquare.isNaN)
    assert(poisoned.update(Pga2dPoint(0, 0), Pga2dPoint(7, 0))) // heals
    assert(poisoned.distanceSquare == 49.0)

    assert(!poisoned.update(Pga2dPoint(Double.NaN, 0), Pga2dPoint(0, 0)))
    assert(poisoned.distanceSquare == 49.0)
  }
