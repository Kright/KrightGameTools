package com.github.kright.ga

import com.github.kright.mathutil.Sign

class GAMultiplication[T](val geometric: T,
                          val dot: T,
                          val wedge: T):
  def map[U](f: T => U) = new GAMultiplication[U](
    f(geometric),
    f(dot),
    f(wedge)
  )

object GAMultiplication:
  def apply(signature: Signature): GAMultiplication[CachedBinaryOp] =
    val geometric = CachedBinaryOp(signature, { (a: BasisBlade, b: BasisBlade) =>
      val allBasisVectors = a.generators ++ b.generators
      val sorted = allBasisVectors.sorted

      val paritySign = if (Permutation.parity(allBasisVectors, sorted)) Sign.Positive else Sign.Negative
      val removedSigns = a.getCommon(b).generators.map(_.squareSign)
      val sign = removedSigns.fold(paritySign)(_ * _)

      if (sign == Sign.Zero) BasisBladeWithSign(BasisBlade.scalar(signature), sign)
      else BasisBladeWithSign(BasisBlade(a.bits ^ b.bits, signature), sign)
    })

    val dot = CachedBinaryOp(signature, { (left: BasisBlade, right: BasisBlade) =>
      if (left.isContraction(right)) {
        geometric(left, right)
      } else {
        BasisBladeWithSign.zero(signature)
      }
    })

    val wedge = CachedBinaryOp(signature, { (left: BasisBlade, right: BasisBlade) =>
      if (left.hasCommonBasisVectors(right)) {
        BasisBladeWithSign.zero(signature)
      } else {
        geometric(left, right)
      }
    })

    new GAMultiplication(geometric, dot, wedge)
