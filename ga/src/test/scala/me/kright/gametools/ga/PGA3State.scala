package me.kright.gametools.ga

case class PGA3State[T](motor: MultiVector[T],
                        localB: MultiVector[T]):

  def madd(derivative: PGA3State[T], dt: T)(using num: Numeric[T]): PGA3State[T] =
    PGA3State(
      motor = motor + derivative.motor * dt,
      localB = localB + derivative.localB * dt
    )

object PGA3State:
  def zero(using GA: PGA3): PGA3State[Double] =
    PGA3State(
      MultiVector.scalar(1.0),
      MultiVector.zero,
    )

  extension (state: PGA3State[Double])
    def normalized: PGA3State[Double] =
      state.copy(motor = state.motor.normalizedByBulk)

    def getEnergy(bodyInertia: PGA3Inertia[Double]): Double =
      (state.localB v bodyInertia(state.localB)).scalar * 0.5

    def getL(bodyInertia: PGA3Inertia[Double]): MultiVector[Double] =
      state.motor.sandwich(bodyInertia(state.localB))

    def center(using ga: PGA3): MultiVector[Double] =
      state.motor.sandwich(PGA3.zeroPoint[Double])  
