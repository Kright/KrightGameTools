package com.github.kright.ga

import com.github.kright.util.Sign

case class BasisBladeWithSign(basisBlade: BasisBlade, sign: Sign = Sign.Positive):
  def *(anotherSign: Sign): BasisBladeWithSign = this.copy(sign = this.sign * anotherSign)

object BasisBladeWithSign:
  def zero(signature: Signature): BasisBladeWithSign = BasisBladeWithSign(BasisBlade.scalar(signature), Sign.Zero)
