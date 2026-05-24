package com.github.kright

import com.badlogic.gdx.math.Matrix4
import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.physics.*

class WingNut:
  private def state = Pga3dPhysicsBody(
    Pga3dInertiaLocal.cube(mass = 1.0, 3.0, 2.0, 1.0),
    Pga3dMotor.id,
    Pga3dForque.torque(Pga3dVector(0.05, 4.0, 0.05)).dual
  )

  val step = 0.001
  val system = Pga3dPhysicsSystem(Array(state), Pga3dPhysicsSolverRK4)
  var t: Double = 0.0

  def rotation: Pga3dQuaternion =
    system.state(0).motor.toQuaternionUnsafe

  def setToMatrix(m: Matrix4): Unit =
    val array = Pga3dMatrixForPoints(rotation.toMotor).asArray

    m.idt()
    for (i <- 0 until 12) {
      m.`val`(i) = array(i).toFloat
    }
    m.tra()

  def simulate(dtSeconds: Double): Unit =
    val tt = t + dtSeconds
    while (t < tt) {
      t += step
      system.doStep(step, addForquesToBodies = { _ => () })
    }
