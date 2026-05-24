package com.github.kright.pga3d.codegen.common

import me.kright.gametools.ga.{BasisBlade, BasisBladeWithSign}
import me.kright.gametools.mathutil.Sign

case class MultivectorField(name: String,
                            basisBladeWithSign: BasisBladeWithSign):
  def basisBlade: BasisBlade = basisBladeWithSign.basisBlade

  def sign: Sign = basisBladeWithSign.sign
