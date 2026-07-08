package me.kright.gametools.pga3d.physics

import me.kright.gametools.matrix.MatrixPrinter
import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.Pga3dGenerators.{normalizedRotors, vectors}
import me.kright.gametools.pga3d.physics.Pga3dInertiaGenerators.inertiaMovedLocal
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.scalactic.anyvals.PosInt
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dInertiaTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  extension (p: Pga3dInertiaSummable)
    def str: String =
      s"""Pga3dSymBivector(ww = ${p.ww},
         |                 wx = ${p.wx}, wy = ${p.wy}, wz = ${p.wz},
         |                 xx = ${p.xx}, yy = ${p.yy}, zz = ${p.zz},
         |                 xy = ${p.xy}, xz = ${p.xz}, yz = ${p.yz})""".stripMargin


  private val shifts: Seq[Pga3dVector] = Seq(
    Pga3dVector(0.0, 0.0, 0.0),
    Pga3dVector(1.0, 0.0, 0.0),
    Pga3dVector(0.0, 1.0, 0.0),
    Pga3dVector(0.0, 0.0, 1.0),
    Pga3dVector(1.0, 1.0, 1.0),
  )

  val models: Gen[Pga3dInertia] = Gen.oneOf(
    Pga3dInertia.point(mass = 1.0),
    Pga3dInertia.cube(mass = 1.0, 1.0),
    Pga3dInertia.cube(mass = 1.0, 1.0, 1.0, 1.0),
    Pga3dInertia.cube(mass = 2.0, 3.0, 4.0, 5.0),
    Pga3dInertia.cylinderX(mass = 1.0, length = 1.0, r = 1.0),
    Pga3dInertia.cylinderX(mass = 2.0, length = 3.0, r = 5.0, innerR = 4.0),
    Pga3dInertia.solidEllipsoid(mass = 2.0, 3.0, 4.0, 5.0),
  )

  val anyShifts: Gen[Pga3dVector] = Gen.oneOf(
    Gen.oneOf(shifts),
    for {
      x <- Pga3dVectorMathGenerators.double1
      y <- Pga3dVectorMathGenerators.double1
      z <- Pga3dVectorMathGenerators.double1
    } yield Pga3dVector(x, y, z),
  )

  def p(p: Pga3dInertiaSummable): Unit =
    println(p.str)
    println(p)
    println()

  def assertEq(a: Pga3dInertiaSummable, b: Pga3dInertiaSummable, eps: Double = 1e-9): Unit = {
    val n = (a - b).norm
    assert(n < eps)
  }

  test("shift") {
    val p1 = Pga3dPoint(-1.0, 0.0, 0.0)
    val p2 = Pga3dPoint(1.0, 0.0, 0.0)
    val sum = Pga3dInertiaSummable.point(p1, mass = 1.0) + Pga3dInertiaSummable.point(p2, mass = 3.0)
    shifts.foreach { shift =>
      val shiftedSum = Pga3dInertiaSummable.point(p1 + shift, mass = 1.0) + Pga3dInertiaSummable.point(p2 + shift, mass = 3.0)
      assertEq(Pga3dTranslator.addVector(shift).sandwich(sum), shiftedSum)
      assertEq(Pga3dTranslator.addVector(shift).toMotor.sandwich(sum), shiftedSum)
    }
  }

  test("rotate") {
    val p1 = Pga3dPoint(-1.0, 0.0, 0.0)
    val p2 = Pga3dPoint(1.0, 0.0, 0.0)
    val sum = Pga3dInertiaSummable.point(p1, mass = 1.0) + Pga3dInertiaSummable.point(p2, mass = 3.0)
    forAll(normalizedRotors) { q =>
      val rotatedSum = Pga3dInertiaSummable.point(q.sandwich(p1).toPoint, mass = 1.0) + Pga3dInertiaSummable.point(q.sandwich(p2).toPoint, mass = 3.0)
      assertEq(q.sandwich(sum), rotatedSum)
      assertEq(q.toMotor.sandwich(sum), rotatedSum)
    }
  }

  test("apply motor") {
    val p1 = Pga3dPoint(-1.0, 0.0, 0.0)
    val p2 = Pga3dPoint(1.0, 0.0, 0.0)
    val sum = Pga3dInertiaSummable.point(p1, mass = 1.0) + Pga3dInertiaSummable.point(p2, mass = 3.0)
    forAll(normalizedRotors(Gen.Parameters.default, Seed(12345L)).get, vectors) { (q, shift) =>
      val motor = q.geometric(Pga3dTranslator.addVector(shift))
      val movedSum = motor.sandwich(sum)
      assertEq(motor.sandwich(sum), movedSum)
    }
  }

  test("apply bivector") {
    val p1 = Pga3dPoint(-2.0, 0.0, 0.0)
    val p2 = Pga3dPoint(2.0, 0.0, 0.0)
    //    val sum = new Pga3dInertia(p1, mass = 1) + new Pga3dInertia(p2, mass = 3)
    val sum = Pga3dInertiaSummable.point(p1, mass = 1) + Pga3dInertiaSummable.point(p2, mass = 7)
    // (3^2 * 1 + 1^2 * 3)
    val b = Pga3dBivector(0.0, 1.0, 0.0)
    val shifted = Pga3dTranslator.addVector(-sum.centerOfMass.toVectorUnsafe).sandwich(sum)
    //    println(shifted.toMatrixString)
    //    println(sum.actOnBivectorAsInertia(b))
    //    println(sum.actOnBivectorAsInertia2(b))
  }

  test("inertia of eight points in cube") {
    val localInertia = Pga3dInertiaLocal(mass = 1.0, mryz = 1 + 1, mrxz = 1 + 1, mrxy = 1 + 1)

    var sumOfPoints = Pga3dInertiaSummable.zero
    for (z <- Seq(-1, 1); y <- Seq(-1, 1); x <- Seq(-1, 1)) {
      sumOfPoints += Pga3dInertiaSummable.point(Pga3dPoint(x, y, z), mass = 1.0 / 8.0)
    }

    assertEq(sumOfPoints, localInertia.toSummable)

    assert(sumOfPoints.toString ==
      "Pga3dInertiaSummable(ww=1.0, wx=0.0, wy=0.0, wz=0.0, xx=1.0, yy=1.0, zz=1.0, xy=0.0, yz=0.0, xz=0.0)")
  }

  test(s"inertia of eight points in cube for non-even mass and size") {
    val mass = 7.0
    val rx = 3.0
    val ry = 4.0
    val rz = 5.0

    val rx2 = rx * rx
    val ry2 = ry * ry
    val rz2 = rz * rz

    val localInertia = Pga3dInertiaLocal(
      mass,
      mryz = mass * (ry2 + rz2),
      mrxz = mass * (rx2 + rz2),
      mrxy = mass * (rx2 + ry2),
    )

    var sumOfPoints = Pga3dInertiaSummable.zero
    for (z <- Seq(-rz, rz); y <- Seq(-ry, ry); x <- Seq(-rx, rx)) {
      sumOfPoints += Pga3dInertiaSummable.point(Pga3dPoint(x, y, z), mass = mass / 8.0)
    }

    assertEq(sumOfPoints, localInertia.toSummable)
  }

  private def getMainAxesSorted(inertia: Pga3dInertiaMovedLocal): Seq[Double] =
    val local = inertia.localInertia
    Seq(local.mryz, local.mrxz, local.mrxy).sorted

  private def maxDiff(lstA: Seq[Double], lstB: Seq[Double]): Double =
    lstA.zip(lstB).map((a, b) => Math.abs(a - b)).max

  test("inertia diagonalization preservers summableInertia") {
    val toStr = MatrixPrinter.squarePrinter5f.copy(elemToStr = _.toString)

    forAll(inertiaMovedLocal, MinSuccessful(100)) { inertiaInitial =>
      val summableInertia = inertiaInitial.toSummable
      val inertiaRestored = summableInertia.toInertiaMovedLocal
      val summableRestored = inertiaRestored.toSummable

      val diff = (summableInertia - summableRestored).norm
      assert(diff < 1e-9,
        s"""diff = ${diff}
           |summableInertia = ${toStr(summableInertia.toMatrixXYZW)}
           |summableRestored = ${toStr(summableInertia.toMatrixXYZW)}
           |""".stripMargin)
    }
  }

  test("inertia diagonalization preservers main axes") {
    forAll(inertiaMovedLocal, MinSuccessful(1000)) { inertiaInitial =>
      val summableInertia = inertiaInitial.toSummable
      val inertiaRestored = summableInertia.toInertiaMovedLocal
      val summableRestored = inertiaRestored.toSummable

      val sortedAxesInitial = getMainAxesSorted(inertiaInitial)
      val sortedAxesRestored = getMainAxesSorted(inertiaRestored)

      assert(maxDiff(sortedAxesInitial, sortedAxesRestored) < 5e-10 * inertiaInitial.mass,
        s"""diff = ${sortedAxesInitial.zip(sortedAxesRestored).map((a, b) => Math.abs(a - b)).mkString(", ")}
           |sortedAxesInitial = ${sortedAxesInitial}
           |sortedAxesRestored = ${sortedAxesRestored}""".stripMargin)
    }
  }

  test("inertia .toSummable.toInertia preserves mass") {
    forAll(inertiaMovedLocal, MinSuccessful(1000)) { inertiaInitial =>
      val inertiaRestored = inertiaInitial.toSummable.toInertiaMovedLocal

      assert(Math.abs(inertiaInitial.mass - inertiaRestored.mass) < 1e-13,
        s"""diff = ${Math.abs(inertiaInitial.mass - inertiaRestored.mass)}
           |initialMass = ${inertiaRestored.mass}
           |restoredMass = ${inertiaRestored.mass}""".stripMargin)
    }
  }

  test("inertia toSummable.toInertia and back applied in the same way") {
    forAll(inertiaMovedLocal, Pga3dGenerators.bivectors, MinSuccessful(1000)) { (inertia, bivector) =>
      val inertia2 = inertia.toSummable.toInertiaMovedLocal

      val applied1 = inertia(bivector)
      val applied2 = inertia2(bivector)

      assert((applied1 - applied2).norm < 1e-9)
    }
  }

  test("inertia sum") {
    forAll(inertiaMovedLocal, inertiaMovedLocal, Pga3dGenerators.bivectors, MinSuccessful(1000)) { (inertia1, inertia2, bivector) =>

      val inertiaSum = (inertia1.toSummable + inertia2.toSummable).toInertiaMovedLocal

      val applied1 = inertia1(bivector) + inertia2(bivector)
      val applied2 = inertiaSum(bivector)

      assert((applied1 - applied2).norm < 1e-9)
    }
  }

  test("inertia toSummable applied in the same way") {
    forAll(inertiaMovedLocal, Pga3dGenerators.bivectors, MinSuccessful(1000)) { (inertia, bivector) =>
      val applied1 = inertia(bivector)
      val applied2 = inertia.toSummable(bivector)

      assert((applied1 - applied2).norm < 1e-9)
    }
  }

  test("inertia .toInertia() acts the same way") {
    val probes = Seq(
      Pga3dBivector(1, 0, 0, 0, 0, 0),
      Pga3dBivector(0, 1, 0, 0, 0, 0),
      Pga3dBivector(0, 0, 1, 0, 0, 0),
      Pga3dBivector(0, 0, 0, 1, 0, 0),
      Pga3dBivector(0, 0, 0, 0, 1, 0),
      Pga3dBivector(0, 0, 0, 0, 0, 1),
    )

    val inertiaProbes = Seq(
      Pga3dInertiaSummable(1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
      Pga3dInertiaSummable(1, 0, 1, 0, 0, 0, 0, 0, 0, 0),
      Pga3dInertiaSummable(1, 0, 0, 1, 0, 0, 0, 0, 0, 0),
      Pga3dInertiaSummable(1, 0, 0, 0, 1, 0, 0, 0, 0, 0),
      Pga3dInertiaSummable(1, 0, 0, 0, 0, 1, 0, 0, 0, 0),
      Pga3dInertiaSummable(1, 0, 0, 0, 0, 0, 1, 0, 0, 0),
      Pga3dInertiaSummable(1, 0, 0, 0, 0, 0, 0, 1, 0, 0),
      Pga3dInertiaSummable(1, 0, 0, 0, 0, 0, 0, 0, 1, 0),
      Pga3dInertiaSummable(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    )

    for (summable <- inertiaProbes) {
      for (probe <- probes) {
        //        println(s"probe = $probe")
        //        println(s"inertia = ${summable}")
        //        println(s"inertiaLocal(probe) = ${summable.toInertia.apply(probe)}")
        //        println(s"inertiaGlobl(probe) = ${summable.apply(probe)}")
        assert((summable.toInertiaMovedLocal.apply(probe) - summable.apply(probe)).norm < 1e-9)
      }
    }
  }

  test("sandwich may return narrower type") {
    val a = Pga3dInertiaSimple(1, 2)

    val m = Pga3dMotor.id
    val r: Pga3dInertiaMovedSimple = m.sandwich(a)
    // Code just compiles and that it.
  }

  test("any inertia is equal to summable") {
    forAll(Pga3dInertiaGenerators.anyInertia, Pga3dGenerators.bivectors, MinSuccessful(10000)) { (inertia, b) =>
      val summable = inertia.toSummable

      val L1 = inertia(b)
      val L2 = summable(b)

      assert((L1 - L2).norm < 1e-12)
    }
  }

  test("any inertia reverse is equal to apply") {
    forAll(Pga3dInertiaGenerators.anyInertia, Pga3dGenerators.bivectors, MinSuccessful(1000)) { (inertia, b) =>
      val eps = 2e-12
      val a = inertia(b)
      val b2 = inertia.invert(a)
      assert((b - b2).norm < eps)
    }
  }

  test("any inertia acceleration") {
    forAll(Pga3dInertiaGenerators.anyInertia, Pga3dGenerators.bivectors, Pga3dGenerators.bivectors, MinSuccessful(10000)) { (inertia, b, forque) =>
      val a = inertia.getAcceleration(b, forque)
      val a2 = inertia.invert(b.cross(inertia(b)) + forque)

      assert((a - a2).norm < 3e-12)
    }
  }

  test("any inertia representations are equal") {
    forAll(Pga3dInertiaGenerators.anyInertia, MinSuccessful(10000)) { inertia =>
      val eps =
        if (inertia.isInstanceOf[Pga3dInertiaSummable]) 3e-11
        else 2e-13

      assert((inertia.toSummable - inertia.toInertiaMovedLocal.toSummable).norm < eps)
      assert((inertia.toSummable - inertia.toPrecomputed.toSummable).norm < eps)
      assert((inertia.toSummable - inertia.toFastestRepresentation.toSummable).norm < eps)
    }
  }

  test("any inertia multiplied by motor") {
    forAll(Pga3dInertiaGenerators.anyInertia, Pga3dGenerators.normalizedMotors, MinSuccessful(10000)) { (inertia, motor) =>
      val s1 = motor.sandwich(inertia).toSummable
      val s2 = inertia.toSummable.movedBy(motor)
      val s3 = inertia.toInertiaMovedLocal.movedBy(motor).toSummable

      val eps =
        if (inertia.isInstanceOf[Pga3dInertiaSummable]) 2e-11
        else 1e-12

      assert((s1 - s2).norm < eps)
      assert((s1 - s3).norm < eps)
    }
  }

  test("any inertia moved by translator same as moved by motor") {
    forAll(Pga3dInertiaGenerators.anyInertia, Pga3dGenerators.translators, MinSuccessful(10000)) { (inertia, translator) =>
      val s1 = inertia.movedBy(translator).toSummable
      val s2 = inertia.movedBy(translator.toMotor).toSummable

      val eps = 1e-12
      assert((s1 - s2).norm < eps)
    }
  }

  test("any inertia moved by rotor same as moved by motor") {
    forAll(Pga3dInertiaGenerators.anyInertia, Pga3dGenerators.normalizedRotors, MinSuccessful(10000)) { (inertia, rotor) =>
      val s1 = inertia.movedBy(rotor).toSummable
      val s2 = inertia.movedBy(rotor.toMotor).toSummable

      val eps = 1e-12
      assert((s1 - s2).norm < eps)
    }
  }

  test("inertia for disk") {
    val eps = 1e-15

    val disk = Pga3dInertia.cylinderX(mass = 1.0, length = 0.0, r = 1.0)

    assert(Math.abs(disk.mryz - 0.5) < eps, s"${disk} ${disk.toSummable}")
    assert(Math.abs(disk.mrxz - 0.25) < eps, s"${disk} ${disk.toSummable}")
    assert(Math.abs(disk.mrxy - 0.25) < eps, s"${disk} ${disk.toSummable}")
  }

  test("inertia for circle disk") {
    val eps = 1e-15

    val circle = Pga3dInertia.cylinderX(mass = 1.0, length = 0.0, r = 1.0, innerR = 1.0)

    assert(Math.abs(circle.mryz - 1.0) < eps)
    assert(Math.abs(circle.mrxz - 0.5) < eps)
    assert(Math.abs(circle.mrxy - 0.5) < eps)
  }

  test("inertia for rod") {
    val eps = 1e-15

    val circle = Pga3dInertia.cylinderX(mass = 12.0, length = 1.0, r = 0.0)

    assert(Math.abs(circle.mryz - 0.0) < eps)
    assert(Math.abs(circle.mrxy - 1.0) < eps)
    assert(Math.abs(circle.mrxz - 1.0) < eps)
  }

  test("inertia for cylinder") {
    val eps = 1e-15

    val cylinder = Pga3dInertia.cylinderX(mass = 1.0, length = 1.0, r = 1.0)

    assert(Math.abs(cylinder.mryz - 0.5) < eps)
    assert(Math.abs(cylinder.mrxz - (0.25 + 1.0 / 12.0)) < eps)
    assert(Math.abs(cylinder.mrxy - (0.25 + 1.0 / 12.0)) < eps)
  }

  test("inertia for cylinder as sum of rod and cylinder") {
    val eps = 1e-15

    val mass = 4.0
    val length = 3.0
    val r = 1.0

    val rod = Pga3dInertia.cylinderX(mass = mass, length = length, r = 0)
    val circle = Pga3dInertia.cylinderX(mass = mass, length = 0.0, r = r)
    val cylinder = Pga3dInertia.cylinderX(mass = mass, length = length, r = r)

    assert(cylinder.mass == circle.mass && cylinder.mass == rod.mass)
    assert(Math.abs(rod.mrxy + circle.mrxy - cylinder.mrxy) < eps)
    assert(Math.abs(rod.mrxz + circle.mrxz - cylinder.mrxz) < eps)
    assert(Math.abs(rod.mryz + circle.mryz - cylinder.mryz) < eps)
  }

  test("Parallel axis theorem for models") {
    forAll(anyShifts, models, MinSuccessful(100)) { (offset, model) =>
      val shift = Pga3dTranslator.addVector(offset)
      val movedPoint = Pga3dInertia.point(model.mass).movedBy(shift)
      val movedPointPlusCube = movedPoint.toSummable + model.toSummable
      val movedModel = model.movedBy(shift).toSummable
      assert((movedPointPlusCube.copy(ww = movedModel.ww) - movedModel).norm < 1e-12)
    }
  }

  test("sum of shifts is shift of sum") {
    forAll(anyShifts, anyShifts, models, MinSuccessful(100)) { (offset0, offset1, model) =>
      val afterTwoMoves = model.movedBy(Pga3dTranslator.addVector(offset0)).movedBy(Pga3dTranslator.addVector(offset1))
      val afterSingleMove = model.movedBy(Pga3dTranslator.addVector(offset0 + offset1))
      assert((afterTwoMoves.toSummable - afterSingleMove.toSummable).norm < 1e-12)
    }
  }

  test("inertia for cylinder equal to sum of two its parts") {
    forAll(
      Pga3dVectorMathGenerators.double01.map(_ + 0.1),
      Pga3dVectorMathGenerators.double01.map(_ + 0.1),
      Pga3dVectorMathGenerators.double01.map(_ + 0.1),
      Pga3dVectorMathGenerators.double01.map(_ + 0.1),
    ) { (mass, length, r1, r2) =>
      val innerR = math.min(r1, r2)
      val outerR = math.max(r1, r2)

      val cylinder = Pga3dInertia.cylinderX(mass = mass, length = length, r = outerR, innerR = innerR)

      val part0 = Pga3dInertia.cylinderX(mass = mass * 0.5, length = length * 0.5, r = outerR, innerR = innerR)
        .movedBy(Pga3dTranslator.addVector(Pga3dVector(length / 4, 0, 0)))

      val part1 = Pga3dInertia.cylinderX(mass = mass * 0.5, length = length * 0.5, r = outerR, innerR = innerR)
        .movedBy(Pga3dTranslator.addVector(Pga3dVector(-length / 4, 0, 0)))

      val eps = 1e-15

      val sum = part0.toSummable + part1.toSummable
      assert((sum - cylinder.toSummable).norm < eps)
    }
  }

  test("inertia of cube as sum of 8 small cubes") {
    val mass = 8.0
    val r = 6.0

    val cube = Pga3dInertia.cube(mass, r, r, r)

    val smallCube = Pga3dInertia.cube(mass / 8, r / 2, r / 2, r / 2)

    val smallCubes =
      for (dx <- Seq(-1, 1); dy <- Seq(-1, 1); dz <- Seq(-1, 1))
        yield smallCube.movedBy(Pga3dTranslator.addVector(Pga3dVector(dx * r / 2, dy * r / 2, dz * r / 2)))

    val sum = smallCubes.map(_.toSummable).reduce(_ + _)

    assert(cube.mass == sum.mass &&
      cube.toSummable.xx == sum.xx &&
      cube.toSummable.yy == sum.yy &&
      cube.toSummable.zz == sum.zz,
      s"${cube.toSummable} != ${sum}")
  }

  test("inertia of parallelepiped equal to a sum of two cubes") {
    val mass = 5.0
    val parallelepiped = Pga3dInertia.cube(mass, 1.0, 0.5, 0.5)
    val cube = Pga3dInertia.cube(mass / 2, 0.5, 0.5, 0.5)

    val sum = cube.movedBy(Pga3dTranslator.addVector(Pga3dVector(0.5, 0.0, 0.0))).toSummable +
      cube.movedBy(Pga3dTranslator.addVector(Pga3dVector(-0.5, 0.0, 0.0))).toSummable

    assert((parallelepiped.toSummable - sum).norm < 1e-14)
  }

  test("test method fromXXYYZZ") {
    val eps = 1e-15

    val localInertiaGenerator = for {
      mass <- Pga3dVectorMathGenerators.double01.map(_ + 0.1)
      rx <- Pga3dVectorMathGenerators.double01.map(_ + 0.1)
      ry <- Pga3dVectorMathGenerators.double01.map(_ + 0.1)
      rz <- Pga3dVectorMathGenerators.double01.map(_ + 0.1)
    } yield Pga3dInertiaLocal.cube(mass, rx, ry, rz)

    forAll(localInertiaGenerator) { inertia =>
      val summable = inertia.toSummable
      val restored = Pga3dInertia.fromXXYYZZ(summable.mass, summable.xx / summable.mass, summable.yy / summable.mass, summable.zz / summable.mass)

      assert(Math.abs(inertia.mass - restored.mass) < eps)
      assert(Math.abs(inertia.mrxy - restored.mrxy) < eps)
      assert(Math.abs(inertia.mrxz - restored.mrxz) < eps)
      assert(Math.abs(inertia.mryz - restored.mryz) < eps)
    }
  }
