package com.github.kright.ga

import com.github.kright.solvers.DifferentialSolvers

class PGA3OneBody(val inertia: PGA3Inertia[Double],
                  val initialState: PGA3State[Double]) {
  var state: PGA3State[Double] = initialState
  val initialE = getEnergy()
  val initialL = getL()

  def getEnergy(): Double =
    state.getEnergy(inertia)

  def getL(): MultiVector[Double] =
    state.getL(inertia)

  def getErrorE(): Double =
    math.abs(getEnergy() - initialE) / initialE

  def getErrorL(): Double =
    (getL() - initialL).norm / initialL.norm

  def getError() = PGA3OneBody.Error(getErrorE(), getErrorL())

  def doStepRK2(dt: Double, globalForque: MultiVector[Double]): Unit =
    state = DifferentialSolvers.rungeKutta2(state, time = 0.0, dt,
      getDerivative = getDerivative(globalForque),
      nextState = getNextState,
    )

  def doStepEuler2(dt: Double, globalForque: MultiVector[Double])(using ga: PGA3): Unit =
    val (k1, k2) = DifferentialSolvers.euler2K[PGA3State[Double], PGA3State[Double]](state, time = 0.0, dt,
      getDerivative = getDerivative(globalForque),
      nextState = getNextState,
    )

    state = state
      .madd(k1, dt * 0.5)
      .madd(k2, dt * 0.5)
      .normalized

  def doStepRK4(dt: Double, globalForque: MultiVector[Double]): Unit = {
    doStepRK4F(dt, (state, time) => {
      val localForque = state.motor.reverse.sandwich(globalForque)
      localForque
    })
  }

  def doStepRK4F(dt: Double, getLocalForque: (state: PGA3State[Double], time: Double) => MultiVector[Double]): Unit = {
    val (k1, k2, k3, k4) = DifferentialSolvers.rungeKutta4K(state, time = 0.0, dt,
      getDerivative = (state, time) => {
        val localForque = getLocalForque(state, time)
        PGA3State(
          motor = state.motor.geometric(state.localB) * -0.5,
          localB = inertia.invert(state.localB.cross(inertia(state.localB)) + localForque)
        )
      },
      nextState = getNextState,
    )

    state = state
      .madd(k1, dt * (1.0 / 6.0))
      .madd(k2, dt * (2.0 / 6.0))
      .madd(k3, dt * (2.0 / 6.0))
      .madd(k4, dt * (1.0 / 6.0))
      .normalized
  }

  private def getDerivative(globalForque: MultiVector[Double])(state: PGA3State[Double], time: Double): PGA3State[Double] =
    val localForque = state.motor.reverse.sandwich(globalForque)
    PGA3State(
      motor = state.motor.geometric(state.localB) * -0.5,
      localB = inertia.invert(state.localB.cross(inertia(state.localB)) + localForque)
    )

  private def getNextState(state: PGA3State[Double], derivative: PGA3State[Double], dt: Double): PGA3State[Double] =
    state.madd(derivative, dt).normalized
}

object PGA3OneBody:
  case class Error(errorE: Double, errorL: Double):
    def this() = this(0.0, 0.0)

    infix def max(other: Error): Error =
      Error(Math.max(errorE, other.errorE), Math.max(errorL, other.errorL))

    def <(other: Error): Boolean =
      errorE <= other.errorE && errorL <= other.errorL

  def simple123()(using ga: PGA3): PGA3OneBody =
    new PGA3OneBody(
      PGA3Inertia(1.0, 3.0, 2.0, 1.0),
      PGA3State(
        MultiVector.scalar[Double](1.0),
        MultiVector[Double](
          "xy" -> 1.0,
          "yz" -> 1.0,
          "zx" -> 1.0,
        )
      )
    )