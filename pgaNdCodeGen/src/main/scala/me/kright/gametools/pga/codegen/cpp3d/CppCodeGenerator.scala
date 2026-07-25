package me.kright.gametools.pga.codegen.cpp3d

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppSubclass, Pga3dCodeGenCpp}
import scala.collection.immutable.ArraySeq

trait CppCodeGenerator:
  def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = ArraySeq()

  protected def structBodyPart(code: String,
                               includes: Seq[String] = ArraySeq()): Seq[StructBodyPart] = {
    if (code.nonEmpty || includes.nonEmpty) {
      ArraySeq(StructBodyPart(includes, code, this))
    } else
      ArraySeq()
  }

  def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = ArraySeq()


class CppCodeGeneratorSum(val generators: Seq[CppCodeGenerator]) extends CppCodeGenerator {
  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    generators.flatMap(_.generateStructBody(cls))
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    generators.flatMap(_.generateFiles(codeGen))
  }
}


case class StructBodyPart(includes: Seq[String],
                          structCode: String,
                          sourceGenerator: CppCodeGenerator)
