package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.Pga3dBivector

import scala.annotation.targetName

class Pga3dBivectorMutable:
  private var wx: Double = 0.0
  private var wy: Double = 0.0
  private var wz: Double = 0.0
  private var xy: Double = 0.0
  private var xz: Double = 0.0
  private var yz: Double = 0.0

  def setZero(): Unit =
    wx = 0.0
    wy = 0.0
    wz = 0.0
    xy = 0.0
    xz = 0.0
    yz = 0.0

  @targetName("set")
  def :=(b: Pga3dBivector): Unit =
    wx = b.wx
    wy = b.wy
    wz = b.wz
    xy = b.xy
    xz = b.xz
    yz = b.yz

  @targetName("set")
  def :=(b: Pga3dBivectorMutable): Unit =
    wx = b.wx
    wy = b.wy
    wz = b.wz
    xy = b.xy
    xz = b.xz
    yz = b.yz

  @targetName("plusAssign")
  def +=(b: Pga3dBivector): Unit =
    wx += b.wx
    wy += b.wy
    wz += b.wz
    xy += b.xy
    xz += b.xz
    yz += b.yz

  @targetName("minusAssign")
  def -=(b: Pga3dBivector): Unit =
    wx -= b.wx
    wy -= b.wy
    wz -= b.wz
    xy -= b.xy
    xz -= b.xz
    yz -= b.yz

  def toBivector: Pga3dBivector =
    Pga3dBivector(wx, wy, wz, xy, xz, yz)
