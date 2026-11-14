package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.physics1d.{OdeSolver, OdeSolverRKF45}
import org.scalatest.funsuite.AnyFunSuiteLike
import scala.collection.immutable.ArraySeq

class Pga3dBodiesOdeSolverRKF45Test extends AnyFunSuiteLike:

  private def freeBodySystem(solver: OdeSolver): Pga3dPhysicsSystemForTest =
    Pga3dPhysicsSystemForTest(Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)), solver)

  private def norm(values: Array[Double]): Double =
    Math.sqrt(values.map(v => v * v).sum)

  private def motorPlusLocalBErrorOfSingleBody(solver: OdeSolverRKF45): Double =
    require(solver.lastErrors.length == Pga3dBodyState.componentsCount)
    val (motorErrors, localBErrors) = solver.lastErrors.splitAt(Pga3dMotor.componentsCount)
    norm(motorErrors) + norm(localBErrors)

  test("calculate free rotation body precession for RKF45") {
    val stepsCount = 1000
    val dt = 0.01

    val system = freeBodySystem(OdeSolverRKF45())
    val errors = for (_ <- 0 until stepsCount) yield {
      system.doStep(dt, _ => ())
      system.getError()
    }
    val maxError = errors.reduce(_ max _)
    // measured 6.2e-12 / 1.8e-10 - the Fehlberg 4th-order tableau has ~8x smaller error
    // constants on this problem than the classic RK4 one (5.3e-11 / 8.6e-10)
    assert(maxError < ErrorOfEnergyAndMomentum(errorE = 1e-11, errorL = 3e-10))
  }

  test("the error estimate scales as dt^5") {
    for (dt <- ArraySeq(0.02, 0.01)) {
      val solver = OdeSolverRKF45()
      val system = freeBodySystem(solver)

      system.doStep(dt, _ => ())
      val eFull = motorPlusLocalBErrorOfSingleBody(solver)

      val solverHalf = OdeSolverRKF45()
      val systemHalf = freeBodySystem(solverHalf)
      systemHalf.doStep(dt * 0.5, _ => ())
      val eHalf = motorPlusLocalBErrorOfSingleBody(solverHalf)

      val ratio = eFull / eHalf
      // println(s"dt = $dt, eFull = $eFull, eHalf = $eHalf, ratio = $ratio")
      assert(ratio > 25.0 && ratio < 40.0, s"dt = $dt, ratio = $ratio, expected ~32")
    }
  }

  test("the error estimate matches the actual local error of the step") {
    for (dt <- ArraySeq(0.05, 0.02, 0.01)) {
      val solver = OdeSolverRKF45()
      val system = freeBodySystem(solver)
      system.doStep(dt, _ => ())
      val estimate = motorPlusLocalBErrorOfSingleBody(solver)

      val referenceBodies = Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id))
      val referenceSystem = Pga3dPhysicsSystemForTest(referenceBodies, Pga3dOdeSolverRKMK4(referenceBodies))
      val substeps = 1000
      for (_ <- 0 until substeps) {
        referenceSystem.doStep(dt / substeps, _ => ())
      }

      val state = Pga3dBodyState(system.bodies.head)
      val reference = Pga3dBodyState(referenceSystem.bodies.head)
      val actual = (state.motor - reference.motor).norm + (state.localB - reference.localB).norm

      // measured ratios 0.98-1.01: the estimate is essentially exact for a single step
      assert(estimate / actual > 0.5 && estimate / actual < 2.0,
        s"dt = $dt, estimate = $estimate, actual = $actual")
    }
  }

  test("the error estimate maps where the error lives: stiff spring vs soft spring") {
    val dt = 0.01

    def maxEstimateWithSpring(k: Double): Double =
      val body = Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(1.0, 1.0, 1.0, 1.0), Pga3dMotor.id)
      val solver = OdeSolverRKF45()
      val system = Pga3dPhysicsSystem(Array(body), solver)
      val springCenter = Pga3dPoint(3.0, 4.0, 0.0)

      var maxEstimate = 0.0
      for (_ <- 0 until 100) {
        system.doStep(dt, _ => {
          val globalForque = (system.bodies.head.globalCenter v springCenter) * k
          system.bodies.head.addGlobalForque(globalForque)
        })
        maxEstimate = Math.max(maxEstimate, motorPlusLocalBErrorOfSingleBody(solver))
      }
      maxEstimate

    // measured: soft = 7.2e-13, stiff = 6.5e-4 - the estimate immediately points at the
    // stiff constraint without rerunning the simulation at a smaller dt
    val soft = maxEstimateWithSpring(k = 1.0)
    val stiff = maxEstimateWithSpring(k = 1000.0)
    assert(stiff > soft * 1000.0, s"soft = $soft, stiff = $stiff")
  }
