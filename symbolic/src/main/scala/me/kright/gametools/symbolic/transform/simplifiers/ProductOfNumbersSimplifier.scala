package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.Symbolic.Func
import me.kright.gametools.symbolic.SymbolicStr.{Number, isNumber}
import me.kright.gametools.symbolic.transform.simplifiers.ProductOfNumbersSimplifier.trySimplify
import me.kright.gametools.symbolic.{Symbolic, SymbolicStr}

class ProductOfNumbersSimplifier extends SymbolicStrTransformDepthFirst({
  case Symbolic.Func("*", elems) =>
    if (elems.size == 1) Option(elems.head)
    else trySimplify(elems)
  case _ => None
})

object ProductOfNumbersSimplifier:
  private def trySimplify(elems: Seq[SymbolicStr]): Option[SymbolicStr] = {
    require(elems.size >= 2, elems)

    val (numbers, others) = elems.partitionMap {
      case Number(v) => Left(v)
      case other => Right(other)
    }

    if (numbers.isEmpty) return None

    val product = numbers.product

    if (product == 0.0) return Option(SymbolicStr.zero)

    if (others.isEmpty) {
      // numbers size at least 2
      return Option(Number(product))
    }

    if (numbers.size == 1) {
      if (numbers.head == 1.0) return Option(makeProductOrSimplify(others))
      if (!isNumber(elems.head)) return Option(makeProductOrSimplify(Seq(SymbolicStr(numbers.head)) ++ others))
      return None
    }

    // numbers.size >= 2

    Option {
      if (product != 1.0)
        makeProductOrSimplify(Seq(SymbolicStr(product)) ++ others)
      else {
        makeProductOrSimplify(others)
      }
    }
  }