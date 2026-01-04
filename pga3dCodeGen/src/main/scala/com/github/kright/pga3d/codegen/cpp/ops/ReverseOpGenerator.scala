package com.github.kright.pga3d.codegen.cpp.ops

import com.github.kright.ga.MultiVector
import com.github.kright.pga3d.codegen.common.FileContent
import com.github.kright.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import com.github.kright.symbolic.Sym

class ReverseOpGenerator extends ReverseOrAntiReverseOpsGenerator("reversed", _.reverse)

class AntiReverseOpGenerator extends ReverseOrAntiReverseOpsGenerator("antiReversed", _.antiReverse)

private class ReverseOrAntiReverseOpsGenerator(name: String,
                                               operation: MultiVector[Sym] => MultiVector[Sym]) extends CppCodeGenerator {

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(Seq(s"#include \"${codeGen.Headers.types}\""), getClass.getName)

    code.namespace(codeGen.namespace) {
      for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
        val result = operation(cls.makeSymbolic("a"))
        val target = CppSubclasses.findMatchingClass(result)
        if (target != CppSubclasses.zeroCls) {
          code(s"constexpr ${target.name} $name(const ${cls.name}& a) noexcept { return ${target.makeBracesInit(result, multiline = true)}; }")
          code(s"constexpr ${target.name} ${cls.name}::$name() const noexcept { return pga3d::${name}(*this); }")
          code("")
        }
      }
    }

    Seq(FileContent(codeGen.directory.resolve(s"ops${name.capitalize}.h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val result = operation(cls.self)
    val target = CppSubclasses.findMatchingClass(result)
    val code =
      if (target == CppSubclasses.zeroCls) ""
      else s"[[nodiscard]] constexpr ${target.name} $name() const noexcept;"

    structBodyPart(code)
  }
}

