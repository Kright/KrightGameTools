package com.github.kright.pga3d.codegen.scala

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.symbolic.Sym

object GeneratedCode:
  def apply(f: ScalaCodeBuilder => Unit): Option[String] = {
    val codeGen = ScalaCodeBuilder()
    f(codeGen)
    Option(codeGen.toString).filter(_ != "")
  }

object GeneratedValue:
  def apply(cls: ScalaMultivectorSubClass, name: String, result: MultiVector[Sym], targetName: String | Null = null): Option[String] = {
    val resultCls = ScalaMultivectorSubClass.findMatchingClass(result)
    if (resultCls == ScalaMultivectorSubClass.zeroCls) {
      None
    } else {
      GeneratedCode { code =>
        code("")
        if (targetName != null) {
          code(s"@targetName(\"$targetName\")")
        }
        code(s"def $name: ${resultCls.typeName} =")
        code.block {
          if (result == cls.self && resultCls == cls) {
            code("this")
          } else if (result == -cls.self && resultCls == cls && name != "unary_- ") {
            code("-this")
          } else {
            code(resultCls.makeConstructor(result))
          }
        }
      }
    }
  }
