package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.Symbolic.{Func, Symbol}
import me.kright.gametools.symbolic.SymbolicStr

import scala.math.Ordering

class ArgsSorter(val ordering: Ordering[SymbolicStr],
                 isCommutative: String => Boolean,
                 isKnown: String => Boolean) extends SymbolicStrTransformDepthFirst({
  case Symbol(_) => None
  case f@Func(name, elems) =>
    if (isCommutative(name)) {
      val sortedElems = elems.sorted(using ordering)
      if (sortedElems != elems) {
        Option(Func(name, sortedElems))
      } else None
    } else {
      require(isKnown(name), s"unknown function: ${f}")
      None
    }
})
