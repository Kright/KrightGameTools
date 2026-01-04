package com.github.kright.pga3d.codegen.cpp.ops

import com.github.kright.pga3d.codegen.common.FileContent
import com.github.kright.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

class DualOpGenerator extends CppCodeGenerator {
  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(Seq(s"#include \"${codeGen.Headers.types}\""), getClass.getName)

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

    Seq(FileContent(codeGen.directory.resolve("opsDual.h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val result = cls.self.dual
    val target = CppSubclasses.findMatchingClass(result)
    if (target == CppSubclasses.zeroCls) Seq()
    else structBodyPart(s"[[nodiscard]] constexpr ${target.name} dual() const noexcept;")
  }
}
