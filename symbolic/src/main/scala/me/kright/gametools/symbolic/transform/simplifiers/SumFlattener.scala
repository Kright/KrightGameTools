package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.Symbolic
import me.kright.gametools.symbolic.Symbolic.Func
import scala.collection.immutable.ArraySeq


class SumFlattener extends SymbolicStrTransformDepthFirst({
  case Func("+", elems) if elems.exists {
    case Func("+", _) => true
    case _ => false
  } => Option {
    Func("+", elems.flatMap {
      case Func("+", elems) => elems
      case other => ArraySeq(other)
    })
  }
  case _ => None
})
