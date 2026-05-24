package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*

import scala.util.chaining.scalaUtilChainingOps


class Pga3dPhysicsBody(var inertia: Pga3dInertia,
                       _motor: Pga3dMotor,
                       var localB: Pga3dBivector) extends Pga3dMotorWithMatrix(_motor):

  private val globalForqueAccumulator = Pga3dBivectorMutable()

  def deepCopy: Pga3dPhysicsBody =
    Pga3dPhysicsBody(inertia, motor, localB).tap { b =>
      b.globalForqueAccumulator := globalForqueAccumulator
    }

  def globalForque: Pga3dBivector =
    globalForqueAccumulator.toBivector

  def localForque: Pga3dBivector =
    motor.reverseSandwich(globalForque)

  def addGlobalForque(globalForque: Pga3dBivector): Unit =
    globalForqueAccumulator += globalForque

  /** Otherwise it's too easy to forget adding paired force */
  def addGlobalForquePaired(globalForque: Pga3dBivector, other: Pga3dPhysicsBody): Unit =
    globalForqueAccumulator += globalForque
    other.globalForqueAccumulator -= globalForque

  def resetForqueAccum(): Unit =
    globalForqueAccumulator.setZero()

  /** @return kinetic energy */
  def getKineticEnergy: Double =
    inertia.getKineticEnergy(localB)

  /** @return combination of linear and rotational momentum */
  def getL: Pga3dBivector =
    motor.sandwich(inertia(localB))

  def globalCenterOfMass: Pga3dPoint =
    motorSandwich(inertia.centerOfMass)

  override def toString: String =
    s"PhysicsBody(inertia = ${inertia}, motor=${motor}, localB=${localB})"


object Pga3dPhysicsBody:
  def motionless(inertia: Pga3dInertia, motor: Pga3dMotor): Pga3dPhysicsBody =
    Pga3dPhysicsBody(inertia, motor, Pga3dBivector())

  def getL(bodies: Iterable[Pga3dPhysicsBody]): Pga3dBivector =
    val sum = Pga3dBivectorMutable()
    for (b <- bodies) {
      sum += b.getL
    }
    sum.toBivector

  def getKineticEnergy(bodies: Iterable[Pga3dPhysicsBody]): Double =
    var sum: Double = 0.0
    for (b <- bodies) {
      sum += b.getKineticEnergy
    }
    sum
