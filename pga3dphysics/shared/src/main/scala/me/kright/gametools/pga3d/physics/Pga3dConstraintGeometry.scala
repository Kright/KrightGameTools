package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dPoint, Pga3dVector}

/**
 * the per-constraint geometry shared by [[Pga3dConstraintResolver]] and
 * [[Pga3dPhysicsSolverVerletConstrained]]: world anchors, direction, the constraint forque
 * line j, the inverse-inertia response twists and the total generalized inverse mass
 */
final case class Pga3dConstraintGeometry(pA: Pga3dPoint,
                                         pB: Pga3dPoint,
                                         dist: Double,
                                         n: Pga3dVector,
                                         j: Pga3dBivector,
                                         gA: Pga3dBivector,
                                         gB: Pga3dBivector,
                                         wSum: Double)
