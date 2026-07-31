package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq

/**
 * a chain of N bodies hanging on rods from the world anchor, released at an angle and
 * swinging under gravity: the constraints are coupled through the shared bodies, so this
 * exercises the Gauss-Seidel forque solve, the projection and the order of every solver on a
 * genuinely multi-constraint problem
 */
class Pga3dConstrainedChainTest extends AnyFunSuiteLike:
  private val origin = Pga3dPoint(0, 0, 0)
  private val linkLength = 0.5
  private val chainSize = 4
  private val gravity = Pga3dVector(0, -9.8, 0)

  private def chain(angle: Double): (Array[Pga3dPhysicsBody], Pga3dConstraintResolver) =
    val dir = Pga3dVector(Math.sin(angle), -Math.cos(angle), 0.0)
    val bodies = Array.tabulate(chainSize) { k =>
      Pga3dPhysicsBody.motionless(
        Pga3dInertiaLocal(1.0, 0.02, 0.02, 0.02),
        Pga3dTranslator.addVector(dir * (linkLength * (k + 1))).toMotor)
    }
    val constraints = ArraySeq.tabulate(chainSize) { k =>
      if (k == 0) Pga3dDistanceConstraint.rod(0, origin, Pga3dDistanceConstraint.world, origin, linkLength)
      else Pga3dDistanceConstraint.rod(k, origin, k - 1, origin, linkLength)
    }
    (bodies, Pga3dConstraintResolver(constraints))

  private def addGravity(bodies: Array[Pga3dPhysicsBody]): Unit =
    for (body <- bodies) {
      body.addGlobalForque(Pga3dForque.force(body.globalCenterOfMass, gravity * body.inertia.mass))
    }

  private def maxRodError(bodies: Array[Pga3dPhysicsBody], resolver: Pga3dConstraintResolver): Double =
    resolver.constraints.map { c =>
      val pA = if (c.bodyA >= 0) bodies(c.bodyA).motorSandwich(c.anchorA) else c.anchorA
      val pB = if (c.bodyB >= 0) bodies(c.bodyB).motorSandwich(c.anchorB) else c.anchorB
      Math.abs((pA - pB).norm - linkLength)
    }.max

  private def energy(bodies: Array[Pga3dPhysicsBody]): Double =
    bodies.map { body =>
      body.getKineticEnergy - body.inertia.mass * gravity.antiDotI(body.globalCenterOfMass.toVectorUnsafe)
    }.sum

  private def runChain(inner: Pga3dPhysicsSolver[Pga3dPhysicsBody],
                       dt: Double,
                       totalTime: Double): (Array[Pga3dPhysicsBody], Pga3dConstraintResolver) =
    val (bodies, resolver) = chain(angle = 0.3)
    val system = Pga3dPhysicsSystem(bodies, Pga3dPhysicsSolverConstrained(inner, resolver))
    for (_ <- 0 until Math.round(totalTime / dt).toInt) {
      system.doStep(dt, _ => addGravity(bodies))
    }
    (bodies, resolver)

  private def stateDifference(bodies: Array[Pga3dPhysicsBody], reference: Array[Pga3dPhysicsBody]): Double =
    bodies.indices.map { i =>
      (bodies(i).motor - reference(i).motor).norm + (bodies(i).localB - reference(i).localB).norm
    }.sum

  test("every solver converges to the same chain trajectory with its own order") {
    val totalTime = 1.0
    val (reference, _) = runChain(Pga3dPhysicsSolverRK4, dt = 1e-4, totalTime)

    val solversAndExpectedOrder = ArraySeq[(Pga3dPhysicsSolver[Pga3dPhysicsBody], Double, Double)](
      (Pga3dPhysicsSolverEuler, 0.7, 1.5),
      (Pga3dPhysicsSolverHeun, 1.7, 2.5),
      (Pga3dPhysicsSolverMidPoint, 1.7, 2.5),
      (Pga3dPhysicsSolverRK4, 3.5, 4.5),
      (Pga3dPhysicsSolverRKMK4, 3.5, 4.5),
      (Pga3dPhysicsSolverRKF45(), 3.5, 4.5),
      (Pga3dPhysicsSolverGaussLegendre(iterations = 6), 3.5, 4.5),
    )

    for ((inner, minOrder, maxOrder) <- solversAndExpectedOrder) {
      val dts = Seq(0.02, 0.01, 0.005)
      val errors = dts.map(dt => stateDifference(runChain(inner, dt, totalTime)._1, reference))

      // convergence: the error must shrink monotonically with the step
      for (i <- 1 until dts.length) {
        assert(errors(i) < errors(i - 1),
          s"solver = $inner, errors = $errors do not decrease for dts = $dts")
      }

      val order = math.log(errors.head / errors.last) / math.log(dts.head / dts.last)
      // measured: euler 1.21, heun 2.05, midPoint 2.05, rk4 4.15, rkmk4 4.15, rkf45 4.33,
      // gaussLegendre(6) 3.94
      assert(order > minOrder && order < maxOrder,
        s"solver = $inner, order = $order, expected in ($minOrder, $maxOrder), errors = $errors")
    }
  }

  test("the swinging chain holds every rod and conserves the energy") {
    val (bodies, resolver) = chain(angle = 0.3)
    val system = Pga3dPhysicsSystem(bodies, Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, resolver))
    val initialEnergy = energy(bodies)

    var maxDistError = 0.0
    var maxEnergyError = 0.0
    for (_ <- 0 until 1000) {
      system.doStep(0.01, _ => addGravity(bodies))
      maxDistError = Math.max(maxDistError, maxRodError(bodies, resolver))
      maxEnergyError = Math.max(maxEnergyError, Math.abs(energy(bodies) - initialEnergy))
    }
    // measured 2.2e-16 and 4.8e-7 at |E| = 46.8: the converged Gauss-Seidel sweeps hold the
    // rods to the machine epsilon and the energy error stays at the RK4 truncation level
    assert(maxDistError < 1e-13)
    assert(maxEnergyError < 1e-7 * Math.abs(initialEnergy))
  }

  test("two very different solvers agree on the chain trajectory") {
    val totalTime = 1.0
    val (rk4Bodies, _) = runChain(Pga3dPhysicsSolverRK4, dt = 0.002, totalTime)
    val (glBodies, _) = runChain(Pga3dPhysicsSolverGaussLegendre(iterations = 6), dt = 0.002, totalTime)

    val difference = stateDifference(rk4Bodies, glBodies) // measured 3.8e-9
    assert(difference < 1e-7, s"difference = $difference")
  }
