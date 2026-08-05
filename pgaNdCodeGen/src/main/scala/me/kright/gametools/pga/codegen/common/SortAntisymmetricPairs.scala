package me.kright.gametools.pga.codegen.common

import me.kright.gametools.symbolic.Symbolic.{Func, Symbol}
import me.kright.gametools.symbolic.SymbolicStr
import me.kright.gametools.symbolic.transform.PartialTransform

import scala.collection.immutable.ArraySeq

/**
 * Regroups the summands of a bilinear operation's component so that every mirror pair
 *
 *   c * left.f * right.g   and   -c * left.g * right.f
 *
 * becomes one parenthesized difference, all pairs before all unpaired terms:
 * (a - b) + (c - d) + rest. This buys two exact identities the canonical flat sort
 * ((a + c - b) - d) cannot provide:
 *
 *  - exact zero on proportional operands: for cross(u, u * 2^k) (and in particular
 *    cross(u, u)) the mirrored products round to bit-identical doubles, each parenthesized
 *    difference is exactly 0.0 and so is the sum;
 *  - exact antisymmetry: swapping the operands turns each pair (a - b) into (b - a), which
 *    negates it exactly (rounding is sign-symmetric), and the sum of exactly-negated pairs is
 *    the exact negation of the original sum. A flat left-to-right sum loses this: the running
 *    partial sums round differently in the two directions.
 *
 * Pairs-first also matters for the zero case: an unpaired term U before a pair would leave the
 * accumulator at U, and fl(fl(U + p) - p) != U.
 *
 * A factor belongs to an operand by its qualifier - the text before the last dot ("r.wx" is the
 * field wx of the operand r; a bare "wx" is a field of `this`, with the empty qualifier). Two
 * summands mirror each other when they have the same two distinct qualifiers, the field names
 * swapped between them and opposite coefficients; no operand naming is configured here, so the
 * same transform serves the Scala (bare + "r.") and the C++ ("a." + "b.") generators.
 *
 * The input comes canonically sorted (stable across generator runs), the greedy pass below is
 * deterministic in that order, so the emitted code is just as stable. Expressions where no
 * mirror pair is split apart are left unchanged.
 *
 * Applied to the top-level sum only (via Sym.map), not depth-first; the result must not be
 * re-simplified (the sum flattener and the sorter would undo the grouping).
 */
object SortAntisymmetricPairs extends PartialTransform[SymbolicStr]:

  /** a summand of the shape [number *] qual1.name1 * qual2.name2, with qual1 < qual2 */
  private case class Monomial(coef: Double, qual1: String, name1: String, qual2: String, name2: String):
    def isMirrorOf(other: Monomial): Boolean =
      qual1 == other.qual1 && qual2 == other.qual2 &&
        name1 == other.name2 && name2 == other.name1 &&
        coef == -other.coef

  /** "r.wx" -> ("r", "wx"); "wx" -> ("", "wx") */
  private def splitQualifier(full: String): (String, String) =
    val dot = full.lastIndexOf('.')
    if (dot < 0) ("", full) else (full.take(dot), full.drop(dot + 1))

  private def parseMonomial(e: SymbolicStr): Option[Monomial] =
    e match
      case Func("*", args) =>
        var coef = 1.0
        var onlySymbols = true
        val names = ArraySeq.newBuilder[String]
        for (arg <- args) {
          arg match
            case Symbol(v: Double) => coef *= v
            case Symbol(s: String) => names += s
            case _ => onlySymbols = false
        }
        if (!onlySymbols) None
        else names.result() match
          case ArraySeq(n1, n2) => classify(coef, n1, n2)
          case _ => None
      case _ => None

  private def classify(coef: Double, n1: String, n2: String): Option[Monomial] =
    val (qual1, name1) = splitQualifier(n1)
    val (qual2, name2) = splitQualifier(n2)
    if (qual1 == qual2) None // both factors from the same operand: not a bilinear mirror candidate
    else if (qual1 < qual2) Some(Monomial(coef, qual1, name1, qual2, name2))
    else Some(Monomial(coef, qual2, name2, qual1, name1))

  override def apply(value: SymbolicStr): Option[SymbolicStr] =
    value match
      // a 2-term sum is a single (possibly paired) group already: nothing can be interleaved
      case Func("+", args) if args.size >= 3 =>
        val monomials = args.map(parseMonomial)
        val used = Array.fill(args.size)(false)
        val paired = ArraySeq.newBuilder[SymbolicStr]
        val unpaired = ArraySeq.newBuilder[SymbolicStr]

        for (i <- args.indices if !used(i)) {
          used(i) = true
          val mirrorIndex = monomials(i).flatMap { m =>
            args.indices.find(j => !used(j) && monomials(j).exists(_.isMirrorOf(m)))
          }
          mirrorIndex match
            case Some(j) =>
              used(j) = true
              paired += Func("+", ArraySeq(args(i), args(j)))
            case None =>
              unpaired += args(i)
        }

        val newArgs = paired.result() ++ unpaired.result()
        Option.when(newArgs != args)(Func("+", newArgs))
      case _ => None
