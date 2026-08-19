package me.kright.gametools.physics1d

/**
 * Rate-independent rubber bushing hysteresis, the friction part of the Berg bushing model
 * (M. Berg, "A non-linear rubber spring model for vehicle dynamics analysis", 1997-1998).
 *
 * Compared to [[DahlFriction]] the force approaches saturation algebraically
 * (a hyperbola) instead of exponentially — a longer tail that fits measured rubber
 * friction loops better. In our sign convention (the force resists motion), the branch
 * growing from the last reversal point (xs, Fs) is:
 *
 *   u = |x - xs|,  target = -maxForce * sign(x - xs)
 *   F(x) = Fs + u / (c + u) * (target - Fs),  c = halfSaturationTravel * |target - Fs| / maxForce
 *
 * halfSaturationTravel is Berg's x2: starting from zero force, the force reaches
 * maxForce / 2 after exactly that travel. The initial slope of any branch is
 * maxForce / halfSaturationTravel regardless of where the reversal happened
 * (the alpha-dependent denominator of the original paper folds into c above).
 *
 * Unlike the Dahl exponent, this curve is NOT invariant under re-anchoring in the middle
 * of a branch, so the state keeps the reversal anchor explicitly: while the motion
 * direction is unchanged the branch keeps growing from the same anchor (so substeps of
 * one direction compose exactly and the model stays rate-independent), and a direction
 * change re-anchors the branch at the last committed point.
 *
 * When the stages read the force frozen over the step (forceAt at the step-start
 * coordinate), it MUST be paired with a viscous term on the same coordinate, exactly as
 * for [[DahlFriction]]: the splitting leaves the internal stiffness
 * maxForce / halfSaturationTravel undamped and ratchets under a constant load. Reading
 * [[forceAt]] at the stage coordinate removes the force lag.
 */
final class BergFriction private(val maxForce: Double,
                                 val halfSaturationTravel: Double,
                                 private var anchorX: Double,
                                 private var anchorForce: Double,
                                 private var lastX: Double,
                                 private var lastForce: Double,
                                 private var direction: Double) extends HystereticFriction:
  require(maxForce >= 0.0 && halfSaturationTravel > 0.0)

  def this(maxForce: Double, halfSaturationTravel: Double) =
    this(maxForce, halfSaturationTravel,
      anchorX = Double.NaN, anchorForce = 0.0, lastX = Double.NaN, lastForce = 0.0, direction = 0.0)

  private def branch(xs: Double, fs: Double, x: Double): Double =
    val u = Math.abs(x - xs)
    if (u == 0.0 || maxForce == 0.0) return fs
    val target = -maxForce * Math.signum(x - xs)
    val c = halfSaturationTravel * Math.abs(target - fs) / maxForce
    fs + u / (c + u) * (target - fs)

  override def forceAt(x: Double): Double =
    if (lastX.isNaN) return lastForce
    val dx = x - lastX
    if (dx == 0.0) return lastForce

    if (Math.signum(dx) == direction) branch(anchorX, anchorForce, x)
    else branch(lastX, lastForce, x) // a (trial) reversal starts a new branch from the committed point

  override def advance(x: Double): Unit =
    if (lastX.isNaN) {
      anchorX = x
      anchorForce = lastForce
      lastX = x
      return
    }
    val dx = x - lastX
    if (dx == 0.0) return

    val dir = Math.signum(dx)
    if (dir != direction) {
      anchorX = lastX
      anchorForce = lastForce
      direction = dir
    }
    lastForce = branch(anchorX, anchorForce, x)
    lastX = x

  /** any branch starts with this slope, so the stuck state acts as a spring of it */
  override def tangentStiffness: Double =
    maxForce / halfSaturationTravel

  override def deepCopy(): BergFriction =
    new BergFriction(maxForce, halfSaturationTravel, anchorX, anchorForce, lastX, lastForce, direction)
