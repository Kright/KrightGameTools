package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dMotor, Pga3dPoint, Pga3dVector}

/**
 * Helper methods for matrix with 4x3 size which maps Pga3dPoint => Pga3dPoint and Pga3dVector => Pga3dVector.
 * represented as an array with 12 elements
 */
object Pga3dMatrixForPoints:
  opaque type Pga3dMatrixForPoints = Array[Double]

  def apply(motor: Pga3dMotor): Pga3dMatrixForPoints =
    val arr = new Array[Double](12)
    arr := motor
    arr

  extension (matrix: Pga3dMatrixForPoints)
    def *(point: Pga3dPoint): Pga3dPoint =
      Pga3dPoint(
        x = matrix(0) * point.x + matrix(1) * point.y + matrix(2) * point.z + matrix(3),
        y = matrix(4) * point.x + matrix(5) * point.y + matrix(6) * point.z + matrix(7),
        z = matrix(8) * point.x + matrix(9) * point.y + matrix(10) * point.z + matrix(11)
      )

    def *(vector: Pga3dVector): Pga3dVector = {
      Pga3dVector(
        x = matrix(0) * vector.x + matrix(1) * vector.y + matrix(2) * vector.z,
        y = matrix(4) * vector.x + matrix(5) * vector.y + matrix(6) * vector.z,
        z = matrix(8) * vector.x + matrix(9) * vector.y + matrix(10) * vector.z,
      )
    }
    
    def getCenter: Pga3dPoint =
      Pga3dPoint(matrix(3), matrix(7), matrix(11))

    def :=(m: Pga3dMotor): Unit =
      val sMs = m.s * m.s
      val sMxy = m.s * m.xy
      val sMxz = m.s * m.xz
      val sMyz = m.s * m.yz
      val xyMxy = m.xy * m.xy
      val xyMxz = m.xy * m.xz
      val xyMyz = m.xy * m.yz
      val xzMxz = m.xz * m.xz
      val xzMyz = m.xz * m.yz
      val yzMyz = m.yz * m.yz

      matrix(0) = -(xyMxy + xzMxz - sMs - yzMyz)
      matrix(1) = -2.0 * (xzMyz - sMxy)
      matrix(2) = -2.0 * (-sMxz - xyMyz)
      matrix(3) = -2.0 * (m.i * m.yz + m.s * m.wx + m.wy * m.xy + m.wz * m.xz)

      matrix(4) = 2.0 * (-sMxy - xzMyz)
      matrix(5) = (sMs + xzMxz - xyMxy - yzMyz)
      matrix(6) = 2.0 * (sMyz - xyMxz)
      matrix(7) = 2.0 * (m.i * m.xz + m.wx * m.xy - m.s * m.wy - m.wz * m.yz)

      matrix(8) = -2.0 * (sMxz - xyMyz)
      matrix(9) = -2.0 * (sMyz + xyMxz)
      matrix(10) = -(xzMxz + yzMyz - sMs - xyMxy)
      matrix(11) = -2.0 * (m.i * m.xy + m.s * m.wz - m.wx * m.xz - m.wy * m.yz)

    def asArray: Array[Double] =
      matrix
