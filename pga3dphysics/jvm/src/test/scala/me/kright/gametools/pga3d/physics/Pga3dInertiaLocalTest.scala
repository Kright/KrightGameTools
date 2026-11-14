package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.physics1d.*
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dInertiaLocalTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val eps = 1e-12

  private given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)


  test("getAcceleration is equal to apply invert") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors, Pga3dInertiaGenerators.inertiaLocal(0.1, 10.0, 0.1, 10.0), MinSuccessful(1000)) { (forque, b, inertia) =>

      val a0 = inertia.getAcceleration(b, forque)
      val a1 = inertia.invert(b.cross(inertia(b)) + forque)

      assert((a0 - a1).norm < 1e-12, s"a0 = $a0, a1 = $a1")
    }
  }

  test("getAcceleration test") {
    val localB = Pga3dBivector(0.1, 0.2, 0.3, 0.4, 0.5, 0.6)
    val localForque = Pga3dBivector(0.7, 0.8, 0.9, 1.0, 1.1, 1.2)
    val body = Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)
    val system = Pga3dPhysicsSystemForTest(Array(body), OdeSolverRK4)
    val acc = body.inertia.getAcceleration(localB, localForque)
    val accNaive = body.inertia.invert(localB.cross(body.inertia(localB)) + localForque)
    val err = (acc - accNaive).norm
    assert(err < 2e-16)
  }
