package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.{FileContent, FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.cpp3d.Pga3dProvider.pga3
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import scala.collection.immutable.ArraySeq

class BivectorOpsGenerator extends CppCodeGenerator {

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        s"#include \"opsArithmetic.h\"",
        s"#include \"opsNorm.h\""
      ),
      code.generatorName(this)
    )

    code.namespace(codeGen.namespace) {
      val bivector = CppSubclasses.bivector
      val bivectorWeight = CppSubclasses.bivectorWeight

      code("")
      code(s"inline std::pair<${bivector.name}, ${bivectorWeight.name}> ${bivector.name}::split() const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.bivectorSplitGuard))
        code.block {
          code(
            s"""return {
               |    ${bivector.name}{ .wx = 0.0, .wy = 0.0, .wz = 0.0, .xy = xy, .xz = xz, .yz = yz },
               |    ${bivectorWeight.name}{ .wx = wx, .wy = wy, .wz = wz }
               |};""".stripMargin)
        }
        code("}")
        code("")
        code(FormulaTemplate.renderCpp(SharedFormulas.bivectorSplitPseudoScalar(bivector.self)))
        code(s"const ${bivectorWeight.name} shiftAlongLine${bivectorWeight.makeBracesInit(SharedFormulas.bivectorSplitShift(bivector.self), multiline = true)};")
        code("")
        code(s"const ${bivector.name} line = (*this) - shiftAlongLine;")
        code("return {line, shiftAlongLine};")
      }
      code("}")
      code("")

      code(s"inline ${CppSubclasses.motor.name} ${bivector.name}::exp() const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.expSinDivLen("bulkNorm")))
        code("")
        code(FormulaTemplate.renderCpp(SharedFormulas.expSinMinusCos))
        code("")
        code(s"return ${CppSubclasses.motor.name} ${CppSubclasses.motor.makeBracesInit(SharedFormulas.bivectorExpResult(bivector.self), multiline = true)};")
      }
      code("}")
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsBivector.h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val includes = if (cls == CppSubclasses.bivector) {
      ArraySeq("<utility>")
    } else {
      Seq.empty
    }

    val code = if (cls == CppSubclasses.bivector) {
      s"""[[nodiscard]] inline std::pair<${cls.name}, ${CppSubclasses.bivectorWeight.name}> split() const noexcept;
         |
         |[[nodiscard]] inline ${CppSubclasses.motor.name} exp() const noexcept;""".stripMargin
    } else ""
    
    structBodyPart(code, includes)
  }
}
