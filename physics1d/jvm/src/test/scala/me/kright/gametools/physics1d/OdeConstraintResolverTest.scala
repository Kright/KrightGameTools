package me.kright.gametools.physics1d

import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer

class OdeConstraintResolverTest extends AnyFunSuiteLike:
  private val solvers: Seq[(solver: OdeSolver, order: Int, forceEvaluationsPerStep: Int)] = Seq(
    (OdeSolverEuler, 1, 1),
    (OdeSolverMidPoint, 2, 2),
    (OdeSolverHeun, 2, 2),
    (OdeSolverRK4, 4, 4),
    (OdeSolverRKF45(), 4, 6),
    (OdeSolverGaussLegendre(iterations = 6), 4, 3 + 2 * 6),
  )

  test("the constraint forces are added after the force callback, the projection follows the final load") {
    for (s <- solvers) {
      val log = ArrayBuffer[String]()
      s.solver.step(OdeSolverCallRecorder(log), 0.01, _ => log += "forces", resolver = OdeConstraintResolverCallRecorder(log))

      val evaluation = Seq("reset", "forces", "constraint forces", "derivative")
      val expected = Seq("save") ++ evaluation ++ Seq.fill(s.forceEvaluationsPerStep - 1)("load" +: evaluation).flatten ++ Seq("load", "project")
      assert(log.toSeq == expected, s"solver ${s.solver}")
    }
  }

  private val massA = 2.0
  private val massB = 3.0
  private val rodLength = 1.5
  private val initialA = 0.5
  private val initialVelocity = 0.7
  private val forceAmplitude = 40.0
  private val forceFrequency = 5.0

  private def exactDisplacementOfRigidPair(time: Double): Double =
    val accelerationAmplitude = forceAmplitude / (massA + massB)
    initialVelocity * time + accelerationAmplitude / (forceFrequency * forceFrequency) * (1.0 - Math.cos(forceFrequency * time))

  private def runTwoMassesWithForceOnTheFirst(solver: OdeSolver, dt: Double, totalTime: Double, withRod: Boolean): (positionError: Double, rodError: Double) =
    val a = PointMass1d(massA, initialA, initialVelocity)
    val b = PointMass1d(massB, initialA + rodLength, initialVelocity)
    val system = Integrable(a, b)
    val cache = OdeSolverCache()
    val resolver: OdeConstraintResolver | Null = if (withRod) RigidRod1dConstraintResolver(a, b, rodLength) else null

    var maxPositionError = 0.0
    var maxRodError = 0.0
    for (step <- 1 to Math.round(totalTime / dt).toInt) {
      val stepStart = (step - 1) * dt
      solver.step(system, dt, timeInsideStep => a.addForce(forceAmplitude * Math.cos(forceFrequency * (stepStart + timeInsideStep))), cache, resolver)

      val exactA = initialA + exactDisplacementOfRigidPair(step * dt)
      maxPositionError = Math.max(maxPositionError, Math.max(Math.abs(a.x - exactA), Math.abs(b.x - exactA - rodLength)))
      maxRodError = Math.max(maxRodError, Math.abs(b.x - a.x - rodLength))
    }
    (maxPositionError, maxRodError)

  test("two masses on a rigid rod move as one body: the order of the solver is kept, the rod holds to the rounding") {
    for (s <- solvers) {
      val result = runTwoMassesWithForceOnTheFirst(s.solver, 0.01, totalTime = 2.0, withRod = true)
      val resultAtHalfStep = runTwoMassesWithForceOnTheFirst(s.solver, 0.005, totalTime = 2.0, withRod = true)

      val measuredOrder = Math.log(result.positionError / resultAtHalfStep.positionError) / Math.log(2.0)
      assert(Math.abs(measuredOrder - s.order) < 0.1, s"${s.solver}: measured order $measuredOrder, expected ${s.order}")
      assert(result.rodError < 1e-14 && resultAtHalfStep.rodError < 1e-14, s"solver ${s.solver}")
    }
  }

  test("without the resolver the masses part") {
    val result = runTwoMassesWithForceOnTheFirst(OdeSolverRK4, 0.01, totalTime = 2.0, withRod = false)
    assert(result.rodError > 1.0)
  }

  test("a null resolver is the same as no resolver") {
    def bits(system: Integrable): Seq[Long] =
      val state = new Array[Double](system.stateSize)
      system.saveState(state, 0)
      ArraySeq.unsafeWrapArray(state.map(java.lang.Double.doubleToRawLongBits))

    for (s <- solvers) {
      val omitted = PointMass1d(2.0, 1.0, 0.5)
      val explicitNull = PointMass1d(2.0, 1.0, 0.5)
      for (_ <- 0 until 20) {
        s.solver.step(omitted, 0.01, _ => omitted.addForce(-40.0 * omitted.x))
        s.solver.step(explicitNull, 0.01, _ => explicitNull.addForce(-40.0 * explicitNull.x), null, null)
      }
      assert(bits(omitted) == bits(explicitNull), s"solver ${s.solver}")
    }
  }


final class OdeConstraintResolverCallRecorder(log: ArrayBuffer[String]) extends OdeConstraintResolver:
  override def addConstraintForces(): Unit =
    log += "constraint forces"

  override def project(): Unit =
    log += "project"
