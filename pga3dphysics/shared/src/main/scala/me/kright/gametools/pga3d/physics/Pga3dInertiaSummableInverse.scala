package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.Pga3dBivector

/**
 * The inverse of the momentum map of a [[Pga3dInertiaSummable]], cached as its 3x3 blocks.
 *
 * In vector form [[Pga3dInertiaSummable.apply]] is the classic 6x6 spatial inertia built from
 * 3x3 blocks: with the twist split into v = (wx, wy, wz) and omega = (yz, -xz, xy), and
 * h = (wx, wy, wz) = mass * centerOfMass,
 *   l = J * omega + h x v,   p = mass * v - h x omega,
 * where J = trace(S) * E - S is the inertia tensor about the origin. The Schur complement of
 * the mass block is exactly the inertia tensor about the center of mass (the parallel axis
 * theorem backwards): Jc = J - mass * (|c|^2 E - c c^T) - so the inverse needs only the center
 * of mass, 1/mass and the analytically inverted symmetric 3x3 Jc, and [[apply]] is about as
 * cheap as the forward map (see `InertiaBenchmark`).
 */
final class Pga3dInertiaSummableInverse(val cx: Double, val cy: Double, val cz: Double,
                                        val invMass: Double,
                                        val ixx: Double, val iyy: Double, val izz: Double,
                                        val ixy: Double, val iyz: Double, val ixz: Double):

  /** the twist b with summable(b) == momentum */
  def apply(momentum: Pga3dBivector): Pga3dBivector =
    // the vector views of the momentum: l = the weight part, p = the bulk part as (yz, -xz, xy)
    val px = momentum.yz
    val py = -momentum.xz
    val pz = momentum.xy
    // transport the angular momentum to the center of mass: lc = l - c x p
    val lcx = momentum.wx - (cy * pz - cz * py)
    val lcy = momentum.wy - (cz * px - cx * pz)
    val lcz = momentum.wz - (cx * py - cy * px)
    // omega = inverse of the inertia tensor about the center of mass, applied to lc
    val ox = ixx * lcx + ixy * lcy + ixz * lcz
    val oy = ixy * lcx + iyy * lcy + iyz * lcz
    val oz = ixz * lcx + iyz * lcy + izz * lcz
    // v = p / mass + c x omega
    val vx = px * invMass + (cy * oz - cz * oy)
    val vy = py * invMass + (cz * ox - cx * oz)
    val vz = pz * invMass + (cx * oy - cy * ox)
    Pga3dBivector(wx = vx, wy = vy, wz = vz, xy = oz, xz = -oy, yz = ox)


object Pga3dInertiaSummableInverse:
  def apply(s: Pga3dInertiaSummable): Pga3dInertiaSummableInverse =
    val invMass = 1.0 / s.ww
    val cx = s.wx * invMass
    val cy = s.wy * invMass
    val cz = s.wz * invMass

    // the inertia tensor about the center of mass: J - mass * (|c|^2 E - c c^T)
    val m = s.ww
    val jxx = (s.yy + s.zz) - m * (cy * cy + cz * cz)
    val jyy = (s.xx + s.zz) - m * (cx * cx + cz * cz)
    val jzz = (s.xx + s.yy) - m * (cx * cx + cy * cy)
    val jxy = -s.xy + m * cx * cy
    val jyz = -s.yz + m * cy * cz
    val jxz = -s.xz + m * cx * cz

    // the analytic inverse of the symmetric 3x3 via the adjugate
    val cofXX = jyy * jzz - jyz * jyz
    val cofXY = jyz * jxz - jxy * jzz
    val cofXZ = jxy * jyz - jyy * jxz
    val invDet = 1.0 / (jxx * cofXX + jxy * cofXY + jxz * cofXZ)

    new Pga3dInertiaSummableInverse(
      cx, cy, cz, invMass,
      ixx = cofXX * invDet,
      iyy = (jxx * jzz - jxz * jxz) * invDet,
      izz = (jxx * jyy - jxy * jxy) * invDet,
      ixy = cofXY * invDet,
      iyz = (jxy * jxz - jxx * jyz) * invDet,
      ixz = cofXZ * invDet)
