package com.github.kright.pga3d

import me.kright.gametools.mathutil.FlatDoubleSerializer
import scala.annotation.targetName


/** this code is generated, see com.github.kright.pga3d.codegen.CodeGenClass */
final case class Pga3dProjectiveTranslator(s: Double = 0.0,
                                           wx: Double = 0.0,
                                           wy: Double = 0.0,
                                           wz: Double = 0.0) derives CanEqual, FlatDoubleSerializer:

  override def toString: String =
    s"Pga3dProjectiveTranslator(s = $s, wx = $wx, wy = $wy, wz = $wz)"

  def dual: Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = wz,
      xz = -wy,
      yz = wx,
      i = s,
    )

  def weight: Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = wx,
      wy = wy,
      wz = wz,
    )

  def bulk: Double =
    s

  @targetName("unaryMinus")
  def unary_- : Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = -s,
      wx = -wx,
      wy = -wy,
      wz = -wz,
    )

  def reverse: Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s,
      wx = -wx,
      wy = -wy,
      wz = -wz,
    )

  def antiReverse: Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s,
      wx = -wx,
      wy = -wy,
      wz = -wz,
    )

  def bulkNormSquare: Double =
    s * s

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
    (s * s + wx * wx + wy * wy + wz * wz)

  def norm: Double =
    Math.sqrt(normSquare)

  def normalizedByNorm =
    this / norm

  @targetName("times")
  def *(v: Double): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s * v,
      wx = v * wx,
      wy = v * wy,
      wz = v * wz,
    )

  @targetName("div")
  def /(v: Double): Pga3dProjectiveTranslator =
    this * (1.0 / v)

  @targetName("plus")
  def +(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = (s + v.s),
      wx = (v.wx + wx),
      wy = (v.wy + wy),
      wz = (v.wz + wz),
    )

  @targetName("minus")
  def -(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = (s - v.s),
      wx = (wx - v.wx),
      wy = (wy - v.wy),
      wz = (wz - v.wz),
    )

  def madd(v: Pga3dProjectiveTranslator, mult: Double): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = (s + mult * v.s),
      wx = (wx + mult * v.wx),
      wy = (wy + mult * v.wy),
      wz = (wz + mult * v.wz),
    )

  def multiplyElementwise(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s * v.s,
      wx = v.wx * wx,
      wy = v.wy * wy,
      wz = v.wz * wz,
    )

  def toMultivector: Pga3dMultivector =
    Pga3dMultivector(
      s = s,
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
      s = s,
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
      s = s,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
    )

  def toTranslatorUnsafe: Pga3dTranslator =
    Pga3dTranslator(
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

  def toTranslator: Pga3dTranslator =
    val mult = (1.0 / s)
    Pga3dTranslator(
      wx = mult * wx,
      wy = mult * wy,
      wz = mult * wz,
    )

  infix def geometric(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = s * v.s,
      wx = (s * v.wx + v.s * wx - v.xy * wy - v.xz * wz),
      wy = (s * v.wy + v.s * wy + v.xy * wx - v.yz * wz),
      wz = (s * v.wz + v.s * wz + v.xz * wx + v.yz * wy),
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (s * v.i + v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dPlane): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (s * v.w + v.x * wx + v.y * wy + v.z * wz),
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
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
      wx = (s * v.wx - v.xy * wy - v.xz * wz),
      wy = (s * v.wy + v.xy * wx - v.yz * wz),
      wz = (s * v.wz + v.xz * wx + v.yz * wy),
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = (s * v.x - v.w * wx),
      y = (s * v.y - v.w * wy),
      z = (s * v.z - v.w * wz),
      w = s * v.w,
    )

  infix def geometric(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = s * v.s,
      wx = (v.s * wx - v.xy * wy - v.xz * wz),
      wy = (v.s * wy + v.xy * wx - v.yz * wz),
      wz = (v.s * wz + v.xz * wx + v.yz * wy),
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s * v.s,
      wx = (s * v.wx + v.s * wx),
      wy = (s * v.wy + v.s * wy),
      wz = (s * v.wz + v.s * wz),
    )

  infix def geometric(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s,
      wx = (wx + s * v.wx),
      wy = (wy + s * v.wy),
      wz = (wz + s * v.wz),
    )

  infix def geometric(v: Pga3dVector): Pga3dVector =
    Pga3dVector(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
    )

  infix def geometric(v: Pga3dPoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = (-wx + s * v.x),
      y = (-wy + s * v.y),
      z = (-wz + s * v.z),
      w = s,
    )

  infix def geometric(v: Pga3dPlaneIdeal): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
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
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  infix def geometric(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = s * v.wx,
      wy = s * v.wy,
      wz = s * v.wz,
    )

  infix def geometric(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = s * v.i,
    )

  infix def geometric(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = -wx,
      y = -wy,
      z = -wz,
      w = s,
    )

  infix def dot(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = s * v.s,
      wx = (s * v.wx + v.s * wx),
      wy = (s * v.wy + v.s * wy),
      wz = (s * v.wz + v.s * wz),
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = s * v.i,
    )

  infix def dot(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
      w = (s * v.w + v.x * wx + v.y * wy + v.z * wz),
    )

  infix def dot(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = s * v.wx,
      wy = s * v.wy,
      wz = s * v.wz,
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
    )

  infix def dot(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
      w = s * v.w,
    )

  infix def dot(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = s * v.s,
      wx = v.s * wx,
      wy = v.s * wy,
      wz = v.s * wz,
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = 0.0,
    )

  infix def dot(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s * v.s,
      wx = (s * v.wx + v.s * wx),
      wy = (s * v.wy + v.s * wy),
      wz = (s * v.wz + v.s * wz),
    )

  infix def dot(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s,
      wx = (wx + s * v.wx),
      wy = (wy + s * v.wy),
      wz = (wz + s * v.wz),
    )

  infix def dot(v: Pga3dVector): Pga3dVector =
    Pga3dVector(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
    )

  infix def dot(v: Pga3dPoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
      w = s,
    )

  infix def dot(v: Pga3dPlaneIdeal): Pga3dPlane =
    Pga3dPlane(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
      w = (v.x * wx + v.y * wy + v.z * wz),
    )

  infix def dot(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
    )

  infix def dot(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = s * v.wx,
      wy = s * v.wy,
      wz = s * v.wz,
    )

  infix def dot(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = s * v.i,
    )

  infix def dot(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = s,
    )

  infix def wedge(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = s * v.s,
      wx = (s * v.wx + v.s * wx),
      wy = (s * v.wy + v.s * wy),
      wz = (s * v.wz + v.s * wz),
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (s * v.i + v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dMotor): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dMotor): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dPlane): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = s * v.w,
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
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
      wx = s * v.wx,
      wy = s * v.wy,
      wz = s * v.wz,
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dBivector): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dBivector): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
      w = s * v.w,
    )

  inline infix def ^(v: Pga3dProjectivePoint): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dProjectivePoint): Pga3dProjectivePoint = wedge(v)

  infix def wedge(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = s * v.s,
      wx = v.s * wx,
      wy = v.s * wy,
      wz = v.s * wz,
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dQuaternion): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dQuaternion): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s * v.s,
      wx = (s * v.wx + v.s * wx),
      wy = (s * v.wy + v.s * wy),
      wz = (s * v.wz + v.s * wz),
    )

  inline infix def ^(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator = wedge(v)

  inline infix def meet(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator = wedge(v)

  infix def wedge(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s,
      wx = (wx + s * v.wx),
      wy = (wy + s * v.wy),
      wz = (wz + s * v.wz),
    )

  inline infix def ^(v: Pga3dTranslator): Pga3dProjectiveTranslator = wedge(v)

  inline infix def meet(v: Pga3dTranslator): Pga3dProjectiveTranslator = wedge(v)

  infix def wedge(v: Pga3dVector): Pga3dVector =
    Pga3dVector(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
    )

  inline infix def ^(v: Pga3dVector): Pga3dVector = wedge(v)

  inline infix def meet(v: Pga3dVector): Pga3dVector = wedge(v)

  infix def wedge(v: Pga3dPoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
      w = s,
    )

  inline infix def ^(v: Pga3dPoint): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dPoint): Pga3dProjectivePoint = wedge(v)

  infix def wedge(v: Pga3dPlaneIdeal): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = s * v.x,
      y = s * v.y,
      z = s * v.z,
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
      xy = s * v.xy,
      xz = s * v.xz,
      yz = s * v.yz,
      i = (v.xy * wz + v.yz * wx - v.xz * wy),
    )

  inline infix def ^(v: Pga3dBivectorBulk): Pga3dMotor = wedge(v)

  inline infix def meet(v: Pga3dBivectorBulk): Pga3dMotor = wedge(v)

  infix def wedge(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = s * v.wx,
      wy = s * v.wy,
      wz = s * v.wz,
    )

  inline infix def ^(v: Pga3dBivectorWeight): Pga3dBivectorWeight = wedge(v)

  inline infix def meet(v: Pga3dBivectorWeight): Pga3dBivectorWeight = wedge(v)

  infix def wedge(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = s * v.i,
    )

  inline infix def ^(v: Pga3dPseudoScalar): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dPseudoScalar): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = s,
    )

  inline infix def ^(v: Pga3dPointCenter.type): Pga3dProjectivePoint = wedge(v)

  inline infix def meet(v: Pga3dPointCenter.type): Pga3dProjectivePoint = wedge(v)

  infix def antiGeometric(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = (s * v.i + v.xy * wz + v.yz * wx - v.xz * wy),
      wx = (v.i * wx + v.wy * wz - v.wz * wy),
      wy = (v.i * wy + v.wz * wx - v.wx * wz),
      wz = (v.i * wz + v.wx * wy - v.wy * wx),
      xy = (v.xz * wx + v.yz * wy - s * v.wz - v.s * wz),
      xz = (s * v.wy + v.s * wy + v.yz * wz - v.xy * wx),
      yz = (-s * v.wx - v.s * wx - v.xy * wy - v.xz * wz),
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
      xyz = (v.x * wx + v.y * wy + v.z * wz - s * v.w),
      i = 0.0,
    )

  infix def antiGeometric(v: Pga3dBivector): Pga3dMotor =
    Pga3dMotor(
      s = (v.xy * wz + v.yz * wx - v.xz * wy),
      wx = (v.wy * wz - v.wz * wy),
      wy = (v.wz * wx - v.wx * wz),
      wz = (v.wx * wy - v.wy * wx),
      xy = (v.xz * wx + v.yz * wy - s * v.wz),
      xz = (s * v.wy + v.yz * wz - v.xy * wx),
      yz = (-s * v.wx - v.xy * wy - v.xz * wz),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dProjectivePoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
      x = (-s * v.x - v.w * wx),
      y = (-s * v.y - v.w * wy),
      z = (-s * v.z - v.w * wz),
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
      xy = (-s * v.wz - v.s * wz),
      xz = (s * v.wy + v.s * wy),
      yz = (-s * v.wx - v.s * wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = (v.wy * wz - v.wz * wy),
      wy = (v.wz * wx - v.wx * wz),
      wz = (v.wx * wy - v.wy * wx),
      xy = (-wz - s * v.wz),
      xz = (wy + s * v.wy),
      yz = (-wx - s * v.wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dVector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.x * wx + v.y * wy + v.z * wz),
      x = -s * v.x,
      y = -s * v.y,
      z = -s * v.z,
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
      x = (-wx - s * v.x),
      y = (-wy - s * v.y),
      z = (-wz - s * v.z),
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
      xy = -s * v.wz,
      xz = s * v.wy,
      yz = -s * v.wx,
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiGeometric(v: Pga3dPseudoScalar): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s * v.i,
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
      s = s * v.i,
      wx = v.i * wx,
      wy = v.i * wy,
      wz = v.i * wz,
      xy = (-s * v.wz - v.s * wz),
      xz = (s * v.wy + v.s * wy),
      yz = (-s * v.wx - v.s * wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dPlane): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = -v.w * wx,
      y = -v.w * wy,
      z = -v.w * wz,
      w = (v.x * wx + v.y * wy + v.z * wz - s * v.w),
    )

  infix def antiDot(v: Pga3dBivector): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = -s * v.wz,
      xz = s * v.wy,
      yz = -s * v.wx,
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dProjectivePoint): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = -s * v.x,
      y = -s * v.y,
      z = -s * v.z,
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
      xy = (-s * v.wz - v.s * wz),
      xz = (s * v.wy + v.s * wy),
      yz = (-s * v.wx - v.s * wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dTranslator): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = (-wz - s * v.wz),
      xz = (wy + s * v.wy),
      yz = (-wx - s * v.wx),
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dVector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = 0.0,
      x = -s * v.x,
      y = -s * v.y,
      z = -s * v.z,
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
      x = -s * v.x,
      y = -s * v.y,
      z = -s * v.z,
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
      xy = -s * v.wz,
      xz = s * v.wy,
      yz = -s * v.wx,
      i = (-v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dPseudoScalar): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s * v.i,
      wx = v.i * wx,
      wy = v.i * wy,
      wz = v.i * wz,
    )

  infix def antiWedge(v: Pga3dMotor): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = (s * v.i + v.xy * wz + v.yz * wx - v.xz * wy),
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
      s = s * v.i,
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
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dMotor(
      s = sMs * v.s,
      wx = (2.0 * (-sMwy * v.xy - sMwz * v.xz) + sMs * v.wx),
      wy = (2.0 * (sMwx * v.xy - sMwz * v.yz) + sMs * v.wy),
      wz = (2.0 * (sMwx * v.xz + sMwy * v.yz) + sMs * v.wz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
      i = sMs * v.i,
    )

  infix def sandwich(v: Pga3dPlane): Pga3dPlane =
    val sMs = s * s
    Pga3dPlane(
      x = sMs * v.x,
      y = sMs * v.y,
      z = sMs * v.z,
      w = (sMs * v.w + 2.0 * s * (v.x * wx + v.y * wy + v.z * wz)),
    )

  infix def sandwich(v: Pga3dBivector): Pga3dBivector =
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dBivector(
      wx = (2.0 * (-sMwy * v.xy - sMwz * v.xz) + sMs * v.wx),
      wy = (2.0 * (sMwx * v.xy - sMwz * v.yz) + sMs * v.wy),
      wz = (2.0 * (sMwx * v.xz + sMwy * v.yz) + sMs * v.wz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
    )

  infix def sandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    val sMs = s * s
    Pga3dProjectivePoint(
      x = (sMs * v.x - 2.0 * s * v.w * wx),
      y = (sMs * v.y - 2.0 * s * v.w * wy),
      z = (sMs * v.z - 2.0 * s * v.w * wz),
      w = sMs * v.w,
    )

  infix def sandwich(v: Pga3dQuaternion): Pga3dMotor =
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dMotor(
      s = sMs * v.s,
      wx = 2.0 * (-sMwy * v.xy - sMwz * v.xz),
      wy = 2.0 * (sMwx * v.xy - sMwz * v.yz),
      wz = 2.0 * (sMwx * v.xz + sMwy * v.yz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
      i = 0.0,
    )

  infix def sandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    val sMs = s * s
    Pga3dProjectiveTranslator(
      s = sMs * v.s,
      wx = sMs * v.wx,
      wy = sMs * v.wy,
      wz = sMs * v.wz,
    )

  infix def sandwich(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    val sMs = s * s
    Pga3dProjectiveTranslator(
      s = sMs,
      wx = sMs * v.wx,
      wy = sMs * v.wy,
      wz = sMs * v.wz,
    )

  infix def sandwich(v: Pga3dVector): Pga3dVector =
    val sMs = s * s
    Pga3dVector(
      x = sMs * v.x,
      y = sMs * v.y,
      z = sMs * v.z,
    )

  infix def sandwich(v: Pga3dPoint): Pga3dProjectivePoint =
    val sMs = s * s
    Pga3dProjectivePoint(
      x = (sMs * v.x - 2.0 * s * wx),
      y = (sMs * v.y - 2.0 * s * wy),
      z = (sMs * v.z - 2.0 * s * wz),
      w = sMs,
    )

  infix def sandwich(v: Pga3dPlaneIdeal): Pga3dPlane =
    val sMs = s * s
    Pga3dPlane(
      x = sMs * v.x,
      y = sMs * v.y,
      z = sMs * v.z,
      w = 2.0 * s * (v.x * wx + v.y * wy + v.z * wz),
    )

  infix def sandwich(v: Pga3dBivectorBulk): Pga3dBivector =
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dBivector(
      wx = 2.0 * (-sMwy * v.xy - sMwz * v.xz),
      wy = 2.0 * (sMwx * v.xy - sMwz * v.yz),
      wz = 2.0 * (sMwx * v.xz + sMwy * v.yz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
    )

  infix def sandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    val sMs = s * s
    Pga3dBivectorWeight(
      wx = sMs * v.wx,
      wy = sMs * v.wy,
      wz = sMs * v.wz,
    )

  infix def sandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = s * s * v.i,
    )

  infix def sandwich(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = -2.0 * s * wx,
      y = -2.0 * s * wy,
      z = -2.0 * s * wz,
      w = s * s,
    )

  infix def reverseSandwich(v: Pga3dMotor): Pga3dMotor =
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dMotor(
      s = sMs * v.s,
      wx = (2.0 * (sMwy * v.xy + sMwz * v.xz) + sMs * v.wx),
      wy = (2.0 * (sMwz * v.yz - sMwx * v.xy) + sMs * v.wy),
      wz = (2.0 * (-sMwx * v.xz - sMwy * v.yz) + sMs * v.wz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
      i = sMs * v.i,
    )

  infix def reverseSandwich(v: Pga3dPlane): Pga3dPlane =
    val sMs = s * s
    Pga3dPlane(
      x = sMs * v.x,
      y = sMs * v.y,
      z = sMs * v.z,
      w = (sMs * v.w + 2.0 * s * (-v.x * wx - v.y * wy - v.z * wz)),
    )

  infix def reverseSandwich(v: Pga3dBivector): Pga3dBivector =
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dBivector(
      wx = (2.0 * (sMwy * v.xy + sMwz * v.xz) + sMs * v.wx),
      wy = (2.0 * (sMwz * v.yz - sMwx * v.xy) + sMs * v.wy),
      wz = (2.0 * (-sMwx * v.xz - sMwy * v.yz) + sMs * v.wz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
    )

  infix def reverseSandwich(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    val sMs = s * s
    Pga3dProjectivePoint(
      x = (sMs * v.x + 2.0 * s * v.w * wx),
      y = (sMs * v.y + 2.0 * s * v.w * wy),
      z = (sMs * v.z + 2.0 * s * v.w * wz),
      w = sMs * v.w,
    )

  infix def reverseSandwich(v: Pga3dQuaternion): Pga3dMotor =
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dMotor(
      s = sMs * v.s,
      wx = 2.0 * (sMwy * v.xy + sMwz * v.xz),
      wy = 2.0 * (sMwz * v.yz - sMwx * v.xy),
      wz = 2.0 * (-sMwx * v.xz - sMwy * v.yz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
      i = 0.0,
    )

  infix def reverseSandwich(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    val sMs = s * s
    Pga3dProjectiveTranslator(
      s = sMs * v.s,
      wx = sMs * v.wx,
      wy = sMs * v.wy,
      wz = sMs * v.wz,
    )

  infix def reverseSandwich(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    val sMs = s * s
    Pga3dProjectiveTranslator(
      s = sMs,
      wx = sMs * v.wx,
      wy = sMs * v.wy,
      wz = sMs * v.wz,
    )

  infix def reverseSandwich(v: Pga3dVector): Pga3dVector =
    val sMs = s * s
    Pga3dVector(
      x = sMs * v.x,
      y = sMs * v.y,
      z = sMs * v.z,
    )

  infix def reverseSandwich(v: Pga3dPoint): Pga3dProjectivePoint =
    val sMs = s * s
    Pga3dProjectivePoint(
      x = (sMs * v.x + 2.0 * s * wx),
      y = (sMs * v.y + 2.0 * s * wy),
      z = (sMs * v.z + 2.0 * s * wz),
      w = sMs,
    )

  infix def reverseSandwich(v: Pga3dPlaneIdeal): Pga3dPlane =
    val sMs = s * s
    Pga3dPlane(
      x = sMs * v.x,
      y = sMs * v.y,
      z = sMs * v.z,
      w = 2.0 * s * (-v.x * wx - v.y * wy - v.z * wz),
    )

  infix def reverseSandwich(v: Pga3dBivectorBulk): Pga3dBivector =
    val sMs = s * s
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    Pga3dBivector(
      wx = 2.0 * (sMwy * v.xy + sMwz * v.xz),
      wy = 2.0 * (sMwz * v.yz - sMwx * v.xy),
      wz = 2.0 * (-sMwx * v.xz - sMwy * v.yz),
      xy = sMs * v.xy,
      xz = sMs * v.xz,
      yz = sMs * v.yz,
    )

  infix def reverseSandwich(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    val sMs = s * s
    Pga3dBivectorWeight(
      wx = sMs * v.wx,
      wy = sMs * v.wy,
      wz = sMs * v.wz,
    )

  infix def reverseSandwich(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = s * s * v.i,
    )

  infix def reverseSandwich(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 2.0 * s * wx,
      y = 2.0 * s * wy,
      z = 2.0 * s * wz,
      w = s * s,
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


object Pga3dProjectiveTranslator:

  inline val componentsCount = 4

  val zero: Pga3dProjectiveTranslator = Pga3dProjectiveTranslator(0.0, 0.0, 0.0, 0.0)