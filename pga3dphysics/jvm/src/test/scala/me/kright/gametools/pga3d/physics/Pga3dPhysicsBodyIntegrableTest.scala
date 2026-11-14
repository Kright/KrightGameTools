package me.kright.gametools.pga3d.physics

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dPhysicsBodyIntegrableTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val marker = -777.0
  private val size = Pga3dBodyState.componentsCount

  private def body(motor: Pga3dMotor, localB: Pga3dBivector): Pga3dPhysicsBody =
    Pga3dPhysicsBody(Pga3dInertia.moved(motor.reverse, Pga3dInertiaLocal(1.0, 3.0, 2.0, 1.0)), motor, localB)

  test("the state is the flat form of Pga3dBodyState") {
    assert(size == FlatDoubleSerializer.getSize[Pga3dBodyState])
    assert(size == Pga3dMotor.componentsCount + Pga3dBivector.componentsCount)

    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors) { (motor, localB) =>
      val b = body(motor, localB)
      assert(b.stateSize == size)

      val dst = Array.fill(size + 4)(marker)
      b.saveState(dst, 3)

      assert(dst(2) == marker && dst(3 + size) == marker)
      assert(FlatDoubleSerializer.read[Pga3dBodyState](dst, 3) == Pga3dBodyState(b))
    }
  }

  test("loadState of a saved state restores the body, the motor up to the rounding of its renormalization") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors, Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors) {
      (motor, localB, otherMotor, otherLocalB) =>
        val source = body(motor, localB)
        val saved = new Array[Double](size + 2)
        source.saveState(saved, 2)

        val target = body(otherMotor, otherLocalB)
        target.loadState(saved, 2)

        assert(target.localB == source.localB)
        assert((target.motor - source.motor).norm < 1e-15)
        assert(Math.abs(target.motor.bulkNormSquare - 1.0) < 1e-15)
    }
  }

  test("loadState projects a raw linear combination of motors back to a unit motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors) { (motorA, motorB, localB) =>
      val raw = motorA * 0.7 + motorB * 0.2
      whenever(raw.bulkNormSquare > 1e-3) {
        val flat = new Array[Double](size)
        FlatDoubleSerializer.write(Pga3dBodyState(raw, localB), flat, 0)

        val b = body(Pga3dMotor.id, Pga3dBivector.zero)
        b.loadState(flat, 0)

        assert((b.motor - raw.renormalized).norm == 0.0)
        assert(b.transform == Pga3dTransform(raw))
        assert(b.localB == localB)
      }
    }
  }

  test("saveDerivative is the flat form of Pga3dBodyState.derivative, with the accumulated forques") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors, Pga3dGenerators.bivectors) { (motor, localB, forque) =>
      val b = body(motor, localB)
      b.addGlobalForque(forque)

      val dst = Array.fill(size + 2)(marker)
      b.saveDerivative(dst, 1)

      assert(dst(0) == marker && dst(size + 1) == marker)
      assert(FlatDoubleSerializer.read[Pga3dBodyState](dst, 1) == Pga3dBodyState.derivative(b))
    }
  }

  test("resetAccum drops the accumulated forque") {
    val b = body(Pga3dMotor.id, Pga3dBivector.zero)
    b.addGlobalForque(Pga3dBivector(wx = 1.0, xy = 2.0))
    b.resetAccum()
    assert(b.globalForque == Pga3dBivector.zero)
  }
