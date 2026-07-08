package me.kright.gametools.pga.codegen.scala.common

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.symbolic.Sym

case class MultivectorUnaryOp(f: (ScalaMultivectorSubClass, MultiVector[Sym]) => Option[String]):
  def apply(cls: ScalaMultivectorSubClass, v: MultiVector[Sym]): Option[String] = f(cls, v)
