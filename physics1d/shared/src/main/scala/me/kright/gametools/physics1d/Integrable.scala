package me.kright.gametools.physics1d

import scala.reflect.ClassTag

/**
 * The interface for an integrator ([[OdeSolver]]). See `Pga3dPhysicsBody` of pga3dphysics as the
 * reference implementation: its accumulator of forces is updated from outside, the integrator
 * needs only the derivative (based on the forces) and the reset of the accumulator.
 *
 * One derivative evaluation of a solver is: [[loadState]] of every object of the system,
 * [[resetAccum]] of every object, the force callback of the step, [[saveDerivative]] of every
 * object. So the callback and [[saveDerivative]] may read the other objects, all of them hold
 * the same trial state; [[loadState]] may not, the others can still hold the previous one.
 */
trait Integrable:
  val stateSize: Int

  def saveState(dst: Array[Double], offset: Int): Unit

  /**
   * The values are a raw linear combination of states and derivatives: a state living on a
   * manifold (a unit vector, a motor) must be projected back onto it here.
   */
  def loadState(src: Array[Double], offset: Int): Unit

  /** in the layout of [[saveState]]: the double number j is the derivative of the double number j of the state */
  def saveDerivative(dst: Array[Double], offset: Int): Unit

  def resetAccum(): Unit


object Integrable:
  val empty: Integrable = new Integrable:
    override val stateSize: Int = 0

    override def saveState(dst: Array[Double], offset: Int): Unit = ()

    override def loadState(src: Array[Double], offset: Int): Unit = ()

    override def saveDerivative(dst: Array[Double], offset: Int): Unit = ()

    override def resetAccum(): Unit = ()

    override def toString: String = "Integrable.empty"

  def apply[T <: Integrable](elements: Array[T]): IntegrableAnyArray[T] =
    IntegrableAnyArray(elements)

  def apply[T <: Integrable : ClassTag](elements: T*): IntegrableAnyArray[T] =
    IntegrableAnyArray(elements*)

  inline def uniformElementSize[T <: Integrable](array: Array[T], inline elementSize: Int, checkElementSizes: Boolean = true): IntegrableUniformArray[T] =
    IntegrableUniformArray[T](array, elementSize, checkElementSizes)
