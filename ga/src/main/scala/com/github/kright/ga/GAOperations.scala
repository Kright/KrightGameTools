package com.github.kright.ga

import com.github.kright.mathutil.Sign


class GAOperations(val signature: Signature):
  val multiplication = GAMultiplication(signature)

  val dual = CachedUnaryOp(signature, { (a: BasisBlade) =>
    val b = a.unsignedComplement
    val sign = if (Ordering.by((v: BasisBlade) => (v.grade, v.generators)).compare(a, b) < 0) {
      multiplication.geometric(a, b).sign
    } else {
      multiplication.geometric(b, a).sign
    }
    BasisBladeWithSign(b, sign)
  })

  val anti: GAMultiplication[CachedBinaryOp] =
    multiplication.map { op =>
      CachedBinaryOp(signature, { (a, b) =>
        dual(op(dual(a), dual(b)))
      })
    }

  val complement: GAComplement[CachedUnaryOp] = GAComplement(signature, multiplication.geometric)

  private def isBulk(a: BasisBlade): Boolean =
    require(signature.negatives == 0)
    multiplication.geometric(a, a).sign != Sign.Zero

  val bulk = CachedUnaryOp(signature, { (a: BasisBlade) =>
    if (isBulk(a)) BasisBladeWithSign(a) else BasisBladeWithSign.zero(signature)
  })

  val weight = CachedUnaryOp(signature, { (a: BasisBlade) =>
    if (isBulk(a)) BasisBladeWithSign.zero(signature) else BasisBladeWithSign(a)
  })

  val reverse = CachedUnaryOp(signature, { (a: BasisBlade) =>
    val gr = a.grade
    BasisBladeWithSign(a, Sign.Negative.power(gr * (gr - 1) / 2))
  })

  val antiReverse = CachedUnaryOp(signature, { (a: BasisBlade) =>
    val ag = a.antiGrade
    BasisBladeWithSign(a, Sign.Negative.power(ag * (ag - 1) / 2))
  })

