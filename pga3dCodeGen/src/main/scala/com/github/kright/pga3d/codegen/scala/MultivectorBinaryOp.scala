package com.github.kright.pga3d.codegen.scala

import me.kright.gametools.ga.MultiVector
import com.github.kright.symbolic.Sym

class MultivectorBinaryOp(val names: Seq[String],
                          val op: (MultiVector[Sym], MultiVector[Sym]) => Option[MultiVector[Sym]]):

  require(names.nonEmpty)

  def apply(a: MultiVector[Sym], b: MultiVector[Sym]): Option[MultiVector[Sym]] = op(a, b)

  def name: String = names.head


object MultivectorBinaryOp:
  def option(names: Seq[String],
             op: (MultiVector[Sym], MultiVector[Sym]) => Option[MultiVector[Sym]]): MultivectorBinaryOp =
    new MultivectorBinaryOp(names, op)

  def apply(names: Seq[String],
            op: (MultiVector[Sym], MultiVector[Sym]) => MultiVector[Sym]): MultivectorBinaryOp =
    new MultivectorBinaryOp(names, (a, b) => Option(op(a, b)))