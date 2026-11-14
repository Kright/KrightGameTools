package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.physics1d.*
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq

class Pga3dBodiesOdeSolverTest extends AnyFunSuiteLike:
  private def world(bodies: Array[Pga3dPhysicsBody], others: Integrable*): Integrable =
    Integrable((IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount) +: others)*)

  private val solvers: Seq[(solver: OdeSolver, order: Int)] = Seq(
    (OdeSolverEuler, 1),
    (OdeSolverMidPoint, 2),
    (OdeSolverHeun, 2),
    (OdeSolverRK4, 4),
    (OdeSolverRKF45(), 4),
    (OdeSolverGaussLegendre(iterations = 6), 4),
  )

  private val solversForArraysWithUniversalOnes: Seq[(forArray: Pga3dPhysicsSolver, universal: OdeSolver)] = Seq(
    (Pga3dPhysicsSolverEuler, OdeSolverEuler),
    (Pga3dPhysicsSolverMidPoint, OdeSolverMidPoint),
    (Pga3dPhysicsSolverHeun, OdeSolverHeun),
    (Pga3dPhysicsSolverRK4, OdeSolverRK4),
    (Pga3dPhysicsSolverRKF45(), OdeSolverRKF45()),
    (Pga3dPhysicsSolverGaussLegendre(iterations = 2), OdeSolverGaussLegendre(iterations = 2)),
  )

  private def bits(system: Integrable): Seq[Long] =
    val state = new Array[Double](system.stateSize)
    system.saveState(state, 0)
    ArraySeq.unsafeWrapArray(state.map(java.lang.Double.doubleToRawLongBits))

  private def bits(bodies: Array[Pga3dPhysicsBody]): Seq[Long] =
    bits(IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount))

  private def makeBodies(): Array[Pga3dPhysicsBody] =
    Array.tabulate(3) { i =>
      val motor = Pga3dBivector(wx = 0.3 * i, wy = -0.2, wz = 0.1 * i, xy = 0.4 - 0.1 * i, xz = 0.05 * i, yz = -0.3).exp
      Pga3dPhysicsBody(
        Pga3dInertia.moved(motor.reverse, Pga3dInertiaLocal(1.0 + 0.5 * i, 3.0, 2.0 + 0.1 * i, 1.0)),
        motor,
        Pga3dBivector(wx = 0.1, wy = 0.2 * i, wz = -0.3, xy = 1.0, xz = -1.0 + 0.3 * i, yz = 0.7))
    }

  private def addTimeDependentGravityAndSpringsBetweenNeighbours(bodies: Array[Pga3dPhysicsBody], timeInsideStep: Double): Unit =
    for (i <- bodies.indices) {
      val body = bodies(i)
      val gravity = Pga3dVector(0.0, -9.8 * (1.0 + timeInsideStep), 0.0) * body.inertia.mass
      body.addGlobalForque(Pga3dForque.force(body.globalCenterOfMass, gravity))
      if (i > 0) {
        val a = body.motorSandwich(Pga3dPoint(0.1, 0.2, -0.1))
        val b = bodies(i - 1).motorSandwich(Pga3dPoint(-0.2, 0.1, 0.3))
        body.addGlobalForquePaired(Pga3dForque.spring(a, b, 50.0, 0.5), bodies(i - 1))
      }
    }

  private def makeMass(): PointMass1d = PointMass1d(2.0, 1.0, 0.5)

  private def makeDirection(): UnitDirection = UnitDirection(0.7, 0.3, 5.0)

  private def addMassForce(mass: PointMass1d): Unit =
    mass.addForce(-40.0 * mass.x)

  private val steps = 50
  private val dt = 0.003

  test("a bodies array is an IntegrableUniformArray with the layout of an IntegrableAnyArray of the same bodies") {
    val bodies = makeBodies()
    val bodiesArray = IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)
    val general = Integrable(bodies)

    assert(bodiesArray.elementSize == Pga3dBodyState.componentsCount)
    assert(bodiesArray.stateSize == general.stateSize)
    assert(bodies.indices.map(bodiesArray.offsetOf) == bodies.indices.map(general.offsetOf))
    assert(bodiesArray.elements eq bodies)

    addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, 0.0)
    val marker = -777.0
    val ownDerivative, generalDerivative = Array.fill(bodiesArray.stateSize + 3)(marker)
    bodiesArray.saveDerivative(ownDerivative, 2)
    general.saveDerivative(generalDerivative, 2)

    assert(bits(bodiesArray) == bits(general))
    assert(ownDerivative.toSeq == generalDerivative.toSeq)
    assert(ownDerivative(1) == marker && ownDerivative(bodiesArray.stateSize + 2) == marker)

    bodiesArray.resetForces()
    assert(bodies.forall(_.globalForque == Pga3dBivector.zero))
  }

  test("loading a bodies array restores every body, the motor up to the rounding of its renormalization") {
    val source = makeBodies()
    val saved = new Array[Double](IntegrableUniformArray(source, Pga3dBodyState.componentsCount).stateSize + 5)
    IntegrableUniformArray(source, Pga3dBodyState.componentsCount).saveState(saved, 5)

    val target = Array.fill(source.length)(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id))
    IntegrableUniformArray(target, Pga3dBodyState.componentsCount).loadState(saved, 5)

    for (i <- source.indices) {
      assert(target(i).localB == source(i).localB)
      assert((target(i).motor - source(i).motor).norm < 1e-15)
    }
  }

  test("a bodies array over an empty array is legal") {
    val empty = IntegrableUniformArray(Array.empty[Pga3dPhysicsBody], Pga3dBodyState.componentsCount)
    assert(empty.stateSize == 0)
    empty.saveState(new Array[Double](0), 0)
    empty.resetForces()
  }

  private def runBodies(solver: OdeSolver): Array[Pga3dPhysicsBody] =
    val bodies = makeBodies()
    val bodiesArray = IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)
    val cache = OdeSolverCache()
    for (_ <- 0 until steps) {
      solver.step(bodiesArray, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep), cache)
    }
    bodies

  test("a solver for an array of bodies gives exactly the universal solver over the bodies array") {
    for (pair <- solversForArraysWithUniversalOnes) {
      val bodies = makeBodies()
      for (_ <- 0 until steps) {
        pair.forArray.step(bodies, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep))
      }
      assert(bits(bodies) == bits(runBodies(pair.universal)), s"solver ${pair.forArray}")
    }
  }

  test("the default iterations count of GaussLegendre is the one of the universal solver") {
    assert(Pga3dPhysicsSolverGaussLegendre().iterations == OdeSolverGaussLegendre().iterations)
    assert(Pga3dPhysicsSolverGaussLegendre(iterations = 5).iterations == 5)
  }

  test("the per-body estimates of RKF45 are the norms of the per-double estimates of the universal solver") {
    val forArray = Pga3dPhysicsSolverRKF45()
    val bodies = makeBodies()
    forArray.step(bodies, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep))

    val universal = OdeSolverRKF45()
    val sameBodies = makeBodies()
    val bodiesArray = IntegrableUniformArray(sameBodies, Pga3dBodyState.componentsCount)
    universal.step(bodiesArray, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(sameBodies, timeInsideStep))

    assert(forArray.lastMotorErrors.length == 3 && forArray.lastLocalBErrors.length == 3)
    assert(universal.lastErrors.length == bodiesArray.stateSize)

    var maxError = 0.0
    for (pos <- bodies.indices) {
      val errorsOfBody = universal.lastErrors.slice(bodiesArray.offsetOf(pos), bodiesArray.offsetOf(pos + 1))
      val motorNorm = Math.sqrt(errorsOfBody.take(Pga3dMotor.componentsCount).map(e => e * e).sum)
      val localBNorm = Math.sqrt(errorsOfBody.drop(Pga3dMotor.componentsCount).map(e => e * e).sum)

      assert(forArray.lastMotorErrors(pos) > 0.0 && forArray.lastLocalBErrors(pos) > 0.0)
      assert(Math.abs(forArray.lastMotorErrors(pos) - motorNorm) <= 1e-14 * motorNorm)
      assert(Math.abs(forArray.lastLocalBErrors(pos) - localBNorm) <= 1e-14 * localBNorm)
      maxError = Math.max(maxError, forArray.lastMotorErrors(pos) + forArray.lastLocalBErrors(pos))
    }
    assert(forArray.lastMaxError == maxError)
  }

  test("a body and a point mass of the same mass under the same force coincide at every stage: no stage is loaded into only a part of the system") {
    for (s <- solvers) {
      val bodies = Array(Pga3dPhysicsBody.motionless(Pga3dInertia.sphere(2.0, 0.5), Pga3dMotor.id))
      val mass = PointMass1d(2.0, x = 0.0, v = 0.0)
      val system = world(bodies, mass)
      val cache = OdeSolverCache()

      var maxDifference = 0.0
      for (_ <- 0 until 100) {
        s.solver.step(system, 0.01, timeInsideStep => {
          val center = bodies(0).globalCenterOfMass
          maxDifference = Math.max(maxDifference, Math.abs(center.x - mass.x))

          val force = 3.0 + 100.0 * timeInsideStep
          bodies(0).addGlobalForque(Pga3dForque.force(center, Pga3dVector(force, 0.0, 0.0)))
          mass.addForce(force)
        }, cache)
      }

      val tolerance = 1e-13
      assert(mass.x > 1e12 * tolerance, s"solver ${s.solver}: the objects must travel far more than the tolerance")
      assert(maxDifference < tolerance, s"solver ${s.solver}")
    }
  }

  private def bodyAndPointMassOnSpringError(solver: OdeSolver, dt: Double, totalTime: Double): Double =
    val bodyMass = 2.0
    val pointMass = 3.0
    val stiffness = 50.0
    val restLength = 2.0
    val initialBodyX = 1.0
    val initialMassX = 4.0
    val spinAroundCenterOfMass = Pga3dBivector(xy = 1.0, xz = -0.5, yz = 0.3)

    val body = Pga3dPhysicsBody(
      Pga3dInertia.sphere(bodyMass, 0.5),
      Pga3dTranslator.addVector(Pga3dVector(initialBodyX, 0.0, 0.0)).toMotor,
      spinAroundCenterOfMass)
    val mass = PointMass1d(pointMass, initialMassX, 0.0)
    val system = world(Array(body), mass)
    val cache = OdeSolverCache()

    val initialStretch = initialMassX - initialBodyX - restLength
    val reducedMass = 1.0 / (1.0 / bodyMass + 1.0 / pointMass)
    val frequency = Math.sqrt(stiffness / reducedMass)

    var maxError = 0.0
    for (step <- 1 to Math.round(totalTime / dt).toInt) {
      solver.step(system, dt, _ => {
        val center = body.globalCenterOfMass
        val stretch = mass.x - center.x - restLength
        body.addGlobalForque(Pga3dForque.force(center, Pga3dVector(stiffness * stretch, 0.0, 0.0)))
        mass.addForce(-stiffness * stretch)
      }, cache)

      val stretchChange = initialStretch * Math.cos(frequency * step * dt) - initialStretch
      val exactBodyCenter = Pga3dPoint(initialBodyX - stretchChange * pointMass / (bodyMass + pointMass), 0.0, 0.0)
      val exactMassX = initialMassX + stretchChange * bodyMass / (bodyMass + pointMass)

      val bodyError = (body.globalCenterOfMass - exactBodyCenter).norm
      maxError = Math.max(maxError, Math.max(bodyError, Math.abs(mass.x - exactMassX)))
    }
    maxError

  test("a spinning body and a point mass joined by a spring converge to the exact solution with the order of the solver") {
    for (s <- solvers) {
      val error = bodyAndPointMassOnSpringError(s.solver, 0.005, totalTime = 1.0)
      val errorAtHalfStep = bodyAndPointMassOnSpringError(s.solver, 0.0025, totalTime = 1.0)
      val measuredOrder = Math.log(error / errorAtHalfStep) / Math.log(2.0)
      assert(Math.abs(measuredOrder - s.order) < 0.1, s"${s.solver}: measured order $measuredOrder, expected ${s.order}")
    }
  }

  test("bodies split between nested arrays and shifted by other objects keep their trajectory") {
    for (s <- solvers) {
      val bodies = makeBodies()
      val system = Integrable(
        makeMass(),
        Integrable(IntegrableUniformArray(bodies.take(1), Pga3dBodyState.componentsCount), makeDirection()),
        Integrable(Integrable(IntegrableUniformArray(bodies.slice(1, 2), Pga3dBodyState.componentsCount))),
        bodies(2))
      val cache = OdeSolverCache()
      for (_ <- 0 until steps) {
        s.solver.step(system, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep), cache)
      }
      assert(bits(bodies) == bits(runBodies(s.solver)), s"solver ${s.solver}")
    }
  }

  test("uncoupled bodies and other objects do not affect each other") {
    for (s <- solvers) {
      val bodiesAlone = runBodies(s.solver)

      val massAlone = makeMass()
      val directionAlone = makeDirection()
      for (_ <- 0 until steps) {
        s.solver.step(massAlone, dt, _ => addMassForce(massAlone))
        s.solver.step(directionAlone, dt, _ => ())
      }

      val bodies = makeBodies()
      val mass = makeMass()
      val direction = makeDirection()
      val system = world(bodies, mass, direction)
      val cache = OdeSolverCache()
      for (_ <- 0 until steps) {
        s.solver.step(system, dt, timeInsideStep => {
          addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep)
          addMassForce(mass)
        }, cache)
      }

      assert(bits(bodies) == bits(bodiesAlone), s"solver ${s.solver}")
      assert(bits(mass) == bits(massAlone), s"solver ${s.solver}")
      assert(bits(direction) == bits(directionAlone), s"solver ${s.solver}")
    }
  }

  test("constraint forques added in the callback and a projection after the step give exactly Pga3dPhysicsSolverConstrained") {
    val origin = Pga3dPoint(0.0, 0.0, 0.0)
    val constraints = ArraySeq(
      Pga3dDistanceConstraint.rod(0, origin, Pga3dDistanceConstraint.world, Pga3dPoint(0.0, 1.0, 0.0), 1.2),
      Pga3dDistanceConstraint.rod(1, origin, 2, Pga3dPoint(0.1, 0.0, 0.0), 0.8))

    val expectedBodies = makeBodies()
    val constrained = Pga3dPhysicsSolverConstrained(Pga3dPhysicsSolverRK4, Pga3dConstraintResolver(constraints))
    for (_ <- 0 until steps) {
      constrained.step(expectedBodies, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(expectedBodies, timeInsideStep))
    }

    val bodies = makeBodies()
    val uncoupledMass = makeMass()
    val system = world(bodies, uncoupledMass)
    val resolver = Pga3dConstraintResolver(constraints)
    val cache = OdeSolverCache()
    for (_ <- 0 until steps) {
      OdeSolverRK4.step(system, dt, timeInsideStep => {
        addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep)
        addMassForce(uncoupledMass)
        resolver.addConstraintForques(bodies)
      }, cache)
      resolver.project(bodies)
    }

    assert(bits(bodies) == bits(expectedBodies))
  }

  private def runBodiesWithMassOnSpring(solver: OdeSolver, nextCache: () => OdeSolverCache | Null): Seq[Long] =
    val bodies = makeBodies()
    val mass = makeMass()
    val system = world(bodies, mass, makeDirection())
    for (_ <- 0 until steps) {
      solver.step(system, dt, timeInsideStep => {
        addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep)
        val center = bodies(0).globalCenterOfMass
        val stretch = mass.x - center.x
        bodies(0).addGlobalForque(Pga3dForque.force(center, Pga3dVector(30.0 * stretch, 0.0, 0.0)))
        mass.addForce(-30.0 * stretch)
      }, nextCache())
    }
    bits(system)

  test("the result with bodies does not depend on the cache: absent, new on every step, reused, filled with NaN before every step") {
    for (s <- solvers) {
      val withoutCache = runBodiesWithMassOnSpring(s.solver, () => null)

      assert(runBodiesWithMassOnSpring(s.solver, () => OdeSolverCache()) == withoutCache, s"solver ${s.solver}, a new cache on every step")

      val reused = OdeSolverCache()
      assert(runBodiesWithMassOnSpring(s.solver, () => reused) == withoutCache, s"solver ${s.solver}, a reused cache")

      val filledWithNaN = OdeSolverCache()
      val poison = () => {
        for (array <- filledWithNaN.getArrays(16, 100)) {
          java.util.Arrays.fill(array, Double.NaN)
        }
        filledWithNaN
      }
      assert(runBodiesWithMassOnSpring(s.solver, poison) == withoutCache, s"solver ${s.solver} read a double it did not write during the step")
    }
  }
