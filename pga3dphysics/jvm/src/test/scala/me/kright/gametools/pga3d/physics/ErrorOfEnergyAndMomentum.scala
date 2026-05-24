package me.kright.gametools.pga3d.physics


case class ErrorOfEnergyAndMomentum(errorE: Double, errorL: Double):
  def this() = this(0.0, 0.0)

  infix def max(other: ErrorOfEnergyAndMomentum): ErrorOfEnergyAndMomentum =
    ErrorOfEnergyAndMomentum(Math.max(errorE, other.errorE), Math.max(errorL, other.errorL))

  def <(other: ErrorOfEnergyAndMomentum): Boolean =
    errorE <= other.errorE && errorL <= other.errorL