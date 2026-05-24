package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dMotor, Pga3dPoint, Pga3dVector}

class Pga3dMotorWithMatrix(private var _motor: Pga3dMotor):

  /** multiplication to matrix is faster than sandwich product with Pga3dMotor */
  private val motorAsMatrix = Pga3dMatrixForPoints(_motor)

  def motor: Pga3dMotor = _motor

  def motor_=(m: Pga3dMotor): Unit =
    _motor = m
    motorAsMatrix := m

  def motorSandwich(point: Pga3dPoint): Pga3dPoint =
    motorAsMatrix * point

  def motorSandwich(vector: Pga3dVector): Pga3dVector =
    motorAsMatrix * vector

  def globalCenter: Pga3dPoint =
    motorAsMatrix.getCenter  
