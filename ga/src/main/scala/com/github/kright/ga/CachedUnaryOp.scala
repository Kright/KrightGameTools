package com.github.kright.ga

import com.github.kright.mathutil.MathUtil.*

class CachedUnaryOp(val signature: Signature,
                    val singleOp: UnaryOp) extends UnaryOp:
  private val data = new Array[BasisBladeWithSign](signature.bladesCount)

  override def apply(x: BasisBlade): BasisBladeWithSign =
    data.getOrElseUpdate(x.bits, singleOp(x))
