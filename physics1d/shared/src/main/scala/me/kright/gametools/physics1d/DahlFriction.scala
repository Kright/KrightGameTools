package me.kright.gametools.physics1d

/**
 * Rate-independent hysteresis (the Dahl model), e.g. rubber bushing friction along a rod.
 *
 * The force relaxes exponentially towards the saturation resisting the current direction:
 *
 *   F(x) = F_target + (F0 - F_target) * exp(-|x - x0| / saturationTravel),
 *   F_target = -maxForce * sign(x - x0)
 *
 * where (x0, F0) is the state committed by the last [[advance]]. This is the exact
 * solution of the Dahl equation for a monotone move: the raw ODE
 * dF/dt = -sigma * (v + |v| * F / maxForce) is stiff while sliding
 * (lambda = |v| / saturationTravel) and would blow up inside an explicit integrator,
 * while the exponential form is unconditionally stable and rate-independent by
 * construction: substeps of the same direction compose exactly as one big step.
 * With saturationTravel far below the step travel the model degrades to bang-bang
 * +-maxForce instead of exploding.
 *
 * Sign convention matches [[HystereticFriction]]: after the coordinate grew the force
 * tends to -maxForce and acts to decrease it back.
 *
 * When the stages read the force frozen over the step (forceAt at the step-start
 * coordinate), it MUST be paired with a viscous term on the same coordinate: that
 * splitting leaves the internal stiffness maxForce / saturationTravel undamped, and its
 * limit cycle under a constant load ratchets the joint instead of holding it — a light
 * viscous damping (zeta ~ 0.05 of that mode) settles it. Reading [[forceAt]] at the
 * stage coordinate removes the force lag, so the viscous pairing becomes a physical
 * choice rather than a scheme requirement.
 */
final class DahlFriction private(val maxForce: Double,
                                 val saturationTravel: Double,
                                 private var currentForce: Double,
                                 private var lastX: Double) extends HystereticFriction:
  require(maxForce >= 0.0 && saturationTravel > 0.0)

  def this(maxForce: Double, saturationTravel: Double) =
    this(maxForce, saturationTravel, currentForce = 0.0, lastX = Double.NaN)

  override def forceAt(x: Double): Double =
    if (lastX.isNaN) currentForce
    else DahlFriction.updatedForce(currentForce, x - lastX, maxForce, saturationTravel)

  override def advance(x: Double): Unit =
    currentForce = forceAt(x)
    lastX = x

  /** the stuck hysteresis acts as an extra spring of this stiffness */
  override def tangentStiffness: Double =
    maxForce / saturationTravel

  override def deepCopy(): DahlFriction =
    new DahlFriction(maxForce, saturationTravel, currentForce, lastX)


object DahlFriction:
  /** the exact Dahl step for a constant-direction move of dx; composing substeps of the
   * same sign is exactly equivalent to one big step, which makes the model rate-independent */
  def updatedForce(force: Double, dx: Double, maxForce: Double, saturationTravel: Double): Double =
    if (dx == 0.0) force
    else
      val target = -maxForce * Math.signum(dx)
      target + (force - target) * Math.exp(-Math.abs(dx) / saturationTravel)
