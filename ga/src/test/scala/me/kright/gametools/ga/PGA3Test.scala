package me.kright.gametools.ga

import me.kright.gametools.ga.PGA3.*
import me.kright.gametools.vector.{Vector3d, VectorMathGenerators}
import com.github.kright.symbolic.Sym
import me.kright.gametools.mathutil.EqualityEps
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.math.Numeric.Implicits.infixNumericOps

class PGA3Test extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private given ga: PGA3 = GA.pga3

  private val eps = 1e-12

  private given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)

  private given equalityEps: EqualityEps = EqualityEps(eps)

  test("point is intersection of three planes") {
    val num = summon[Numeric[Sym]]
    val one = num.one
    val zero = num.zero

    val x = Sym("x")
    val y = Sym("y")
    val z = Sym("z")

    val px = plane(one, zero, zero, -x)
    val py = plane(zero, one, zero, -y)
    val pz = plane(zero, zero, one, -z)

    val p = point(x, y, z)
    val intersectionPoint = px ^ py ^ pz

    assert(p === intersectionPoint, s"\n${p.toMultilineString} != ${intersectionPoint.toMultilineString}")
  }

  test("planeX mirrors points") {
    val p = point(Sym("p.x"), Sym("p.y"), Sym("p.z"))
    val plane = Sym.planeX()

    val mirrored = plane.sandwich(p).withoutZeros
    val expected = point(-Sym("p.x"), Sym("p.y"), Sym("p.z"))

    assert(mirrored == expected, s"\n${mirrored.toMultilineString} != ${expected.toMultilineString}")
  }

  private def getVector(v: MultiVector[Double]): Vector3d =
    Vector3d(v("x"), v("y"), v("z"))

  test("plane mirrors points") {
    forAll(PGA3Generator.planeGen, PGA3Generator.pointGen) { (plane, point) =>
      val mirrored = plane.sandwich(point)

      val planeNormal = getVector(plane)
      val pointVec = getVector(point.dual)
      val mirroredVec = getVector(mirrored.dual)

      val pointD = planeNormal.dot(pointVec)
      val mirroredD = planeNormal.dot(mirroredVec)

      assert(planeNormal.mag === 1.0)
      assert((0.5 * (pointVec + mirroredVec)).dot(planeNormal) === -plane("w"))
      assert((pointVec - mirroredVec).rejected(axis = planeNormal) === Vector3d.zero)
    }
  }

  test("line defined by two points and by intersection are equal") {
    val planeX = plane(1.0, 0.0, 0.0, 0.0)
    val planeY = plane(0.0, 1.0, 0.0, 0.0)
    val pointCenter = point(0.0, 0.0, 0.0)
    val pointZ = point(0.0, 0.0, 1.0)

    assert((pointCenter v pointZ) == (planeX ^ planeY))
    assert((pointCenter v pointZ) == (planeX ^ planeY))
  }

  test("plane defined by three points") {
    val pointCenter = point(0.0, 0.0, 0.0)
    val pointX = point(1.0, 0.0, 0.0)
    val pointY = point(0.0, 1.0, 0.0)

    assert((pointCenter v pointY v pointX) == plane(0.0, 0.0, 1.0, 0.0))
  }

  test("translated rotor is rotor with translated axis of rotation") {
    forAll(VectorMathGenerators.vectors3InCube, VectorMathGenerators.vectors3normalized, VectorMathGenerators.double1) { (translation, rotAxis, angle) =>
      val tr = translator(translation)

      val axis = zeroPoint[Double] v point(rotAxis.x, rotAxis.y, rotAxis.z)
      val rot1 = tr.sandwich(rotor(angle, axis))
      val rot2 = rotor(angle, tr.sandwich(axis))

      assert((rot1 - rot2).squareMagnitude <= 1e-12)
    }
  }

  test("rotor, expForRotor and exponent by series sum are same for line through center") {
    forAll(VectorMathGenerators.vectors3normalized, VectorMathGenerators.double1) { (rotAxis, angle) =>
      val axis = zeroPoint[Double] v point(rotAxis)

      val rot1 = rotor(angle, axis)
      val rot2 = expForLine(axis * 0.5 * angle)
      val rot3 = (axis * 0.5 * angle).exponentBySeriesSum(thresholdNorm = 1e-12)

      assert((rot1 - rot3).norm <= 1e-10, s"${rot1.toMultilineString} != ${rot3.toMultilineString}")
      assert((rot2 - rot3).norm <= 1e-10, s"${rot1.toMultilineString} != ${rot3.toMultilineString}")
    }
  }

  test("rotor, expForRotor and exponent by series sum are same for any line") {
    forAll(VectorMathGenerators.vectors3normalized, VectorMathGenerators.double1, VectorMathGenerators.vectors3InCube) { (rotAxis, angle, center) =>
      val axis = point(center) v point(center + rotAxis)

      val rot1 = rotor(angle, axis)
      val rot2 = expForLine(axis * 0.5 * angle)
      val rot3 = (axis * 0.5 * angle).exponentBySeriesSum(thresholdNorm = 1e-15)

      assert((rot1 - rot3).norm <= 1e-10, s"${rot1.toMultilineString} != ${rot3.toMultilineString}")
      assert((rot2 - rot3).norm <= 1e-10, s"${rot2.toMultilineString} != ${rot3.toMultilineString}")
    }
  }

  test("rotation and orthogonal translation") {
    val lineUp = zeroPoint[Double] v point(0.0, 0.0, 1.0)

    val translateZ = translator(zeroPoint[Sym], point(Sym(0.0), Sym(0.0), Sym("dz")))

    val rotateXY = MultiVector(
      "1" -> Sym("cos"),
      "xy" -> Sym("sin"),
      "xz" -> Sym(0.0),
      "yz" -> Sym(0.0),
    )

    assert(translateZ.geometric(rotateXY) == rotateXY.geometric(translateZ))
  }

  test("exp for bivector") {
    forAll(GAGenerator.bladesGen(grade = 2), MinSuccessful(1000)) { bv =>
      assert((bv.exponentBySeriesSum(1e-15) - expForBivector(bv)).norm < 1e-14)
    }
  }

  test("line by points and line by zero dot shift") {
    GA.pga3.use {

      val shift = MultiVector(
        "x" -> Sym("s.x"),
        "y" -> Sym("s.y"),
        "z" -> Sym("s.z"),
      )

      {
        val p = Sym.idealPoint("s")
        val o = zeroPoint[Sym]

        val expected = MultiVector(
          "xy" -> Sym("s.z"),
          "xz" -> -Sym("s.y"),
          "yz" -> Sym("s.x"),
        )

        assert((o v (p + o)) == expected)
        assert((o dot shift) == expected)
        assert((shift dot o) == expected)

      }

      {
        val a = Sym.point("a")

        val expected = MultiVector(
          "wx" -> (Sym("a.y") * Sym("s.z") - Sym("a.z") * Sym("s.y")),
          "wy" -> (Sym("a.z") * Sym("s.x") - Sym("a.x") * Sym("s.z")),
          "wz" -> (Sym("a.x") * Sym("s.y") - Sym("a.y") * Sym("s.x")),
          "xy" -> Sym("s.z"),
          "xz" -> -Sym("s.y"),
          "yz" -> Sym("s.x"),
        )

        assert((a v (a + shift.dual)) == expected)
        assert((a v shift.dual) == expected)
        assert((shift dot a) == expected)
      }
    }
  }

  test("bivector split") {
    forAll(GAGenerator.bladesGen(2)) { b =>
      val (line, shift) = PGA3.bivectorSplit(b)

      assert(line.values.forall((b, v) => b.grade == 2))
      assert(shift.values.forall((b, v) => b.grade == 2))
      assert(shift.withoutZeros.weight == shift)

      val lineExp = PGA3.expForBivector(line)
      val shiftExp = PGA3.expForBivector(shift)

      def requireEq(a: MultiVector[Double], b: MultiVector[Double]) =
        require((a - b).norm <= 1e-14)

      requireEq(line + shift, b)
      requireEq(lineExp ⟑ shiftExp, shiftExp ⟑ lineExp)
      requireEq(lineExp, PGA.expForLine(line))
      requireEq(shiftExp, PGA.expForLine(shift))
      requireEq(PGA.expForBivector(b), lineExp ⟑ shiftExp)
    }
  }

  test("bivector split for zero") {
    val zero = MultiVector.zero[Double]
    assert(bivectorSplit(zero) == (zero, zero))
  }

  test("log is inverse of exp") {
    forAll(GAGenerator.bladesGen(2)) { b =>
      val exp = PGA3.expForBivector(b)
      val log = PGA3.motorLog(exp)

      require((b - log).norm < 1e-12,
        s"""
           |b = ${b}
           |exp = ${exp}
           |log = ${log}
           |diff = ${log - b}
           |""".stripMargin)
    }
  }

  test("log is inverse of exp even for small vectors") {
    forAll(GAGenerator.bladesGen(2).filter(_.norm > 1e-3)) { bb =>
      for (i <- -10 to 0) {
        val b = bb * (math.pow(10, i) / bb.norm)
        val exp = PGA3.expForBivector(b)
        val log = PGA3.motorLog(exp)

        val err = (log - b).norm
        assert(err <= b.norm * 2e-7,
          s"""
             |s"$i:(log(exp(b)) - b).norm = ${err}, b.norm = ${b.norm}"
             |""".stripMargin)
      }
    }
  }

  test("momentum") {
    val mass = Sym("mass")

    val pos = Sym.point("pos")
    val posx = pos.dual("x")
    val posy = pos.dual("y")
    val posz = pos.dual("z")

    val antiX = pos - pos.weight * Sym(2.0)

    val vx = Sym("v.x")
    val vy = Sym("v.y")
    val vz = Sym("v.z")
    val v = MultiVector(
      "x" -> vx,
      "y" -> vy,
      "z" -> vz,
    )

    val forque = (v dot pos) * mass

    assert(
      forque ==
        MultiVector(
          "wx" -> (mass * posy * vz - mass * posz * vy),
          "wy" -> (mass * posz * vx - mass * posx * vz),
          "wz" -> (mass * posx * vy - mass * posy * vx),
          "xy" -> mass * vz,
          "xz" -> -mass * vy,
          "yz" -> mass * vx,
        ),
      s"""
         |pos = ${pos.toMultilineString}
         |v = ${v.toMultilineString}
         |forque = (v dot x * mass) = ${(v dot pos * mass).toMultilineString}
         |forque.dual = ${forque.dual.toMultilineString}
         |
         |forque in point x = forque - ((v * mass) dot (x.weight) = ${(forque - ((v * mass) dot (pos.weight))).toMultilineString}
         |
         |antiPos = ${antiX}
         |impulse + anti = ${((v dot pos * mass) + (v dot antiX * mass)).toMultilineString}
         |
         |impulse + antiPosSame = ${((v dot pos * mass) + (-v dot antiX) * mass).toMultilineString}
         |""".stripMargin
    )
  }

  test("dual reverse is reverse dual") {
    val a = Sym.multiVector("a")

    for (grade <- 0 to 4) {
      if (grade % 2 == 0) {
        assert(a.grade(grade).dual.reverse == a.grade(grade).reverse.dual)
      } else {
        assert(a.grade(grade).dual.reverse == -a.grade(grade).reverse.dual)
      }
    }
  }
