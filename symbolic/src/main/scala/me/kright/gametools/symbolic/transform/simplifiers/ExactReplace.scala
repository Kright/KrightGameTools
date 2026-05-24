package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.SymbolicStr


class ExactReplace(val value: SymbolicStr,
                   val replacement: SymbolicStr) extends SymbolicStrTransformDepthFirst({ expr =>
  if (expr == value) Option(replacement)
  else None
})
  
  