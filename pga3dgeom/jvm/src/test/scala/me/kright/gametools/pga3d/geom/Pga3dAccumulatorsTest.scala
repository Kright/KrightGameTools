package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint
import org.scalatest.funsuite.AnyFunSuiteLike

class Pga3dAccumulatorsTest extends AnyFunSuiteLike:

  test("Pga3dNearestPoint.update returns true exactly when it improves") {
    val acc = new Pga3dNearestPoint(Pga3dPoint(0, 0, 0))

    assert(acc.update(Pga3dPoint(10, 0, 0))) // first candidate always wins over the empty state
    assert(acc.update(Pga3dPoint(0, 5, 0)))
    assert(!acc.update(Pga3dPoint(0, 0, 7))) // farther: rejected
    assert(!acc.update(Pga3dPoint(5, 0, 0))) // equal distance: rejected, first stays
    assert(acc.update(Pga3dPoint(1, 0, 0)))

    assert(acc.nearestPoint.nn == Pga3dPoint(1, 0, 0))
    assert(acc.distanceSquare == 1.0)
  }

  test("Pga3dNearestPoint: a NaN candidate never wins") {
    val acc = new Pga3dNearestPoint(Pga3dPoint(0, 0, 0), Pga3dPoint(1, 0, 0))
    assert(!acc.update(Pga3dPoint(Double.NaN, 0, 0)))
    assert(acc.nearestPoint.nn == Pga3dPoint(1, 0, 0))
    assert(acc.distanceSquare == 1.0)
  }

  test("Pga3dNearestPoint: a NaN state is healed by the first real candidate") {
    val acc = new Pga3dNearestPoint(Pga3dPoint(0, 0, 0), Pga3dPoint(Double.NaN, 0, 0))
    assert(acc.distanceSquare.isNaN)

    assert(acc.update(Pga3dPoint(3, 0, 0)))
    assert(acc.distanceSquare == 9.0)
    assert(!acc.update(Pga3dPoint(4, 0, 0)))
  }

  test("Pga3dPairOfNearestPoints.update returns true exactly when it improves") {
    val acc = new Pga3dPairOfNearestPoints(Pga3dPoint(0, 0, 0), Pga3dPoint(10, 0, 0))

    assert(acc.update(Pga3dPoint(0, 0, 0), Pga3dPoint(5, 0, 0)))
    assert(!acc.update(Pga3dPoint(0, 0, 0), Pga3dPoint(0, 8, 0)))
    assert(!acc.update((Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 5)))) // equal: rejected
    assert(acc.update((Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 2))))

    assert(acc.pair == (Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 2)))
    assert(acc.distanceSquare == 4.0)
  }

  test("Pga3dPairOfNearestPoints: NaN safety in both directions") {
    val poisoned = new Pga3dPairOfNearestPoints(Pga3dPoint(Double.NaN, 0, 0), Pga3dPoint(0, 0, 0))
    assert(poisoned.distanceSquare.isNaN)
    assert(poisoned.update(Pga3dPoint(0, 0, 0), Pga3dPoint(7, 0, 0))) // heals
    assert(poisoned.distanceSquare == 49.0)

    assert(!poisoned.update(Pga3dPoint(Double.NaN, 0, 0), Pga3dPoint(0, 0, 0))) // never poisons back
    assert(poisoned.distanceSquare == 49.0)
  }
