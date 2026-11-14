package me.kright.gametools.physics1d

/**
 * A solver calls [[addConstraintForces]] at every derivative evaluation, after the force callback
 * of the step, and [[project]] once after the state of the completed step is loaded.
 */
trait OdeConstraintResolver:
  def addConstraintForces(): Unit

  def project(): Unit


object OdeConstraintResolver:
  given CanEqual[OdeConstraintResolver | Null, Null] = CanEqual.derived
