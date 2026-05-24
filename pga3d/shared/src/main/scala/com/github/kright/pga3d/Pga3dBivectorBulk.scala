package com.github.kright.pga3d

import me.kright.gametools.mathutil.FlatDoubleSerializer
import scala.annotation.targetName


/** this code is generated, see com.github.kright.pga3d.codegen.CodeGenClass */
final case class Pga3dBivectorBulk(xy: Double = 0.0,
                                   xz: Double = 0.0,
                                   yz: Double = 0.0) derives CanEqual, FlatDoubleSerializer:

  override def toString: String =
    s"Pga3dBivectorBulk(xy = $xy, xz = $xz, yz = $yz)"

  def dual: Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = yz,
      wy = -xz,
      wz = xy,
    )

  def bulk: Pga3dBivectorBulk =
    this

  @targetName("unaryMinus")
  def unary_- : Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = -xy,
      xz = -xz,
      yz = -yz,
    )

  def reverse: Pga3dBivectorBulk =
    -this

  def antiReverse: Pga3dBivectorBulk =
    -this

  def bulkNormSquare: Double =
    (xy * xy + xz * xz + yz * yz)

  def bulkNorm: Double =
    Math.sqrt(bulkNormSquare)

  def normalizedByBulk =
    this / bulkNorm

  def normSquare: Double =
    (xy * xy + xz * xz + yz * yz)

  def norm: Double =
    Math.sqrt(normSquare)

  def normalizedByNorm =
    this / norm

  @targetName("times")
  def *(v: Double): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v * xy,
      xz = v * xz,
      yz = v * yz,
    )

  @targetName("div")
  def /(v: Double): Pga3dBivectorBulk =
    this * (1.0 / v)

  @targetName("plus")
  def +(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = v.wx,
      wy = v.wy,
      wz = v.wz,
      xy = (v.xy + xy),
      xz = (v.xz + xz),
      yz = (v.yz + yz),
    )

  @targetName("minus")
  def -(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = -v.wx,
      wy = -v.wy,
      wz = -v.wz,
      xy = (xy - v.xy),
      xz = (xz - v.xz),
      yz = (yz - v.yz),
    )

  def madd(v: Pga3dBivector, mult: Double): Pga3dBivector =
    Pga3dBivector(
      wx = mult * v.wx,
      wy = mult * v.wy,
      wz = mult * v.wz,
      xy = (xy + mult * v.xy),
      xz = (xz + mult * v.xz),
      yz = (yz + mult * v.yz),
    )

  @targetName("plus")
  def +(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.xy + xy),
      xz = (v.xz + xz),
      yz = (v.yz + yz),
    )

  @targetName("minus")
  def -(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (xy - v.xy),
      xz = (xz - v.xz),
      yz = (yz - v.yz),
    )

  def madd(v: Pga3dBivectorBulk, mult: Double): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (xy + mult * v.xy),
      xz = (xz + mult * v.xz),
      yz = (yz + mult * v.yz),
    )

  @targetName("plus")
  def +(v: Pga3dBivectorWeight): Pga3dBivector =
    Pga3dBivector(
      wx = v.wx,
      wy = v.wy,
      wz = v.wz,
      xy = xy,
      xz = xz,
      yz = yz,
    )

  @targetName("minus")
  def -(v: Pga3dBivectorWeight): Pga3dBivector =
    Pga3dBivector(
      wx = -v.wx,
      wy = -v.wy,
      wz = -v.wz,
      xy = xy,
      xz = xz,
      yz = yz,
    )

  def madd(v: Pga3dBivectorWeight, mult: Double): Pga3dBivector =
    Pga3dBivector(
      wx = mult * v.wx,
      wy = mult * v.wy,
      wz = mult * v.wz,
      xy = xy,
      xz = xz,
      yz = yz,
    )

  def multiplyElementwise(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.xy * xy,
      xz = v.xz * xz,
      yz = v.yz * yz,
    )

  def exp(): Pga3dQuaternion =
    val len = bulkNorm
    val cos = Math.cos(len)

    val sinDivLen = if (len > 1e-5) {
      Math.sin(len) / len
    } else 1.0 - (len * len) / 6.0

    Pga3dQuaternion(
      s = cos,
      xy = sinDivLen * xy,
      xz = sinDivLen * xz,
      yz = sinDivLen * yz,
    )

  def exp(t: Double): Pga3dQuaternion =
    val len = bulkNorm * Math.abs(t)
    val cos = Math.cos(len)

    val sinDivLen = if (len > 1e-5) {
      Math.sin(len) / len
    } else 1.0 - (len * len) / 6.0

    Pga3dQuaternion(
      s = cos,
      xy = sinDivLen * t * xy,
      xz = sinDivLen * t * xz,
      yz = sinDivLen * t * yz,
    )

  def toMultivector: Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = 0.0,
      y = 0.0,
      z = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = xy,
      xz = xz,
      yz = yz,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = 0.0,
      i = 0.0,
    )

  def toMotor: Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = xy,
      xz = xz,
      yz = yz,
      i = 0.0,
    )

  def toBivector: Pga3dBivector =
    Pga3dBivector(
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = xy,
      xz = xz,
      yz = yz,
    )

  def toQuaternion: Pga3dQuaternion =
    Pga3dQuaternion(
      s = 0.0,
      xy = xy,
      xz = xz,
      yz = yz,
    )

  infix def geometric(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = (-v.xy * xy - v.xz * xz - v.yz * yz),
      wx = (v.wy * xy + v.wz * xz - v.i * yz),
      wy = (v.i * xz + v.wz * yz - v.wx * xy),
      wz = (-v.i * xy - v.wx * xz - v.wy * yz),
      xy = (v.s * xy + v.xz * yz - v.yz * xz),
      xz = (v.s * xz + v.yz * xy - v.xy * yz),
      yz = (v.s * yz + v.xy * xz - v.xz * xy),
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  infix def geometric(v: Pga3dPlane): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = v.w * xy,
      wxz = v.w * xz,
      wyz = v.w * yz,
      xyz = (v.x * yz + v.z * xy - v.y * xz),
      i = 0.0,
    )

  infix def geometric(v: Pga3dBivector): Pga3dMotor =
    Pga3dMotor(
      s = (-v.xy * xy - v.xz * xz - v.yz * yz),
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
      xy = (v.xz * yz - v.yz * xz),
      xz = (v.yz * xy - v.xy * yz),
      yz = (v.xy * xz - v.xz * xy),
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  infix def geometric(v: Pga3dProjectivePoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
      x = -v.w * yz,
      y = v.w * xz,
      z = -v.w * xy,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.x * xz + v.y * yz),
      wxz = (v.z * yz - v.x * xy),
      wyz = (-v.y * xy - v.z * xz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dQuaternion): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (-v.xy * xy - v.xz * xz - v.yz * yz),
      xy = (v.s * xy + v.xz * yz - v.yz * xz),
      xz = (v.s * xz + v.yz * xy - v.xy * yz),
      yz = (v.s * yz + v.xy * xz - v.xz * xy),
    )

  infix def geometric(v: Pga3dProjectiveTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
      xy = v.s * xy,
      xz = v.s * xz,
      yz = v.s * yz,
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  infix def geometric(v: Pga3dTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
      xy = xy,
      xz = xz,
      yz = yz,
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  infix def geometric(v: Pga3dVector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
      x = 0.0,
      y = 0.0,
      z = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.x * xz + v.y * yz),
      wxz = (v.z * yz - v.x * xy),
      wyz = (-v.y * xy - v.z * xz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dPoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
      x = -yz,
      y = xz,
      z = -xy,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.x * xz + v.y * yz),
      wxz = (v.z * yz - v.x * xy),
      wyz = (-v.y * xy - v.z * xz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dPlaneIdeal): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.x * yz + v.z * xy - v.y * xz),
      i = 0.0,
    )

  infix def geometric(v: Pga3dBivectorBulk): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (-v.xy * xy - v.xz * xz - v.yz * yz),
      xy = (v.xz * yz - v.yz * xz),
      xz = (v.yz * xy - v.xy * yz),
      yz = (v.xy * xz - v.xz * xy),
    )

  infix def geometric(v: Pga3dBivectorWeight): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  infix def geometric(v: Pga3dPseudoScalar): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = -v.i * yz,
      wy = v.i * xz,
      wz = -v.i * xy,
    )

  infix def geometric(v: Pga3dPointCenter.type): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = -yz,
      y = xz,
      z = -xy,
    )

  infix def dot(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = (-v.xy * xy - v.xz * xz - v.yz * yz),
      wx = -v.i * yz,
      wy = v.i * xz,
      wz = -v.i * xy,
      xy = v.s * xy,
      xz = v.s * xz,
      yz = v.s * yz,
      i = 0.0,
    )

  infix def dot(v: Pga3dPlane): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  infix def dot(v: Pga3dBivector): Double =
    (-v.xy * xy - v.xz * xz - v.yz * yz)

  infix def dot(v: Pga3dProjectivePoint): Pga3dPlane =
    Pga3dPlane(
      x = -v.w * yz,
      y = v.w * xz,
      z = -v.w * xy,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  infix def dot(v: Pga3dQuaternion): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (-v.xy * xy - v.xz * xz - v.yz * yz),
      xy = v.s * xy,
      xz = v.s * xz,
      yz = v.s * yz,
    )

  infix def dot(v: Pga3dProjectiveTranslator): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.s * xy,
      xz = v.s * xz,
      yz = v.s * yz,
    )

  infix def dot(v: Pga3dTranslator): Pga3dBivectorBulk =
    this

  infix def dot(v: Pga3dVector): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  infix def dot(v: Pga3dPoint): Pga3dPlane =
    Pga3dPlane(
      x = -yz,
      y = xz,
      z = -xy,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  infix def dot(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  infix def dot(v: Pga3dBivectorBulk): Double =
    (-v.xy * xy - v.xz * xz - v.yz * yz)

  infix def dot(v: Pga3dPseudoScalar): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = -v.i * yz,
      wy = v.i * xz,
      wz = -v.i * xy,
    )

  infix def dot(v: Pga3dPointCenter.type): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = -yz,
      y = xz,
      z = -xy,
    )

  infix def wedge(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = v.s * xy,
      xz = v.s * xz,
      yz = v.s * yz,
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  inline infix def ^(v: Pga3dMotor): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dMotor): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dPlane): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = -v.w * yz,
      y = v.w * xz,
      z = -v.w * xy,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  inline infix def ^(v: Pga3dPlane): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dPlane): Pga3dProjectivePoint = wedge(v)

  infix def wedge(v: Pga3dBivector): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  inline infix def ^(v: Pga3dBivector): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dBivector): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dQuaternion): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.s * xy,
      xz = v.s * xz,
      yz = v.s * yz,
    )

  inline infix def ^(v: Pga3dQuaternion): Pga3dBivectorBulk = wedge(v)

  inline infix def meet(v: Pga3dQuaternion): Pga3dBivectorBulk = wedge(v)

  infix def wedge(v: Pga3dProjectiveTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = v.s * xy,
      xz = v.s * xz,
      yz = v.s * yz,
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  inline infix def ^(v: Pga3dProjectiveTranslator): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dProjectiveTranslator): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = xy,
      xz = xz,
      yz = yz,
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  inline infix def ^(v: Pga3dTranslator): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dTranslator): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dPlaneIdeal): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  inline infix def ^(v: Pga3dPlaneIdeal): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dPlaneIdeal): Pga3dProjectivePoint = wedge(v)

  infix def wedge(v: Pga3dBivectorWeight): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (v.wx * yz + v.wz * xy - v.wy * xz),
    )

  inline infix def ^(v: Pga3dBivectorWeight): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dBivectorWeight): Pga3dPseudoScalar = wedge(v)

  infix def antiGeometric(v: Pga3dMotor): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.wx * yz + v.wz * xy - v.wy * xz),
      xy = (v.i * xy - v.wx * xz - v.wy * yz),
      xz = (v.i * xz + v.wx * xy - v.wz * yz),
      yz = (v.i * yz + v.wy * xy + v.wz * xz),
    )

  infix def antiGeometric(v: Pga3dPlane): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = -v.w * yz,
      y = v.w * xz,
      z = -v.w * xy,
    )

  infix def antiGeometric(v: Pga3dBivector): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.wx * yz + v.wz * xy - v.wy * xz),
      xy = (-v.wx * xz - v.wy * yz),
      xz = (v.wx * xy - v.wz * yz),
      yz = (v.wy * xy + v.wz * xz),
    )

  infix def antiGeometric(v: Pga3dProjectivePoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.x * yz + v.z * xy - v.y * xz),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dProjectiveTranslator): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.wx * yz + v.wz * xy - v.wy * xz),
      xy = (-v.wx * xz - v.wy * yz),
      xz = (v.wx * xy - v.wz * yz),
      yz = (v.wy * xy + v.wz * xz),
    )

  infix def antiGeometric(v: Pga3dTranslator): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.wx * yz + v.wz * xy - v.wy * xz),
      xy = (-v.wx * xz - v.wy * yz),
      xz = (v.wx * xy - v.wz * yz),
      yz = (v.wy * xy + v.wz * xz),
    )

  infix def antiGeometric(v: Pga3dVector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.x * yz + v.z * xy - v.y * xz),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dPoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.x * yz + v.z * xy - v.y * xz),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dBivectorWeight): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.wx * yz + v.wz * xy - v.wy * xz),
      xy = (-v.wx * xz - v.wy * yz),
      xz = (v.wx * xy - v.wz * yz),
      yz = (v.wy * xy + v.wz * xz),
    )

  infix def antiGeometric(v: Pga3dPseudoScalar): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.i * xy,
      xz = v.i * xz,
      yz = v.i * yz,
    )

  infix def antiDot(v: Pga3dMotor): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.i * xy,
      xz = v.i * xz,
      yz = v.i * yz,
    )

  infix def antiDot(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  infix def antiDot(v: Pga3dVector): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  infix def antiDot(v: Pga3dPoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * yz + v.z * xy - v.y * xz),
    )

  infix def antiDot(v: Pga3dPseudoScalar): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.i * xy,
      xz = v.i * xz,
      yz = v.i * yz,
    )

  infix def antiWedge(v: Pga3dMotor): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.wx * yz + v.wz * xy - v.wy * xz),
      xy = v.i * xy,
      xz = v.i * xz,
      yz = v.i * yz,
    )

  inline infix def v(v: Pga3dMotor): Pga3dQuaternion = antiWedge(v)

  inline infix def join(v: Pga3dMotor): Pga3dQuaternion = antiWedge(v)

  infix def antiWedge(v: Pga3dBivector): Double =
    (v.wx * yz + v.wz * xy - v.wy * xz)

  inline infix def v(v: Pga3dBivector): Double = antiWedge(v)

  inline infix def join(v: Pga3dBivector): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dProjectivePoint): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  inline infix def v(v: Pga3dProjectivePoint): Pga3dPlaneIdeal = antiWedge(v)

  inline infix def join(v: Pga3dProjectivePoint): Pga3dPlaneIdeal = antiWedge(v)

  infix def antiWedge(v: Pga3dProjectiveTranslator): Double =
    (v.wx * yz + v.wz * xy - v.wy * xz)

  inline infix def v(v: Pga3dProjectiveTranslator): Double = antiWedge(v)

  inline infix def join(v: Pga3dProjectiveTranslator): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dTranslator): Double =
    (v.wx * yz + v.wz * xy - v.wy * xz)

  inline infix def v(v: Pga3dTranslator): Double = antiWedge(v)

  inline infix def join(v: Pga3dTranslator): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dVector): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  inline infix def v(v: Pga3dVector): Pga3dPlaneIdeal = antiWedge(v)

  inline infix def join(v: Pga3dVector): Pga3dPlaneIdeal = antiWedge(v)

  infix def antiWedge(v: Pga3dPoint): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  inline infix def v(v: Pga3dPoint): Pga3dPlaneIdeal = antiWedge(v)

  inline infix def join(v: Pga3dPoint): Pga3dPlaneIdeal = antiWedge(v)

  infix def antiWedge(v: Pga3dBivectorWeight): Double =
    (v.wx * yz + v.wz * xy - v.wy * xz)

  inline infix def v(v: Pga3dBivectorWeight): Double = antiWedge(v)

  inline infix def join(v: Pga3dBivectorWeight): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dPseudoScalar): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = v.i * xy,
      xz = v.i * xz,
      yz = v.i * yz,
    )

  inline infix def v(v: Pga3dPseudoScalar): Pga3dBivectorBulk = antiWedge(v)

  inline infix def join(v: Pga3dPseudoScalar): Pga3dBivectorBulk = antiWedge(v)

  infix def sandwich(v: Pga3dMotor): Pga3dMotor =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dMotor(
      s = v.s * (xyMxy + xzMxz + yzMyz),
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
      i = v.i * (xyMxy + xzMxz + yzMyz),
    )

  infix def sandwich(v: Pga3dPlane): Pga3dPlane =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dPlane(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
      w = v.w * (xyMxy + xzMxz + yzMyz),
    )

  infix def sandwich(v: Pga3dBivector): Pga3dBivector =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dBivector(
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
    )

  infix def sandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectivePoint(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
      w = v.w * (xyMxy + xzMxz + yzMyz),
    )

  infix def sandwich(v: Pga3dQuaternion): Pga3dQuaternion =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dQuaternion(
      s = v.s * (xyMxy + xzMxz + yzMyz),
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
    )

  infix def sandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectiveTranslator(
      s = v.s * (xyMxy + xzMxz + yzMyz),
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
    )

  infix def sandwich(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectiveTranslator(
      s = (xyMxy + xzMxz + yzMyz),
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
    )

  infix def sandwich(v: Pga3dVector): Pga3dVector =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dVector(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
    )

  infix def sandwich(v: Pga3dPoint): Pga3dProjectivePoint =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectivePoint(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
      w = (xyMxy + xzMxz + yzMyz),
    )

  infix def sandwich(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dPlaneIdeal(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
    )

  infix def sandwich(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dBivectorBulk(
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
    )

  infix def sandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dBivectorWeight(
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
    )

  infix def sandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = v.i * (xy * xy + xz * xz + yz * yz),
    )

  infix def sandwich(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (xy * xy + xz * xz + yz * yz),
    )

  infix def reverseSandwich(v: Pga3dMotor): Pga3dMotor =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dMotor(
      s = v.s * (xyMxy + xzMxz + yzMyz),
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
      i = v.i * (xyMxy + xzMxz + yzMyz),
    )

  infix def reverseSandwich(v: Pga3dPlane): Pga3dPlane =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dPlane(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
      w = v.w * (xyMxy + xzMxz + yzMyz),
    )

  infix def reverseSandwich(v: Pga3dBivector): Pga3dBivector =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dBivector(
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
    )

  infix def reverseSandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectivePoint(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
      w = v.w * (xyMxy + xzMxz + yzMyz),
    )

  infix def reverseSandwich(v: Pga3dQuaternion): Pga3dQuaternion =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dQuaternion(
      s = v.s * (xyMxy + xzMxz + yzMyz),
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
    )

  infix def reverseSandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectiveTranslator(
      s = v.s * (xyMxy + xzMxz + yzMyz),
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
    )

  infix def reverseSandwich(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectiveTranslator(
      s = (xyMxy + xzMxz + yzMyz),
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
    )

  infix def reverseSandwich(v: Pga3dVector): Pga3dVector =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dVector(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
    )

  infix def reverseSandwich(v: Pga3dPoint): Pga3dProjectivePoint =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dProjectivePoint(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
      w = (xyMxy + xzMxz + yzMyz),
    )

  infix def reverseSandwich(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dPlaneIdeal(
      x = (2.0 * (v.z * xyMyz - v.y * xzMyz) + v.x * (yzMyz - xyMxy - xzMxz)),
      y = (2.0 * (-v.x * xzMyz - v.z * xyMxz) + v.y * (xzMxz - xyMxy - yzMyz)),
      z = (2.0 * (v.x * xyMyz - v.y * xyMxz) + v.z * (xyMxy - xzMxz - yzMyz)),
    )

  infix def reverseSandwich(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dBivectorBulk(
      xy = (2.0 * (v.xz * xyMxz + v.yz * xyMyz) + v.xy * (xyMxy - xzMxz - yzMyz)),
      xz = (2.0 * (v.xy * xyMxz + v.yz * xzMyz) + v.xz * (xzMxz - xyMxy - yzMyz)),
      yz = (2.0 * (v.xy * xyMyz + v.xz * xzMyz) + v.yz * (yzMyz - xyMxy - xzMxz)),
    )

  infix def reverseSandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val yzMyz = yz * yz
    Pga3dBivectorWeight(
      wx = (2.0 * (v.wz * xyMyz - v.wy * xzMyz) + v.wx * (yzMyz - xyMxy - xzMxz)),
      wy = (2.0 * (-v.wx * xzMyz - v.wz * xyMxz) + v.wy * (xzMxz - xyMxy - yzMyz)),
      wz = (2.0 * (v.wx * xyMyz - v.wy * xyMxz) + v.wz * (xyMxy - xzMxz - yzMyz)),
    )

  infix def reverseSandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = v.i * (xy * xy + xz * xz + yz * yz),
    )

  infix def reverseSandwich(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (xy * xy + xz * xz + yz * yz),
    )

  infix def cross(v: Pga3dMotor): Pga3dBivector =
    Pga3dBivector(
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
      xy = (v.xz * yz - v.yz * xz),
      xz = (v.yz * xy - v.xy * yz),
      yz = (v.xy * xz - v.xz * xy),
    )

  infix def cross(v: Pga3dPlane): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  infix def cross(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
      xy = (v.xz * yz - v.yz * xz),
      xz = (v.yz * xy - v.xy * yz),
      yz = (v.xy * xz - v.xz * xy),
    )

  infix def cross(v: Pga3dProjectivePoint): Pga3dVector =
    Pga3dVector(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  infix def cross(v: Pga3dQuaternion): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.xz * yz - v.yz * xz),
      xz = (v.yz * xy - v.xy * yz),
      yz = (v.xy * xz - v.xz * xy),
    )

  infix def cross(v: Pga3dProjectiveTranslator): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
    )

  infix def cross(v: Pga3dTranslator): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
    )

  infix def cross(v: Pga3dVector): Pga3dVector =
    Pga3dVector(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  infix def cross(v: Pga3dPoint): Pga3dVector =
    Pga3dVector(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  infix def cross(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = (v.y * xy + v.z * xz),
      y = (v.z * yz - v.x * xy),
      z = (-v.x * xz - v.y * yz),
    )

  infix def cross(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = (v.xz * yz - v.yz * xz),
      xz = (v.yz * xy - v.xy * yz),
      yz = (v.xy * xz - v.xz * xy),
    )

  infix def cross(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (v.wy * xy + v.wz * xz),
      wy = (v.wz * yz - v.wx * xy),
      wz = (-v.wx * xz - v.wy * yz),
    )


object Pga3dBivectorBulk:

  inline val componentsCount = 3

  val zero: Pga3dBivectorBulk = Pga3dBivectorBulk(0.0, 0.0, 0.0)