package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.Pga3dVector

/**
 * The 3d counterpart of [[me.kright.gametools.physics1d.DahlFriction]]: the force relaxes
 * exponentially towards the saturation resisting the current move direction u,
 *
 *   F(s) = target + (F0 - target) * exp(-s / saturationTravel),   target = -maxForce * u,
 *
 * the exact solution of the vector Dahl equation dF/ds = -sigma * (u + F / maxForce) for a
 * straight move of length s (sigma = maxForce / saturationTravel). Substeps of one straight
 * move compose exactly, the ball |F| <= maxForce is invariant, the step is rotation-invariant
 * and reduces to the 1d model on a collinear path, reversals included.
 *
 * Passive: with F = sigma * (x - z) the slider z creeps along the force at the rate
 * |F| / maxForce of the point speed, so F.v = d/dt(|F|^2 / 2 sigma) + |v| |F|^2 / maxForce,
 * stored energy plus a non-negative dissipation. That soft slider is also the one difference
 * from [[Pga3dIwanFriction]] on a curved path: on a steady circle of radius A the force settles
 * at maxForce / sqrt(1 + (saturationTravel / A)^2), below saturation, lagging the motion by
 * atan(saturationTravel / A); an Iwan element stays on its sphere. Negligible while the path
 * curvature radius is far above saturationTravel.
 *
 * Integrator contract and the chord caveat: see [[Pga3dHystereticFriction]].
 */
final class Pga3dDahlFriction private(val maxForce: Double,
                                      val saturationTravel: Double,
                                      private var forceX: Double,
                                      private var forceY: Double,
                                      private var forceZ: Double,
                                      private var lastX: Double,
                                      private var lastY: Double,
                                      private var lastZ: Double) extends Pga3dHystereticFriction:
  require(maxForce >= 0.0 && saturationTravel > 0.0)

  def this(maxForce: Double, saturationTravel: Double) =
    this(maxForce, saturationTravel, 0.0, 0.0, 0.0, Double.NaN, Double.NaN, Double.NaN)

  override def forceAt(x: Double, y: Double, z: Double): Pga3dVector =
    if (lastX.isNaN) Pga3dVector(forceX, forceY, forceZ)
    else Pga3dDahlFriction.updatedForce(forceX, forceY, forceZ, x - lastX, y - lastY, z - lastZ, maxForce, saturationTravel)

  override def advance(x: Double, y: Double, z: Double): Unit =
    val force = forceAt(x, y, z)
    forceX = force.x
    forceY = force.y
    forceZ = force.z
    lastX = x
    lastY = y
    lastZ = z

  /** the stuck hysteresis acts as an extra spring of this stiffness */
  override def tangentStiffness: Double =
    maxForce / saturationTravel

  override def deepCopy(): Pga3dDahlFriction =
    new Pga3dDahlFriction(maxForce, saturationTravel, forceX, forceY, forceZ, lastX, lastY, lastZ)


object Pga3dDahlFriction:
  /** the exact Dahl step for a straight move dx; composing substeps of the same straight move
   * is exactly equivalent to one big step */
  def updatedForce(force: Pga3dVector, dx: Pga3dVector, maxForce: Double, saturationTravel: Double): Pga3dVector =
    updatedForce(force.x, force.y, force.z, dx.x, dx.y, dx.z, maxForce, saturationTravel)

  def updatedForce(fx: Double, fy: Double, fz: Double,
                   dx: Double, dy: Double, dz: Double,
                   maxForce: Double, saturationTravel: Double): Pga3dVector =
    // the move direction and length scaled by the largest component: no overflow or underflow of
    // the squares anywhere in the double range (dx ~ 1e-160 or 1e200 squared leaves it). An infinite
    // component (the difference of two finite points overflowed) gives the direction alone, Inf / Inf would be a NaN
    val scale = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)))
    if (scale == 0.0) return Pga3dVector(fx, fy, fz)
    val sx = if (scale.isInfinite) (if (dx.isInfinite) Math.signum(dx) else 0.0) else dx / scale
    val sy = if (scale.isInfinite) (if (dy.isInfinite) Math.signum(dy) else 0.0) else dy / scale
    val sz = if (scale.isInfinite) (if (dz.isInfinite) Math.signum(dz) else 0.0) else dz / scale
    val scaledTravel = Math.sqrt(sx * sx + sy * sy + sz * sz) // in [1, sqrt(3)]
    val travel = scale * scaledTravel
    val targetScale = -maxForce / scaledTravel // target = -maxForce * u
    val targetX = sx * targetScale
    val targetY = sy * targetScale
    val targetZ = sz * targetScale
    // force + (target - force) * (1 - exp(-travel / saturationTravel)); expm1 keeps the step exact for tiny
    // moves (1 - exp(-1e-10) loses half the digits, and a 1e-100 move must not shake the force by an ulp)
    val approach = -Math.expm1(-travel / saturationTravel)
    Pga3dVector(
      fx + (targetX - fx) * approach,
      fy + (targetY - fy) * approach,
      fz + (targetZ - fz) * approach)
