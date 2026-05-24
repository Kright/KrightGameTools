package com.github.kright.ga

import com.github.kright.util.Sign
import scala.collection.mutable
import scala.math.Numeric.Implicits.infixNumericOps

trait BinaryOp:
  def apply(left: BasisBlade, right: BasisBlade): BasisBladeWithSign

  def apply(left: BasisBladeWithSign, right: BasisBladeWithSign): BasisBladeWithSign =
    apply(left.basisBlade, right.basisBlade) * (left.sign * right.sign)

  def apply[T](left: MultiVector[T], right: MultiVector[T])(using num: Numeric[T]): MultiVector[T] =
    require(left.ga == right.ga)
    val result = new mutable.HashMap[BasisBlade, T]()
    for ((l, lv) <- left.values) {
      for ((r, rv) <- right.values) {
        val rr = apply(l, r)
        rr.sign match
          case Sign.Positive => result(rr.basisBlade) = result.getOrElse(rr.basisBlade, num.zero) + lv * rv
          case Sign.Negative => result(rr.basisBlade) = result.getOrElse(rr.basisBlade, num.zero) - lv * rv
          case Sign.Zero =>
      }
    }
    MultiVector[T](left.ga, result.toMap)
