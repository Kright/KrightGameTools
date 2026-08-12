package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.*

import scala.util.chaining.scalaUtilChainingOps


/**
 * A rigid body: inertia, pose and velocity.
 *
 * The pose is stored as a [[Pga3dTransform]], so applying it to points, vectors and bivectors many
 * times per step is a plain matrix multiplication. The motor accessors are derived from it: reading
 * `motor` is free, assigning `motor = m` renormalizes m and rebuilds the transform (about the cost
 * of one sandwich).
 */
class Pga3dPhysicsBody(var inertia: Pga3dInertia,
                       var transform: Pga3dTransform,
                       var localB: Pga3dBivector):

  def this(inertia: Pga3dInertia, motor: Pga3dMotor, localB: Pga3dBivector) =
    this(inertia, Pga3dTransform(motor), localB)

  private val globalForqueAccumulator = Pga3dBivectorMutable()

  def motor: Pga3dMotor = transform.motor

  @Deprecated
  def motor_=(m: Pga3dMotor): Unit =
    transform = Pga3dTransform(m)

  def motorSandwich(point: Pga3dPoint): Pga3dPoint =
    transform.sandwich(point)

  def motorSandwich(vector: Pga3dVector): Pga3dVector =
    transform.sandwich(vector)

  def globalCenter: Pga3dPoint =
    transform.sandwich(Pga3dPointCenter)

  def deepCopy: Pga3dPhysicsBody =
    Pga3dPhysicsBody(inertia, transform, localB).tap { b =>
      b.globalForqueAccumulator := globalForqueAccumulator
    }

  def globalForque: Pga3dBivector =
    globalForqueAccumulator.toBivector

  def localForque: Pga3dBivector =
    transform.reverseSandwich(globalForque)

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
    transform.sandwich(inertia(localB))

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
