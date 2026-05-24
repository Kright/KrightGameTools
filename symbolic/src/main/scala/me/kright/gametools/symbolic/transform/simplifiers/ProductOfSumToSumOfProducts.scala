package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.Symbolic.Func
import me.kright.gametools.symbolic.{Symbolic, SymbolicStr}

class ProductOfSumToSumOfProducts extends SymbolicStrTransformDepthFirst({
  case Func("*", elems) if elems.exists {
    case Func("+", _) => true
    case _ => false
  } => Option(flatten(elems))
  case _ => None
})


private def flatten(elems: Seq[SymbolicStr]): SymbolicStr = {
  var result: Seq[Seq[SymbolicStr]] = Seq(Seq.empty)
  for (e <- elems) {
    result = e match
      case Func("+", sumElems) =>
        require(sumElems.size >= 2)
        result.flatMap { variant =>
          sumElems.map { a =>
            variant ++ Seq(a)
          }
        }
      case other => result.map(_ ++ Seq(other))
  }

  Symbolic.Func("+", result.map {
    case Seq() => ???
    case Seq(singleElement) => singleElement
    case seq => Func("*", seq)
  })
}
