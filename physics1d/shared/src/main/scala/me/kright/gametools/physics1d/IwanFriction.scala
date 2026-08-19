package me.kright.gametools.physics1d

import me.kright.gametools.mathutil.MathUtil.clamp

/**
 * One Jenkins element of the Iwan model: a spring of the given stiffness in series with
 * a Coulomb slider that starts slipping at breakForce. Stuck, it acts as the spring;
 * slipping, it transmits exactly +-breakForce.
 */
final case class IwanElement(stiffness: Double, breakForce: Double):
  require(stiffness > 0.0 && breakForce >= 0.0)

  /** the deformation at which the slider starts to slip */
  def breakDeformation: Double = breakForce / stiffness

  /** the energy this element dissipates per steady cycle of the given amplitude
   * (a displacement cycle between -amplitude and +amplitude); zero while it never slips */
  def dissipationPerCycle(amplitude: Double): Double =
    4.0 * breakForce * Math.max(0.0, amplitude - breakDeformation)


/**
 * Rate-independent hysteresis as N parallel Jenkins elements (the discrete Iwan model,
 * also known as Prandtl-Ishlinskii): each element is a spring in series with a Coulomb
 * slider, the total force is the sum over the elements.
 *
 * The state is one deformation force per element, f_i in [-breakForce_i, +breakForce_i];
 * a monotone move dx updates each element independently and exactly:
 *
 *   f_i <- clamp(f_i + stiffness_i * dx, +-breakForce_i),   force = -sum(f_i)
 *
 * Clamps of same-direction moves compose exactly (clamp(clamp(f + a) + b) = clamp(f + a + b)
 * for a, b of one sign), so the model is rate-independent and, like the Dahl exponent,
 * needs no reversal anchor: the element forces are a complete state.
 *
 * Shape (all piecewise-linear, the classic Masing family):
 *  - the virgin curve from the zero state: force(x) = -sum(min(stiffness_i * x, breakForce_i));
 *  - a reversal branch retraces the virgin curve doubled: dForce(u) = 2 * virgin(u / 2)
 *    (the Masing rule), so fitted loops close exactly;
 *  - the steady cycle of amplitude A dissipates sum(4 * breakForce_i * (A - breakDeformation_i))
 *    over the slipping elements — spreading breakDeformation_i across the amplitude range
 *    of interest keeps the loss factor roughly constant there, which is what measured
 *    rubber bushings show and what a single Dahl/Berg element cannot do.
 *
 * Fitting guidance (a car suspension bushing): 2-3 elements are enough, e.g. a "micro"
 * element (stiff, small breakForce: ~4e6 N/m, ~150 N) plus a "macro" element
 * (~1e6 N/m, ~250 N); fit against measured loops at 2-3 amplitudes.
 *
 * Sign convention matches [[HystereticFriction]]: after the coordinate grew the force is
 * negative and resists further growth. The frozen-force splitting (forceAt at the
 * step-start coordinate) MUST be paired with a viscous term, exactly as for
 * [[DahlFriction]]; reading [[forceAt]] at the stage coordinate removes the lag.
 */
final class IwanFriction private(val elements: IndexedSeq[IwanElement],
                                 private val elementForces: Array[Double],
                                 private var lastX: Double) extends HystereticFriction:
  require(elements.nonEmpty)

  def this(elements: Seq[IwanElement]) =
    this(elements.toIndexedSeq, new Array[Double](elements.size), Double.NaN)

  /** the saturation force: the bound of |force| */
  val maxForce: Double =
    elements.map(_.breakForce).sum

  /** the force of the element i after a monotone move dx from the committed coordinate */
  private def elementForceAfter(i: Int, dx: Double): Double =
    val e = elements(i)
    (elementForces(i) + e.stiffness * dx).clamp(-e.breakForce, e.breakForce)

  override def forceAt(x: Double): Double =
    val dx = if (lastX.isNaN) 0.0 else x - lastX
    var total = 0.0
    for (i <- elements.indices)
      total += elementForceAfter(i, dx)
    -total

  override def advance(x: Double): Unit =
    if (!lastX.isNaN) {
      val dx = x - lastX
      if (dx != 0.0) {
        for (i <- elements.indices)
          elementForces(i) = elementForceAfter(i, dx)
      }
    }
    lastX = x

  /** all sliders stuck: the sum of the element stiffnesses (the instantaneous tangent
   * stiffness is the sum over the not-yet-slipping elements, so this is its upper bound) */
  override val tangentStiffness: Double =
    elements.map(_.stiffness).sum

  override def deepCopy(): IwanFriction =
    new IwanFriction(elements, elementForces.clone(), lastX)


object IwanFriction:
  def apply(elements: IwanElement*): IwanFriction =
    new IwanFriction(elements)
