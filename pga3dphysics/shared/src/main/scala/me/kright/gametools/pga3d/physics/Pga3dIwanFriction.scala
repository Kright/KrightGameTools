package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.pga3d.Pga3dVector
import me.kright.gametools.physics1d.IwanElement

/**
 * The 3d counterpart of [[me.kright.gametools.physics1d.IwanFriction]]: a point held in a volume
 * by N parallel Jenkins elements (the same [[IwanElement]] parameters), e.g. a rubber bushing
 * around the attachment point of a rod. The coordinate is a point in any frame the caller
 * chooses, the force resists the realized motion: force = -sum(f_i).
 *
 * The state is one force vector per element inside the ball |f_i| <= breakForce_i. A straight
 * move dx updates it exactly: f_i += stiffness_i * dx while inside the ball, then the force stays
 * on the sphere and turns towards the move direction along the great circle,
 * tan(theta/2) = tan(theta0/2) * exp(-travel / breakDeformation_i). This is the closed form of
 * the continuous element, so substeps of one straight move compose exactly (a plain "add and
 * project back on the sphere" would not), the step is rotation-invariant and reduces to the
 * 1d clamp on a collinear path.
 *
 * Integrator contract and the chord caveat: see [[Pga3dHystereticFriction]]. Only the slipping
 * elements notice the chord: second order in the step while the step travel is below the break
 * deformations, first order far beyond them. RK4 keeps its 4th order on a straight monotone
 * path, loses it on a reversal inside a step (as in 1d) and on a curved slipping path.
 *
 * Storage: a flat Array[Double], three per element.
 */
final class Pga3dIwanFriction private(val elements: IArray[IwanElement],
                                      private val elementForces: Array[Double], // x, y, z of each element force
                                      private var lastX: Double,
                                      private var lastY: Double,
                                      private var lastZ: Double) extends Pga3dHystereticFriction:
  require(elements.nonEmpty)

  def this(elements: Seq[IwanElement]) =
    this(IArray.from(elements), new Array[Double](3 * elements.size), Double.NaN, Double.NaN, Double.NaN)

  /** the saturation force: the bound of |force|, reached after a long enough straight move */
  override val maxForce: Double =
    elements.map(_.breakForce).sum

  /** writes the force of the element i after a straight move (dx, dy, dz) from the committed
   * point into out(offset), out(offset + 1), out(offset + 2) */
  private def moveElement(i: Int, dx: Double, dy: Double, dz: Double, out: Array[Double], offset: Int): Unit =
    val at = 3 * i
    Pga3dIwanFriction.moveForce(elementForces(at), elementForces(at + 1), elementForces(at + 2),
      dx, dy, dz, elements(i), out, offset)

  override def forceAt(x: Double, y: Double, z: Double): Pga3dVector =
    val latched = !lastX.isNaN
    val dx = if (latched) x - lastX else 0.0
    val dy = if (latched) y - lastY else 0.0
    val dz = if (latched) z - lastZ else 0.0

    val moved = new Array[Double](3)
    var totalX = 0.0
    var totalY = 0.0
    var totalZ = 0.0
    for (i <- FastRange(elements.length)) {
      moveElement(i, dx, dy, dz, moved, 0)
      totalX += moved(0)
      totalY += moved(1)
      totalZ += moved(2)
    }
    Pga3dVector(-totalX, -totalY, -totalZ)

  override def advance(x: Double, y: Double, z: Double): Unit =
    if (!lastX.isNaN) {
      val dx = x - lastX
      val dy = y - lastY
      val dz = z - lastZ
      if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
        for (i <- FastRange(elements.length))
          moveElement(i, dx, dy, dz, elementForces, 3 * i)
      }
    }
    lastX = x
    lastY = y
    lastZ = z

  /** all sliders stuck: the sum of the element stiffnesses, the same in every direction
   * (the instantaneous tangent stiffness is the sum over the not-yet-slipping elements,
   * so this is its upper bound) */
  override val tangentStiffness: Double =
    elements.map(_.stiffness).sum

  override def deepCopy(): Pga3dIwanFriction =
    new Pga3dIwanFriction(elements, elementForces.clone(), lastX, lastY, lastZ)


object Pga3dIwanFriction:
  def apply(elements: IwanElement*): Pga3dIwanFriction =
    new Pga3dIwanFriction(elements)

  /**
   * the exact step of one Jenkins element for a straight move dx: the force moves linearly
   * until it reaches the sphere |f| = breakForce, then turns towards the move direction along
   * the great circle with tan(theta/2) decaying as exp(-travel / breakDeformation).
   * Substeps of one straight move compose exactly into the whole move.
   */
  def movedForce(force: Pga3dVector, dx: Pga3dVector, element: IwanElement): Pga3dVector =
    val out = new Array[Double](3)
    moveForce(force.x, force.y, force.z, dx.x, dx.y, dx.z, element, out, 0)
    Pga3dVector(out(0), out(1), out(2))

  /** [[movedForce]] on plain doubles: the result goes to out(offset), out(offset + 1), out(offset + 2);
   * out may hold the input force itself (the inputs are read before anything is written) */
  def moveForce(fx: Double, fy: Double, fz: Double,
                dx: Double, dy: Double, dz: Double,
                element: IwanElement, out: Array[Double], offset: Int): Unit =
    val k = element.stiffness
    val r = element.breakForce
    if (r == 0.0) {
      write(out, offset, 0.0, 0.0, 0.0)
      return
    }
    val scale = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)))
    if (scale == 0.0) {
      write(out, offset, fx, fy, fz)
      return
    }

    val movedX = fx + k * dx
    val movedY = fy + k * dy
    val movedZ = fz + k * dz
    // everything below is in units of the break force: the squares of forces up to 1e300 would overflow
    val movedRX = movedX / r
    val movedRY = movedY / r
    val movedRZ = movedZ / r
    if (movedRX * movedRX + movedRY * movedRY + movedRZ * movedRZ <= 1.0) {
      write(out, offset, movedX, movedY, movedZ) // stuck during the whole move
      return
    }

    // the move direction and length scaled by the largest component: no overflow or underflow of
    // the squares anywhere in the double range, and u stays a unit vector even for subnormal dx.
    // An infinite component (the difference of two finite points overflowed) gives the direction
    // alone, Inf / Inf would be a NaN
    val sx = if (scale.isInfinite) (if (dx.isInfinite) Math.signum(dx) else 0.0) else dx / scale
    val sy = if (scale.isInfinite) (if (dy.isInfinite) Math.signum(dy) else 0.0) else dy / scale
    val sz = if (scale.isInfinite) (if (dz.isInfinite) Math.signum(dz) else 0.0) else dz / scale
    val scaledTravel = Math.sqrt(sx * sx + sy * sy + sz * sz) // in [1, sqrt(3)]
    val travel = scale * scaledTravel
    val ux = sx / scaledTravel
    val uy = sy / scaledTravel
    val uz = sz / scaledTravel

    // the force g = f / r split along and across the move; the across part does not change while stuck
    val gx = fx / r
    val gy = fy / r
    val gz = fz / r
    val along = gx * ux + gy * uy + gz * uz
    val acrossX = gx - ux * along
    val acrossY = gy - uy * along
    val acrossZ = gz - uz * along
    val acrossNorm = Math.sqrt(acrossX * acrossX + acrossY * acrossY + acrossZ * acrossZ)
    val gNormSquare = gx * gx + gy * gy + gz * gz

    // the exit point of the chord inside the ball: cos(theta0) = its along component, sin(theta0) = across.
    // A force already on the sphere (within rounding) and moving outward IS the exit point: extrapolating
    // the chord from a point 1e-16 outside the sphere is ill-conditioned when the move is nearly tangent
    var cos0 = 0.0
    var sin0 = 0.0
    var stuckTravel = 0.0
    if (gNormSquare >= 1.0 && along >= 0.0) {
      val gNorm = Math.sqrt(gNormSquare)
      cos0 = along / gNorm
      sin0 = acrossNorm / gNorm
    } else {
      // along^2 + (1 - |g|^2) = 1 - across^2, in the form that keeps the digits of a small along
      cos0 = Math.sqrt(Math.max(0.0, along * along + (1.0 - gNormSquare)))
      sin0 = acrossNorm
      stuckTravel = Math.min(travel, Math.max(0.0, (cos0 - along) / k * r))
    }
    val slipTravel = travel - stuckTravel

    // slipping part: tan(theta/2) = sin / (1 + cos) decays exponentially over the slipping travel
    // (in break deformations)
    val halfTan = sin0 / (1.0 + cos0) * Math.exp(-slipTravel * k / r)
    val halfTanSq = halfTan * halfTan
    val cos = (1.0 - halfTanSq) / (1.0 + halfTanSq)
    val sin = 2.0 * halfTan / (1.0 + halfTanSq)

    if (acrossNorm == 0.0) write(out, offset, ux * r, uy * r, uz * r)
    else
      val alongScale = r * cos
      val acrossScale = r * sin / acrossNorm
      write(out, offset,
        ux * alongScale + acrossX * acrossScale,
        uy * alongScale + acrossY * acrossScale,
        uz * alongScale + acrossZ * acrossScale)

  private inline def write(out: Array[Double], offset: Int, x: Double, y: Double, z: Double): Unit =
    out(offset) = x
    out(offset + 1) = y
    out(offset + 2) = z
