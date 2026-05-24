package com.github.kright.pga3d

import com.github.kright.util.FlatDoubleSerializer
import scala.annotation.targetName


/** this code is generated, see com.github.kright.pga3d.codegen.CodeGenClass */
final case class Pga3dPseudoScalar(i: Double = 0.0) derives CanEqual, FlatDoubleSerializer:

  override def toString: String =
    s"Pga3dPseudoScalar(i = $i)"

  def dual: Double =
    i

  def weight: Pga3dPseudoScalar =
    this

  @targetName("unaryMinus")
  def unary_- : Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = -i,
    )

  def reverse: Pga3dPseudoScalar =
    this

  def antiReverse: Pga3dPseudoScalar =
    this

  def weightNormSquare: Double =
    i * i

  def weightNorm: Double =
    Math.sqrt(weightNormSquare)

  def normalizedByWeight =
    this / weightNorm

  def normSquare: Double =
    i * i

  def norm: Double =
    Math.sqrt(normSquare)

  def normalizedByNorm =
    this / norm

  @targetName("times")
  def *(v: Double): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v,
    )

  @targetName("div")
  def /(v: Double): Pga3dPseudoScalar =
    this * (1.0 / v)

  @targetName("plus")
  def +(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (i + v.i),
    )

  @targetName("minus")
  def -(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (i - v.i),
    )

  def madd(v: Pga3dPseudoScalar, mult: Double): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = (i + mult * v.i),
    )

  def multiplyElementwise(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.i,
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
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = 0.0,
      i = i,
    )

  def toMotor: Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = i,
    )

  infix def geometric(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = i * v.s,
    )

  infix def geometric(v: Pga3dPlane): Pga3dVector =
    Pga3dVector(
      x = -i * v.x,
      y = -i * v.y,
      z = -i * v.z,
    )

  infix def geometric(v: Pga3dBivector): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
    )

  infix def geometric(v: Pga3dProjectivePoint): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i * v.w,
    )

  infix def geometric(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = i * v.s,
    )

  infix def geometric(v: Pga3dProjectiveTranslator): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.s,
    )

  infix def geometric(v: Pga3dTranslator): Pga3dPseudoScalar =
    this

  infix def geometric(v: Pga3dPoint): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i,
    )

  infix def geometric(v: Pga3dPlaneIdeal): Pga3dVector =
    Pga3dVector(
      x = -i * v.x,
      y = -i * v.y,
      z = -i * v.z,
    )

  infix def geometric(v: Pga3dBivectorBulk): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
    )

  infix def geometric(v: Pga3dPointCenter.type): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i,
    )

  infix def dot(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = i * v.s,
    )

  infix def dot(v: Pga3dPlane): Pga3dVector =
    Pga3dVector(
      x = -i * v.x,
      y = -i * v.y,
      z = -i * v.z,
    )

  infix def dot(v: Pga3dBivector): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
    )

  infix def dot(v: Pga3dProjectivePoint): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i * v.w,
    )

  infix def dot(v: Pga3dQuaternion): Pga3dMotor =
    Pga3dMotor(
      s = 0.0,
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      i = i * v.s,
    )

  infix def dot(v: Pga3dProjectiveTranslator): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.s,
    )

  infix def dot(v: Pga3dTranslator): Pga3dPseudoScalar =
    this

  infix def dot(v: Pga3dPoint): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i,
    )

  infix def dot(v: Pga3dPlaneIdeal): Pga3dVector =
    Pga3dVector(
      x = -i * v.x,
      y = -i * v.y,
      z = -i * v.z,
    )

  infix def dot(v: Pga3dBivectorBulk): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = -i * v.yz,
      wy = i * v.xz,
      wz = -i * v.xy,
    )

  infix def dot(v: Pga3dPointCenter.type): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i,
    )

  infix def wedge(v: Pga3dMotor): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.s,
    )

  inline infix def ^(v: Pga3dMotor): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dMotor): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dQuaternion): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.s,
    )

  inline infix def ^(v: Pga3dQuaternion): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dQuaternion): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dProjectiveTranslator): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.s,
    )

  inline infix def ^(v: Pga3dProjectiveTranslator): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dProjectiveTranslator): Pga3dPseudoScalar = wedge(v)

  infix def wedge(v: Pga3dTranslator): Pga3dPseudoScalar =
    this

  inline infix def ^(v: Pga3dTranslator): Pga3dPseudoScalar = wedge(v)

  inline infix def meet(v: Pga3dTranslator): Pga3dPseudoScalar = wedge(v)

  infix def antiGeometric(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = i * v.s,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
      i = i * v.i,
    )

  infix def antiGeometric(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i * v.w,
    )

  infix def antiGeometric(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  infix def antiGeometric(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i * v.w,
    )

  infix def antiGeometric(v: Pga3dQuaternion): Pga3dQuaternion =
    Pga3dQuaternion(
      s = i * v.s,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  infix def antiGeometric(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = i * v.s,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  infix def antiGeometric(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = i,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  infix def antiGeometric(v: Pga3dVector): Pga3dVector =
    Pga3dVector(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
    )

  infix def antiGeometric(v: Pga3dPoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i,
    )

  infix def antiGeometric(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
    )

  infix def antiGeometric(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  infix def antiGeometric(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  infix def antiGeometric(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.i,
    )

  infix def antiGeometric(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = i,
    )

  infix def antiDot(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = i * v.s,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
      i = i * v.i,
    )

  infix def antiDot(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i * v.w,
    )

  infix def antiDot(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  infix def antiDot(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i * v.w,
    )

  infix def antiDot(v: Pga3dQuaternion): Pga3dQuaternion =
    Pga3dQuaternion(
      s = i * v.s,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  infix def antiDot(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = i * v.s,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  infix def antiDot(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = i,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  infix def antiDot(v: Pga3dVector): Pga3dVector =
    Pga3dVector(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
    )

  infix def antiDot(v: Pga3dPoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i,
    )

  infix def antiDot(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
    )

  infix def antiDot(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  infix def antiDot(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  infix def antiDot(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.i,
    )

  infix def antiDot(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = i,
    )

  infix def antiDotI(v: Pga3dPseudoScalar): Double =
    i * v.i

  infix def antiWedge(v: Pga3dMotor): Pga3dMotor =
    Pga3dMotor(
      s = i * v.s,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
      i = i * v.i,
    )

  inline infix def v(v: Pga3dMotor): Pga3dMotor = antiWedge(v)

  inline infix def join(v: Pga3dMotor): Pga3dMotor = antiWedge(v)

  infix def antiWedge(v: Pga3dPlane): Pga3dPlane =
    Pga3dPlane(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i * v.w,
    )

  inline infix def v(v: Pga3dPlane): Pga3dPlane = antiWedge(v)

  inline infix def join(v: Pga3dPlane): Pga3dPlane = antiWedge(v)

  infix def antiWedge(v: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  inline infix def v(v: Pga3dBivector): Pga3dBivector = antiWedge(v)

  inline infix def join(v: Pga3dBivector): Pga3dBivector = antiWedge(v)

  infix def antiWedge(v: Pga3dProjectivePoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i * v.w,
    )

  inline infix def v(v: Pga3dProjectivePoint): Pga3dProjectivePoint = antiWedge(v)

  inline infix def join(v: Pga3dProjectivePoint): Pga3dProjectivePoint = antiWedge(v)

  infix def antiWedge(v: Pga3dQuaternion): Pga3dQuaternion =
    Pga3dQuaternion(
      s = i * v.s,
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  inline infix def v(v: Pga3dQuaternion): Pga3dQuaternion = antiWedge(v)

  inline infix def join(v: Pga3dQuaternion): Pga3dQuaternion = antiWedge(v)

  infix def antiWedge(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = i * v.s,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  inline infix def v(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator = antiWedge(v)

  inline infix def join(v: Pga3dProjectiveTranslator): Pga3dProjectiveTranslator = antiWedge(v)

  infix def antiWedge(v: Pga3dTranslator): Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = i,
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  inline infix def v(v: Pga3dTranslator): Pga3dProjectiveTranslator = antiWedge(v)

  inline infix def join(v: Pga3dTranslator): Pga3dProjectiveTranslator = antiWedge(v)

  infix def antiWedge(v: Pga3dVector): Pga3dVector =
    Pga3dVector(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
    )

  inline infix def v(v: Pga3dVector): Pga3dVector = antiWedge(v)

  inline infix def join(v: Pga3dVector): Pga3dVector = antiWedge(v)

  infix def antiWedge(v: Pga3dPoint): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
      w = i,
    )

  inline infix def v(v: Pga3dPoint): Pga3dProjectivePoint = antiWedge(v)

  inline infix def join(v: Pga3dPoint): Pga3dProjectivePoint = antiWedge(v)

  infix def antiWedge(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = i * v.x,
      y = i * v.y,
      z = i * v.z,
    )

  inline infix def v(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal = antiWedge(v)

  inline infix def join(v: Pga3dPlaneIdeal): Pga3dPlaneIdeal = antiWedge(v)

  infix def antiWedge(v: Pga3dBivectorBulk): Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = i * v.xy,
      xz = i * v.xz,
      yz = i * v.yz,
    )

  inline infix def v(v: Pga3dBivectorBulk): Pga3dBivectorBulk = antiWedge(v)

  inline infix def join(v: Pga3dBivectorBulk): Pga3dBivectorBulk = antiWedge(v)

  infix def antiWedge(v: Pga3dBivectorWeight): Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = i * v.wx,
      wy = i * v.wy,
      wz = i * v.wz,
    )

  inline infix def v(v: Pga3dBivectorWeight): Pga3dBivectorWeight = antiWedge(v)

  inline infix def join(v: Pga3dBivectorWeight): Pga3dBivectorWeight = antiWedge(v)

  infix def antiWedge(v: Pga3dPseudoScalar): Pga3dPseudoScalar =
    Pga3dPseudoScalar(
      i = i * v.i,
    )

  inline infix def v(v: Pga3dPseudoScalar): Pga3dPseudoScalar = antiWedge(v)

  inline infix def join(v: Pga3dPseudoScalar): Pga3dPseudoScalar = antiWedge(v)

  infix def antiWedge(v: Pga3dPointCenter.type): Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = i,
    )

  inline infix def v(v: Pga3dPointCenter.type): Pga3dProjectivePoint = antiWedge(v)

  inline infix def join(v: Pga3dPointCenter.type): Pga3dProjectivePoint = antiWedge(v)

  infix def cross(v: Pga3dPlane): Pga3dVector =
    Pga3dVector(
      x = -i * v.x,
      y = -i * v.y,
      z = -i * v.z,
    )

  infix def cross(v: Pga3dProjectivePoint): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i * v.w,
    )

  infix def cross(v: Pga3dPoint): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i,
    )

  infix def cross(v: Pga3dPlaneIdeal): Pga3dVector =
    Pga3dVector(
      x = -i * v.x,
      y = -i * v.y,
      z = -i * v.z,
    )

  infix def cross(v: Pga3dPointCenter.type): Pga3dPlane =
    Pga3dPlane(
      x = 0.0,
      y = 0.0,
      z = 0.0,
      w = -i,
    )


object Pga3dPseudoScalar:

  inline val componentsCount = 1

  val zero: Pga3dPseudoScalar = Pga3dPseudoScalar(0.0)