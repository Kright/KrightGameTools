package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.common.FileContent
import me.kright.gametools.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

class ConversionOpGenerator extends CppCodeGenerator:
  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val code = CppCodeBuilder()

    for (target <- CppSubclasses.all if cls != target && target.shouldBeGenerated && target != CppSubclasses.pseudoScalar) {
      if (target.isMatching(cls.self)) {
        code(s"[[nodiscard]] constexpr ${target.name} to${target.name}() const noexcept;")
      } else {
        val simplifiedSelf = cls.self.filter((blade, _) => target.variableFields.exists(_.basisBlade == blade))
        if (simplifiedSelf.values.nonEmpty) {
          code(s"[[nodiscard]] constexpr ${target.name} to${target.name}Unsafe() const noexcept;")
        }
      }
    }

    if (cls == CppSubclasses.projectivePoint) {
      val point = CppSubclasses.point
      code(s"[[nodiscard]] constexpr ${point.name} to${point.name}() const noexcept;")
    }

    if (cls == CppSubclasses.projectiveTranslator) {
      val translator = CppSubclasses.translator
      code(s"[[nodiscard]] constexpr ${translator.name} to${translator.name}() const noexcept;")
    }

    structBodyPart(code.toString)
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(Seq(s"#include \"${codeGen.Headers.types}\""), getClass.getName)

    code.namespace(codeGen.namespace) {
      for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
        for (target <- CppSubclasses.all if cls != target && target.shouldBeGenerated && target != CppSubclasses.pseudoScalar) {
          if (target.isMatching(cls.self)) {
            code(s"constexpr ${target.name} ${cls.name}::to${target.name}() const noexcept { return ${target.makeBracesInit(cls.self)}; }")
          } else {
            val simplifiedSelf = cls.self.filter((blade, _) => target.variableFields.exists(_.basisBlade == blade))
            if (simplifiedSelf.values.nonEmpty) {
              code(s"constexpr ${target.name} ${cls.name}::to${target.name}Unsafe() const noexcept { return ${target.makeBracesInit(simplifiedSelf)}; }")
            }
          }
        }
        code("")

        if (cls == CppSubclasses.projectivePoint) {
          val point = CppSubclasses.point
          code(s"constexpr ${point.name} ${cls.name}::to${point.name}() const noexcept { const double inv = 1.0 / w; return { .x = x * inv, .y = y * inv, .z = z * inv }; }")
        }

        if (cls == CppSubclasses.projectiveTranslator) {
          val translator = CppSubclasses.translator
          code(s"constexpr ${translator.name} ${CppSubclasses.projectiveTranslator.name}::to${translator.name}() const noexcept { const double inv = 1.0 / s; return Translator{ .wx = wx * inv, .wy = wy * inv, .wz = wz * inv }; }")
        }
      }
    }

    Seq(FileContent(codeGen.directory.resolve("opsTo.h"), code.toString))
  }
