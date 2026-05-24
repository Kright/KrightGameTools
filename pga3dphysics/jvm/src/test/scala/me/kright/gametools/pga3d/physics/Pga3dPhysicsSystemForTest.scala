package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dMotor}

class Pga3dPhysicsSystemForTest(state: Array[Pga3dPhysicsBody],
                                solver: Pga3dPhysicsSolver[Pga3dPhysicsBody]) extends Pga3dPhysicsSystem(state, solver):

  val initialState: Array[Pga3dBodyState] = state.map(Pga3dBodyState(_))
  val initialE = getKineticEnergy()
  val initialL = getL()

  def getErrorE(): Double =
    math.abs(getKineticEnergy() - initialE) / initialE

  def getErrorL(): Double =
    (getL() - initialL).norm / initialL.norm

  def getError(): ErrorOfEnergyAndMomentum =
    ErrorOfEnergyAndMomentum(getErrorE(), getErrorL())


object Pga3dPhysicsSystemForTest:
  def simpleBody(motor: Pga3dMotor): Pga3dPhysicsBody =
    Pga3dPhysicsBody(
      Pga3dInertia.moved(motor.reverse, Pga3dInertiaLocal(1.0, 3.0, 2.0, 1.0)),
      motor,
      motor.reverseSandwich(Pga3dBivector(
        wx = 0.0,
        wy = 0.0,
        wz = 0.0,
        xy = 1.0,
        yz = 1.0,
        xz = -1.0,
      ))
    )