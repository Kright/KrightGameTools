package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.transform.PartialTransform
import me.kright.gametools.symbolic.{Symbolic, SymbolicStr}

import scala.collection.mutable

class ReplaceSumOrProduct(val func: Symbolic.Func[SymbolicStr.F, SymbolicStr.S],
                          val replacement: SymbolicStr) extends PartialTransform[SymbolicStr]:
  require(func.func == "+" || func.func == "*")
  private val argsCount = count(func.args)

  private def count(args: Seq[SymbolicStr]): Map[SymbolicStr, Int] =
    args.groupBy(a => a).map((a, lst) => a -> lst.size)

  private val patternTransform: (SymbolicStr) => Option[SymbolicStr] = {
    case f@Symbolic.Func(name, args) if name == func.func => {
      val matchedArgsCount = count(args)
      val canReplace = argsCount.forall((a, n) => matchedArgsCount.getOrElse(a, 0) >= n)
      if (!canReplace) {
        None
      } else {
        val remainingCount: mutable.HashMap[SymbolicStr, Int] = argsCount.to(mutable.HashMap)
        val newArgs = args.filter { a =>
          if (remainingCount.getOrElse(a, 0) > 0) {
            remainingCount(a) -= 1
            false
          } else {
            true
          }
        } :+ replacement

        Option(
          if (newArgs.size == 1) {
            newArgs.head
          } else {
            Symbolic.Func(name, newArgs)
          }
        )
      }
    }
    case _ => None
  }

  override def apply(value: SymbolicStr): Option[SymbolicStr] =
    SymbolicStrTransformDepthFirst.depthFirstTransform(value, patternTransform)

object ReplaceSumOrProduct:
  def apply(func: SymbolicStr, replacement: SymbolicStr): ReplaceSumOrProduct =
    func match
      case f@Symbolic.Func(_, _) => new ReplaceSumOrProduct(f, replacement)
      case _ => ???
