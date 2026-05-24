package com.github.kright.pga3d

import me.kright.gametools.mathutil.FlatDoubleSerializer
import scala.annotation.targetName


/** this code is generated, see com.github.kright.pga3d.codegen.CodeGenClass */
final case class Pga3dTranslator(wx: Double = 0.0,
                                 wy: Double = 0.0,
                                 wz: Double = 0.0) derives CanEqual, FlatDoubleSerializer:
  inline val s = 1.0

  override def toString: String =
    s"Pga3dTranslator(wx = $wx, wy = $wy, wz = $wz)"

  def dual: Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = wz,
      xz = -wy,
      yz = wx,
      i = 1.0,
    )

  def weight: Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = wx,
      wy = wy,
      wz = wz,
    )

  def bulk: Double =
    1.0

  @targetName("unaryMinus")
  def unary_- : Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = -1.0,
      wx = -wx,
      wy = -wy,
      wz = -wz,
    )

  def reverse: Pga3dTranslator =
    Pga3dTranslator(
      wx = -wx,
      wy = -wy,
      wz = -wz,
    )

  def antiReverse: Pga3dTranslator =
    Pga3dTranslator(
      wx = -wx,
      wy = -wy,
      wz = -wz,
    )

  def bulkNormSquare: Double =
    1.0

  def bulkNorm: Double =
    Math.sqrt(bulkNormSquare)

  def normalizedByBulk =
    this / bulkNorm

  def weightNormSquare: Double =
    (wx * wx + wy * wy + wz * wz)

  def weightNorm: Double =
    Math.sqrt(weightNormSquare)

  def normalizedByWeight =
    this / weightNorm

  def normSquare: Double =
    (1.0 + wx * wx + wy * wy + wz * wz)

  def norm: Double =
    Math.sqrt(normSquare)

  def normalizedByNorm =
    this / norm

  @targetName("times")
  def *(v: Double): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = v,
      wx = v * wx,
      wy = v * wy,
      wz = v * wz,
    )

  @targetName("div")
  def /(v: Double): Pga3dProjectiveTranslator =
    this * (1.0 / v)

  @targetName("plus")
  def +(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = 2.0,
      wx = (v.wx + wx),
      wy = (v.wy + wy),
      wz = (v.wz + wz),
    )

  @targetName("minus")
  def -(v: Pga3dTranslator): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (wx - v.wx),
      wy = (wy - v.wy),
      wz = (wz - v.wz),
    )

  def madd(v: Pga3dTranslator, mult: Double): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = (1.0 + mult),
      wx = (wx + mult * v.wx),
      wy = (wy + mult * v.wy),
      wz = (wz + mult * v.wz),
    )

  def multiplyElementwise(v: Pga3dTranslator): Pga3dTranslator =
    Pga3dTranslator(
      wx = v.wx * wx,
      wy = v.wy * wy,
      wz = v.wz * wz,
    )

  def log(): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = wx,
      wy = wy,
      wz = wz,
    )

  def toMultivector: Pga3dMultivector =
    Pga3dMultivector(
      s = 1.0,
      w = 0.0,
      x = 0.0,
      y = 0.0,
      z = 0.0,
      wx = wx,
      wy = wy,
      wz = wz,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = 0.0,
      i = 0.0,
    )

  def toMotor: Pga3dMotor =
    Pga3dMotor(
      s = 1.0,
      wx = wx,
      wy = wy,
      wz = wz,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = 0.0,
    )

  def toBivectorUnsafe: Pga3dBivector =
    Pga3dBivector(
      wx = wx,
      wy = wy,
      wz = wz,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
    )

  def toQuaternionUnsafe: Pga3dQuaternion =
    Pga3dQuaternion(
      s = 1.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
    )

  def toProjectiveTranslator: Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = 1.0,
      wx = wx,
      wy = wy,
      wz = wz,
    )

  def toBivectorWeightUnsafe: Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = wx,
      wy = wy,
      wz = wz,
    )

  infix def geometric(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = (v.wx + v.s * wx - v.xy * wy - v.xz * wz),
      wy = (v.wy + v.s * wy + v.xy * wx - v.yz * wz),
      wz = (v.wz + v.s * wz + v.xz * wx + v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.i + v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dPlane): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.w + v.x * wx + v.y * wy + v.z * wz),
      x = v.x,
      y = v.y,
      z = v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dBivector): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wx - v.xy * wy - v.xz * wz),
      wy = (v.wy + v.xy * wx - v.yz * wz),
      wz = (v.wz + v.xz * wx + v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = (v.x - v.w * wx),
      y = (v.y - v.w * wy),
      z = (v.z - v.w * wz),
      w = v.w,
    )

  infix def geometric(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = (v.s * wx - v.xy * wy - v.xz * wz),
      wy = (v.s * wy + v.xy * wx - v.yz * wz),
      wz = (v.s * wz + v.xz * wx + v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = v.s,
      wx = (v.wx + v.s * wx),
      wy = (v.wy + v.s * wy),
      wz = (v.wz + v.s * wz),
    )

  infix def geometric(v: Pga3dTranslator): Pga3dTranslator =
    Pga3dTranslator(
      wx = (v.wx + wx),
      wy = (v.wy + wy),
      wz = (v.wz + wz),
    )

  infix def geometric(v: Pga3dVector): Pga3dVector =
    v

  infix def geometric(v: Pga3dPoint): Pga3dPoint =
    Pga3dPoint(
      x = (v.x - wx),
      y = (v.y - wy),
      z = (v.z - wz),
    )

  infix def geometric(v: Pga3dPlaneIdeal): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
      x = v.x,
      y = v.y,
      z = v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def geometric(v: Pga3dBivectorBulk): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (-v.xy * wy - v.xz * wz),
      wy = (v.xy * wx - v.yz * wz),
      wz = (v.xz * wx + v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    v

  infix def geometric(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    v

  infix def geometric(v: Pga3dPointCenter.type): Pga3dPoint =
    Pga3dPoint(
      x = -wx,
      y = -wy,
      z = -wz,
    )

  infix def dot(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = (v.wx + v.s * wx),
      wy = (v.wy + v.s * wy),
      wz = (v.wz + v.s * wz),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = v.i,
    )

  infix def dot(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = v.x,
      y = v.y,
      z = v.z,
      w = (v.w + v.x * wx + v.y * wy + v.z * wz),
    )

  infix def dot(v: Pga3dBivector): Pga3dBivector =
    v

  infix def dot(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    v

  infix def dot(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = v.s * wx,
      wy = v.s * wy,
      wz = v.s * wz,
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = 0.0,
    )

  infix def dot(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = v.s,
      wx = (v.wx + v.s * wx),
      wy = (v.wy + v.s * wy),
      wz = (v.wz + v.s * wz),
    )

  infix def dot(v: Pga3dTranslator): Pga3dTranslator =
    Pga3dTranslator(
      wx = (v.wx + wx),
      wy = (v.wy + wy),
      wz = (v.wz + wz),
    )

  infix def dot(v: Pga3dVector): Pga3dVector =
    v

  infix def dot(v: Pga3dPoint): Pga3dPoint =
    v

  infix def dot(v: Pga3dPlaneIdeal): Pga3dPlane =
    Pga3dPlane(
      x = v.x,
      y = v.y,
      z = v.z,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  infix def dot(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    v

  infix def dot(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    v

  infix def dot(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    v

  infix def dot(v: Pga3dPointCenter.type): Pga3dPointCenter.type =
    v

  infix def wedge(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = (v.wx + v.s * wx),
      wy = (v.wy + v.s * wy),
      wz = (v.wz + v.s * wz),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.i + v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dMotor): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dMotor): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dPlane): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = v.w,
      x = v.x,
      y = v.y,
      z = v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  inline infix def ^(v: Pga3dPlane): Pga3dMultivector = wedge(v)

  inline infix def meet(v: Pga3dPlane): Pga3dMultivector = wedge(v)

  infix def wedge(v: Pga3dBivector): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = v.wx,
      wy = v.wy,
      wz = v.wz,
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dBivector): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dBivector): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    v

  inline infix def ^(v: Pga3dProjectivePoint): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dProjectivePoint): Pga3dProjectivePoint = wedge(v)

  infix def wedge(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = v.s * wx,
      wy = v.s * wy,
      wz = v.s * wz,
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dQuaternion): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dQuaternion): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = v.s,
      wx = (v.wx + v.s * wx),
      wy = (v.wy + v.s * wy),
      wz = (v.wz + v.s * wz),
    )

  inline infix def ^(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator = wedge(v)

  inline infix def meet(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator = wedge(v)

  infix def wedge(v: Pga3dTranslator): Pga3dTranslator =
    Pga3dTranslator(
      wx = (v.wx + wx),
      wy = (v.wy + wy),
      wz = (v.wz + wz),
    )

  inline infix def ^(v: Pga3dTranslator): Pga3dTranslator = wedge(v)

  inline infix def meet(v: Pga3dTranslator): Pga3dTranslator = wedge(v)

  infix def wedge(v: Pga3dVector): Pga3dVector =
    v

  inline infix def ^(v: Pga3dVector): Pga3dVector = wedge(v)

  inline infix def meet(v: Pga3dVector): Pga3dVector = wedge(v)

  infix def wedge(v: Pga3dPoint): Pga3dPoint =
    v

  inline infix def ^(v: Pga3dPoint): Pga3dPoint = wedge(v)

  inline infix def meet(v: Pga3dPoint): Pga3dPoint = wedge(v)

  infix def wedge(v: Pga3dPlaneIdeal): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = v.x,
      y = v.y,
      z = v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  inline infix def ^(v: Pga3dPlaneIdeal): Pga3dMultivector = wedge(v)

  inline infix def meet(v: Pga3dPlaneIdeal): Pga3dMultivector = wedge(v)

  infix def wedge(v: Pga3dBivectorBulk): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dBivectorBulk): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dBivectorBulk): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    v

  inline infix def ^(v: Pga3dBivectorWeight): Pga3dBivectorWeight = wedge(v)

  inline infix def meet(v: Pga3dBivectorWeight): Pga3dBivectorWeight = wedge(v)

  infix def wedge(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    v

  inline infix def ^(v: Pga3dPseudoScalar): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dPseudoScalar): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dPointCenter.type): Pga3dPointCenter.type =
    v

  inline infix def ^(v: Pga3dPointCenter.type): Pga3dPointCenter.type = wedge(v)

  inline infix def meet(v: Pga3dPointCenter.type): Pga3dPointCenter.type = wedge(v)

  infix def antiGeometric(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = (v.i + v.xy * wz + v.yz * wx - v.xz * wy),
      wx = (v.i * wx + v.wy * wz - v.wz * wy),
      wy = (v.i * wy + v.wz * wx - v.wx * wz),
      wz = (v.i * wz + v.wx * wy - v.wy * wx),
      xy = (-v.wz + v.xz * wx + v.yz * wy - v.s * wz),
      xz = (v.wy + v.s * wy + v.yz * wz - v.xy * wx),
      yz = (-v.wx - v.s * wx - v.xy * wy - v.xz * wz),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dPlane): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.y * wz - v.z * wy),
      y = (v.z * wx - v.x * wz),
      z = (v.x * wy - v.y * wx),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = v.w * wz,
      wxz = -v.w * wy,
      wyz = v.w * wx,
      xyz = (-v.w + v.x * wx + v.y * wy + v.z * wz),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dBivector): Pga3dMotor =
    Pga3dMotor(
      s = (v.xy * wz + v.yz * wx - v.xz * wy),
      wx = (v.wy * wz - v.wz * wy),
      wy = (v.wz * wx - v.wx * wz),
      wz = (v.wx * wy - v.wy * wx),
      xy = (-v.wz + v.xz * wx + v.yz * wy),
      xz = (v.wy + v.yz * wz - v.xy * wx),
      yz = (-v.wx - v.xy * wy - v.xz * wz),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dProjectivePoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
      x = (-v.x - v.w * wx),
      y = (-v.y - v.w * wy),
      z = (-v.z - v.w * wz),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dQuaternion): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.xy * wz + v.yz * wx - v.xz * wy),
      xy = (v.xz * wx + v.yz * wy - v.s * wz),
      xz = (v.s * wy + v.yz * wz - v.xy * wx),
      yz = (-v.s * wx - v.xy * wy - v.xz * wz),
    )

  infix def antiGeometric(v: Pga3dProjectiveTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wy * wz - v.wz * wy),
      wy = (v.wz * wx - v.wx * wz),
      wz = (v.wx * wy - v.wy * wx),
      xy = (-v.wz - v.s * wz),
      xz = (v.wy + v.s * wy),
      yz = (-v.wx - v.s * wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wy * wz - v.wz * wy),
      wy = (v.wz * wx - v.wx * wz),
      wz = (v.wx * wy - v.wy * wx),
      xy = (-v.wz - wz),
      xz = (v.wy + wy),
      yz = (-v.wx - wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dVector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
      x = -v.x,
      y = -v.y,
      z = -v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dPoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
      x = (-v.x - wx),
      y = (-v.y - wy),
      z = (-v.z - wz),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dPlaneIdeal): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = (v.y * wz - v.z * wy),
      y = (v.z * wx - v.x * wz),
      z = (v.x * wy - v.y * wx),
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = (v.x * wx + v.y * wy + v.z * wz),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dBivectorBulk): Pga3dQuaternion =
    Pga3dQuaternion(
      s = (v.xy * wz + v.yz * wx - v.xz * wy),
      xy = (v.xz * wx + v.yz * wy),
      xz = (v.yz * wz - v.xy * wx),
      yz = (-v.xy * wy - v.xz * wz),
    )

  infix def antiGeometric(v: Pga3dBivectorWeight): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wy * wz - v.wz * wy),
      wy = (v.wz * wx - v.wx * wz),
      wz = (v.wx * wy - v.wy * wx),
      xy = -v.wz,
      xz = v.wy,
      yz = -v.wx,
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dPseudoScalar): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = v.i,
      wx = v.i * wx,
      wy = v.i * wy,
      wz = v.i * wz,
    )

  infix def antiGeometric(v: Pga3dPointCenter.type): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = -wx,
      y = -wy,
      z = -wz,
    )

  infix def antiDot(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = v.i,
      wx = v.i * wx,
      wy = v.i * wy,
      wz = v.i * wz,
      xy = (-v.wz - v.s * wz),
      xz = (v.wy + v.s * wy),
      yz = (-v.wx - v.s * wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dPlane): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = -v.w * wx,
      y = -v.w * wy,
      z = -v.w * wz,
      w = (-v.w + v.x * wx + v.y * wy + v.z * wz),
    )

  infix def antiDot(v: Pga3dBivector): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = -v.wz,
      xz = v.wy,
      yz = -v.wx,
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dProjectivePoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = -v.x,
      y = -v.y,
      z = -v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def antiDot(v: Pga3dQuaternion): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = -v.s * wz,
      xz = v.s * wy,
      yz = -v.s * wx,
    )

  infix def antiDot(v: Pga3dProjectiveTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = (-v.wz - v.s * wz),
      xz = (v.wy + v.s * wy),
      yz = (-v.wx - v.s * wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = (-v.wz - wz),
      xz = (v.wy + wy),
      yz = (-v.wx - wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dVector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = -v.x,
      y = -v.y,
      z = -v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def antiDot(v: Pga3dPoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = -v.x,
      y = -v.y,
      z = -v.z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = (v.y * wx - v.x * wy),
      wxz = (v.z * wx - v.x * wz),
      wyz = (v.z * wy - v.y * wz),
      xyz = 0.0,
      i = 0.0,
    )

  infix def antiDot(v: Pga3dPlaneIdeal): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  infix def antiDot(v: Pga3dBivectorWeight): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = -v.wz,
      xz = v.wy,
      yz = -v.wx,
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dPseudoScalar): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = v.i,
      wx = v.i * wx,
      wy = v.i * wy,
      wz = v.i * wz,
    )

  infix def antiWedge(v: Pga3dMotor): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = (v.i + v.xy * wz + v.yz * wx - v.xz * wy),
      wx = v.i * wx,
      wy = v.i * wy,
      wz = v.i * wz,
    )

  inline infix def v(v: Pga3dMotor): Pga3dProjectiveTranslator = antiWedge(v)

  inline infix def join(v: Pga3dMotor): Pga3dProjectiveTranslator = antiWedge(v)

  infix def antiWedge(v: Pga3dBivector): Double =
    (v.xy * wz + v.yz * wx - v.xz * wy)

  inline infix def v(v: Pga3dBivector): Double = antiWedge(v)

  inline infix def join(v: Pga3dBivector): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dProjectivePoint): Pga3dPlane =
    Pga3dPlane(
      x = -v.w * wx,
      y = -v.w * wy,
      z = -v.w * wz,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  inline infix def v(v: Pga3dProjectivePoint): Pga3dPlane = antiWedge(v)

  inline infix def join(v: Pga3dProjectivePoint): Pga3dPlane = antiWedge(v)

  infix def antiWedge(v: Pga3dQuaternion): Double =
    (v.xy * wz + v.yz * wx - v.xz * wy)

  inline infix def v(v: Pga3dQuaternion): Double = antiWedge(v)

  inline infix def join(v: Pga3dQuaternion): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dVector): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  inline infix def v(v: Pga3dVector): Pga3dPlane = antiWedge(v)

  inline infix def join(v: Pga3dVector): Pga3dPlane = antiWedge(v)

  infix def antiWedge(v: Pga3dPoint): Pga3dPlane =
    Pga3dPlane(
      x = -wx,
      y = -wy,
      z = -wz,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  inline infix def v(v: Pga3dPoint): Pga3dPlane = antiWedge(v)

  inline infix def join(v: Pga3dPoint): Pga3dPlane = antiWedge(v)

  infix def antiWedge(v: Pga3dBivectorBulk): Double =
    (v.xy * wz + v.yz * wx - v.xz * wy)

  inline infix def v(v: Pga3dBivectorBulk): Double = antiWedge(v)

  inline infix def join(v: Pga3dBivectorBulk): Double = antiWedge(v)

  infix def antiWedge(v: Pga3dPseudoScalar): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = v.i,
      wx = v.i * wx,
      wy = v.i * wy,
      wz = v.i * wz,
    )

  inline infix def v(v: Pga3dPseudoScalar): Pga3dProjectiveTranslator = antiWedge(v)

  inline infix def join(v: Pga3dPseudoScalar): Pga3dProjectiveTranslator = antiWedge(v)

  infix def antiWedge(v: Pga3dPointCenter.type): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = -wx,
      y = -wy,
      z = -wz,
    )

  inline infix def v(v: Pga3dPointCenter.type): Pga3dPlaneIdeal = antiWedge(v)

  inline infix def join(v: Pga3dPointCenter.type): Pga3dPlaneIdeal = antiWedge(v)

  infix def sandwich(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = (v.wx - 2.0 * v.xy * wy - 2.0 * v.xz * wz),
      wy = (v.wy - 2.0 * v.yz * wz + 2.0 * v.xy * wx),
      wz = (v.wz + 2.0 * v.xz * wx + 2.0 * v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = v.i,
    )

  infix def sandwich(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = v.x,
      y = v.y,
      z = v.z,
      w = (v.w + 2.0 * v.x * wx + 2.0 * v.y * wy + 2.0 * v.z * wz),
    )

  infix def sandwich(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = (v.wx - 2.0 * v.xy * wy - 2.0 * v.xz * wz),
      wy = (v.wy - 2.0 * v.yz * wz + 2.0 * v.xy * wx),
      wz = (v.wz + 2.0 * v.xz * wx + 2.0 * v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
    )

  infix def sandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = (v.x - 2.0 * v.w * wx),
      y = (v.y - 2.0 * v.w * wy),
      z = (v.z - 2.0 * v.w * wz),
      w = v.w,
    )

  infix def sandwich(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = 2.0 * (-v.xy * wy - v.xz * wz),
      wy = 2.0 * (v.xy * wx - v.yz * wz),
      wz = 2.0 * (v.xz * wx + v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = 0.0,
    )

  infix def sandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    v

  infix def sandwich(v: Pga3dTranslator): Pga3dTranslator =
    v

  infix def sandwich(v: Pga3dVector): Pga3dVector =
    v

  infix def sandwich(v: Pga3dPoint): Pga3dPoint =
    Pga3dPoint(
      x = (v.x - 2.0 * wx),
      y = (v.y - 2.0 * wy),
      z = (v.z - 2.0 * wz),
    )

  infix def sandwich(v: Pga3dPlaneIdeal): Pga3dPlane =
    Pga3dPlane(
      x = v.x,
      y = v.y,
      z = v.z,
      w = 2.0 * (v.x * wx + v.y * wy + v.z * wz),
    )

  infix def sandwich(v: Pga3dBivectorBulk): Pga3dBivector =
    Pga3dBivector(
      wx = 2.0 * (-v.xy * wy - v.xz * wz),
      wy = 2.0 * (v.xy * wx - v.yz * wz),
      wz = 2.0 * (v.xz * wx + v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
    )

  infix def sandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    v

  infix def sandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    v

  infix def sandwich(v: Pga3dPointCenter.type): Pga3dPoint =
    Pga3dPoint(
      x = -2.0 * wx,
      y = -2.0 * wy,
      z = -2.0 * wz,
    )

  infix def reverseSandwich(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = (v.wx + 2.0 * v.xy * wy + 2.0 * v.xz * wz),
      wy = (v.wy - 2.0 * v.xy * wx + 2.0 * v.yz * wz),
      wz = (v.wz - 2.0 * v.xz * wx - 2.0 * v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = v.i,
    )

  infix def reverseSandwich(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = v.x,
      y = v.y,
      z = v.z,
      w = (v.w - 2.0 * v.x * wx - 2.0 * v.y * wy - 2.0 * v.z * wz),
    )

  infix def reverseSandwich(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = (v.wx + 2.0 * v.xy * wy + 2.0 * v.xz * wz),
      wy = (v.wy - 2.0 * v.xy * wx + 2.0 * v.yz * wz),
      wz = (v.wz - 2.0 * v.xz * wx - 2.0 * v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
    )

  infix def reverseSandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = (v.x + 2.0 * v.w * wx),
      y = (v.y + 2.0 * v.w * wy),
      z = (v.z + 2.0 * v.w * wz),
      w = v.w,
    )

  infix def reverseSandwich(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = v.s,
      wx = 2.0 * (v.xy * wy + v.xz * wz),
      wy = 2.0 * (v.yz * wz - v.xy * wx),
      wz = 2.0 * (-v.xz * wx - v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
      i = 0.0,
    )

  infix def reverseSandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    v

  infix def reverseSandwich(v: Pga3dTranslator): Pga3dTranslator =
    v

  infix def reverseSandwich(v: Pga3dVector): Pga3dVector =
    v

  infix def reverseSandwich(v: Pga3dPoint): Pga3dPoint =
    Pga3dPoint(
      x = (v.x + 2.0 * wx),
      y = (v.y + 2.0 * wy),
      z = (v.z + 2.0 * wz),
    )

  infix def reverseSandwich(v: Pga3dPlaneIdeal): Pga3dPlane =
    Pga3dPlane(
      x = v.x,
      y = v.y,
      z = v.z,
      w = 2.0 * (-v.x * wx - v.y * wy - v.z * wz),
    )

  infix def reverseSandwich(v: Pga3dBivectorBulk): Pga3dBivector =
    Pga3dBivector(
      wx = 2.0 * (v.xy * wy + v.xz * wz),
      wy = 2.0 * (v.yz * wz - v.xy * wx),
      wz = 2.0 * (-v.xz * wx - v.yz * wy),
      xy = v.xy,
      xz = v.xz,
      yz = v.yz,
    )

  infix def reverseSandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    v

  infix def reverseSandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    v

  infix def reverseSandwich(v: Pga3dPointCenter.type): Pga3dPoint =
    Pga3dPoint(
      x = 2.0 * wx,
      y = 2.0 * wy,
      z = 2.0 * wz,
    )

  infix def cross(v: Pga3dMotor): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (-v.xy * wy - v.xz * wz),
      wy = (v.xy * wx - v.yz * wz),
      wz = (v.xz * wx + v.yz * wy),
    )

  infix def cross(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  infix def cross(v: Pga3dBivector): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (-v.xy * wy - v.xz * wz),
      wy = (v.xy * wx - v.yz * wz),
      wz = (v.xz * wx + v.yz * wy),
    )

  infix def cross(v: Pga3dProjectivePoint): Pga3dVector =
    Pga3dVector(
      x = -v.w * wx,
      y = -v.w * wy,
      z = -v.w * wz,
    )

  infix def cross(v: Pga3dQuaternion): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (-v.xy * wy - v.xz * wz),
      wy = (v.xy * wx - v.yz * wz),
      wz = (v.xz * wx + v.yz * wy),
    )

  infix def cross(v: Pga3dPoint): Pga3dVector =
    Pga3dVector(
      x = -wx,
      y = -wy,
      z = -wz,
    )

  infix def cross(v: Pga3dPlaneIdeal): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  infix def cross(v: Pga3dBivectorBulk): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = (-v.xy * wy - v.xz * wz),
      wy = (v.xy * wx - v.yz * wz),
      wz = (v.xz * wx + v.yz * wy),
    )

  infix def cross(v: Pga3dPointCenter.type): Pga3dVector =
    Pga3dVector(
      x = -wx,
      y = -wy,
      z = -wz,
    )


object Pga3dTranslator:

  inline val componentsCount = 3

  val id: Pga3dTranslator = Pga3dTranslator(0.0, 0.0, 0.0)

  def addVector(v: Pga3dVector): Pga3dTranslator =
    Pga3dTranslator(
      wx = -0.5 * v.x,
      wy = -0.5 * v.y,
      wz = -0.5 * v.z,
    )