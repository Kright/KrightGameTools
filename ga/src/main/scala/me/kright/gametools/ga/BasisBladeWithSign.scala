package me.kright.gametools.ga

import com.github.kright.mathutil.Sign

case class BasisBladeWithSign(basisBlade: BasisBlade, sign: Sign = Sign.Positive):
  def *(anotherSign: Sign): BasisBladeWithSign = this.copy(sign = this.sign * anotherSign)

object BasisBladeWithSign:
  def zero(signature: Signature): BasisBladeWithSign = BasisBladeWithSign(BasisBlade.scalar(signature), Sign.Zero)
