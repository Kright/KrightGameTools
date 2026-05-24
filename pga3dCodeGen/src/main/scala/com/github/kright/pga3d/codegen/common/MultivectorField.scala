package com.github.kright.pga3d.codegen.common

import com.github.kright.ga.{BasisBlade, BasisBladeWithSign}
import com.github.kright.util.Sign

case class MultivectorField(name: String,
                            basisBladeWithSign: BasisBladeWithSign):
  def basisBlade: BasisBlade = basisBladeWithSign.basisBlade

  def sign: Sign = basisBladeWithSign.sign
