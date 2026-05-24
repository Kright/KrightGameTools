package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.common.FileContent
import me.kright.gametools.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

class BivectorBulkOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls == CppSubclasses.bivectorBulk) {
      structBodyPart(s"""[[nodiscard]] inline ${CppSubclasses.quaternion.name} exp() const noexcept;""")
    } else Seq()
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      Seq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        "#include \"opsNorm.h\""
      ), 
      getClass.getName)

    code.namespace(codeGen.namespace) {
      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.quaternion.name} ${CppSubclasses.bivectorBulk.name}::exp() const noexcept {
           |    const double len = bulkNorm();
           |    const double cos = std::cos(len);
           |
           |    const double sinDivLen = (len > 1e-5)
           |        ? (std::sin(len) / len)
           |        : (1.0 - (len * len) / 6.0);
           |
           |    return ${CppSubclasses.quaternion.name}{
           |        .s = cos,
           |        .xy = sinDivLen * xy,
           |        .xz = sinDivLen * xz,
           |        .yz = sinDivLen * yz,
           |    };
           |}
           |""".stripMargin)
    }

    Seq(FileContent(codeGen.directory.resolve("opsBivectorBulk.h"), code.toString))
  }
}
