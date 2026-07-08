package me.kright.gametools.ga

import me.kright.gametools.mathutil.MathUtil.*

class CachedUnaryOp(val signature: Signature,
                    val singleOp: UnaryOp) extends UnaryOp:
  private val data = new Array[BasisBladeWithSign | Null](signature.bladesCount)

  override def apply(x: BasisBlade): BasisBladeWithSign =
    data.getOrElseUpdate(x.bits, singleOp(x))
