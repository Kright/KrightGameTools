package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.{FileContent, SharedFormulas}
import me.kright.gametools.pga.codegen.cpp3d.Pga3dProvider.pga3
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import scala.collection.immutable.ArraySeq

class BivectorWeightOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls == CppSubclasses.bivectorWeight) {
      structBodyPart(s"""[[nodiscard]] constexpr ${CppSubclasses.translator.name} exp() const noexcept;""")
    } else ArraySeq()
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(ArraySeq(s"#include \"${codeGen.Headers.types}\""), code.generatorName(this))

    code.namespace(codeGen.namespace) {
      code("")
      code(s"[[nodiscard]] constexpr ${CppSubclasses.translator.name} ${CppSubclasses.bivectorWeight.name}::exp() const noexcept {")
      code.block {
        code(s"return ${CppSubclasses.translator.name} ${CppSubclasses.translator.makeBracesInit(SharedFormulas.weightExpResult(CppSubclasses.bivectorWeight.self), multiline = true)};")
      }
      code("}")
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsBivectorWeight.h"), code.toString))
  }
}
