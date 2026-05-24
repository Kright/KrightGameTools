package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dVelocityTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private def makeSystem(localB: Pga3dBivector) =
    Pga3dPhysicsSystem(
      state = Array(
        Pga3dPhysicsBody(
          Pga3dInertiaSimple(1.0, 1.0),
          Pga3dMotor.id,
          localB,
        )
      ),
      solver = Pga3dPhysicsSolverRK4,
    )

  test("linear velocity") {
    val dt = 1.0

    forAll(Pga3dGenerators.vectors, MinSuccessful(100)) { linearVelocity =>
      val velocity = Pga3dVelocity.linear(linearVelocity).toBivector
      val system = makeSystem(velocity)
      system.doStep(dt, addForquesToBodies = { _ => })

      val expectedPos: Pga3dPoint = Pga3dPoint.center + linearVelocity * dt

      val newPos: Pga3dPoint = system.state(0).globalCenterOfMass
      val newPos2: Pga3dPoint = velocity.exp(-dt * 0.5).sandwich(Pga3dPoint.center).toPoint

      assert((newPos - expectedPos).norm < 1e-15)
      assert((newPos2 - expectedPos).norm < 1e-15)
    }
  }

  test("linear velocity of point") {
    forAll(Pga3dGenerators.vectors, Pga3dGenerators.points, MinSuccessful(100)) { (linearVelocity, point) =>
      val velocity = Pga3dVelocity.linear(linearVelocity).toBivector
      val pointLinearVelocity: Pga3dVector = Pga3dVelocity.pointLinearVelocity(point, velocity)
      assert((pointLinearVelocity - linearVelocity).norm < 1e-15)
    }
  }

  test("angular velocity don't moves center") {
    val dt = 1.0

    forAll(Pga3dGenerators.bivectorBulks, MinSuccessful(100)) { w =>
      val system = makeSystem(w.toBivector)
      system.doStep(dt, addForquesToBodies = { _ => })

      val newCenterPos = system.state(0).motor.sandwich(Pga3dPoint.center).toPoint
      assert((newCenterPos - Pga3dPoint.center).norm < 1e-15)
    }
  }

  test("angular velocity center velocity is zero") {
    forAll(Pga3dGenerators.bivectorBulks, MinSuccessful(100)) { w =>
      val linearVelocity: Pga3dVector = Pga3dVelocity.pointLinearVelocity(Pga3dPoint.center, w.toBivector)
      assert(linearVelocity.norm < 1e-15)
    }
  }

  test("angular velocity for fixed center") {
    forAll(Pga3dGenerators.bivectorBulks, Pga3dGenerators.points, MinSuccessful(100)) { (w, rotationCenter) =>
      val velocity = Pga3dVelocity.angular(w, rotationCenter)
      val centerLinearVelocity = Pga3dVelocity.pointLinearVelocity(rotationCenter, velocity)
      assert(centerLinearVelocity.norm < 1e-15)
    }
  }
