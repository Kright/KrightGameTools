package com.github.kright.pga3d

import me.kright.gametools.mathutil.FlatDoubleSerializer
import scala.annotation.targetName


/** this code is generated, see com.github.kright.pga3d.codegen.CodeGenClass */
final case class Pga3dMultivector(s: Double = 0.0,
                                  w: Double = 0.0,
                                  x: Double = 0.0,
                                  y: Double = 0.0,
                                  z: Double = 0.0,
                                  wx: Double = 0.0,
                                  wy: Double = 0.0,
                                  wz: Double = 0.0,
                                  xy: Double = 0.0,
                                  xz: Double = 0.0,
                                  yz: Double = 0.0,
                                  wxy: Double = 0.0,
                                  wxz: Double = 0.0,
                                  wyz: Double = 0.0,
                                  xyz: Double = 0.0,
                                  i: Double = 0.0) derives CanEqual, FlatDoubleSerializer:

  override def toString: String =
    s"Pga3dMultivector(s = $s, w = $w, x = $x, y = $y, z = $z, wx = $wx, wy = $wy, wz = $wz, xy = $xy, xz = $xz, yz = $yz, wxy = $wxy, wxz = $wxz, wyz = $wyz, xyz = $xyz, i = $i)"

  def dual: Pga3dMultivector =
    Pga3dMultivector(
      s = i,
      w = xyz,
      x = -wyz,
      y = wxz,
      z = -wxy,
      wx = yz,
      wy = -xz,
      wz = xy,
      xy = wz,
      xz = -wy,
      yz = wx,
      wxy = -z,
      wxz = y,
      wyz = -x,
      xyz = w,
      i = s,
    )

  def weight: Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = w,
      x = 0.0,
      y = 0.0,
      z = 0.0,
      wx = wx,
      wy = wy,
      wz = wz,
      xy = 0.0,
      xz = 0.0,
      yz = 0.0,
      wxy = wxy,
      wxz = wxz,
      wyz = wyz,
      xyz = 0.0,
      i = i,
    )

  def bulk: Pga3dMultivector =
    Pga3dMultivector(
      s = s,
      w = 0.0,
      x = x,
      y = y,
      z = z,
      wx = 0.0,
      wy = 0.0,
      wz = 0.0,
      xy = xy,
      xz = xz,
      yz = yz,
      wxy = 0.0,
      wxz = 0.0,
      wyz = 0.0,
      xyz = xyz,
      i = 0.0,
    )

  @targetName("unaryMinus")
  def unary_- : Pga3dMultivector =
    Pga3dMultivector(
      s = -s,
      w = -w,
      x = -x,
      y = -y,
      z = -z,
      wx = -wx,
      wy = -wy,
      wz = -wz,
      xy = -xy,
      xz = -xz,
      yz = -yz,
      wxy = -wxy,
      wxz = -wxz,
      wyz = -wyz,
      xyz = -xyz,
      i = -i,
    )

  def reverse: Pga3dMultivector =
    Pga3dMultivector(
      s = s,
      w = w,
      x = x,
      y = y,
      z = z,
      wx = -wx,
      wy = -wy,
      wz = -wz,
      xy = -xy,
      xz = -xz,
      yz = -yz,
      wxy = -wxy,
      wxz = -wxz,
      wyz = -wyz,
      xyz = -xyz,
      i = i,
    )

  def antiReverse: Pga3dMultivector =
    Pga3dMultivector(
      s = s,
      w = -w,
      x = -x,
      y = -y,
      z = -z,
      wx = -wx,
      wy = -wy,
      wz = -wz,
      xy = -xy,
      xz = -xz,
      yz = -yz,
      wxy = wxy,
      wxz = wxz,
      wyz = wyz,
      xyz = xyz,
      i = i,
    )

  def bulkNormSquare: Double =
    (s * s + x * x + xy * xy + xyz * xyz + xz * xz + y * y + yz * yz + z * z)

  def bulkNorm: Double =
    Math.sqrt(bulkNormSquare)

  def normalizedByBulk =
    this / bulkNorm

  def weightNormSquare: Double =
    (i * i + w * w + wx * wx + wxy * wxy + wxz * wxz + wy * wy + wyz * wyz + wz * wz)

  def weightNorm: Double =
    Math.sqrt(weightNormSquare)

  def normalizedByWeight =
    this / weightNorm

  def normSquare: Double =
    (i * i + s * s + w * w + wx * wx + wxy * wxy + wxz * wxz + wy * wy + wyz * wyz + wz * wz + x * x + xy * xy + xyz * xyz + xz * xz + y * y + yz * yz + z * z)

  def norm: Double =
    Math.sqrt(normSquare)

  def normalizedByNorm =
    this / norm

  @targetName("times")
  def *(v: Double): Pga3dMultivector =
    Pga3dMultivector(
      s = s * v,
      w = v * w,
      x = v * x,
      y = v * y,
      z = v * z,
      wx = v * wx,
      wy = v * wy,
      wz = v * wz,
      xy = v * xy,
      xz = v * xz,
      yz = v * yz,
      wxy = v * wxy,
      wxz = v * wxz,
      wyz = v * wyz,
      xyz = v * xyz,
      i = i * v,
    )

  @targetName("div")
  def /(v: Double): Pga3dMultivector =
    this * (1.0 / v)

  @targetName("plus")
  def +(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = (s + v.s),
      w = (v.w + w),
      x = (v.x + x),
      y = (v.y + y),
      z = (v.z + z),
      wx = (v.wx + wx),
      wy = (v.wy + wy),
      wz = (v.wz + wz),
      xy = (v.xy + xy),
      xz = (v.xz + xz),
      yz = (v.yz + yz),
      wxy = (v.wxy + wxy),
      wxz = (v.wxz + wxz),
      wyz = (v.wyz + wyz),
      xyz = (v.xyz + xyz),
      i = (i + v.i),
    )

  @targetName("minus")
  def -(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = (s - v.s),
      w = (w - v.w),
      x = (x - v.x),
      y = (y - v.y),
      z = (z - v.z),
      wx = (wx - v.wx),
      wy = (wy - v.wy),
      wz = (wz - v.wz),
      xy = (xy - v.xy),
      xz = (xz - v.xz),
      yz = (yz - v.yz),
      wxy = (wxy - v.wxy),
      wxz = (wxz - v.wxz),
      wyz = (wyz - v.wyz),
      xyz = (xyz - v.xyz),
      i = (i - v.i),
    )

  def madd(v: Pga3dMultivector, mult: Double): Pga3dMultivector =
    Pga3dMultivector(
      s = (s + mult * v.s),
      w = (w + mult * v.w),
      x = (x + mult * v.x),
      y = (y + mult * v.y),
      z = (z + mult * v.z),
      wx = (wx + mult * v.wx),
      wy = (wy + mult * v.wy),
      wz = (wz + mult * v.wz),
      xy = (xy + mult * v.xy),
      xz = (xz + mult * v.xz),
      yz = (yz + mult * v.yz),
      wxy = (wxy + mult * v.wxy),
      wxz = (wxz + mult * v.wxz),
      wyz = (wyz + mult * v.wyz),
      xyz = (xyz + mult * v.xyz),
      i = (i + mult * v.i),
    )

  def multiplyElementwise(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = s * v.s,
      w = v.w * w,
      x = v.x * x,
      y = v.y * y,
      z = v.z * z,
      wx = v.wx * wx,
      wy = v.wy * wy,
      wz = v.wz * wz,
      xy = v.xy * xy,
      xz = v.xz * xz,
      yz = v.yz * yz,
      wxy = v.wxy * wxy,
      wxz = v.wxz * wxz,
      wyz = v.wyz * wyz,
      xyz = v.xyz * xyz,
      i = i * v.i,
    )

  def toMotorUnsafe: Pga3dMotor =
    Pga3dMotor(
      s = s,
      wx = wx,
      wy = wy,
      wz = wz,
      xy = xy,
      xz = xz,
      yz = yz,
      i = i,
    )

  def toPlaneUnsafe: Pga3dPlane =
    Pga3dPlane(
      x = x,
      y = y,
      z = z,
      w = w,
    )

  def toBivectorUnsafe: Pga3dBivector =
    Pga3dBivector(
      wx = wx,
      wy = wy,
      wz = wz,
      xy = xy,
      xz = xz,
      yz = yz,
    )

  def toProjectivePointUnsafe: Pga3dProjectivePoint =
    Pga3dProjectivePoint(
      x = -wyz,
      y = wxz,
      z = -wxy,
      w = xyz,
    )

  def toQuaternionUnsafe: Pga3dQuaternion =
    Pga3dQuaternion(
      s = s,
      xy = xy,
      xz = xz,
      yz = yz,
    )

  def toProjectiveTranslatorUnsafe: Pga3dProjectiveTranslator =
    Pga3dProjectiveTranslator(
      s = s,
      wx = wx,
      wy = wy,
      wz = wz,
    )

  def toTranslatorUnsafe: Pga3dTranslator =
    Pga3dTranslator(
      wx = wx,
      wy = wy,
      wz = wz,
    )

  def toVectorUnsafe: Pga3dVector =
    Pga3dVector(
      x = -wyz,
      y = wxz,
      z = -wxy,
    )

  def toPointUnsafe: Pga3dPoint =
    Pga3dPoint(
      x = -wyz,
      y = wxz,
      z = -wxy,
    )

  def toPlaneIdealUnsafe: Pga3dPlaneIdeal =
    Pga3dPlaneIdeal(
      x = x,
      y = y,
      z = z,
    )

  def toBivectorBulkUnsafe: Pga3dBivectorBulk =
    Pga3dBivectorBulk(
      xy = xy,
      xz = xz,
      yz = yz,
    )

  def toBivectorWeightUnsafe: Pga3dBivectorWeight =
    Pga3dBivectorWeight(
      wx = wx,
      wy = wy,
      wz = wz,
    )

  infix def geometric(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = (s * v.s + v.x * x + v.y * y + v.z * z - v.xy * xy - v.xyz * xyz - v.xz * xz - v.yz * yz),
      w = (s * v.w + v.i * xyz + v.s * w + v.x * wx + v.y * wy + v.z * wz - i * v.xyz - v.wx * x - v.wxy * xy - v.wxz * xz - v.wy * y - v.wyz * yz - v.wz * z - v.xy * wxy - v.xz * wxz - v.yz * wyz),
      x = (s * v.x + v.s * x + v.y * xy + v.z * xz - v.xy * y - v.xyz * yz - v.xz * z - v.yz * xyz),
      y = (s * v.y + v.s * y + v.xy * x + v.xyz * xz + v.xz * xyz + v.z * yz - v.x * xy - v.yz * z),
      z = (s * v.z + v.s * z + v.xz * x + v.yz * y - v.x * xz - v.xy * xyz - v.xyz * xy - v.y * yz),
      wx = (s * v.wx + v.s * wx + v.wxy * y + v.wxz * z + v.wy * xy + v.wyz * xyz + v.wz * xz + v.x * w + v.y * wxy + v.z * wxz - i * v.yz - v.i * yz - v.w * x - v.xy * wy - v.xyz * wyz - v.xz * wz),
      wy = (i * v.xz + s * v.wy + v.i * xz + v.s * wy + v.wyz * z + v.wz * yz + v.xy * wx + v.xyz * wxz + v.y * w + v.z * wyz - v.w * y - v.wx * xy - v.wxy * x - v.wxz * xyz - v.x * wxy - v.yz * wz),
      wz = (s * v.wz + v.s * wz + v.wxy * xyz + v.xz * wx + v.yz * wy + v.z * w - i * v.xy - v.i * xy - v.w * z - v.wx * xz - v.wxz * x - v.wy * yz - v.wyz * y - v.x * wxz - v.xyz * wxy - v.y * wyz),
      xy = (s * v.xy + v.s * xy + v.xyz * z + v.xz * yz + v.y * x + v.z * xyz - v.x * y - v.yz * xz),
      xz = (s * v.xz + v.s * xz + v.yz * xy + v.z * x - v.x * z - v.xy * yz - v.xyz * y - v.y * xyz),
      yz = (s * v.yz + v.s * yz + v.x * xyz + v.xy * xz + v.xyz * x + v.z * y - v.xz * xy - v.y * z),
      wxy = (i * v.z + s * v.wxy + v.s * wxy + v.w * xy + v.wx * y + v.wxz * yz + v.xy * w + v.xyz * wz + v.xz * wyz + v.y * wx - v.i * z - v.wy * x - v.wyz * xz - v.wz * xyz - v.x * wy - v.yz * wxz),
      wxz = (s * v.wxz + v.i * y + v.s * wxz + v.w * xz + v.wx * z + v.wy * xyz + v.wyz * xy + v.xz * w + v.yz * wxy + v.z * wx - i * v.y - v.wxy * yz - v.wz * x - v.x * wz - v.xy * wyz - v.xyz * wy),
      wyz = (i * v.x + s * v.wyz + v.s * wyz + v.w * yz + v.wxy * xz + v.wy * z + v.xy * wxz + v.xyz * wx + v.yz * w + v.z * wy - v.i * x - v.wx * xyz - v.wxz * xy - v.wz * y - v.xz * wxy - v.y * wz),
      xyz = (s * v.xyz + v.s * xyz + v.x * yz + v.xy * z + v.yz * x + v.z * xy - v.xz * y - v.y * xz),
      i = (i * v.s + s * v.i + v.wx * yz + v.wxz * y + v.wz * xy + v.x * wyz + v.xy * wz + v.xyz * w + v.yz * wx + v.z * wxy - v.w * xyz - v.wxy * z - v.wy * xz - v.wyz * x - v.xz * wy - v.y * wxz),
    )

  infix def dot(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = (s * v.s + v.x * x + v.y * y + v.z * z - v.xy * xy - v.xyz * xyz - v.xz * xz - v.yz * yz),
      w = (s * v.w + v.i * xyz + v.s * w + v.x * wx + v.y * wy + v.z * wz - i * v.xyz - v.wx * x - v.wxy * xy - v.wxz * xz - v.wy * y - v.wyz * yz - v.wz * z - v.xy * wxy - v.xz * wxz - v.yz * wyz),
      x = (s * v.x + v.s * x + v.y * xy + v.z * xz - v.xy * y - v.xyz * yz - v.xz * z - v.yz * xyz),
      y = (s * v.y + v.s * y + v.xy * x + v.xyz * xz + v.xz * xyz + v.z * yz - v.x * xy - v.yz * z),
      z = (s * v.z + v.s * z + v.xz * x + v.yz * y - v.x * xz - v.xy * xyz - v.xyz * xy - v.y * yz),
      wx = (s * v.wx + v.s * wx + v.wxy * y + v.wxz * z + v.y * wxy + v.z * wxz - i * v.yz - v.i * yz),
      wy = (i * v.xz + s * v.wy + v.i * xz + v.s * wy + v.wyz * z + v.z * wyz - v.wxy * x - v.x * wxy),
      wz = (s * v.wz + v.s * wz - i * v.xy - v.i * xy - v.wxz * x - v.wyz * y - v.x * wxz - v.y * wyz),
      xy = (s * v.xy + v.s * xy + v.xyz * z + v.z * xyz),
      xz = (s * v.xz + v.s * xz - v.xyz * y - v.y * xyz),
      yz = (s * v.yz + v.s * yz + v.x * xyz + v.xyz * x),
      wxy = (i * v.z + s * v.wxy + v.s * wxy - v.i * z),
      wxz = (s * v.wxz + v.i * y + v.s * wxz - i * v.y),
      wyz = (i * v.x + s * v.wyz + v.s * wyz - v.i * x),
      xyz = (s * v.xyz + v.s * xyz),
      i = (i * v.s + s * v.i),
    )

  infix def wedge(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = s * v.s,
      w = (s * v.w + v.s * w),
      x = (s * v.x + v.s * x),
      y = (s * v.y + v.s * y),
      z = (s * v.z + v.s * z),
      wx = (s * v.wx + v.s * wx + v.x * w - v.w * x),
      wy = (s * v.wy + v.s * wy + v.y * w - v.w * y),
      wz = (s * v.wz + v.s * wz + v.z * w - v.w * z),
      xy = (s * v.xy + v.s * xy + v.y * x - v.x * y),
      xz = (s * v.xz + v.s * xz + v.z * x - v.x * z),
      yz = (s * v.yz + v.s * yz + v.z * y - v.y * z),
      wxy = (s * v.wxy + v.s * wxy + v.w * xy + v.wx * y + v.xy * w + v.y * wx - v.wy * x - v.x * wy),
      wxz = (s * v.wxz + v.s * wxz + v.w * xz + v.wx * z + v.xz * w + v.z * wx - v.wz * x - v.x * wz),
      wyz = (s * v.wyz + v.s * wyz + v.w * yz + v.wy * z + v.yz * w + v.z * wy - v.wz * y - v.y * wz),
      xyz = (s * v.xyz + v.s * xyz + v.x * yz + v.xy * z + v.yz * x + v.z * xy - v.xz * y - v.y * xz),
      i = (i * v.s + s * v.i + v.wx * yz + v.wxz * y + v.wz * xy + v.x * wyz + v.xy * wz + v.xyz * w + v.yz * wx + v.z * wxy - v.w * xyz - v.wxy * z - v.wy * xz - v.wyz * x - v.xz * wy - v.y * wxz),
    )

  inline infix def ^(v: Pga3dMultivector): Pga3dMultivector = wedge(v)

  inline infix def meet(v: Pga3dMultivector): Pga3dMultivector = wedge(v)

  infix def antiGeometric(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = (i * v.s + s * v.i + v.w * xyz + v.wx * yz + v.wxy * z + v.wyz * x + v.wz * xy + v.xy * wz + v.y * wxz + v.yz * wx - v.wxz * y - v.wy * xz - v.x * wyz - v.xyz * w - v.xz * wy - v.z * wxy),
      w = (i * v.w + v.i * w + v.wxz * wy + v.wy * wxz - v.wx * wyz - v.wxy * wz - v.wyz * wx - v.wz * wxy),
      x = (i * v.x + s * v.wyz + v.i * x + v.wxz * xy + v.wy * z + v.xy * wxz + v.y * wz + v.yz * w - v.s * wyz - v.w * yz - v.wx * xyz - v.wxy * xz - v.wz * y - v.xyz * wx - v.xz * wxy - v.z * wy),
      y = (i * v.y + v.i * y + v.s * wxz + v.w * xz + v.wyz * xy + v.wz * x + v.xy * wyz + v.z * wx - s * v.wxz - v.wx * z - v.wxy * yz - v.wy * xyz - v.x * wz - v.xyz * wy - v.xz * w - v.yz * wxy),
      z = (i * v.z + s * v.wxy + v.i * z + v.wx * y + v.wyz * xz + v.x * wy + v.xy * w + v.xz * wyz - v.s * wxy - v.w * xy - v.wxz * yz - v.wy * x - v.wz * xyz - v.xyz * wz - v.y * wx - v.yz * wxz),
      wx = (i * v.wx + v.i * wx + v.wxz * wxy + v.wy * wz - v.w * wyz - v.wxy * wxz - v.wyz * w - v.wz * wy),
      wy = (i * v.wy + v.i * wy + v.w * wxz + v.wxz * w + v.wyz * wxy + v.wz * wx - v.wx * wz - v.wxy * wyz),
      wz = (i * v.wz + v.i * wz + v.wx * wy + v.wyz * wxz - v.w * wxy - v.wxy * w - v.wxz * wyz - v.wy * wx),
      xy = (i * v.xy + v.i * xy + v.w * z + v.wxz * x + v.wyz * y + v.x * wxz + v.xyz * wxy + v.xz * wx + v.y * wyz + v.yz * wy - s * v.wz - v.s * wz - v.wx * xz - v.wxy * xyz - v.wy * yz - v.z * w),
      xz = (i * v.xz + s * v.wy + v.i * xz + v.s * wy + v.wx * xy + v.wyz * z + v.xyz * wxz + v.y * w + v.yz * wz + v.z * wyz - v.w * y - v.wxy * x - v.wxz * xyz - v.wz * yz - v.x * wxy - v.xy * wx),
      yz = (i * v.yz + v.i * yz + v.w * x + v.wy * xy + v.wz * xz + v.xyz * wyz - s * v.wx - v.s * wx - v.wxy * y - v.wxz * z - v.wyz * xyz - v.x * w - v.xy * wy - v.xz * wz - v.y * wxy - v.z * wxz),
      wxy = (i * v.wxy + v.i * wxy + v.w * wz + v.wxz * wx + v.wyz * wy + v.wz * w - v.wx * wxz - v.wy * wyz),
      wxz = (i * v.wxz + v.i * wxz + v.wx * wxy + v.wyz * wz - v.w * wy - v.wxy * wx - v.wy * w - v.wz * wyz),
      wyz = (i * v.wyz + v.i * wyz + v.w * wx + v.wx * w + v.wy * wxy + v.wz * wxz - v.wxy * wy - v.wxz * wz),
      xyz = (i * v.xyz + v.i * xyz + v.s * w + v.wx * x + v.wy * y + v.wz * z + v.x * wx + v.xy * wxy + v.xz * wxz + v.y * wy + v.yz * wyz + v.z * wz - s * v.w - v.wxy * xy - v.wxz * xz - v.wyz * yz),
      i = (i * v.i + v.wxy * wxy + v.wxz * wxz + v.wyz * wyz - v.w * w - v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiDot(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = (i * v.s + s * v.i),
      w = (i * v.w + v.i * w),
      x = (i * v.x + s * v.wyz + v.i * x - v.s * wyz),
      y = (i * v.y + v.i * y + v.s * wxz - s * v.wxz),
      z = (i * v.z + s * v.wxy + v.i * z - v.s * wxy),
      wx = (i * v.wx + v.i * wx - v.w * wyz - v.wyz * w),
      wy = (i * v.wy + v.i * wy + v.w * wxz + v.wxz * w),
      wz = (i * v.wz + v.i * wz - v.w * wxy - v.wxy * w),
      xy = (i * v.xy + v.i * xy + v.wxz * x + v.wyz * y + v.x * wxz + v.y * wyz - s * v.wz - v.s * wz),
      xz = (i * v.xz + s * v.wy + v.i * xz + v.s * wy + v.wyz * z + v.z * wyz - v.wxy * x - v.x * wxy),
      yz = (i * v.yz + v.i * yz - s * v.wx - v.s * wx - v.wxy * y - v.wxz * z - v.y * wxy - v.z * wxz),
      wxy = (i * v.wxy + v.i * wxy + v.w * wz + v.wxz * wx + v.wyz * wy + v.wz * w - v.wx * wxz - v.wy * wyz),
      wxz = (i * v.wxz + v.i * wxz + v.wx * wxy + v.wyz * wz - v.w * wy - v.wxy * wx - v.wy * w - v.wz * wyz),
      wyz = (i * v.wyz + v.i * wyz + v.w * wx + v.wx * w + v.wy * wxy + v.wz * wxz - v.wxy * wy - v.wxz * wz),
      xyz = (i * v.xyz + v.i * xyz + v.s * w + v.wx * x + v.wy * y + v.wz * z + v.x * wx + v.xy * wxy + v.xz * wxz + v.y * wy + v.yz * wyz + v.z * wz - s * v.w - v.wxy * xy - v.wxz * xz - v.wyz * yz),
      i = (i * v.i + v.wxy * wxy + v.wxz * wxz + v.wyz * wyz - v.w * w - v.wx * wx - v.wy * wy - v.wz * wz),
    )

  infix def antiWedge(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = (i * v.s + s * v.i + v.w * xyz + v.wx * yz + v.wxy * z + v.wyz * x + v.wz * xy + v.xy * wz + v.y * wxz + v.yz * wx - v.wxz * y - v.wy * xz - v.x * wyz - v.xyz * w - v.xz * wy - v.z * wxy),
      w = (i * v.w + v.i * w + v.wxz * wy + v.wy * wxz - v.wx * wyz - v.wxy * wz - v.wyz * wx - v.wz * wxy),
      x = (i * v.x + v.i * x + v.wxz * xy + v.xy * wxz - v.wx * xyz - v.wxy * xz - v.xyz * wx - v.xz * wxy),
      y = (i * v.y + v.i * y + v.wyz * xy + v.xy * wyz - v.wxy * yz - v.wy * xyz - v.xyz * wy - v.yz * wxy),
      z = (i * v.z + v.i * z + v.wyz * xz + v.xz * wyz - v.wxz * yz - v.wz * xyz - v.xyz * wz - v.yz * wxz),
      wx = (i * v.wx + v.i * wx + v.wxz * wxy - v.wxy * wxz),
      wy = (i * v.wy + v.i * wy + v.wyz * wxy - v.wxy * wyz),
      wz = (i * v.wz + v.i * wz + v.wyz * wxz - v.wxz * wyz),
      xy = (i * v.xy + v.i * xy + v.xyz * wxy - v.wxy * xyz),
      xz = (i * v.xz + v.i * xz + v.xyz * wxz - v.wxz * xyz),
      yz = (i * v.yz + v.i * yz + v.xyz * wyz - v.wyz * xyz),
      wxy = (i * v.wxy + v.i * wxy),
      wxz = (i * v.wxz + v.i * wxz),
      wyz = (i * v.wyz + v.i * wyz),
      xyz = (i * v.xyz + v.i * xyz),
      i = i * v.i,
    )

  inline infix def v(v: Pga3dMultivector): Pga3dMultivector = antiWedge(v)

  inline infix def join(v: Pga3dMultivector): Pga3dMultivector = antiWedge(v)

  infix def sandwich(v: Pga3dMultivector): Pga3dMultivector =
    val sMs = s * s
    val sMw = s * w
    val sMx = s * x
    val sMy = s * y
    val sMz = s * z
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    val sMxy = s * xy
    val sMxz = s * xz
    val sMyz = s * yz
    val sMwxy = s * wxy
    val sMwxz = s * wxz
    val sMwyz = s * wyz
    val sMxyz = s * xyz
    val sMi = i * s
    val wMx = w * x
    val wMy = w * y
    val wMz = w * z
    val wMxy = w * xy
    val wMxz = w * xz
    val wMyz = w * yz
    val wMxyz = w * xyz
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val xMwx = wx * x
    val xMwy = wy * x
    val xMwz = wz * x
    val xMxy = x * xy
    val xMxz = x * xz
    val xMyz = x * yz
    val xMwxy = wxy * x
    val xMwxz = wxz * x
    val xMwyz = wyz * x
    val xMxyz = x * xyz
    val xMi = i * x
    val yMy = y * y
    val yMz = y * z
    val yMwx = wx * y
    val yMwy = wy * y
    val yMwz = wz * y
    val yMxy = xy * y
    val yMxz = xz * y
    val yMyz = y * yz
    val yMwxy = wxy * y
    val yMwxz = wxz * y
    val yMwyz = wyz * y
    val yMxyz = xyz * y
    val yMi = i * y
    val zMz = z * z
    val zMwx = wx * z
    val zMwy = wy * z
    val zMwz = wz * z
    val zMxy = xy * z
    val zMxz = xz * z
    val zMyz = yz * z
    val zMwxy = wxy * z
    val zMwxz = wxz * z
    val zMwyz = wyz * z
    val zMxyz = xyz * z
    val zMi = i * z
    val wxMxy = wx * xy
    val wxMxz = wx * xz
    val wxMyz = wx * yz
    val wxMxyz = wx * xyz
    val wyMxy = wy * xy
    val wyMxz = wy * xz
    val wyMyz = wy * yz
    val wyMxyz = wy * xyz
    val wzMxy = wz * xy
    val wzMxz = wz * xz
    val wzMyz = wz * yz
    val wzMxyz = wz * xyz
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xyMwxy = wxy * xy
    val xyMwxz = wxz * xy
    val xyMwyz = wyz * xy
    val xyMxyz = xy * xyz
    val xyMi = i * xy
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val xzMwxy = wxy * xz
    val xzMwxz = wxz * xz
    val xzMwyz = wyz * xz
    val xzMxyz = xyz * xz
    val xzMi = i * xz
    val yzMyz = yz * yz
    val yzMwxy = wxy * yz
    val yzMwxz = wxz * yz
    val yzMwyz = wyz * yz
    val yzMxyz = xyz * yz
    val yzMi = i * yz
    val wxyMxyz = wxy * xyz
    val wxzMxyz = wxz * xyz
    val wyzMxyz = wyz * xyz
    val xyzMxyz = xyz * xyz
    val xyzMi = i * xyz
    Pga3dMultivector(
      s = (2.0 * (v.x * (sMx + yzMxyz - yMxy - zMxz) + v.y * (sMy + xMxy - xzMxyz - zMyz) + v.z * (sMz + xMxz + xyMxyz + yMyz)) + v.s * (sMs + xMx + xyMxy + xyzMxyz + xzMxz + yMy + yzMyz + zMz)),
      w = (2.0 * (v.i * (sMxyz + yMxz - xMyz - zMxy) + v.s * (sMw + xMwx + xyMwxy + xyzMi + xzMwxz + yMwy + yzMwyz + zMwz) + v.x * (sMwx + wMx + wyzMxyz + yzMi - wyMxy - wzMxz - yMwxy - zMwxz) + v.y * (sMwy + wMy + wxMxy + xMwxy - wxzMxyz - wzMyz - xzMi - zMwyz) + v.z * (sMwz + wMz + wxMxz + wxyMxyz + wyMyz + xMwxz + xyMi + yMwyz)) + v.w * (sMs + xyMxy + xzMxz + yzMyz - xMx - xyzMxyz - yMy - zMz)),
      x = (2.0 * (v.s * (sMx + yMxy + yzMxyz + zMxz) + v.y * (sMxy + xMy - xzMyz - zMxyz) + v.z * (sMxz + xMz + xyMyz + yMxyz)) + v.x * (sMs + xMx + xyzMxyz + yzMyz - xyMxy - xzMxz - yMy - zMz)),
      y = (2.0 * (v.s * (sMy + zMyz - xMxy - xzMxyz) + v.x * (xMy + zMxyz - sMxy - xzMyz) + v.z * (sMyz + yMz - xMxyz - xyMxz)) + v.y * (sMs + xyzMxyz + xzMxz + yMy - xMx - xyMxy - yzMyz - zMz)),
      z = (2.0 * (v.s * (sMz + xyMxyz - xMxz - yMyz) + v.x * (xMz + xyMyz - sMxz - yMxyz) + v.y * (xMxyz + yMz - sMyz - xyMxz)) + v.z * (sMs + xyMxy + xyzMxyz + zMz - xMx - xzMxz - yMy - yzMyz)),
      wx = (2.0 * (v.wxy * (sMy + xzMxyz - xMxy - zMyz) + v.wxz * (sMz + yMyz - xMxz - xyMxyz) + v.wy * (sMxy + zMxyz - xMy - xzMyz) + v.wyz * (sMxyz + zMxy - xMyz - yMxz) + v.wz * (sMxz + xyMyz - xMz - yMxyz) + v.xy * (wMy + wxMxy + wxzMxyz + wzMyz - sMwy - xMwxy - xzMi - zMwyz) + v.xyz * (wMyz + wxMxyz + xyMwxz + yMwz - sMwyz - xMi - xzMwxy - zMwy) + v.xz * (wMz + wxMxz + xyMi + yMwyz - sMwz - wxyMxyz - wyMyz - xMwxz) + v.yz * (wMxyz + wxMyz + wyMxz + zMwxy - sMi - wzMxy - xMwyz - yMwxz)) + v.wx * (sMs + yMy + yzMyz + zMz - xMx - xyMxy - xyzMxyz - xzMxz)),
      wy = (2.0 * (v.wx * (-sMxy - xMy - xzMyz - zMxyz) + v.wxy * (yzMxyz + zMxz - sMx - yMxy) + v.wxz * (-sMxyz - xMyz - yMxz - zMxy) + v.wyz * (sMz + xMxz - xyMxyz - yMyz) + v.wz * (sMyz + xMxyz - xyMxz - yMz) + v.xy * (sMwx + wyMxy + wyzMxyz + zMwxz - wMx - wzMxz - yMwxy - yzMi) + v.xyz * (sMwxz + wyMxyz + xyMwyz + zMwx - wMxz - xMwz - yMi - yzMwxy) + v.xz * (sMi + wxMyz + wyMxz + wzMxy - wMxyz - xMwyz - yMwxz - zMwxy) + v.yz * (wMz + wyMyz + xMwxz + xyMi - sMwz - wxMxz - wxyMxyz - yMwyz)) + v.wy * (sMs + xMx + xzMxz + zMz - xyMxy - xyzMxyz - yMy - yzMyz)),
      wz = (2.0 * (v.wx * (xyMyz + yMxyz - sMxz - xMz) + v.wxy * (sMxyz + xMyz - yMxz - zMxy) + v.wxz * (yMxy + yzMxyz - sMx - zMxz) + v.wy * (-sMyz - xMxyz - xyMxz - yMz) + v.wyz * (-sMy - xMxy - xzMxyz - zMyz) + v.xy * (wMxyz + wyMxz + wzMxy + xMwyz - sMi - wxMyz - yMwxz - zMwxy) + v.xyz * (wMxy + wzMxyz + xMwy + xzMwyz - sMwxy - yMwx - yzMwxz - zMi) + v.xz * (sMwx + wyzMxyz + wzMxz + yMwxy - wMx - wyMxy - yzMi - zMwxz) + v.yz * (sMwy + wxMxy + wzMyz + xzMi - wMy - wxzMxyz - xMwxy - zMwyz)) + v.wz * (sMs + xMx + xyMxy + yMy - xyzMxyz - xzMxz - yzMyz - zMz)),
      xy = (2.0 * (v.xyz * (sMz + xyMxyz - xMxz - yMyz) + v.xz * (sMyz + xyMxz - xMxyz - yMz) + v.yz * (xMz + xyMyz - sMxz - yMxyz)) + v.xy * (sMs + xyMxy + xyzMxyz + zMz - xMx - xzMxz - yMy - yzMyz)),
      xz = (2.0 * (v.xy * (xMxyz + xyMxz - sMyz - yMz) + v.xyz * (xMxy + xzMxyz - sMy - zMyz) + v.yz * (sMxy + xzMyz - xMy - zMxyz)) + v.xz * (sMs + xyzMxyz + xzMxz + yMy - xMx - xyMxy - yzMyz - zMz)),
      yz = (2.0 * (v.xy * (sMxz + xMz + xyMyz + yMxyz) + v.xyz * (sMx + yMxy + yzMxyz + zMxz) + v.xz * (xzMyz + zMxyz - sMxy - xMy)) + v.yz * (sMs + xMx + xyzMxyz + yzMyz - xyMxy - xzMxz - yMy - zMz)),
      wxy = (2.0 * (v.wx * (sMy + xMxy + xzMxyz + zMyz) + v.wxz * (sMyz + xMxyz + xyMxz + yMz) + v.wy * (yMxy + yzMxyz - sMx - zMxz) + v.wyz * (xyMyz + yMxyz - sMxz - xMz) + v.wz * (yMxz + zMxy - sMxyz - xMyz) + v.xy * (sMw + xyMwxy + xyzMi + zMwz - xMwx - xzMwxz - yMwy - yzMwyz) + v.xyz * (sMwz + wMz + wxyMxyz + xyMi - wxMxz - wyMyz - xMwxz - yMwyz) + v.xz * (sMwyz + wMyz + xyMwxz + xzMwxy - wxMxyz - xMi - yMwz - zMwy) + v.yz * (xMwz + xyMwyz + yzMwxy + zMwx - sMwxz - wMxz - wyMxyz - yMi)) + v.wxy * (sMs + xMx + xyMxy + yMy - xyzMxyz - xzMxz - yzMyz - zMz)),
      wxz = (2.0 * (v.wx * (sMz + xMxz - xyMxyz - yMyz) + v.wxy * (xyMxz + yMz - sMyz - xMxyz) + v.wy * (sMxyz + xMyz + yMxz + zMxy) + v.wyz * (sMxy + xMy + xzMyz + zMxyz) + v.wz * (yzMxyz + zMxz - sMx - yMxy) + v.xy * (wxMxyz + xMi + xyMwxz + xzMwxy - sMwyz - wMyz - yMwz - zMwy) + v.xyz * (wxMxy + wxzMxyz + xMwxy + xzMi - sMwy - wMy - wzMyz - zMwyz) + v.xz * (sMw + xyzMi + xzMwxz + yMwy - xMwx - xyMwxy - yzMwyz - zMwz) + v.yz * (sMwxy + wMxy + xzMwyz + yzMwxz - wzMxyz - xMwy - yMwx - zMi)) + v.wxz * (sMs + xMx + xzMxz + zMz - xyMxy - xyzMxyz - yMy - yzMyz)),
      wyz = (2.0 * (v.wx * (xMyz + yMxz - sMxyz - zMxy) + v.wxy * (sMxz + xyMyz - xMz - yMxyz) + v.wxz * (xMy + xzMyz - sMxy - zMxyz) + v.wy * (sMz + yMyz - xMxz - xyMxyz) + v.wz * (xMxy + zMyz - sMy - xzMxyz) + v.xy * (sMwxz + wMxz + wyMxyz + xMwz + xyMwyz + yMi + yzMwxy + zMwx) + v.xyz * (sMwx + wMx + wyMxy + wyzMxyz + wzMxz + yMwxy + yzMi + zMwxz) + v.xz * (wzMxyz + xzMwyz + yzMwxz + zMi - sMwxy - wMxy - xMwy - yMwx) + v.yz * (sMw + xMwx + xyzMi + yzMwyz - xyMwxy - xzMwxz - yMwy - zMwz)) + v.wyz * (sMs + yMy + yzMyz + zMz - xMx - xyMxy - xyzMxyz - xzMxz)),
      xyz = (2.0 * (v.xy * (sMz + xMxz + xyMxyz + yMyz) + v.xz * (xzMxyz + zMyz - sMy - xMxy) + v.yz * (sMx + yzMxyz - yMxy - zMxz)) + v.xyz * (sMs + xMx + xyMxy + xyzMxyz + xzMxz + yMy + yzMyz + zMz)),
      i = (2.0 * (v.s * (sMi + wyMxz + xMwyz + zMwxy - wMxyz - wxMyz - wzMxy - yMwxz) + v.w * (xMyz + zMxy - sMxyz - yMxz) + v.x * (sMwyz + xMi + xyMwxz + yMwz - wMyz - wxMxyz - xzMwxy - zMwy) + v.y * (wMxz + xyMwyz + yMi + zMwx - sMwxz - wyMxyz - xMwz - yzMwxy) + v.z * (sMwxy + xMwy + xzMwyz + zMi - wMxy - wzMxyz - yMwx - yzMwxz)) + v.i * (sMs + xyMxy + xzMxz + yzMyz - xMx - xyzMxyz - yMy - zMz)),
    )

  infix def reverseSandwich(v: Pga3dMultivector): Pga3dMultivector =
    val sMs = s * s
    val sMw = s * w
    val sMx = s * x
    val sMy = s * y
    val sMz = s * z
    val sMwx = s * wx
    val sMwy = s * wy
    val sMwz = s * wz
    val sMxy = s * xy
    val sMxz = s * xz
    val sMyz = s * yz
    val sMwxy = s * wxy
    val sMwxz = s * wxz
    val sMwyz = s * wyz
    val sMxyz = s * xyz
    val sMi = i * s
    val wMx = w * x
    val wMy = w * y
    val wMz = w * z
    val wMxy = w * xy
    val wMxz = w * xz
    val wMyz = w * yz
    val wMxyz = w * xyz
    val xMx = x * x
    val xMy = x * y
    val xMz = x * z
    val xMwx = wx * x
    val xMwy = wy * x
    val xMwz = wz * x
    val xMxy = x * xy
    val xMxz = x * xz
    val xMyz = x * yz
    val xMwxy = wxy * x
    val xMwxz = wxz * x
    val xMwyz = wyz * x
    val xMxyz = x * xyz
    val xMi = i * x
    val yMy = y * y
    val yMz = y * z
    val yMwx = wx * y
    val yMwy = wy * y
    val yMwz = wz * y
    val yMxy = xy * y
    val yMxz = xz * y
    val yMyz = y * yz
    val yMwxy = wxy * y
    val yMwxz = wxz * y
    val yMwyz = wyz * y
    val yMxyz = xyz * y
    val yMi = i * y
    val zMz = z * z
    val zMwx = wx * z
    val zMwy = wy * z
    val zMwz = wz * z
    val zMxy = xy * z
    val zMxz = xz * z
    val zMyz = yz * z
    val zMwxy = wxy * z
    val zMwxz = wxz * z
    val zMwyz = wyz * z
    val zMxyz = xyz * z
    val zMi = i * z
    val wxMxy = wx * xy
    val wxMxz = wx * xz
    val wxMyz = wx * yz
    val wxMxyz = wx * xyz
    val wyMxy = wy * xy
    val wyMxz = wy * xz
    val wyMyz = wy * yz
    val wyMxyz = wy * xyz
    val wzMxy = wz * xy
    val wzMxz = wz * xz
    val wzMyz = wz * yz
    val wzMxyz = wz * xyz
    val xyMxy = xy * xy
    val xyMxz = xy * xz
    val xyMyz = xy * yz
    val xyMwxy = wxy * xy
    val xyMwxz = wxz * xy
    val xyMwyz = wyz * xy
    val xyMxyz = xy * xyz
    val xyMi = i * xy
    val xzMxz = xz * xz
    val xzMyz = xz * yz
    val xzMwxy = wxy * xz
    val xzMwxz = wxz * xz
    val xzMwyz = wyz * xz
    val xzMxyz = xyz * xz
    val xzMi = i * xz
    val yzMyz = yz * yz
    val yzMwxy = wxy * yz
    val yzMwxz = wxz * yz
    val yzMwyz = wyz * yz
    val yzMxyz = xyz * yz
    val yzMi = i * yz
    val wxyMxyz = wxy * xyz
    val wxzMxyz = wxz * xyz
    val wyzMxyz = wyz * xyz
    val xyzMxyz = xyz * xyz
    val xyzMi = i * xyz
    Pga3dMultivector(
      s = (2.0 * (v.x * (sMx + yMxy + yzMxyz + zMxz) + v.y * (sMy + zMyz - xMxy - xzMxyz) + v.z * (sMz + xyMxyz - xMxz - yMyz)) + v.s * (sMs + xMx + xyMxy + xyzMxyz + xzMxz + yMy + yzMyz + zMz)),
      w = (2.0 * (v.i * (xMyz + zMxy - sMxyz - yMxz) + v.s * (sMw + xyMwxy + xzMwxz + yzMwyz - xMwx - xyzMi - yMwy - zMwz) + v.x * (wMx + wyzMxyz + yMwxy + zMwxz - sMwx - wyMxy - wzMxz - yzMi) + v.y * (wMy + wxMxy + xzMi + zMwyz - sMwy - wxzMxyz - wzMyz - xMwxy) + v.z * (wMz + wxMxz + wxyMxyz + wyMyz - sMwz - xMwxz - xyMi - yMwyz)) + v.w * (sMs + xyMxy + xzMxz + yzMyz - xMx - xyzMxyz - yMy - zMz)),
      x = (2.0 * (v.s * (sMx + yzMxyz - yMxy - zMxz) + v.y * (xMy + zMxyz - sMxy - xzMyz) + v.z * (xMz + xyMyz - sMxz - yMxyz)) + v.x * (sMs + xMx + xyzMxyz + yzMyz - xyMxy - xzMxz - yMy - zMz)),
      y = (2.0 * (v.s * (sMy + xMxy - xzMxyz - zMyz) + v.x * (sMxy + xMy - xzMyz - zMxyz) + v.z * (xMxyz + yMz - sMyz - xyMxz)) + v.y * (sMs + xyzMxyz + xzMxz + yMy - xMx - xyMxy - yzMyz - zMz)),
      z = (2.0 * (v.s * (sMz + xMxz + xyMxyz + yMyz) + v.x * (sMxz + xMz + xyMyz + yMxyz) + v.y * (sMyz + yMz - xMxyz - xyMxz)) + v.z * (sMs + xyMxy + xyzMxyz + zMz - xMx - xzMxz - yMy - yzMyz)),
      wx = (2.0 * (v.wxy * (sMy + xMxy + xzMxyz + zMyz) + v.wxz * (sMz + xMxz - xyMxyz - yMyz) + v.wy * (-sMxy - xMy - xzMyz - zMxyz) + v.wyz * (xMyz + yMxz - sMxyz - zMxy) + v.wz * (xyMyz + yMxyz - sMxz - xMz) + v.xy * (sMwy + wMy + wxMxy + wxzMxyz + wzMyz + xMwxy + xzMi + zMwyz) + v.xyz * (sMwyz + wxMxyz + xyMwxz + zMwy - wMyz - xMi - xzMwxy - yMwz) + v.xz * (sMwz + wMz + wxMxz + xMwxz - wxyMxyz - wyMyz - xyMi - yMwyz) + v.yz * (wxMyz + wyMxz + xMwyz + yMwxz - sMi - wMxyz - wzMxy - zMwxy)) + v.wx * (sMs + yMy + yzMyz + zMz - xMx - xyMxy - xyzMxyz - xzMxz)),
      wy = (2.0 * (v.wx * (sMxy + zMxyz - xMy - xzMyz) + v.wxy * (yMxy + yzMxyz - sMx - zMxz) + v.wxz * (sMxyz + xMyz + yMxz + zMxy) + v.wyz * (sMz + yMyz - xMxz - xyMxyz) + v.wz * (-sMyz - xMxyz - xyMxz - yMz) + v.xy * (wyMxy + wyzMxyz + yMwxy + yzMi - sMwx - wMx - wzMxz - zMwxz) + v.xyz * (wMxz + wyMxyz + xMwz + xyMwyz - sMwxz - yMi - yzMwxy - zMwx) + v.xz * (sMi + wMxyz + wxMyz + wyMxz + wzMxy + xMwyz + yMwxz + zMwxy) + v.yz * (sMwz + wMz + wyMyz + yMwyz - wxMxz - wxyMxyz - xMwxz - xyMi)) + v.wy * (sMs + xMx + xzMxz + zMz - xyMxy - xyzMxyz - yMy - yzMyz)),
      wz = (2.0 * (v.wx * (sMxz + xyMyz - xMz - yMxyz) + v.wxy * (yMxz + zMxy - sMxyz - xMyz) + v.wxz * (yzMxyz + zMxz - sMx - yMxy) + v.wy * (sMyz + xMxyz - xyMxz - yMz) + v.wyz * (xMxy + zMyz - sMy - xzMxyz) + v.xy * (wyMxz + wzMxy + yMwxz + zMwxy - sMi - wMxyz - wxMyz - xMwyz) + v.xyz * (sMwxy + wzMxyz + xzMwyz + yMwx - wMxy - xMwy - yzMwxz - zMi) + v.xz * (wyzMxyz + wzMxz + yzMi + zMwxz - sMwx - wMx - wyMxy - yMwxy) + v.yz * (wxMxy + wzMyz + xMwxy + zMwyz - sMwy - wMy - wxzMxyz - xzMi)) + v.wz * (sMs + xMx + xyMxy + yMy - xyzMxyz - xzMxz - yzMyz - zMz)),
      xy = (2.0 * (v.xyz * (sMz + xMxz + xyMxyz + yMyz) + v.xz * (xMxyz + xyMxz - sMyz - yMz) + v.yz * (sMxz + xMz + xyMyz + yMxyz)) + v.xy * (sMs + xyMxy + xyzMxyz + zMz - xMx - xzMxz - yMy - yzMyz)),
      xz = (2.0 * (v.xy * (sMyz + xyMxz - xMxyz - yMz) + v.xyz * (xzMxyz + zMyz - sMy - xMxy) + v.yz * (xzMyz + zMxyz - sMxy - xMy)) + v.xz * (sMs + xyzMxyz + xzMxz + yMy - xMx - xyMxy - yzMyz - zMz)),
      yz = (2.0 * (v.xy * (xMz + xyMyz - sMxz - yMxyz) + v.xyz * (sMx + yzMxyz - yMxy - zMxz) + v.xz * (sMxy + xzMyz - xMy - zMxyz)) + v.yz * (sMs + xMx + xyzMxyz + yzMyz - xyMxy - xzMxz - yMy - zMz)),
      wxy = (2.0 * (v.wx * (sMy + xzMxyz - xMxy - zMyz) + v.wxz * (xyMxz + yMz - sMyz - xMxyz) + v.wy * (yzMxyz + zMxz - sMx - yMxy) + v.wyz * (sMxz + xyMyz - xMz - yMxyz) + v.wz * (sMxyz + xMyz - yMxz - zMxy) + v.xy * (sMw + xMwx + xyMwxy + yMwy - xyzMi - xzMwxz - yzMwyz - zMwz) + v.xyz * (wMz + wxyMxyz + xMwxz + yMwyz - sMwz - wxMxz - wyMyz - xyMi) + v.xz * (xyMwxz + xzMwxy + yMwz + zMwy - sMwyz - wMyz - wxMxyz - xMi) + v.yz * (sMwxz + wMxz + xyMwyz + yzMwxy - wyMxyz - xMwz - yMi - zMwx)) + v.wxy * (sMs + xMx + xyMxy + yMy - xyzMxyz - xzMxz - yzMyz - zMz)),
      wxz = (2.0 * (v.wx * (sMz + yMyz - xMxz - xyMxyz) + v.wxy * (sMyz + xMxyz + xyMxz + yMz) + v.wy * (-sMxyz - xMyz - yMxz - zMxy) + v.wyz * (xMy + xzMyz - sMxy - zMxyz) + v.wz * (yMxy + yzMxyz - sMx - zMxz) + v.xy * (sMwyz + wMyz + wxMxyz + xMi + xyMwxz + xzMwxy + yMwz + zMwy) + v.xyz * (sMwy + wxMxy + wxzMxyz + zMwyz - wMy - wzMyz - xMwxy - xzMi) + v.xz * (sMw + xMwx + xzMwxz + zMwz - xyMwxy - xyzMi - yMwy - yzMwyz) + v.yz * (xMwy + xzMwyz + yMwx + yzMwxz - sMwxy - wMxy - wzMxyz - zMi)) + v.wxz * (sMs + xMx + xzMxz + zMz - xyMxy - xyzMxyz - yMy - yzMyz)),
      wyz = (2.0 * (v.wx * (sMxyz + zMxy - xMyz - yMxz) + v.wxy * (xyMyz + yMxyz - sMxz - xMz) + v.wxz * (sMxy + xMy + xzMyz + zMxyz) + v.wy * (sMz + xMxz - xyMxyz - yMyz) + v.wz * (-sMy - xMxy - xzMxyz - zMyz) + v.xy * (wyMxyz + xyMwyz + yMi + yzMwxy - sMwxz - wMxz - xMwz - zMwx) + v.xyz * (wMx + wyMxy + wyzMxyz + wzMxz - sMwx - yMwxy - yzMi - zMwxz) + v.xz * (sMwxy + wMxy + wzMxyz + xMwy + xzMwyz + yMwx + yzMwxz + zMi) + v.yz * (sMw + yMwy + yzMwyz + zMwz - xMwx - xyMwxy - xyzMi - xzMwxz)) + v.wyz * (sMs + yMy + yzMyz + zMz - xMx - xyMxy - xyzMxyz - xzMxz)),
      xyz = (2.0 * (v.xy * (sMz + xyMxyz - xMxz - yMyz) + v.xz * (xMxy + xzMxyz - sMy - zMyz) + v.yz * (sMx + yMxy + yzMxyz + zMxz)) + v.xyz * (sMs + xMx + xyMxy + xyzMxyz + xzMxz + yMy + yzMyz + zMz)),
      i = (2.0 * (v.s * (sMi + wMxyz + wyMxz + yMwxz - wxMyz - wzMxy - xMwyz - zMwxy) + v.w * (sMxyz + yMxz - xMyz - zMxy) + v.x * (wMyz + xMi + xyMwxz + zMwy - sMwyz - wxMxyz - xzMwxy - yMwz) + v.y * (sMwxz + xMwz + xyMwyz + yMi - wMxz - wyMxyz - yzMwxy - zMwx) + v.z * (wMxy + xzMwyz + yMwx + zMi - sMwxy - wzMxyz - xMwy - yzMwxz)) + v.i * (sMs + xyMxy + xzMxz + yzMyz - xMx - xyzMxyz - yMy - zMz)),
    )

  infix def cross(v: Pga3dMultivector): Pga3dMultivector =
    Pga3dMultivector(
      s = 0.0,
      w = (v.i * xyz + v.x * wx + v.y * wy + v.z * wz - i * v.xyz - v.wx * x - v.wy * y - v.wz * z),
      x = (v.y * xy + v.z * xz - v.xy * y - v.xz * z),
      y = (v.xy * x + v.z * yz - v.x * xy - v.yz * z),
      z = (v.xz * x + v.yz * y - v.x * xz - v.y * yz),
      wx = (v.wy * xy + v.wyz * xyz + v.wz * xz + v.x * w - v.w * x - v.xy * wy - v.xyz * wyz - v.xz * wz),
      wy = (v.wz * yz + v.xy * wx + v.xyz * wxz + v.y * w - v.w * y - v.wx * xy - v.wxz * xyz - v.yz * wz),
      wz = (v.wxy * xyz + v.xz * wx + v.yz * wy + v.z * w - v.w * z - v.wx * xz - v.wy * yz - v.xyz * wxy),
      xy = (v.xz * yz + v.y * x - v.x * y - v.yz * xz),
      xz = (v.yz * xy + v.z * x - v.x * z - v.xy * yz),
      yz = (v.xy * xz + v.z * y - v.xz * xy - v.y * z),
      wxy = (i * v.z + v.wxz * yz + v.xyz * wz + v.xz * wyz - v.i * z - v.wyz * xz - v.wz * xyz - v.yz * wxz),
      wxz = (v.i * y + v.wy * xyz + v.wyz * xy + v.yz * wxy - i * v.y - v.wxy * yz - v.xy * wyz - v.xyz * wy),
      wyz = (i * v.x + v.wxy * xz + v.xy * wxz + v.xyz * wx - v.i * x - v.wx * xyz - v.wxz * xy - v.xz * wxy),
      xyz = 0.0,
      i = (v.wxz * y + v.x * wyz + v.xyz * w + v.z * wxy - v.w * xyz - v.wxy * z - v.wyz * x - v.y * wxz),
    )


object Pga3dMultivector:

  inline val componentsCount = 16

  val zero: Pga3dMultivector = Pga3dMultivector(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)