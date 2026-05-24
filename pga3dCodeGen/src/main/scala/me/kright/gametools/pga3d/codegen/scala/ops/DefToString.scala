package me.kright.gametools.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga3d.codegen.scala.ScalaMultivectorSubClass.pointCenter
import me.kright.gametools.pga3d.codegen.scala.{GeneratedCode, MultivectorUnaryOp}

object DefToString:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      if (cls.variableFields.nonEmpty) {
        GeneratedCode { code =>
          code(
            s"""
               |override def toString: String =
               |  s"${cls.name}(${cls.variableFields.map(f => s"${f.name} = ${"$" + f.name}").mkString(", ")})"""".stripMargin)
        }
      } else if (cls == pointCenter) {
        GeneratedCode { code =>
          code(
            s"""
               |override def toString: String =
               |  "${cls.name}"""".stripMargin)
        }
      } else {
        assert(false, s"unknown class ${cls}")
      }
    }
