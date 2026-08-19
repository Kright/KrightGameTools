package me.kright.gametools.physics1d

/**
 * Spring with a soft zone around zero deflection: stiffness blends from softK at zero
 * to stiffK at the edge of the zone (|deflection| = softZoneTravel) and stays stiffK outside.
 *
 * Sign convention matches [[HystereticFriction]] and the viscous friction convention of
 * gametools (Pga3dFriction: Linear(k)(v) = -k*v): the force is a generalized force along
 * the coordinate r (deflection = r - restLength), negative acts to decrease r. A stretched
 * spring (deflection > 0) has a negative force, so the spring, viscous and hysteretic
 * terms of one joint can simply be summed.
 *
 * force(x) = -stiffness(x) * x, where stiffness (with travel = softZoneTravel) is:
 *  - travel == 0:     stiffK (plain linear spring, softK is ignored)
 *  - |x| < travel:    softK + (stiffK - softK) * x^2 / (3 * travel^2) (the secant stiffness;
 *                     the tangent dForce/dx is -(softK + (stiffK - softK) * x^2 / travel^2))
 *  - |x| >= travel:   stiffK with the offset: force = -stiffK * x + sign(x) * (stiffK - softK) * (2/3) * travel
 *
 * At |x| = travel both branches give force = -(softK * travel + (stiffK - softK) * travel / 3)
 * and dForce/dx = -stiffK, so the force is C1-continuous (the second derivative
 * -2 * (stiffK - softK) * x / travel^2 jumps to 0 there).
 */
final case class SpringElasticity(softK: Double,
                                  softZoneTravel: Double,
                                  stiffK: Double) {
  require(softK >= 0 && softZoneTravel >= 0 && stiffK >= 0)

  /** positive deflection = stretched; negative force acts to decrease r,
   * so a stretched spring returns a negative force */
  def force(deflection: Double): Double = {
    val travel = softZoneTravel
    if (travel == 0.0) return -stiffK * deflection

    if (Math.abs(deflection) >= travel) {
      Math.signum(deflection) * (stiffK - softK) * (2.0 / 3.0) * travel - stiffK * deflection
    } else {
      -deflection * (softK + (stiffK - softK) * (deflection * deflection) / (3.0 * travel * travel))
    }
  }

  def maxStiffness: Double = Math.max(softK, stiffK)
}


object SpringElasticity {
  def linear(k: Double): SpringElasticity = SpringElasticity(k, 0.0, k)
}
