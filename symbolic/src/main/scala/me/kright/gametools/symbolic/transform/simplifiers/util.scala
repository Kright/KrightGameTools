package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.Symbolic.Func
import me.kright.gametools.symbolic.SymbolicStr

def makeProductOrSimplify(elems: Seq[SymbolicStr]): SymbolicStr =
  require(elems.nonEmpty)
  if (elems.size == 1) return elems.head
  Func("*", elems)