package com.github.kright.pga3d.codegen.scala

import me.kright.gametools.ga.MultiVector
import com.github.kright.symbolic.Sym

case class MultivectorUnaryOp(f: (ScalaMultivectorSubClass, MultiVector[Sym]) => Option[String]):
  def apply(cls: ScalaMultivectorSubClass, v: MultiVector[Sym]): Option[String] = f(cls, v)
