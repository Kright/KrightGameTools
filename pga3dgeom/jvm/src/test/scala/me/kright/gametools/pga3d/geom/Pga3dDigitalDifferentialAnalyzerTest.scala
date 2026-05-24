package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dDigitalDifferentialAnalyzerTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val halfSize = 10
  private val bounds = Pga3dAABB(
    Pga3dPoint(-halfSize, -halfSize, -halfSize),
    Pga3dPoint(halfSize, halfSize, halfSize)
  )

  private def pointsWithNonRoundCoordinates(eps: Double): Gen[Pga3dPoint] =
    Pga3dPhysicsGenerators.pointIn(bounds)
      .map { p => if (math.abs(p.x - p.x.round) > eps) p else p.copy(x = p.x + 2 * eps) }
      .map { p => if (math.abs(p.y - p.y.round) > eps) p else p.copy(y = p.y + 2 * eps) }
      .map { p => if (math.abs(p.z - p.z.round) > eps) p else p.copy(z = p.z + 2 * eps) }


  private def assertCoords(dda: Pga3dDigitalDifferentialAnalyzer, x: Int, y: Int, z: Int): Unit = {
    assert(dda.x == x)
    assert(dda.y == y)
    assert(dda.z == z)
  }

  test("doStep updates the correct coordinate") {

    val directions = Gen.oneOf(
      Pga3dVector(1.0, 0.0, 0.0),
      Pga3dVector(0.0, 1.0, 0.0),
      Pga3dVector(0.0, 0.0, 1.0),
      Pga3dVector(-1.0, 0.0, 0.0),
      Pga3dVector(0.0, -1.0, 0.0),
      Pga3dVector(0.0, 0.0, -1.0),
    )

    forAll(pointsWithNonRoundCoordinates(1e-10), directions) { (origin, direction) =>
      val dda = new Pga3dDigitalDifferentialAnalyzer(origin, direction)

      for (i <- 0 to 3) {
        val sum = origin + direction * i
        assertCoords(dda, sum.x.floor.toInt, sum.y.floor.toInt, sum.z.floor.toInt)
        dda.doStep()
      }
    }
  }

  test("doStep handles zero direction components correctly") {
    val positiveDirections = Gen.oneOf(
      Pga3dVector(0.0, 1.0, 1.0),
      Pga3dVector(1.0, 0.0, 1.0),
      Pga3dVector(1.0, 1.0, 0.0)
    )

    val signs = Gen.oneOf(-1, 1)

    val anyDirections: Gen[Pga3dVector] =
      for (dir <- positiveDirections;
           signX <- signs;
           signY <- signs;
           signZ <- signs) yield Pga3dVector(dir.x * signX, dir.y * signY, dir.z * signZ)

    forAll(pointsWithNonRoundCoordinates(1e-10), anyDirections) { (origin, direction) =>
      val dda = new Pga3dDigitalDifferentialAnalyzer(origin, direction)

      for (i <- 0 to 3) {
        val sum = origin + direction * i

        assertCoords(dda, sum.x.floor.toInt, sum.y.floor.toInt, sum.z.floor.toInt)

        dda.doStep()
        dda.doStep()
      }
    }
  }

  test("multiple doStep calls traverse cells in correct order") {
    val origin = Pga3dPoint(0.1, 0.1, 0.1)
    val direction = Pga3dVector(1.0, 2.0, 3.0)
    val dda = new Pga3dDigitalDifferentialAnalyzer(origin, direction)

    assertCoords(dda, 0, 0, 0)

    dda.doStep()
    assertCoords(dda, 0, 0, 1)

    dda.doStep()
    assertCoords(dda, 0, 1, 1)

    dda.doStep()
    assertCoords(dda, 0, 1, 2)

    dda.doStep()
    assertCoords(dda, 1, 1, 2)
  }
