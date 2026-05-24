package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dPoint, Pga3dVector}
import me.kright.gametools.pga3d.physics.Pga3dFriction.Clamped
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dFrictionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private val maxV = 100
  private val bounds = (Pga3dPoint(-maxV, -maxV, -maxV), Pga3dPoint(maxV, maxV, maxV))
  private val velocities = Pga3dPhysicsGenerators.vectorIn(bounds._1, bounds._2)

  private val linearFriction: Gen[Pga3dFriction.Linear] =
    for (k <- Pga3dVectorMathGenerators.doubleInRange(0.0, 1e6))
      yield Pga3dFriction.Linear(k)

  private val quadraticFriction: Gen[Pga3dFriction.Quadratic] =
    for (k <- Pga3dVectorMathGenerators.doubleInRange(0.0, 1e4))
      yield Pga3dFriction.Quadratic(k)

  private val zeroFriction: Gen[Pga3dFriction] =
    Gen.oneOf(
      Seq(
        Pga3dFriction.Zero,
        Pga3dFriction.Linear(0.0),
        Pga3dFriction.Quadratic(0.0),
        Pga3dFriction.const(0.1, 0.0),
      )
    )

  private val nonClampedFrictions: Gen[Pga3dFriction] =
    Gen.oneOf(
      linearFriction,
      quadraticFriction,
      zeroFriction,
    )

  private val clampedFrictions: Gen[Pga3dFriction] =
    for (f <- nonClampedFrictions;
         max <- Pga3dVectorMathGenerators.doubleInRange(0, 1e6))
    yield Clamped(f, max)


  private val anyFriction: Gen[Pga3dFriction] =
    Gen.oneOf(
      nonClampedFrictions,
      clampedFrictions
    )

  test("zero friction") {
    forAll(zeroFriction, velocities, MinSuccessful(1000)) { (friction, velocity) =>
      val f = friction(velocity)

      assert(friction(velocity) == Pga3dVector.zero)
      assert(friction(velocity.norm) == 0.0)
    }
  }

  test("friction direction is backward or zero") {
    forAll(anyFriction, velocities, MinSuccessful(1000)) { (friction, velocity) =>
      val f = friction(velocity)

      if (f.norm > 1e-15 && velocity.norm > 1e-15) {
        val cos = f.antiDotI(velocity) / (f.norm * velocity.norm)

        assert(Math.abs(cos - -1.0) < 1e-15,
          s"""
             |f = $f
             |velocity = $velocity
             |velocity.norm = ${velocity.norm}
             |velocity.square = ${velocity.normSquare}
             |friction(velocity) = ${friction(velocity)}
             |""".stripMargin)
      }
    }
  }

  test("friction for vector norm and for vector are the same") {
    forAll(anyFriction, velocities, MinSuccessful(1000)) { (friction, velocity) =>
      val f1 = Math.abs(friction(velocity.norm))
      val f2 = friction(velocity).norm

      val err = Math.abs(f1 - f2) / Math.max(f1 + 1e-20, f2 + 1e-20)
      assert(err < 1e-15,
        s"""
           |f1 = $f1
           |f2 = $f2
           |velocity = $velocity
           |velocity.norm = ${velocity.norm}
           |velocity.square = ${velocity.normSquare}
           |friction(velocity) = ${friction(velocity)}
           |velocity.normalizedByNorm * friction(velocity.norm) = ${velocity.normalizedByNorm * friction(velocity.norm)}
           |""".stripMargin)
    }
  }
