package com.github.kright.ga

import com.github.kright.util.Sign

class GAComplement[T](val left: T,
                      val right: T)

object GAComplement:
  def apply(signature: Signature, geometric: BinaryOp, sign: Sign = Sign.Positive): GAComplement[CachedUnaryOp] =
    new GAComplement[CachedUnaryOp](
      left = CachedUnaryOp(signature, { (a: BasisBlade) =>
        val complement = a.unsignedComplement
        BasisBladeWithSign(complement, geometric(complement, a).sign * sign)
      }),
      right = CachedUnaryOp(signature, { (a: BasisBlade) =>
        val complement = a.unsignedComplement
        BasisBladeWithSign(complement, geometric(a, complement).sign * sign)
      })
    )
