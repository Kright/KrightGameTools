package me.kright.gametools.pga3d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}

/**
 * a contact between a volume (e.g. a sphere) and a surface:
 * the contact point on the surface, the unit normal pointing from the surface towards
 * the volume, and the penetration depth (>= 0)
 */
case class Pga3dContact(point: Pga3dPoint,
                        normal: Pga3dVector,
                        depth: Double) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer
