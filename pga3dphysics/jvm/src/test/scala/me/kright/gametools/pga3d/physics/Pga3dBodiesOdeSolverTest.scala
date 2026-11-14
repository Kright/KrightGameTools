package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.physics1d.*
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.immutable.ArraySeq

class Pga3dBodiesOdeSolverTest extends AnyFunSuiteLike:
  private def world(bodies: Array[Pga3dPhysicsBody], others: Integrable*): Integrable =
    Integrable((IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount) +: others)*)

  private val solvers: ArraySeq[(name: String, make: Array[Pga3dPhysicsBody] => OdeSolver, order: Int)] = ArraySeq(
    ("euler", _ => OdeSolverEuler, 1),
    ("midPoint", _ => OdeSolverMidPoint, 2),
    ("heun", _ => OdeSolverHeun, 2),
    ("rk4", _ => OdeSolverRK4, 4),
    ("rkmk4", Pga3dOdeSolverRKMK4(_), 4),
    ("rkf45", _ => OdeSolverRKF45(), 4),
    ("gaussLegendre(6)", _ => OdeSolverGaussLegendre(iterations = 6), 4),
  )

  private def bits(system: Integrable): ArraySeq[Long] =
    val state = new Array[Double](system.stateSize)
    system.saveState(state, 0)
    ArraySeq.unsafeWrapArray(state.map(java.lang.Double.doubleToRawLongBits))

  private def bits(bodies: Array[Pga3dPhysicsBody]): ArraySeq[Long] =
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

  private def runBodies(makeSolver: Array[Pga3dPhysicsBody] => OdeSolver): Array[Pga3dPhysicsBody] =
    val bodies = makeBodies()
    val solver = makeSolver(bodies)
    val bodiesArray = IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)
    val cache = OdeSolverCache()
    for (_ <- 0 until steps) {
      solver.step(bodiesArray, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep), cache)
    }
    bodies

  test("a body and a point mass of the same mass under the same force coincide at every stage: no stage is loaded into only a part of the system") {
    for (s <- solvers) {
      val bodies = Array(Pga3dPhysicsBody.motionless(Pga3dInertia.sphere(2.0, 0.5), Pga3dMotor.id))
      val mass = PointMass1d(2.0, x = 0.0, v = 0.0)
      val system = world(bodies, mass)
      val solver = s.make(bodies)
      val cache = OdeSolverCache()

      var maxDifference = 0.0
      for (_ <- 0 until 100) {
        solver.step(system, 0.01, timeInsideStep => {
          val center = bodies(0).globalCenterOfMass
          maxDifference = Math.max(maxDifference, Math.abs(center.x - mass.x))

          val force = 3.0 + 100.0 * timeInsideStep
          bodies(0).addGlobalForque(Pga3dForque.force(center, Pga3dVector(force, 0.0, 0.0)))
          mass.addForce(force)
        }, cache)
      }

      val tolerance = 1e-13
      assert(mass.x > 1e12 * tolerance, s"solver ${s.name}: the objects must travel far more than the tolerance")
      assert(maxDifference < tolerance, s"solver ${s.name}")
    }
  }

  private def bodyAndPointMassOnSpringError(makeSolver: Array[Pga3dPhysicsBody] => OdeSolver, dt: Double, totalTime: Double): Double =
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
    val bodies = Array(body)
    val system = world(bodies, mass)
    val solver = makeSolver(bodies)
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
      val error = bodyAndPointMassOnSpringError(s.make, 0.005, totalTime = 1.0)
      val errorAtHalfStep = bodyAndPointMassOnSpringError(s.make, 0.0025, totalTime = 1.0)
      val measuredOrder = Math.log(error / errorAtHalfStep) / Math.log(2.0)
      assert(Math.abs(measuredOrder - s.order) < 0.1, s"${s.name}: measured order $measuredOrder, expected ${s.order}")
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
      val solver = s.make(bodies)
      val cache = OdeSolverCache()
      for (_ <- 0 until steps) {
        solver.step(system, dt, timeInsideStep => addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep), cache)
      }
      assert(bits(bodies) == bits(runBodies(s.make)), s"solver ${s.name}")
    }
  }

  test("uncoupled bodies and other objects do not affect each other") {
    for (s <- solvers) {
      val bodiesAlone = runBodies(s.make)

      val massAlone = makeMass()
      val directionAlone = makeDirection()
      val solverWithoutBodies = s.make(Array.empty)
      for (_ <- 0 until steps) {
        solverWithoutBodies.step(massAlone, dt, _ => addMassForce(massAlone))
        solverWithoutBodies.step(directionAlone, dt, _ => ())
      }

      val bodies = makeBodies()
      val mass = makeMass()
      val direction = makeDirection()
      val system = world(bodies, mass, direction)
      val solver = s.make(bodies)
      val cache = OdeSolverCache()
      for (_ <- 0 until steps) {
        solver.step(system, dt, timeInsideStep => {
          addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep)
          addMassForce(mass)
        }, cache)
      }

      assert(bits(bodies) == bits(bodiesAlone), s"solver ${s.name}")
      assert(bits(mass) == bits(massAlone), s"solver ${s.name}")
      assert(bits(direction) == bits(directionAlone), s"solver ${s.name}")
    }
  }

  test("a universal solver with the resolver of the bodies gives exactly the constraint forces added by the force callback and the projection after the step") {
    val origin = Pga3dPoint(0.0, 0.0, 0.0)
    val constraints = Array(
      Pga3dDistanceConstraint.rod(0, origin, Pga3dDistanceConstraint.world, Pga3dPoint(0.0, 1.0, 0.0), 1.2),
      Pga3dDistanceConstraint.rod(1, origin, 2, Pga3dPoint(0.1, 0.0, 0.0), 0.8))

    val expectedBodies = makeBodies()
    val expectedBodiesArray = IntegrableUniformArray(expectedBodies, Pga3dBodyState.componentsCount)
    val calledByHand = Pga3dConstraintResolver(expectedBodies, constraints)
    for (_ <- 0 until steps) {
      OdeSolverRK4.step(expectedBodiesArray, dt, timeInsideStep => {
        addTimeDependentGravityAndSpringsBetweenNeighbours(expectedBodies, timeInsideStep)
        calledByHand.addConstraintForces()
      })
      calledByHand.project()
    }

    val bodies = makeBodies()
    val uncoupledMass = makeMass()
    val system = world(bodies, uncoupledMass)
    val resolver = Pga3dConstraintResolver(bodies, constraints)
    val cache = OdeSolverCache()
    for (_ <- 0 until steps) {
      OdeSolverRK4.step(system, dt, timeInsideStep => {
        addTimeDependentGravityAndSpringsBetweenNeighbours(bodies, timeInsideStep)
        addMassForce(uncoupledMass)
      }, cache, resolver)
    }

    assert(bits(bodies) == bits(expectedBodies))
  }

  private def runBodiesWithMassOnSpring(makeSolver: Array[Pga3dPhysicsBody] => OdeSolver, nextCache: () => OdeSolverCache | Null): ArraySeq[Long] =
    val bodies = makeBodies()
    val solver = makeSolver(bodies)
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
      val withoutCache = runBodiesWithMassOnSpring(s.make, () => null)

      assert(runBodiesWithMassOnSpring(s.make, () => OdeSolverCache()) == withoutCache, s"solver ${s.name}, a new cache on every step")

      val reused = OdeSolverCache()
      assert(runBodiesWithMassOnSpring(s.make, () => reused) == withoutCache, s"solver ${s.name}, a reused cache")

      val filledWithNaN = OdeSolverCache()
      val poison = () => {
        for (array <- filledWithNaN.getArrays(16, 100)) {
          java.util.Arrays.fill(array, Double.NaN)
        }
        filledWithNaN
      }
      assert(runBodiesWithMassOnSpring(s.make, poison) == withoutCache, s"solver ${s.name} read a double it did not write during the step")
    }
  }
