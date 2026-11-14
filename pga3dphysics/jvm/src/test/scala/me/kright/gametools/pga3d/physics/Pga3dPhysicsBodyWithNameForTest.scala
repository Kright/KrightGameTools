package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dMotor}

final class Pga3dPhysicsBodyWithNameForTest(val name: String,
                                            inertia: Pga3dInertia,
                                            motor: Pga3dMotor,
                                            localB: Pga3dBivector) extends Pga3dPhysicsBody(inertia, motor, localB)
