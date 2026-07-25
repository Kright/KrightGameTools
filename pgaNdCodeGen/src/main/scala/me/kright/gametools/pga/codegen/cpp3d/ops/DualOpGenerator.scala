package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import scala.collection.immutable.ArraySeq

class DualOpGenerator extends CppCodeGenerator {
  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(ArraySeq(s"#include \"${codeGen.Headers.types}\""), code.generatorName(this))

    code.namespace(codeGen.namespace) {
      for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
        val result = cls.makeSymbolic("a").dual
        val target = CppSubclasses.findMatchingClass(result)
        if (target != CppSubclasses.zeroCls) {
          code(s"constexpr ${target.name} dual(const ${cls.name}& a) noexcept { return ${target.makeBracesInit(result, multiline = true)}; }")
          code(s"constexpr ${target.name} ${cls.name}::dual() const noexcept { return pga3d::dual(*this); }")
          code("")
        }
      }
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsDual.h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val result = cls.self.dual
    val target = CppSubclasses.findMatchingClass(result)
    if (target == CppSubclasses.zeroCls) ArraySeq()
    else structBodyPart(s"[[nodiscard]] constexpr ${target.name} dual() const noexcept;")
  }
}
