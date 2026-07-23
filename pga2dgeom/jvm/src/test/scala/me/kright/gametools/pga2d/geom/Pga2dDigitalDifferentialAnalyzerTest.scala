package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dDigitalDifferentialAnalyzerTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 10
  private val bounds = Pga2dAABB(
    Pga2dPoint(-halfSize, -halfSize),
    Pga2dPoint(halfSize, halfSize)
  )

  private def pointsWithNonRoundCoordinates(eps: Double): Gen[Pga2dPoint] =
    Pga2dPhysicsGenerators.pointIn(bounds)
      .map { p => if (math.abs(p.x - p.x.round) > eps) p else p.copy(x = p.x + 2 * eps) }
      .map { p => if (math.abs(p.y - p.y.round) > eps) p else p.copy(y = p.y + 2 * eps) }


  private def assertCoords(dda: Pga2dDigitalDifferentialAnalyzer, x: Int, y: Int): Unit = {
    assert(dda.x == x)
    assert(dda.y == y)
  }

  test("doStep updates the correct coordinate") {

    val directions = Gen.oneOf(
      Pga2dVector(1.0, 0.0),
      Pga2dVector(0.0, 1.0),
      Pga2dVector(-1.0, 0.0),
      Pga2dVector(0.0, -1.0),
    )

    forAll(pointsWithNonRoundCoordinates(1e-10), directions) { (origin, direction) =>
      val dda = new Pga2dDigitalDifferentialAnalyzer(origin, direction)

      for (i <- 0 to 3) {
        val sum = origin + direction * i
        assertCoords(dda, sum.x.floor.toInt, sum.y.floor.toInt)
        dda.doStep()
      }
    }
  }

  test("doStep handles diagonal directions correctly") {
    val signs = Gen.oneOf(-1, 1)

    val anyDirections: Gen[Pga2dVector] =
      for (signX <- signs;
           signY <- signs) yield Pga2dVector(signX, signY)

    forAll(pointsWithNonRoundCoordinates(1e-10), anyDirections) { (origin, direction) =>
      val dda = new Pga2dDigitalDifferentialAnalyzer(origin, direction)

      for (i <- 0 to 3) {
        val sum = origin + direction * i

        assertCoords(dda, sum.x.floor.toInt, sum.y.floor.toInt)

        dda.doStep()
        dda.doStep()
      }
    }
  }

  test("multiple doStep calls traverse cells in correct order") {
    val origin = Pga2dPoint(0.1, 0.1)
    val direction = Pga2dVector(1.0, 2.0)
    val dda = new Pga2dDigitalDifferentialAnalyzer(origin, direction)

    assertCoords(dda, 0, 0)

    dda.doStep()
    assertCoords(dda, 0, 1)

    dda.doStep()
    assertCoords(dda, 1, 1)

    dda.doStep()
    assertCoords(dda, 1, 2)

    dda.doStep()
    assertCoords(dda, 1, 3)
  }
