package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*
import me.kright.gametools.physics1d.{IntegrableUniformArray, OdeSolverRK4}
import org.scalatest.funsuite.AnyFunSuiteLike

class Pga3dPhysicsBodySubclassTest extends AnyFunSuiteLike:
  private val origin = Pga3dPoint(0.0, 0.0, 0.0)

  private def makeBodies(): Array[Pga3dPhysicsBodyWithNameForTest] =
    Array.tabulate(2) { i =>
      Pga3dPhysicsBodyWithNameForTest(
        s"body $i",
        Pga3dInertiaLocal(1.0, 0.1, 0.1, 0.1),
        Pga3dTranslator.addVector(Pga3dVector(1.0 - 2.0 * i, 0.0, 0.0)).toMotor,
        Pga3dBivector(wy = 1.0 - 2.0 * i))
    }

  private def makeBaseBodies(): Array[Pga3dPhysicsBody] =
    makeBodies().map(body => Pga3dPhysicsBody(body.inertia, body.transform, body.localB))

  private def rod(): Array[Pga3dDistanceConstraint] =
    Array(Pga3dDistanceConstraint.rod(0, origin, 1, origin, 2.0))

  private def states(bodies: Array[? <: Pga3dPhysicsBody]): Array[Pga3dBodyState] =
    bodies.map(Pga3dBodyState(_))

  test("the system, the resolver and RKMK4 keep the class of the bodies of an array of a subclass") {
    val bodies = makeBodies()

    val system = Pga3dPhysicsSystem(bodies, OdeSolverRK4)
    val resolver = Pga3dConstraintResolver(bodies, rod())
    val rkmk4 = Pga3dOdeSolverRKMK4(bodies)

    assert(system.bodies(1).name == "body 1")
    assert(resolver.bodies(1).name == "body 1")
    assert(rkmk4.bodies(1).name == "body 1")
    assert((system.bodies eq bodies) && (resolver.bodies eq bodies) && (rkmk4.bodies eq bodies))
  }

  test("an array of a subclass gets the trajectory of the same bodies of the base class, with constraints, for RK4 and RKMK4") {
    val bodies = makeBodies()
    val baseBodies = makeBaseBodies()

    val system = Pga3dPhysicsSystem(bodies, OdeSolverRK4, resolver = Pga3dConstraintResolver(bodies, rod()))
    val baseSystem = Pga3dPhysicsSystem(baseBodies, OdeSolverRK4, resolver = Pga3dConstraintResolver(baseBodies, rod()))
    for (_ <- 0 until 100) {
      system.doStep(0.01, _ => ())
      baseSystem.doStep(0.01, _ => ())
    }
    assert(states(bodies).sameElements(states(baseBodies)))

    val rkmk4 = Pga3dOdeSolverRKMK4(bodies)
    val baseRkmk4 = Pga3dOdeSolverRKMK4(baseBodies)
    val bodiesArray = IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)
    val baseBodiesArray = IntegrableUniformArray(baseBodies, Pga3dBodyState.componentsCount)
    for (_ <- 0 until 100) {
      rkmk4.step(bodiesArray, 0.01, _ => ())
      baseRkmk4.step(baseBodiesArray, 0.01, _ => ())
    }
    assert(states(bodies).sameElements(states(baseBodies)))
    assert(bodies.map(_.name).sameElements(Array("body 0", "body 1")))
  }
