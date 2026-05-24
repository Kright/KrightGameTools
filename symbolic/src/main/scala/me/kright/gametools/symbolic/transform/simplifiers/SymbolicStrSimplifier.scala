package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.SymbolicStr.*
import me.kright.gametools.symbolic.transform.PartialTransform
import me.kright.gametools.symbolic.{Symbolic, SymbolicStr}


object SymbolicStrSimplifier:
  def simplify(maxRepeatCount: Int = 16, argsSorter: ArgsSorter = sortArgs()): PartialTransform[SymbolicStr] =
    PartialTransform.any(
      SumFlattener(),
      ProductFlattener(),

      ProductOfNumbersSimplifier(),
      CombineElemsInSum(argsSorter),

      ProductOfSumToSumOfProducts(),
    ).repeat(maxRepeatCount)

  def sortArgs(commutativeFuncs: Set[String] = Set("+", "*"),
               knownFuncs: Set[String] = Set("/")): ArgsSorter =
    new ArgsSorter(
      SymbolicStr.ordering,
      isCommutative = commutativeFuncs.contains,
      isKnown = knownFuncs.contains,
    )
