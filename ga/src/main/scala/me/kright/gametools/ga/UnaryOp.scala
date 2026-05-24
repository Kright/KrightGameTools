package me.kright.gametools.ga

import me.kright.gametools.mathutil.Sign
import scala.collection.mutable
import scala.math.Numeric.Implicits.infixNumericOps

trait UnaryOp:
  def apply(x: BasisBlade): BasisBladeWithSign

  def apply(x: BasisBladeWithSign): BasisBladeWithSign =
    apply(x.basisBlade) * x.sign

  def apply[T](vec: MultiVector[T])(using num: Numeric[T]): MultiVector[T] = {
    val result = new mutable.HashMap[BasisBlade, T]()
    for ((l, lv) <- vec.values) {
      val rr = apply(l)
      rr.sign match
        case Sign.Positive => result(rr.basisBlade) = result.getOrElse(rr.basisBlade, num.zero) + lv
        case Sign.Negative => result(rr.basisBlade) = result.getOrElse(rr.basisBlade, num.zero) - lv
        case Sign.Zero =>
    }
    MultiVector[T](vec.ga, result.toMap)
  }
