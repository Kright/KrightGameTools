package me.kright.gametools.pga.codegen.common

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.symbolic.Sym

/**
 * The canonical norm-family derivations, shared by the Scala and C++ generators
 * so the two backends cannot drift apart.
 */
object NormSymbolics:
  def fullSquare(s: MultiVector[Sym]): MultiVector[Sym] =
    s.geometric(s.reverse).grade(0) + s.dual.geometric(s.dual.reverse).grade(0)

  def bulkSquare(s: MultiVector[Sym]): MultiVector[Sym] =
    s.geometric(s.reverse).grade(0)

  def weightSquare(s: MultiVector[Sym]): MultiVector[Sym] =
    s.dual.geometric(s.dual.reverse).grade(0)

  /** true when the squared norm is identically 1 (classes with a constant unit blade) */
  def isConstantOne(square: MultiVector[Sym]): Boolean =
    square.values.toSeq match
      case Seq((blade, value)) => blade.grade == 0 && value == Sym.one
      case _ => false
