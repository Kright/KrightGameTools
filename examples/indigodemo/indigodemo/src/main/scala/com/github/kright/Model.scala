package com.github.kright

import com.github.kright.Model.*
import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dPoint, Pga3dTranslator, Pga3dVector}
import me.kright.gametools.pga3d.physics.*

final case class Model(time: Double,
                       systemTime: Double,
                       system: Pga3dPhysicsSystem):

  def afterTime(addTime: Double): Model = {
    val newTime = time + addTime

    val step = 0.005
    val newSystem = clone(system)

    val stepsCount = ((newTime - systemTime) / step).floor.toInt

    doSteps(step, stepsCount, newSystem)

    Model(newTime, systemTime + stepsCount * step, newSystem)
  }

  private def doSteps(dt: Double, count: Int, newSystem: Pga3dPhysicsSystem): Unit = {
    for _ <- 0 until count do
      newSystem.doStep(
        dt,
        addForquesToBodies = _ => {
          val body0 = newSystem.state(0)
          val body1 = newSystem.state(1)
          val center0 = body0.globalCenter
          val center1 = body1.globalCenter

          // springs
          body0.addGlobalForque(
            Pga3dForque.spring(center0, firstBodyConnectionPoint, connectionSpringK, firstConnectionLength)
          )
          body1.addGlobalForque(
            Pga3dForque.spring(center1, secondBodyConnectionPoint, connectionSpringK, secondConnectionLength)
          )

          val springForce = Pga3dForque.spring(center0, center1, springK, springTargetLength)
          body0.addGlobalForquePaired(springForce, body1)

          // gravity
          body0.addGlobalForque(center0 v Pga3dVector(y = -g) * body0.inertia.mass)
          body1.addGlobalForque(center1 v Pga3dVector(y = -g) * body1.inertia.mass)
        }
      )
  }

  private def clone(system: Pga3dPhysicsSystem): Pga3dPhysicsSystem =
    Pga3dPhysicsSystem(
      system.state.map(body => Pga3dPhysicsBody(body.inertia, body.motor, body.localB)),
      system.solver
    )

object Model:
  val firstBodyConnectionPoint: Pga3dPoint = Pga3dPoint(50.0, 50.0, 0.0)
  val secondBodyConnectionPoint: Pga3dPoint = Pga3dPoint(-50.0, 50.0, 0.0)
  val firstConnectionLength: Double = 100.0
  val secondConnectionLength: Double = 100.0
  val connectionSpringK: Double = 100.0
  val springTargetLength: Double = Math.abs(firstBodyConnectionPoint.x - secondBodyConnectionPoint.x)
  val springK: Double = 0.001
  val g: Double = 10.0

  def initial: Model = Model(
    time = 0.0,
    systemTime = 0.0,
    system = Pga3dPhysicsSystem(
      state = Array(
        Pga3dPhysicsBody(
          inertia = Pga3dInertiaSimple(1.0, 1.0),
          _motor = Pga3dTranslator
            .addVector(firstBodyConnectionPoint.toVectorUnsafe + Pga3dVector(y = -firstConnectionLength))
            .toMotor,
          localB = Pga3dBivector(wx = 3.0)
        ),
        Pga3dPhysicsBody(
          inertia = Pga3dInertiaSimple(1.0, 1.0),
          _motor = Pga3dTranslator
            .addVector(secondBodyConnectionPoint.toVectorUnsafe + Pga3dVector(y = -secondConnectionLength))
            .toMotor,
          localB = Pga3dBivector(wx = 0.0)
        )
      ),
      solver = Pga3dPhysicsSolverRK4
    )
  )
