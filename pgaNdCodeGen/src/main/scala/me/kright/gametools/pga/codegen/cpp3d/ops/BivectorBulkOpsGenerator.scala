package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.{FileContent, FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.cpp3d.Pga3dProvider.pga3
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import scala.collection.immutable.ArraySeq

class BivectorBulkOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls == CppSubclasses.bivectorBulk) {
      structBodyPart(s"""[[nodiscard]] inline ${CppSubclasses.rotor.name} exp() const noexcept;""")
    } else ArraySeq()
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        "#include \"opsNorm.h\""
      ), 
      code.generatorName(this))

    code.namespace(codeGen.namespace) {
      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.rotor.name} ${CppSubclasses.bivectorBulk.name}::exp() const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.expSinDivLen("bulkNorm")))
        code("")
        code(s"return ${CppSubclasses.rotor.name} ${CppSubclasses.rotor.makeBracesInit(SharedFormulas.bivectorExpResult(CppSubclasses.bivectorBulk.self), multiline = true)};")
      }
      code("}")
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsBivectorBulk.h"), code.toString))
  }
}
