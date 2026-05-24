package com.github.kright.pga3dphysics

import com.github.kright.pga3dphysics.Pga3dPhysicsSolverUtil.*
import me.kright.gametools.mathutil.FastRange

/**
 * [[https://en.wikipedia.org/wiki/Gauss%E2%80%93Legendre_method]]
 *
 * @param iterations it looks like optimal iterations count is 3 or more. 
 *                   With two iterations precision is similar with [[Pga3dPhysicsSolverRK4]], but RK4 is faster to compute
 */
class Pga3dPhysicsSolverGaussLegendre(val iterations: Int = 3) extends Pga3dPhysicsSolver[Pga3dPhysicsBody]:
  override def step(dynamicBodies: Array[Pga3dPhysicsBody], dt: Double, addForquesToBodies: Double => Unit): Unit = {
    val c1 = 0.5 - Math.sqrt(3) / 6
    val c2 = 0.5 + Math.sqrt(3) / 6

    val a11 = 0.25
    val a12 = 0.25 - Math.sqrt(3) / 6
    val a21 = 0.25 + Math.sqrt(3) / 6
    val a22 = 0.25

    val b1 = 0.5
    val b2 = 0.5
    
    val initial = dynamicBodies.map(Pga3dBodyState(_))

    val d = getDerivative(dynamicBodies, dt * c1, addForquesToBodies)

    var y1 = getNewState(initial, dt * (a11 + a12), d)
    var y2 = getNewState(initial, dt * (a21 + a22), d)

    for (i <- FastRange(iterations)) {
      setNewState(dynamicBodies, y1)
      val fy1 = getDerivative(dynamicBodies, dt * c1, addForquesToBodies)
      setNewState(dynamicBodies, y2)
      val fy2 = getDerivative(dynamicBodies, dt * c2, addForquesToBodies)

      y1 = getNewState(initial, dt, fy1, a11, fy2, a12)
      y2 = getNewState(initial, dt, fy1, a21, fy2, a22)
    }

    setNewState(dynamicBodies, y1)
    val fy1 = getDerivative(dynamicBodies, dt * c1, addForquesToBodies)
    setNewState(dynamicBodies, y2)
    val fy2 = getDerivative(dynamicBodies, dt * c2, addForquesToBodies)

    setNewState(dynamicBodies, initial, dt, fy1, b1, fy2, b2)
  }

  override def toString: String =
    s"Pga3dPhysicsSolverGaussLegendre(iterations=$iterations)"
