package me.kright.gametools.physics1d

import me.kright.gametools.mathutil.FastRange

object OdeSolverUtil:
  def saveDerivative(system: Integrable,
                     timeInsideStep: Double,
                     addForces: (timeInsideStep: Double) => Unit,
                     resolver: OdeConstraintResolver | Null,
                     dst: Array[Double]): Unit =
    system.resetAccum()
    addForces(timeInsideStep)
    if (resolver != null) {
      resolver.addConstraintForces()
    }
    system.saveDerivative(dst, 0)

  def loadFinalState(system: Integrable, src: Array[Double], resolver: OdeConstraintResolver | Null): Unit =
    system.loadState(src, 0)
    if (resolver != null) {
      resolver.project()
    }

  def combine(dst: Array[Double], size: Int, initial: Array[Double], dt: Double, derivative: Array[Double]): Unit =
    for (j <- FastRange(size)) {
      dst(j) = initial(j) + derivative(j) * dt
    }

  def combine(dst: Array[Double], size: Int, initial: Array[Double], dt: Double,
              derivative0: Array[Double], weight0: Double,
              derivative1: Array[Double], weight1: Double): Unit =
    val c0 = dt * weight0
    val c1 = dt * weight1
    for (j <- FastRange(size)) {
      dst(j) = initial(j) + derivative0(j) * c0 + derivative1(j) * c1
    }

  def combine(dst: Array[Double], size: Int, initial: Array[Double], dt: Double,
              derivative0: Array[Double], weight0: Double,
              derivative1: Array[Double], weight1: Double,
              derivative2: Array[Double], weight2: Double,
              derivative3: Array[Double], weight3: Double): Unit =
    val c0 = dt * weight0
    val c1 = dt * weight1
    val c2 = dt * weight2
    val c3 = dt * weight3
    for (j <- FastRange(size)) {
      val change = derivative0(j) * c0 + derivative1(j) * c1 + derivative2(j) * c2 + derivative3(j) * c3
      dst(j) = initial(j) + change
    }
