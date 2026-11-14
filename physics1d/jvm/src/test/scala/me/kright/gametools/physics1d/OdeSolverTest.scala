package me.kright.gametools.physics1d

import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer

class OdeSolverTest extends AnyFunSuiteLike:
  private val solvers: Seq[(solver: OdeSolver, order: Int, forceEvaluationsPerStep: Int)] = Seq(
    (OdeSolverEuler, 1, 1),
    (OdeSolverMidPoint, 2, 2),
    (OdeSolverHeun, 2, 2),
    (OdeSolverRK4, 4, 4),
    (OdeSolverRKF45(), 4, 6),
    (OdeSolverGaussLegendre(iterations = 6), 4, 3 + 2 * 6),
  )

  private def assertOrder(solver: OdeSolver, expected: Int, error: Double, errorAtHalfStep: Double): Unit =
    val measured = Math.log(error / errorAtHalfStep) / Math.log(2.0)
    assert(Math.abs(measured - expected) < 0.1, s"$solver: measured order $measured, expected $expected")

  private def bits(system: Integrable): Seq[Long] =
    val state = new Array[Double](system.stateSize)
    system.saveState(state, 0)
    ArraySeq.unsafeWrapArray(state.map(java.lang.Double.doubleToRawLongBits))

  test("every derivative is taken after a reset and the force callback, at a freshly loaded state") {
    for (s <- solvers) {
      val log = ArrayBuffer[String]()
      s.solver.step(OdeSolverCallRecorder(log), 0.01, _ => log += "forces", OdeSolverCache())

      val evaluation = Seq("reset", "forces", "derivative")
      val expected = Seq("save") ++ evaluation ++ Seq.fill(s.forceEvaluationsPerStep - 1)("load" +: evaluation).flatten ++ Seq("load")
      assert(log.toSeq == expected, s"solver ${s.solver}")
    }
  }

  test("the force callback of RK4 gets the time offsets 0, dt / 2, dt / 2, dt") {
    val offsets = ArrayBuffer[Double]()
    OdeSolverRK4.step(makeMass(), 0.1, timeInsideStep => offsets += timeInsideStep)
    assert(offsets.toSeq == Seq(0.0, 0.05, 0.05, 0.1))
  }

  private def twoMassesOnSpringError(solver: OdeSolver, dt: Double, totalTime: Double): Double =
    val massA = 2.0
    val massB = 3.0
    val stiffness = 50.0
    val restLength = 2.0
    val initialA = 1.0
    val initialB = 4.0

    val a = PointMass1d(massA, initialA, 0.0)
    val b = PointMass1d(massB, initialB, 0.0)
    val system = Integrable(a, b)
    val cache = OdeSolverCache()

    val initialStretch = initialB - initialA - restLength
    val reducedMass = 1.0 / (1.0 / massA + 1.0 / massB)
    val frequency = Math.sqrt(stiffness / reducedMass)

    var maxError = 0.0
    for (step <- 1 to Math.round(totalTime / dt).toInt) {
      solver.step(system, dt, _ => {
        val stretch = b.x - a.x - restLength
        a.addForce(stiffness * stretch)
        b.addForce(-stiffness * stretch)
      }, cache)

      val stretchChange = initialStretch * Math.cos(frequency * step * dt) - initialStretch
      val exactA = initialA - stretchChange * massB / (massA + massB)
      val exactB = initialB + stretchChange * massA / (massA + massB)
      maxError = Math.max(maxError, Math.max(Math.abs(a.x - exactA), Math.abs(b.x - exactB)))
    }
    maxError

  test("two masses joined by a spring converge to the exact solution with the order of the solver") {
    for (s <- solvers) {
      val error = twoMassesOnSpringError(s.solver, 0.005, totalTime = 1.0)
      val errorAtHalfStep = twoMassesOnSpringError(s.solver, 0.0025, totalTime = 1.0)
      assertOrder(s.solver, s.order, error, errorAtHalfStep)
    }
  }

  private def directionError(solver: OdeSolver, dt: Double, normalize: Boolean): (error: Double, normDrift: Double) =
    val damping = 0.7
    val initialAngle = 0.3
    val initialOmega = 5.0
    val totalTime = 2.0
    val direction = UnitDirection(damping, initialAngle, initialOmega, normalize)
    val cache = OdeSolverCache()

    var maxError = 0.0
    var maxNormDrift = 0.0
    for (step <- 1 to Math.round(totalTime / dt).toInt) {
      solver.step(direction, dt, _ => (), cache)

      val angle = direction.exactAngle(initialAngle, initialOmega, step * dt)
      maxError = Math.max(maxError, Math.hypot(direction.cos - Math.cos(angle), direction.sin - Math.sin(angle)))
      maxNormDrift = Math.max(maxNormDrift, Math.abs(direction.norm - 1.0))
    }
    (maxError, maxNormDrift)

  test("a unit vector without normalization converges with the order of the solver") {
    for (s <- solvers) {
      val error = directionError(s.solver, 0.005, normalize = false).error
      val errorAtHalfStep = directionError(s.solver, 0.0025, normalize = false).error
      assertOrder(s.solver, s.order, error, errorAtHalfStep)
    }
  }

  test("normalization in loadState keeps the vector unit and costs no accuracy, so the order is kept too") {
    for (s <- solvers; dt <- Seq(0.005, 0.0025)) {
      val normalized = directionError(s.solver, dt, normalize = true)
      val plain = directionError(s.solver, dt, normalize = false)

      assert(normalized.normDrift < 1e-15, s"solver ${s.solver}, dt = $dt")
      assert(normalized.error < 1.5 * plain.error, s"solver ${s.solver}, dt = $dt")
    }
  }

  test("RK4 alone does not keep the vector unit") {
    val normalized = directionError(OdeSolverRK4, 0.005, normalize = true)
    val plain = directionError(OdeSolverRK4, 0.005, normalize = false)
    assert(plain.normDrift > 1000.0 * normalized.normDrift)
  }

  private def makeMass(): PointMass1d = PointMass1d(2.0, 1.0, 0.5)

  private def makeDirection(): UnitDirection = UnitDirection(0.7, 0.3, 5.0)

  private def addMassForce(mass: PointMass1d): Unit =
    mass.addForce(-40.0 * mass.x)

  private val steps = 50
  private val dt = 0.003

  test("uncoupled objects do not affect each other, in any grouping") {
    for (s <- solvers) {
      val massAlone = makeMass()
      val directionAlone = makeDirection()
      for (_ <- 0 until steps) {
        s.solver.step(massAlone, dt, _ => addMassForce(massAlone))
        s.solver.step(directionAlone, dt, _ => ())
      }

      for (nested <- Seq(false, true)) {
        val mass = makeMass()
        val direction = makeDirection()
        val system =
          if (nested) Integrable(Integrable(), Integrable(Integrable(direction), Integrable.empty), mass)
          else Integrable(mass, direction)
        for (_ <- 0 until steps) {
          s.solver.step(system, dt, _ => addMassForce(mass))
        }

        assert(bits(mass) == bits(massAlone), s"solver ${s.solver}, nested = $nested")
        assert(bits(direction) == bits(directionAlone), s"solver ${s.solver}, nested = $nested")
      }
    }
  }

  private def runCoupledMasses(solver: OdeSolver, nextCache: () => OdeSolverCache | Null): Seq[Long] =
    val a = makeMass()
    val b = PointMass1d(3.0, 2.5, -0.2)
    val system = Integrable(a, makeDirection(), b)
    for (_ <- 0 until steps) {
      solver.step(system, dt, _ => {
        val stretch = b.x - a.x - 1.0
        a.addForce(30.0 * stretch)
        b.addForce(-30.0 * stretch)
      }, nextCache())
    }
    bits(system)

  test("the result does not depend on the cache: absent, new on every step, reused, filled with NaN before every step") {
    for (s <- solvers) {
      val withoutCache = runCoupledMasses(s.solver, () => null)

      assert(runCoupledMasses(s.solver, () => OdeSolverCache()) == withoutCache, s"solver ${s.solver}, a new cache on every step")

      val reused = OdeSolverCache()
      assert(runCoupledMasses(s.solver, () => reused) == withoutCache, s"solver ${s.solver}, a reused cache")

      val filledWithNaN = OdeSolverCache()
      val poison = () => {
        for (array <- filledWithNaN.getArrays(16, 100)) {
          java.util.Arrays.fill(array, Double.NaN)
        }
        filledWithNaN
      }
      assert(runCoupledMasses(s.solver, poison) == withoutCache, s"solver ${s.solver} read a double it did not write during the step")
    }
  }

  test("a cache shared by systems of different sizes gives the same results") {
    val expected = runCoupledMasses(OdeSolverRK4, () => null)

    val shared = OdeSolverCache()
    val biggerSystem = Integrable(Seq.fill(40)(makeDirection())*)
    val stepBiggerSystemFirst = () => {
      OdeSolverRK4.step(biggerSystem, dt, _ => (), shared)
      shared
    }
    assert(runCoupledMasses(OdeSolverRK4, stepBiggerSystemFirst) == expected)
  }

  test("an empty system is a legal step") {
    for (s <- solvers) {
      var calls = 0
      s.solver.step(Integrable.empty, dt, _ => calls += 1, OdeSolverCache())
      s.solver.step(Integrable(), dt, _ => calls += 1)
      assert(calls == 2 * s.forceEvaluationsPerStep, s"solver ${s.solver}")
    }
  }

  test("RKF45 estimates the error of every double of the system, scaling as dt^5") {
    def estimate(dt: Double): OdeSolverRKF45 =
      val solver = OdeSolverRKF45()
      val mass = PointMass1d(1.0, 1.0, 0.7)
      solver.step(Integrable(mass, makeDirection()), dt, _ => mass.addForce(-40.0 * mass.x))
      solver

    val full = estimate(0.02)
    val half = estimate(0.01)

    assert(full.lastErrors.length == 5)
    assert(full.lastMaxError == full.lastErrors.max)

    val expectedRatio = Math.pow(2.0, 5)
    for (j <- 0 until 5) {
      val ratio = full.lastErrors(j) / half.lastErrors(j)
      assert(ratio > expectedRatio / 1.3 && ratio < expectedRatio * 1.3, s"double $j: ratio = $ratio")
    }
  }

  test("the RKF45 estimate matches the actual local error of the step") {
    for (dt <- Seq(0.05, 0.02)) {
      val stiffness = 40.0
      val initialX = 1.0
      val initialV = 0.7
      val solver = OdeSolverRKF45()
      val mass = PointMass1d(1.0, initialX, initialV)
      solver.step(mass, dt, _ => mass.addForce(-stiffness * mass.x))

      val frequency = Math.sqrt(stiffness)
      val exactX = initialX * Math.cos(frequency * dt) + initialV / frequency * Math.sin(frequency * dt)
      val exactV = -initialX * frequency * Math.sin(frequency * dt) + initialV * Math.cos(frequency * dt)

      val estimate = solver.lastErrors(0) + solver.lastErrors(1)
      val actual = Math.abs(mass.x - exactX) + Math.abs(mass.v - exactV)
      val ratio = estimate / actual
      assert(ratio > 0.9 && ratio < 1.1, s"dt = $dt: estimate / actual = $ratio")
    }
  }


final class OdeSolverCallRecorder(log: ArrayBuffer[String]) extends Integrable:
  private var value = 1.0

  override val stateSize: Int = 1

  override def saveState(dst: Array[Double], offset: Int): Unit =
    log += "save"
    dst(offset) = value

  override def loadState(src: Array[Double], offset: Int): Unit =
    log += "load"
    value = src(offset)

  override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    log += "derivative"
    dst(offset) = -value

  override def resetForces(): Unit =
    log += "reset"
