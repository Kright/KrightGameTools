package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ProjectionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val nonDegenerateLines = Pga2dGenerators.lines.filter(_.bulkNorm > 1e-3)

  test("project point onto x axis") {
    // line y = 0: equation x * nx + y * ny + w = 0 with (nx, ny, w) = (0, 1, 0)
    val xAxis = Pga2dLine(0, 1, 0)
    val p = Pga2dPoint(3, 4)
    assert(p.projectOntoLine(xAxis).toPoint == Pga2dPoint(3, 0))
  }

  test("project point onto shifted y axis") {
    // line x = 10: (nx, ny, w) = (1, 0, -10)
    val line = Pga2dLine(1, 0, -10)
    val p = Pga2dPoint(3, 4)
    assert((p.projectOntoLine(line).toPoint - Pga2dPoint(10, 4)).norm < 1e-15)
  }

  test("projection onto a normalized line keeps w = 1") {
    forAll(Pga2dGenerators.points, nonDegenerateLines, MinSuccessful(1000)) { (p, line) =>
      val projected = p.projectOntoLine(line / line.bulkNorm)
      assert(Math.abs(projected.w - 1.0) < 1e-14, s"w = ${projected.w}")
    }
  }

  test("toPointUnsafe is correct after projecting onto a normalized line") {
    val projected = Pga2dPoint(3, 4).projectOntoLine(Pga2dLine(0, 1, 0))
    assert(projected.w == 1.0)
    assert(projected.toPointUnsafe == Pga2dPoint(3, 0))
  }

  test("project point onto ideal line") {
    val projected = Pga2dPoint(3, 4).projectOntoLine(Pga2dLineIdeal(0, 1)) // the line y = 0
    assert(projected.w == 1.0)
    assert(projected.toPointUnsafe == Pga2dPoint(3, 0))
  }

  test("projected point lies on the line") {
    forAll(Pga2dGenerators.points, nonDegenerateLines, MinSuccessful(1000)) { (p, line) =>
      val projected = p.projectOntoLine(line).toPoint
      val lineEquation = projected.x * line.x + projected.y * line.y + line.w
      assert(Math.abs(lineEquation) / line.bulkNorm < 1e-9, s"p = $p, line = $line, projected = $projected")
    }
  }

  test("projection is idempotent") {
    forAll(Pga2dGenerators.points, nonDegenerateLines, MinSuccessful(1000)) { (p, line) =>
      val once = p.projectOntoLine(line).toPoint
      val twice = once.projectOntoLine(line).toPoint
      assert((once - twice).norm < 1e-9)
    }
  }

  test("projection moves point along the line normal") {
    forAll(Pga2dGenerators.points, nonDegenerateLines, MinSuccessful(1000)) { (p, line) =>
      val projected = p.projectOntoLine(line).toPoint
      val delta = p - projected
      // delta is parallel to the line normal (nx, ny): cross product is zero
      assert(Math.abs(delta.x * line.y - delta.y * line.x) / line.bulkNorm < 1e-9)
    }
  }

  test("distance to point is Euclidean") {
    forAll(Pga2dGenerators.points, Pga2dGenerators.points, MinSuccessful(1000)) { (p1, p2) =>
      val dx = p1.x - p2.x
      val dy = p1.y - p2.y
      val expected = Math.sqrt(dx * dx + dy * dy)
      assert(Math.abs(p1.distanceTo(p2) - expected) < 1e-12)
    }
  }

  test("distance to self is zero") {
    forAll(Pga2dGenerators.points) { p =>
      assert(p.distanceTo(p) == 0.0)
    }
  }
