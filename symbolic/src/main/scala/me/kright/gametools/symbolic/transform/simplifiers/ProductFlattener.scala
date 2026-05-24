package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.Symbolic
import me.kright.gametools.symbolic.Symbolic.Func

import scala.collection.mutable.ArrayBuffer

class ProductFlattener extends SymbolicStrTransformDepthFirst({
  case Func("*", elems) => {
    val multipliers = ArrayBuffer[Symbolic[String, Double | String]]()
    val divizors = ArrayBuffer[Symbolic[String, Double | String]]()
    var updated = false

    for (e <- elems) {
      e match {
        case Func("*", elems) => {
          multipliers ++= elems
          updated = true
        }
        case Func("/", elems) => {
          require(elems.size == 2)
          multipliers += elems.head
          divizors += elems.last
          updated = true
        }
        case other => {
          multipliers += other
        }
      }
    }

    if (!updated) {
      None
    } else if (divizors.isEmpty) {
      Option(makeProductOrSimplify(multipliers.toSeq))
    } else {
      Option(Func("/", Seq(makeProductOrSimplify(multipliers.toSeq), makeProductOrSimplify(divizors.toSeq))))
    }
  }

  case _ => None
})
