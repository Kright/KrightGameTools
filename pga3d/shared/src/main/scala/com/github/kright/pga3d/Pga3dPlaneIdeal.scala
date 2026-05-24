package com.github.kright.pga3d

import com.github.kright.mathutil.FlatDoubleSerializer
import scala.annotation.targetName


/** this code is generated, see com.github.kright.pga3d.codegen.CodeGenClass */
final case class Pga3dPlaneIdeal(x: Double = 0.0,
                                 y: Double = 0.0,
                                 z: Double = 0.0) derives CanEqual, FlatDoubleSerializer:

  override def toString: String =
    s"Pga3dPlaneIdeal(x = $x, y = $y, z = $z)"

  def dual: Pga3dVector =
    Pga3dVector(
      x = x,
      y = y,
      z = z,
    )

  def bulk: Pga3dPlaneIdeal =
    this

  @targetName("unaryMinus")
  def unary_- : Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = -x,
      y = -y,
      z = -z,
    )

  def reverse: Pga3dPlaneIdeal =
    this

  def antiReverse: Pga3dPlaneIdeal =
    -this

  def bulkNormSquare: Double =
    (x * x + y * y + z * z)

  def bulkNorm: Double =
    Math.sqrt(bulkNormSquare)

  def normalizedByBulk =
    this / bulkNorm

  def normSquare: Double =
    (x * x + y * y + z * z)

  def norm: Double =
    Math.sqrt(normSquare)

  def normalizedByNorm =
    this / norm

  @targetName("times")
  def *(v: Double): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = v * x,
      y = v * y,
      z = v * z,
    )

  @targetName("div")
  def /(v: Double): Pga3dPlaneIdeal =
    this * (1.0 / v)

  @targetName("plus")
  def +(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.x + x),
      y = (v.y + y),
      z = (v.z + z),
    )

  @targetName("minus")
  def -(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (x - v.x),
      y = (y - v.y),
      z = (z - v.z),
    )

  def madd(v: Pga3dPlaneIdeal, mult: Double): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (x + mult * v.x),
      y = (y + mult * v.y),
      z = (z + mult * v.z),
    )

  def multiplyElementwise(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = v.x * x,
      y = v.y * y,
      z = v.z * z,
    )

  def toMultivector: Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = x,
      y = y,
      z = z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = 0.0,
      i = 0.0,
    )

  def toPlane: Pga3dPlane =
    Pga3dPlane(
      x = x,
      y = y,
      z = z,
      w = 0.0,
    )

  infix def geometric(v: Pga3dMotor): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
      x = (v.s * x - v.xy * y - v.xz * z),
      y = (v.s * y + v.xy * x - v.yz * z),
      z = (v.s * z + v.xz * x + v.yz * y),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.i * z - v.wy * x),
      wxz = (v.i * y + v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.i * x - v.wz * y),
      xyz = (v.xy * z + v.yz * x - v.xz * y),
      i = 0.0,
    )

  infix def geometric(v: Pga3dPlane): Pga3dMotor =
    Pga3dMotor(
      s = (v.x * x + v.y * y + v.z * z),
      wx = -v.w * x,
      wy = -v.w * y,
      wz = -v.w * z,
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
      i = 0.0,
    )

  infix def geometric(v: Pga3dBivector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.wy * x),
      wxz = (v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.wz * y),
      xyz = (v.xy * z + v.yz * x - v.xz * y),
      i = 0.0,
    )

  infix def geometric(v: Pga3dProjectivePoint): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.y * z - v.z * y),
      wy = (v.z * x - v.x * z),
      wz = (v.x * y - v.y * x),
      xy = v.w * z,
      xz = -v.w * y,
      yz = v.w * x,
      i = (v.x * x + v.y * y + v.z * z),
    )

  infix def geometric(v: Pga3dQuaternion): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.s * x - v.xy * y - v.xz * z),
      y = (v.s * y + v.xy * x - v.yz * z),
      z = (v.s * z + v.xz * x + v.yz * y),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.xy * z + v.yz * x - v.xz * y),
      i = 0.0,
    )

  infix def geometric(v: Pga3dProjectiveTranslator): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
      x = v.s * x,
      y = v.s * y,
      z = v.s * z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.wy * x),
      wxz = (v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.wz * y),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dTranslator): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
      x = x,
      y = y,
      z = z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.wy * x),
      wxz = (v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.wz * y),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dVector): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.y * z - v.z * y),
      wy = (v.z * x - v.x * z),
      wz = (v.x * y - v.y * x),
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = (v.x * x + v.y * y + v.z * z),
    )

  infix def geometric(v: Pga3dPoint): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.y * z - v.z * y),
      wy = (v.z * x - v.x * z),
      wz = (v.x * y - v.y * x),
      xy = z,
      xz = -y,
      yz = x,
      i = (v.x * x + v.y * y + v.z * z),
    )

  infix def geometric(v: Pga3dPlaneIdeal): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.x * x + v.y * y + v.z * z),
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def geometric(v: Pga3dBivectorBulk): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.xy * z + v.yz * x - v.xz * y),
      i = 0.0,
    )

  infix def geometric(v: Pga3dBivectorWeight): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
      x = 0.0,
      y = 0.0,
      z = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.wy * x),
      wxz = (v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.wz * y),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dPseudoScalar): Pga3dVector =
    Pga3dVector(
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
    )

  infix def geometric(v: Pga3dPointCenter.type): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = z,
      xz = -y,
      yz = x,
    )

  infix def dot(v: Pga3dMotor): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
      x = (v.s * x - v.xy * y - v.xz * z),
      y = (v.s * y + v.xy * x - v.yz * z),
      z = (v.s * z + v.xz * x + v.yz * y),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = -v.i * z,
      wxz = v.i * y,
      wyz = -v.i * x,
      xyz = 0.0,
      i = 0.0,
    )

  infix def dot(v: Pga3dPlane): Double =
    (v.x * x + v.y * y + v.z * z)

  infix def dot(v: Pga3dBivector): Pga3dPlane =
    Pga3dPlane(
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def dot(v: Pga3dProjectivePoint): Pga3dBivector =
    Pga3dBivector(
      wx = (v.y * z - v.z * y),
      wy = (v.z * x - v.x * z),
      wz = (v.x * y - v.y * x),
      xy = v.w * z,
      xz = -v.w * y,
      yz = v.w * x,
    )

  infix def dot(v: Pga3dQuaternion): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.s * x - v.xy * y - v.xz * z),
      y = (v.s * y + v.xy * x - v.yz * z),
      z = (v.s * z + v.xz * x + v.yz * y),
    )

  infix def dot(v: Pga3dProjectiveTranslator): Pga3dPlane =
    Pga3dPlane(
      x = v.s * x,
      y = v.s * y,
      z = v.s * z,
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def dot(v: Pga3dTranslator): Pga3dPlane =
    Pga3dPlane(
      x = x,
      y = y,
      z = z,
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def dot(v: Pga3dVector): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (v.y * z - v.z * y),
      wy = (v.z * x - v.x * z),
      wz = (v.x * y - v.y * x),
    )

  infix def dot(v: Pga3dPoint): Pga3dBivector =
    Pga3dBivector(
      wx = (v.y * z - v.z * y),
      wy = (v.z * x - v.x * z),
      wz = (v.x * y - v.y * x),
      xy = z,
      xz = -y,
      yz = x,
    )

  infix def dot(v: Pga3dPlaneIdeal): Double =
    (v.x * x + v.y * y + v.z * z)

  infix def dot(v: Pga3dBivectorBulk): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
    )

  infix def dot(v: Pga3dBivectorWeight): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def dot(v: Pga3dPseudoScalar): Pga3dVector =
    Pga3dVector(
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
    )

  infix def dot(v: Pga3dPointCenter.type): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = z,
      xz = -y,
      yz = x,
    )

  infix def wedge(v: Pga3dMotor): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = v.s * x,
      y = v.s * y,
      z = v.s * z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.wy * x),
      wxz = (v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.wz * y),
      xyz = (v.xy * z + v.yz * x - v.xz * y),
      i = 0.0,
    )

  inline infix def ^(v: Pga3dMotor): Pga3dMultivector = wedge(v)

  inline infix def meet(v: Pga3dMotor): Pga3dMultivector = wedge(v)

  infix def wedge(v: Pga3dPlane): Pga3dBivector =
    Pga3dBivector(
      wx = -v.w * x,
      wy = -v.w * y,
      wz = -v.w * z,
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  inline infix def ^(v: Pga3dPlane): Pga3dBivector = wedge(v)

  inline infix def meet(v: Pga3dPlane): Pga3dBivector = wedge(v)

  infix def wedge(v: Pga3dBivector): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = (v.wz * y - v.wy * z),
      y = (v.wx * z - v.wz * x),
      z = (v.wy * x - v.wx * y),
      w = (v.xy * z + v.yz * x - v.xz * y),
    )

  inline infix def ^(v: Pga3dBivector): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dBivector): Pga3dProjectivePoint = wedge(v)

  infix def wedge(v: Pga3dProjectivePoint): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.x * x + v.y * y + v.z * z),
    )

  inline infix def ^(v: Pga3dProjectivePoint): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dProjectivePoint): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dQuaternion): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = v.s * x,
      y = v.s * y,
      z = v.s * z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.xy * z + v.yz * x - v.xz * y),
      i = 0.0,
    )

  inline infix def ^(v: Pga3dQuaternion): Pga3dMultivector = wedge(v)

  inline infix def meet(v: Pga3dQuaternion): Pga3dMultivector = wedge(v)

  infix def wedge(v: Pga3dProjectiveTranslator): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = v.s * x,
      y = v.s * y,
      z = v.s * z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.wy * x),
      wxz = (v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.wz * y),
      xyz = 0.0,
      i = 0.0,
    )

  inline infix def ^(v: Pga3dProjectiveTranslator): Pga3dMultivector = wedge(v)

  inline infix def meet(v: Pga3dProjectiveTranslator): Pga3dMultivector = wedge(v)

  infix def wedge(v: Pga3dTranslator): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = x,
      y = y,
      z = z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.wx * y - v.wy * x),
      wxz = (v.wx * z - v.wz * x),
      wyz = (v.wy * z - v.wz * y),
      xyz = 0.0,
      i = 0.0,
    )

  inline infix def ^(v: Pga3dTranslator): Pga3dMultivector = wedge(v)

  inline infix def meet(v: Pga3dTranslator): Pga3dMultivector = wedge(v)

  infix def wedge(v: Pga3dVector): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.x * x + v.y * y + v.z * z),
    )

  inline infix def ^(v: Pga3dVector): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dVector): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dPoint): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.x * x + v.y * y + v.z * z),
    )

  inline infix def ^(v: Pga3dPoint): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dPoint): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dPlaneIdeal): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  inline infix def ^(v: Pga3dPlaneIdeal): Pga3dBivectorBulk = wedge(v)

  inline infix def meet(v: Pga3dPlaneIdeal): Pga3dBivectorBulk = wedge(v)

  infix def wedge(v: Pga3dBivectorBulk): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.xy * z + v.yz * x - v.xz * y),
    )

  inline infix def ^(v: Pga3dBivectorBulk): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dBivectorBulk): Pga3dProjectivePoint = wedge(v)

  infix def wedge(v: Pga3dBivectorWeight): Pga3dVector =
    Pga3dVector(
      x = (v.wz * y - v.wy * z),
      y = (v.wx * z - v.wz * x),
      z = (v.wy * x - v.wx * y),
    )

  inline infix def ^(v: Pga3dBivectorWeight): Pga3dVector = wedge(v)

  inline infix def meet(v: Pga3dBivectorWeight): Pga3dVector = wedge(v)

  infix def antiGeometric(v: Pga3dMotor): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.i * x + v.wy * z - v.wz * y),
      y = (v.i * y + v.wz * x - v.wx * z),
      z = (v.i * z + v.wx * y - v.wy * x),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.wx * x + v.wy * y + v.wz * z),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dPlane): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.w * z,
      xz = -v.w * y,
      yz = v.w * x,
    )

  infix def antiGeometric(v: Pga3dBivector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.wy * z - v.wz * y),
      y = (v.wz * x - v.wx * z),
      z = (v.wx * y - v.wy * x),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.wx * x + v.wy * y + v.wz * z),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dProjectivePoint): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (-v.x * x - v.y * y - v.z * z),
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def antiGeometric(v: Pga3dProjectiveTranslator): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.wy * z - v.wz * y),
      y = (v.wz * x - v.wx * z),
      z = (v.wx * y - v.wy * x),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.wx * x + v.wy * y + v.wz * z),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dTranslator): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.wy * z - v.wz * y),
      y = (v.wz * x - v.wx * z),
      z = (v.wx * y - v.wy * x),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.wx * x + v.wy * y + v.wz * z),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dVector): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (-v.x * x - v.y * y - v.z * z),
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def antiGeometric(v: Pga3dPoint): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (-v.x * x - v.y * y - v.z * z),
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def antiGeometric(v: Pga3dBivectorWeight): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.wy * z - v.wz * y),
      y = (v.wz * x - v.wx * z),
      z = (v.wx * y - v.wy * x),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.wx * x + v.wy * y + v.wz * z),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dPseudoScalar): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
    )

  infix def antiDot(v: Pga3dMotor): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.wx * x + v.wy * y + v.wz * z),
      i = 0.0,
    )

  infix def antiDot(v: Pga3dBivector): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.wx * x + v.wy * y + v.wz * z),
    )

  infix def antiDot(v: Pga3dProjectivePoint): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def antiDot(v: Pga3dProjectiveTranslator): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.wx * x + v.wy * y + v.wz * z),
    )

  infix def antiDot(v: Pga3dTranslator): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.wx * x + v.wy * y + v.wz * z),
    )

  infix def antiDot(v: Pga3dVector): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def antiDot(v: Pga3dPoint): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def antiDot(v: Pga3dBivectorWeight): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.wx * x + v.wy * y + v.wz * z),
    )

  infix def antiDot(v: Pga3dPseudoScalar): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
    )

  infix def antiWedge(v: Pga3dMotor): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
    )

  inline infix def v(v: Pga3dMotor): Pga3dPlaneIdeal = antiWedge(v)

  inline infix def join(v: Pga3dMotor): Pga3dPlaneIdeal = antiWedge(v)

  infix def antiWedge(v: Pga3dProjectivePoint): Double =
    (-v.x * x - v.y * y - v.z * z)

  inline infix def v(v: Pga3dProjectivePoint): Double = antiWedge(v)

  inline infix def join(v: Pga3dProjectivePoint): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dVector): Double =
    (-v.x * x - v.y * y - v.z * z)

  inline infix def v(v: Pga3dVector): Double = antiWedge(v)

  inline infix def join(v: Pga3dVector): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dPoint): Double =
    (-v.x * x - v.y * y - v.z * z)

  inline infix def v(v: Pga3dPoint): Double = antiWedge(v)

  inline infix def join(v: Pga3dPoint): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dPseudoScalar): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
    )

  inline infix def v(v: Pga3dPseudoScalar): Pga3dPlaneIdeal = antiWedge(v)

  inline infix def join(v: Pga3dPseudoScalar): Pga3dPlaneIdeal = antiWedge(v)

  infix def sandwich(v: Pga3dMotor): Pga3dMotor =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dMotor(
      s = v.s * (xMx + yMy + zMz),
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
      i = v.i * (-xMx - yMy - zMz),
    )

  infix def sandwich(v: Pga3dPlane): Pga3dPlane =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dPlane(
      x = (2.0 * (v.y * xMy + v.z * xMz) + v.x * (xMx - yMy - zMz)),
      y = (2.0 * (v.x * xMy + v.z * yMz) + v.y * (yMy - xMx - zMz)),
      z = (2.0 * (v.x * xMz + v.y * yMz) + v.z * (zMz - xMx - yMy)),
      w = v.w * (-xMx - yMy - zMz),
    )

  infix def sandwich(v: Pga3dBivector): Pga3dBivector =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dBivector(
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
    )

  infix def sandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectivePoint(
      x = (2.0 * (-v.y * xMy - v.z * xMz) + v.x * (yMy + zMz - xMx)),
      y = (2.0 * (-v.x * xMy - v.z * yMz) + v.y * (xMx + zMz - yMy)),
      z = (2.0 * (-v.x * xMz - v.y * yMz) + v.z * (xMx + yMy - zMz)),
      w = v.w * (xMx + yMy + zMz),
    )

  infix def sandwich(v: Pga3dQuaternion): Pga3dQuaternion =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dQuaternion(
      s = v.s * (xMx + yMy + zMz),
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
    )

  infix def sandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectiveTranslator(
      s = v.s * (xMx + yMy + zMz),
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
    )

  infix def sandwich(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectiveTranslator(
      s = (xMx + yMy + zMz),
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
    )

  infix def sandwich(v: Pga3dVector): Pga3dVector =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dVector(
      x = (2.0 * (-v.y * xMy - v.z * xMz) + v.x * (yMy + zMz - xMx)),
      y = (2.0 * (-v.x * xMy - v.z * yMz) + v.y * (xMx + zMz - yMy)),
      z = (2.0 * (-v.x * xMz - v.y * yMz) + v.z * (xMx + yMy - zMz)),
    )

  infix def sandwich(v: Pga3dPoint): Pga3dProjectivePoint =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectivePoint(
      x = (2.0 * (-v.y * xMy - v.z * xMz) + v.x * (yMy + zMz - xMx)),
      y = (2.0 * (-v.x * xMy - v.z * yMz) + v.y * (xMx + zMz - yMy)),
      z = (2.0 * (-v.x * xMz - v.y * yMz) + v.z * (xMx + yMy - zMz)),
      w = (xMx + yMy + zMz),
    )

  infix def sandwich(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dPlaneIdeal(
      x = (2.0 * (v.y * xMy + v.z * xMz) + v.x * (xMx - yMy - zMz)),
      y = (2.0 * (v.x * xMy + v.z * yMz) + v.y * (yMy - xMx - zMz)),
      z = (2.0 * (v.x * xMz + v.y * yMz) + v.z * (zMz - xMx - yMy)),
    )

  infix def sandwich(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dBivectorBulk(
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
    )

  infix def sandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dBivectorWeight(
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
    )

  infix def sandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = v.i * (-x * x - y * y - z * z),
    )

  infix def sandwich(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (x * x + y * y + z * z),
    )

  infix def reverseSandwich(v: Pga3dMotor): Pga3dMotor =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dMotor(
      s = v.s * (xMx + yMy + zMz),
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
      i = v.i * (-xMx - yMy - zMz),
    )

  infix def reverseSandwich(v: Pga3dPlane): Pga3dPlane =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dPlane(
      x = (2.0 * (v.y * xMy + v.z * xMz) + v.x * (xMx - yMy - zMz)),
      y = (2.0 * (v.x * xMy + v.z * yMz) + v.y * (yMy - xMx - zMz)),
      z = (2.0 * (v.x * xMz + v.y * yMz) + v.z * (zMz - xMx - yMy)),
      w = v.w * (-xMx - yMy - zMz),
    )

  infix def reverseSandwich(v: Pga3dBivector): Pga3dBivector =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dBivector(
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectivePoint(
      x = (2.0 * (-v.y * xMy - v.z * xMz) + v.x * (yMy + zMz - xMx)),
      y = (2.0 * (-v.x * xMy - v.z * yMz) + v.y * (xMx + zMz - yMy)),
      z = (2.0 * (-v.x * xMz - v.y * yMz) + v.z * (xMx + yMy - zMz)),
      w = v.w * (xMx + yMy + zMz),
    )

  infix def reverseSandwich(v: Pga3dQuaternion): Pga3dQuaternion =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dQuaternion(
      s = v.s * (xMx + yMy + zMz),
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectiveTranslator(
      s = v.s * (xMx + yMy + zMz),
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectiveTranslator(
      s = (xMx + yMy + zMz),
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dVector): Pga3dVector =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dVector(
      x = (2.0 * (-v.y * xMy - v.z * xMz) + v.x * (yMy + zMz - xMx)),
      y = (2.0 * (-v.x * xMy - v.z * yMz) + v.y * (xMx + zMz - yMy)),
      z = (2.0 * (-v.x * xMz - v.y * yMz) + v.z * (xMx + yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dPoint): Pga3dProjectivePoint =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dProjectivePoint(
      x = (2.0 * (-v.y * xMy - v.z * xMz) + v.x * (yMy + zMz - xMx)),
      y = (2.0 * (-v.x * xMy - v.z * yMz) + v.y * (xMx + zMz - yMy)),
      z = (2.0 * (-v.x * xMz - v.y * yMz) + v.z * (xMx + yMy - zMz)),
      w = (xMx + yMy + zMz),
    )

  infix def reverseSandwich(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dPlaneIdeal(
      x = (2.0 * (v.y * xMy + v.z * xMz) + v.x * (xMx - yMy - zMz)),
      y = (2.0 * (v.x * xMy + v.z * yMz) + v.y * (yMy - xMx - zMz)),
      z = (2.0 * (v.x * xMz + v.y * yMz) + v.z * (zMz - xMx - yMy)),
    )

  infix def reverseSandwich(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dBivectorBulk(
      xy = (2.0 * (v.yz * xMz - v.xz * yMz) + v.xy * (zMz - xMx - yMy)),
      xz = (2.0 * (-v.xy * yMz - v.yz * xMy) + v.xz * (yMy - xMx - zMz)),
      yz = (2.0 * (v.xy * xMz - v.xz * xMy) + v.yz * (xMx - yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val yMy = y * y
    val yMz = y * z
    val zMz = z * z
    Pga3dBivectorWeight(
      wx = (2.0 * (-v.wy * xMy - v.wz * xMz) + v.wx * (yMy + zMz - xMx)),
      wy = (2.0 * (-v.wx * xMy - v.wz * yMz) + v.wy * (xMx + zMz - yMy)),
      wz = (2.0 * (-v.wx * xMz - v.wy * yMz) + v.wz * (xMx + yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = v.i * (-x * x - y * y - z * z),
    )

  infix def reverseSandwich(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (x * x + y * y + z * z),
    )

  infix def cross(v: Pga3dMotor): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = -v.i * z,
      wxz = v.i * y,
      wyz = -v.i * x,
      xyz = 0.0,
      i = 0.0,
    )

  infix def cross(v: Pga3dPlane): Pga3dBivector =
    Pga3dBivector(
      wx = -v.w * x,
      wy = -v.w * y,
      wz = -v.w * z,
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def cross(v: Pga3dBivector): Pga3dPlane =
    Pga3dPlane(
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def cross(v: Pga3dProjectivePoint): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.x * x + v.y * y + v.z * z),
    )

  infix def cross(v: Pga3dQuaternion): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
    )

  infix def cross(v: Pga3dProjectiveTranslator): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def cross(v: Pga3dTranslator): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def cross(v: Pga3dVector): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.x * x + v.y * y + v.z * z),
    )

  infix def cross(v: Pga3dPoint): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.x * x + v.y * y + v.z * z),
    )

  infix def cross(v: Pga3dPlaneIdeal): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.y * x - v.x * y),
      xz = (v.z * x - v.x * z),
      yz = (v.z * y - v.y * z),
    )

  infix def cross(v: Pga3dBivectorBulk): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (-v.xy * y - v.xz * z),
      y = (v.xy * x - v.yz * z),
      z = (v.xz * x + v.yz * y),
    )

  infix def cross(v: Pga3dBivectorWeight): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (-v.wx * x - v.wy * y - v.wz * z),
    )

  infix def cross(v: Pga3dPseudoScalar): Pga3dVector =
    Pga3dVector(
      x = v.i * x,
      y = v.i * y,
      z = v.i * z,
    )


object Pga3dPlaneIdeal:

  inline val componentsCount = 3

  val zero: Pga3dPlaneIdeal = Pga3dPlaneIdeal(0.0, 0.0, 0.0)