package com.github.kright.solvers

/**
 * example:
 * <pre>
 * val inertia = Inertia3d(1.0, Vector3d(1.0, 2.0, 3.0))
 * val initialState = State(Transform3d(), Velocity3d(linear = Vector3d(), angular = Vector3d(0.1, 1.0, 0.1)))
 * val force = Force3d()
 * val dt = 0.01
 *
 * val newState = DifferentialSolvers.rungeKutta4(initialState, time=0.0, dt,
 * getDerivative = (state, time) => inertia.getDerivative(state, force)
 * nextState = (state, derivative, dt) => state.updated(derivative, dt),
 * newZeroDerivative = () => new StateDerivative(),
 * madd = (acc, d, m) => acc.madd(d, m)
 * )
 * </pre>
 */
object DifferentialSolvers:
  /**
   * second order of precision, improved euler method
   * https://en.wikipedia.org/wiki/Heun%27s_method
   * */
  inline def euler2[State, Derivative](initial: State, time: Double, dt: Double,
                                       inline getDerivative: (State, Double) => Derivative,
                                       inline nextState: (State, Derivative, Double) => State,
                                       inline newZeroDerivative: () => Derivative,
                                       inline madd: (Derivative, Derivative, Double) => Unit): State =
    val (k1, k2) = euler2K(initial, time, dt, getDerivative, nextState)

    val kMean = newZeroDerivative()
    madd(kMean, k1, 0.5)
    madd(kMean, k2, 0.5)

    nextState(initial, kMean, dt)

  /**
   * second order of precision, improved euler method
   * https://en.wikipedia.org/wiki/Heun%27s_method
   *
   * example of usage:
   * val (k1, k2) = euler2K(...)
   *
   * state += k1 * (0.5 * dt)
   * state += k2 * (0.5 * dt)
   * state.normalize()
   * */
  inline def euler2K[State, Derivative](initial: State, time: Double, dt: Double,
                                        inline getDerivative: (State, Double) => Derivative,
                                        inline nextState: (State, Derivative, Double) => State): (Derivative, Derivative) =
    val k1 = getDerivative(initial, time)
    val k2 = getDerivative(nextState(initial, k1, dt), time + dt)
    (k1, k2)

  /**
   * second order of precision, Runge–Kutta method
   */
  inline def rungeKutta2[State, Derivative](initial: State, time: Double, dt: Double,
                                            inline getDerivative: (State, Double) => Derivative,
                                            inline nextState: (State, Derivative, Double) => State): State =
    val k1 = getDerivative(initial, time)
    val k2 = getDerivative(nextState(initial, k1, 0.5 * dt), time + 0.5 * dt)
    nextState(initial, k2, dt)

  /**
   * forth order of precision, Runge-Kutta method
   * https://en.wikipedia.org/wiki/Runge%E2%80%93Kutta_methods
   */
  inline def rungeKutta4[State, Derivative](initial: State, time: Double, dt: Double,
                                            inline getDerivative: (State, Double) => Derivative,
                                            inline nextState: (State, Derivative, Double) => State,
                                            inline newZeroDerivative: () => Derivative,
                                            inline madd: (Derivative, Derivative, Double) => Unit): State =
    val (k1, k2, k3, k4) = rungeKutta4K(initial, time, dt, getDerivative, nextState)

    val kMean = newZeroDerivative()
    madd(kMean, k1, 1.0 / 6.0)
    madd(kMean, k2, 2.0 / 6.0)
    madd(kMean, k3, 2.0 / 6.0)
    madd(kMean, k4, 1.0 / 6.0)

    nextState(initial, kMean, dt)

  /**
   * forth order of precision, Runge-Kutta method
   * https://en.wikipedia.org/wiki/Runge%E2%80%93Kutta_methods
   *
   * example of usage:
   * val (k1, k2, k3, k4) = rungeKutta4K(...)
   *
   * state += k1 * (dt * 1.0 / 6.0)
   * state += k2 * (dt * 2.0 / 6.0)
   * state += k3 * (dt * 2.0 / 6.0)
   * state += k4 * (dt * 1.0 / 6.0)
   * state.normalize()
   */
  inline def rungeKutta4K[State, Derivative](initial: State, time: Double, dt: Double,
                                             inline getDerivative: (State, Double) => Derivative,
                                             inline nextState: (State, Derivative, Double) => State): (Derivative, Derivative, Derivative, Derivative) =
    val k1 = getDerivative(initial, time)
    val k2 = getDerivative(nextState(initial, k1, 0.5 * dt), time + 0.5 * dt)
    val k3 = getDerivative(nextState(initial, k2, 0.5 * dt), time + 0.5 * dt)
    val k4 = getDerivative(nextState(initial, k3, dt), time + dt)

    (k1, k2, k3, k4)
