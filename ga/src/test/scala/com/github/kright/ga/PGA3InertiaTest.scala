package com.github.kright.ga

import com.github.kright.symbolic.Sym
import com.github.kright.util.EqualityEps
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.math.Numeric.Implicits.infixNumericOps


class PGA3InertiaTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private given ga: PGA3 = GA.pga3

  private val eps = 1e-12

  private given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)

  private given equalityEps: EqualityEps = EqualityEps(eps)

  private val b = Sym.multiVector("b").grade(2)

  // @formatter:off
  private def bxy = b("xy")
  private def bxz = b("xz")
  private def byz = b("yz")
  private def bwx = b("wx")
  private def bwy = b("wy")
  private def bwz = b("wz")
  // @formatter:on

  test("inertia") {
    val x = Sym.point("x")

    val mass = Sym("mass")
    val inertia = (x v x.crossX2(b) * Sym(0.5)) * mass

    val sq = {
      for (dx <- Seq(-1, 1);
           dy <- Seq(-2, 2);
           dz <- Seq(-4, 4);
           point = PGA3.point(Sym(dx), Sym(dy), Sym(dz));
           inertia = (point v point.crossX2(b) * Sym(0.5)))
      yield inertia
    }.reduce(_ + _).withoutZeros

    assert(
      sq == MultiVector(
        "wx" -> Sym("b.yz") * Sym((2 * 2 + 4 * 4) * 8.0),
        "wy" -> Sym("b.xz") * Sym((1 + 4 * 4) * -8.0),
        "wz" -> Sym("b.xy") * Sym((1 + 2 * 2) * 8.0),
        "xy" -> Sym("b.wz") * Sym(8.0),
        "xz" -> Sym("b.wy") * Sym(-8.0),
        "yz" -> Sym("b.wx") * Sym(8.0),
      )
    )

    val i3d = PGA3Inertia(8, (2 * 2 + 4 * 4) * 8.0, (1 + 4 * 4) * 8.0, (1 + 2 * 2) * 8.0).mapValues(Sym(_))

    assert(i3d.invert(i3d(b)) == b)
  }

  test("kinetic energy") {
    val i3d = PGA3Inertia.cube(3, 1, 1, 4)
    assert(i3d.mass == MultiVector("wx" -> 3.0, "wy" -> 3.0, "wz" -> 3.0, "xy" -> 2.0, "xz" -> 17.0, "yz" -> 17.0))

    val i3dSym = i3d.mapValues(Sym(_))
    val kineticEnergy = (b ^ i3dSym(b)) * Sym(0.5) // as pseudoScalar
    val kineticEnergyV2 = (b v i3dSym(b)) * Sym(0.5) // as scalar


    assert(kineticEnergy == MultiVector("I" -> (Sym(1.5) * (bwx * bwx + bwy * bwy + bwz * bwz) + bxy * bxy + Sym(8.5) * bxz * bxz + Sym(8.5) * byz * byz)))
    assert(kineticEnergyV2 == MultiVector("1" -> (Sym(1.5) * (bwx * bwx + bwy * bwy + bwz * bwz) + bxy * bxy + Sym(8.5) * bxz * bxz + Sym(8.5) * byz * byz)))
  }

  test("power of forque") {
    val f = Sym.multiVector("f", grade = 2)
    val power = b v f
    assert(power == MultiVector(
      "1" -> (bwx * f("yz") + bwz * f("xy") + bxy * f("wz") + byz * f("wx") - bwy * f("xz") - bxz * f("wy"))
    ))
  }

  test("power of linear force and linear speed") {
    val fx = Sym("fx")
    val fy = Sym("fy")
    val fz = Sym("fz")

    val f = Sym.point("p").dot(MultiVector("x" -> fx, "y" -> fy, "z" -> fz))

    val vx = Sym("vx")
    val vy = Sym("vy")
    val vz = Sym("vz")

    val bLinear = MultiVector(
      "wx" -> vx,
      "wy" -> vy,
      "wz" -> vz,
    )

    val power = bLinear v f
    assert(power == MultiVector(
      "1" -> (vx * fx + vy * fy + vz * fz)
    ))
  }

  /**
   * see https://bivector.net/PGAdyn.pdf
   *
   * I[B] = Sum(mass_i X_i v (X_i x B))
   * B' = I**{-1}[B x I[B] + F]
   * M' = -0.5 M B
   * I, B, F are in local frame, where I is diagonal matrix
   */
  test("test precession") {
    //    val i3d = Inertia3d.cube(3, 1, 1, 4).mapValues(Sym(_))
    val m = Sym("mass")
    val mryz = Sym("Ix")
    val mrxz = Sym("Iy")
    val mrxy = Sym("Iz")

    val mInv = Sym("1 / mass")
    val mryzInv = Sym("1 / Ix")
    val mrxzInv = Sym("1 / Iy")
    val mrxyInv = Sym("1 / Iz")

    val i3d = PGA3Inertia(
      MultiVector(
        "wx" -> m,
        "wy" -> m,
        "wz" -> m,
        "yz" -> mryz,
        "xz" -> mrxz,
        "xy" -> mrxy,
      ),
      MultiVector(
        "wx" -> mInv,
        "wy" -> mInv,
        "wz" -> mInv,
        "yz" -> mryzInv,
        "xz" -> mrxzInv,
        "xy" -> mrxyInv,
      ),
    )

    val forque = Sym.multiVector("f", grade = 2)
    val dB = i3d.invert(b.crossX2(i3d(b)) * Sym(0.5) + forque)

    assert(dB.toMultilineString ==
      """MultiVector(
        |"wx" -> (1 / mass * f.yz + 1 / mass * b.wy * b.xy * mass + 1 / mass * b.wz * b.xz * mass)
        |"wy" -> (-1 / mass * f.xz + 1 / mass * b.wz * b.yz * mass - 1 / mass * b.wx * b.xy * mass)
        |"wz" -> (1 / mass * f.xy - 1 / mass * b.wx * b.xz * mass - 1 / mass * b.wy * b.yz * mass)
        |"xy" -> (1 / Iz * f.wz + 1 / Iz * Iy * b.xz * b.yz - 1 / Iz * Ix * b.xz * b.yz)
        |"xz" -> (-1 / Iy * f.wy + 1 / Iy * Ix * b.xy * b.yz - 1 / Iy * Iz * b.xy * b.yz)
        |"yz" -> (1 / Ix * f.wx + 1 / Ix * Iz * b.xy * b.xz - 1 / Ix * Iy * b.xy * b.xz)
        |)""".stripMargin)
  }

  /** same as [com.github.kright.physics3d.InertiaTest] */
  test("calculate free rotation body precession for RK2") {
    val forque = MultiVector.zero[Double]
    val maxError = testOneBodySimple123(100, body => {
      body.doStepRK2(dt = 0.01, forque)
      body.getError()
    }).reduce(_ max _)
    assert(maxError < PGA3OneBody.Error(errorE = 1e-5, errorL = 1.3e-5))
  }

  test("calculate free rotation body precession for Euler2") {
    val forque = MultiVector.zero[Double]
    val maxError = testOneBodySimple123(100, body => {
      body.doStepEuler2(dt = 0.01, forque)
      body.getError()
    }).reduce(_ max _)
    assert(maxError < PGA3OneBody.Error(errorE = 3e-7, errorL = 5e-5))
  }

  test("calculate free rotation body precession for RK4") {
    val stepsCount = 1000
    val forque = MultiVector.zero[Double]

    val maxError = testOneBodySimple123(stepsCount, body => {
      body.doStepRK4(dt = 0.01, forque)
      body.getError()
    }).reduce(_ max _)

    assert(maxError < PGA3OneBody.Error(errorE = 6e-11, errorL = 1e-9))
  }

  private def testOneBodySimple123[T](stepsCount: Int, doStep: PGA3OneBody => T): Iterable[T] = {
    val body = PGA3OneBody.simple123()

    assert(body.initialE == 3.0)
    assert(body.initialL == MultiVector("wx" -> 3.0, "wy" -> 2.0, "wz" -> 1.0))

    for (_ <- 0 until stepsCount)
      yield doStep(body)
  }

  test("calculate energy accumulation for linear force during rotation") {
    val dt = 0.01
    val stepsCount = 100

    val forceDirection = MultiVector("x" -> 1.0)
    val body = PGA3OneBody.simple123()

    assert(body.initialE == 3.0)
    assert(body.initialL == MultiVector("wx" -> 3.0, "wy" -> 2.0, "wz" -> 1.0))

    var accumulatedL = body.initialL

    val errors = for (step <- 1 to stepsCount) yield {
      val t = step * dt

      val bodyCenter = body.state.center
      val globalForque = bodyCenter.dot(forceDirection)
      body.doStepRK4(dt, globalForque)

      val expectedEnergy = body.initialE + 0.5 * forceDirection.squareMagnitude * t * t / body.inertia.linearMass
      accumulatedL += globalForque * dt

      val err = PGA3OneBody.Error(errorE = math.abs(expectedEnergy - body.getEnergy()), errorL = (accumulatedL - body.getL()).norm)
      err
    }

    val maxError = errors.reduce(_ max _)
    assert(maxError < PGA3OneBody.Error(2e-9, 4e-9))

    val endPoint = body.state.center
    val expectedPoint = PGA3.point(0.5, 0.0, 0.0)

    assert((endPoint - expectedPoint).norm < 2e-9)
  }

  test("calculate energy accumulation for linear spring") {
    val dt = 0.01
    val stepsCount = 1000

    val mass = 10.0

    val body = new PGA3OneBody(
      PGA3Inertia(mass, 1.0, 1.0, 1.0),
      PGA3State(
        MultiVector.scalar[Double](1.0),
        MultiVector.zero[Double]
      )
    )

    val springCenter = PGA3.point(3.0, 4.0, 0.0) // len 5
    val springK = 20

    def getEnergy(): Double = (body.state.center - springCenter).squareMagnitude * 0.5 * springK + body.getEnergy()

    val initialEnergy = getEnergy()

    for (step <- 1 to stepsCount) {
      val t = step * dt

      body.doStepRK4F(dt, getLocalForque = (state, time) => {
        val localForque1 = {
          val bodyCenter = state.center
          val globalForque = (bodyCenter v springCenter) * springK
          val localForque = state.motor.reverse.sandwich(globalForque)
          localForque
        }
        val localForque2 = {
          val localSpringPos = state.motor.reverse.sandwich(springCenter)
          val localForque = (PGA3.zeroPoint[Double] v localSpringPos) * springK
          localForque
        }
        assert((localForque1 - localForque2).norm < 1e-10)

        localForque1
      })

      val expectedPos = springCenter - springCenter.weight * Math.cos(t * Math.sqrt(springK / mass))

      val dE = math.abs(initialEnergy - getEnergy()) / initialEnergy
      val dPos = (expectedPos - body.state.center).norm

      assert(dE <= 1.2e-10)
      assert(dPos <= 2.4e-8)
    }
  }
