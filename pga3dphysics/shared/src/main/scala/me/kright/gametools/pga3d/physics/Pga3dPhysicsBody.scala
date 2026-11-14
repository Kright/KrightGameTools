package me.kright.gametools.pga3d.physics

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.physics1d.Integrable
import me.kright.gametools.pga3d.*

import scala.util.chaining.scalaUtilChainingOps


/**
 * A rigid body: inertia, pose and velocity.
 *
 * The pose is stored as a [[Pga3dTransform]], so applying it to points, vectors and bivectors many
 * times per step is a plain matrix multiplication. Reading `motor` is free; a new pose is
 * assigned as `transform = Pga3dTransform(m)`, which renormalizes m and builds the matrix (about
 * the cost of one sandwich).
 */
class Pga3dPhysicsBody(var inertia: Pga3dInertia,
                       var transform: Pga3dTransform,
                       var localB: Pga3dBivector) extends Integrable:

  def this(inertia: Pga3dInertia, motor: Pga3dMotor, localB: Pga3dBivector) =
    this(inertia, Pga3dTransform(motor), localB)

  final override inline val stateSize = Pga3dBodyState.componentsCount

  // the accumulator of the global forque is six plain fields, not a Pga3dBivectorMutable: by the
  // benchmarks an RK4 step takes the same time for 100 bodies, 2-6% less for 10'000 and up to
  // 30% less for 100'000 - the more, the less of the bodies stays in the cache
  private var forqueWx: Double = 0.0
  private var forqueWy: Double = 0.0
  private var forqueWz: Double = 0.0
  private var forqueXy: Double = 0.0
  private var forqueXz: Double = 0.0
  private var forqueYz: Double = 0.0

  def motor: Pga3dMotor = transform.motor

  def globalCenter: Pga3dPoint =
    transform.sandwich(Pga3dPointCenter)

  def deepCopy: Pga3dPhysicsBody =
    Pga3dPhysicsBody(inertia, transform, localB).tap { b =>
      b.forqueWx = forqueWx
      b.forqueWy = forqueWy
      b.forqueWz = forqueWz
      b.forqueXy = forqueXy
      b.forqueXz = forqueXz
      b.forqueYz = forqueYz
    }

  def globalForque: Pga3dBivector =
    Pga3dBivector(forqueWx, forqueWy, forqueWz, forqueXy, forqueXz, forqueYz)

  def localForque: Pga3dBivector =
    transform.reverseSandwich(globalForque)

  def addGlobalForque(globalForque: Pga3dBivector): Unit =
    forqueWx += globalForque.wx
    forqueWy += globalForque.wy
    forqueWz += globalForque.wz
    forqueXy += globalForque.xy
    forqueXz += globalForque.xz
    forqueYz += globalForque.yz

  /** Otherwise it's too easy to forget adding paired force */
  def addGlobalForquePaired(globalForque: Pga3dBivector, other: Pga3dPhysicsBody): Unit =
    addGlobalForque(globalForque)
    other.forqueWx -= globalForque.wx
    other.forqueWy -= globalForque.wy
    other.forqueWz -= globalForque.wz
    other.forqueXy -= globalForque.xy
    other.forqueXz -= globalForque.xz
    other.forqueYz -= globalForque.yz

  final override def saveState(dst: Array[Double], offset: Int): Unit =
    FlatDoubleSerializer.write(Pga3dBodyState(this), dst, offset)

  final override def loadState(src: Array[Double], offset: Int): Unit =
    val state = FlatDoubleSerializer.read[Pga3dBodyState](src, offset)
    transform = Pga3dTransform(state.motor)
    localB = state.localB

  final override def saveDerivative(dst: Array[Double], offset: Int): Unit =
    FlatDoubleSerializer.write(Pga3dBodyState.derivative(this), dst, offset)

  final override def resetAccum(): Unit =
    forqueWx = 0.0
    forqueWy = 0.0
    forqueWz = 0.0
    forqueXy = 0.0
    forqueXz = 0.0
    forqueYz = 0.0

  /** @return kinetic energy */
  def getKineticEnergy: Double =
    inertia.getKineticEnergy(localB)

  /** @return combination of linear and rotational momentum */
  def getL: Pga3dBivector =
    transform.sandwich(inertia(localB))

  def globalCenterOfMass: Pga3dPoint =
    transform.sandwich(inertia.centerOfMass)

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
