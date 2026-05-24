package me.kright.gametools.symbolic.transform.simplifiers

import me.kright.gametools.symbolic.Symbolic.Func
import me.kright.gametools.symbolic.transform.PartialTransform
import me.kright.gametools.symbolic.{Symbolic, SymbolicStr}


class GroupMultipliersInSumOfProducts extends PartialTransform[SymbolicStr]:
  override def apply(value: SymbolicStr): Option[SymbolicStr] =
    value match
      case Func("+", elems) => group(elems)
      case _ => None

  private def group(elems: Seq[SymbolicStr]): Option[SymbolicStr] = {
    if (elems.size <= 1) return None

    val elemsAsProducts = elems.collect {
      case f@Func("*", p) => f.copy(args = convertMinusConstToMinusOneAndConst(f.args))
    }

    if (elemsAsProducts.size < elems.size) return None

    val commonElements = elemsAsProducts.map(_.args.toSet).reduce(_ intersect _) - SymbolicStr.minusOne

    if (commonElements.nonEmpty) {
      val sum = Func("+", elemsAsProducts.map { f =>
        val others: Seq[SymbolicStr] = remove(f.args, commonElements)
        others.size match
          case 0 => SymbolicStr.one
          case 1 => others.head
          case _ => f.copy(args = others)
      })

      return Option(Func("*", commonElements.toSeq :+ apply(sum).getOrElse(sum)))
    }

    // no common elements, let's find most frequent
    val productsSets = elemsAsProducts.map(_.args.toSet)

    val (mostCommonElement, count) = elemsAsProducts
      .flatMap(_.args)
      .distinct
      .filterNot(_ == SymbolicStr.minusOne)
      .map(elem => elem -> productsSets.count(_.contains(elem)))
      .maxBy(_._2)

    if (count <= 1) return None

    val (withCommonElement, withoutCommonElement) = elemsAsProducts.partition(_.args.contains(mostCommonElement))

    val groupedWithElement = {
      val sumWithoutCommonElement = Func("+", withCommonElement.map(f => f.copy(args = remove(f.args, mostCommonElement))))
      Func("*", Seq(mostCommonElement, apply(sumWithoutCommonElement).getOrElse(sumWithoutCommonElement)))
    }

    val groupedWithoutElement =
      group(withoutCommonElement) match
        case None => withoutCommonElement
        case Some(grouped) =>
          grouped match
            case f@Symbolic.Func("*", ee) => Seq(f)
            case Symbolic.Func("+", ee) => ee
            case _ => ???

    Option {
      Func("+", groupedWithoutElement :+ groupedWithElement)
    }
  }

  private def remove(elems: Seq[SymbolicStr], toRemove: SymbolicStr): Seq[SymbolicStr] =
    var firstFound: Boolean = false
    elems.filter { e =>
      if (!firstFound && (toRemove == e)) {
        firstFound = true
        false
      } else true
    }

  private def remove(elems: Seq[SymbolicStr], toRemove: Iterable[SymbolicStr]): Seq[SymbolicStr] =
    var result = elems
    for (e <- toRemove) {
      result = remove(result, e)
    }
    result

  private def convertMinusConstToMinusOneAndConst(elems: Seq[SymbolicStr]): Seq[SymbolicStr] =
    elems.flatMap {
      case SymbolicStr.Number(v) if ((v != -1.0) && (v < 0)) => Seq(SymbolicStr(-1.0), SymbolicStr(-v))
      case e => Seq(e)
    }
  