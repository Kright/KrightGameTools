package com.github.kright.pga3d.codegen.common

import me.kright.gametools.ga.{BasisBlade, MultiVector, PGA3}
import com.github.kright.pga3d.codegen.common.MultivectorField
import me.kright.gametools.symbolic.Sym

class MultivectorSubClass(val name: String,
                          val variableFields: Seq[MultivectorField],
                          val constantFields: Seq[(MultivectorField, Double)] = Seq(),
                          val shouldBeGenerated: Boolean = true)(using pga3: PGA3):

  val fieldBlades: Set[BasisBlade] = {
    val variables = variableFields.map(_.basisBlade).toSet
    val constants = constantFields.map(_._1.basisBlade).toSet
    require(variables.size == variableFields.size)
    require(constants.size == constantFields.size)
    require(variables.intersect(constants).isEmpty)
    variables ++ constants
  }

  def isMatching(vec: MultiVector[Sym]): Boolean = {
    vec.values.forall { (blade, value) => value == Sym.zero || fieldBlades.contains(blade) } &&
      constantFields.forall { (f, v) =>
        vec.get(f.basisBlade) match
          case Some(sym) => sym == Sym(v)
          case None if v == 0.0 => true
          case _ => false
      }
  }

