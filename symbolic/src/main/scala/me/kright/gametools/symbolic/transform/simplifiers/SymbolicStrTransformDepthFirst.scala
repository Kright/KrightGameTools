package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.transform.PartialTransform
import me.kright.gametools.symbolic.{Symbolic, SymbolicStr}

class SymbolicStrTransformDepthFirst(patternTransform: SymbolicStr => Option[SymbolicStr]) extends PartialTransform[SymbolicStr]:

  override def apply(value: SymbolicStr): Option[SymbolicStr] =
    SymbolicStrTransformDepthFirst.depthFirstTransform(value, patternTransform)


object SymbolicStrTransformDepthFirst:
  def depthFirstTransform[F, S](symbolic: Symbolic[F, S], patternTransform: Symbolic[F, S] => Option[Symbolic[F, S]]): Option[Symbolic[F, S]] =
    symbolic match
      case s@Symbolic.Symbol(_) => patternTransform(s)
      case f@Symbolic.Func(func, args) =>
        val newArgs = args.map(depthFirstTransform(_, patternTransform))
        if (newArgs.forall(_.isEmpty)) {
          patternTransform(f)
        } else {
          Option {
            val newF = Symbolic.Func(f.func, newArgs.zip(args).map((next, prev) => next.getOrElse(prev)))
            patternTransform(newF).getOrElse(newF)
          }
        }
