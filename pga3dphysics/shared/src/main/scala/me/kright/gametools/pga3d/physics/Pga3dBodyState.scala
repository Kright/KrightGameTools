package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dMotor, Pga3dPointCenter, Pga3dProjectivePoint}

case class Pga3dBodyState(motor: Pga3dMotor,
                          localB: Pga3dBivector):

  def madd(derivative: Pga3dBodyState, dt: Double): Pga3dBodyState =
    Pga3dBodyState(
      motor = motor + derivative.motor * dt,
      localB = localB + derivative.localB * dt
    )

  def normalized: Pga3dBodyState =
    copy(motor = motor.renormalized)

  def getL(bodyInertia: Pga3dInertia): Pga3dBivector =
    motor.sandwich(bodyInertia(localB))

  def center: Pga3dProjectivePoint =
    motor.sandwich(Pga3dPointCenter)


object Pga3dBodyState:
  def id: Pga3dBodyState =
    Pga3dBodyState(
      Pga3dMotor.id,
      Pga3dBivector.zero,
    )

  def apply(b: Pga3dPhysicsBody): Pga3dBodyState =
    new Pga3dBodyState(
      motor = b.motor,
      localB = b.localB,
    )

  def derivative(b: Pga3dPhysicsBody): Pga3dBodyState =
    new Pga3dBodyState(
      motor = b.motor.geometric(b.localB) * -0.5,
      localB = b.inertia.getAcceleration(b.localB, b.localForque)
    )
