package me.kright.gametools.pga.codegen.common

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.symbolic.Sym

/**
 * The shared symbolic derivation of the rotor axis methods (axisX = sandwich of the basis
 * vector, sign-corrected for dual-representation fields), consumed by the Scala 2d/3d and
 * C++ generators so the three backends cannot drift apart.
 */
object AxesSymbolics:
  case class Axis(methodName: String, result: MultiVector[Sym])

  def rotorAxes(rotorSelf: MultiVector[Sym], vectorSelf: MultiVector[Sym]): Seq[Axis] =
    val basisVectors = vectorSelf.values.keys.toSeq.sortBy(_.bits).reverse.map(blade => vectorSelf.filter((b, _) => b == blade))
    basisVectors.map { axis =>
      val axisOne = axis.mapValues(_ => Sym(1.0))
      val isMinus = axis.values.values.head.toString.contains("-")
      val methodName = s"axis${axis.values.values.head.toString.replace("-", "").toUpperCase}"
      val result = if (isMinus) rotorSelf.sandwich(axisOne) * Sym(-1.0) else rotorSelf.sandwich(axisOne)
      Axis(methodName, result)
    }
