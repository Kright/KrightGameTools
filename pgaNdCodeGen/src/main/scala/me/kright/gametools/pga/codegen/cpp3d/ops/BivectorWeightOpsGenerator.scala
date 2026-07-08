package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

class BivectorWeightOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls == CppSubclasses.bivectorWeight) {
      structBodyPart(s"""[[nodiscard]] constexpr ${CppSubclasses.translator.name} exp() const noexcept;""")
    } else Seq()
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(Seq(s"#include \"${codeGen.Headers.types}\""), code.generatorName(this))

    code.namespace(codeGen.namespace) {
      code(
        s"""
           |[[nodiscard]] constexpr ${CppSubclasses.translator.name} ${CppSubclasses.bivectorWeight.name}::exp() const noexcept {
           |    return ${CppSubclasses.translator.name}{
           |        .wx = wx,
           |        .wy = wy,
           |        .wz = wz,
           |    };
           |}
           |""".stripMargin)
    }

    Seq(FileContent(codeGen.directory.resolve("opsBivectorWeight.h"), code.toString))
  }
}
