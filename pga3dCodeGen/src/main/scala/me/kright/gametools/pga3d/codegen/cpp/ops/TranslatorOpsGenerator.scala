package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.common.FileContent
import me.kright.gametools.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

/**
 * Generates simple operations for Translator similar to QuaternionOpsGenerator but only log().
 */
class TranslatorOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls != CppSubclasses.translator) return Seq()

    val code = new CppCodeBuilder()

    code(s"[[nodiscard]] static constexpr ${cls.name} id() noexcept { return {}; }")
    code(s"[[nodiscard]] static constexpr ${cls.name} addVector(const ${CppSubclasses.vector.name}& v) noexcept;")
    code(s"")
    code(s"[[nodiscard]] constexpr ${CppSubclasses.bivectorWeight.name} log() const noexcept;")
    code(s"[[nodiscard]] constexpr ${CppSubclasses.translator.name} pow(double p) const noexcept;")

    structBodyPart(code.toString())
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(Seq(s"#include \"${codeGen.Headers.types}\""), getClass.getName)

    val cls = CppSubclasses.translator

    code.namespace(codeGen.namespace) {
      code(
        s"""
           |[[nodiscard]] constexpr Translator Translator::addVector(const Vector& v) noexcept {
           |    return {
           |        .wx = -0.5 * v.x,
           |        .wy = -0.5 * v.y,
           |        .wz = -0.5 * v.z
           |    };
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] constexpr ${CppSubclasses.bivectorWeight.name} ${cls.name}::log() const noexcept {
           |    return ${CppSubclasses.bivectorWeight.name} {
           |        .wx = wx,
           |        .wy = wy,
           |        .wz = wz,
           |    };
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] constexpr ${CppSubclasses.translator.name} ${CppSubclasses.translator.name}::pow(double p) const noexcept {
           |   return (log() * p).exp();
           |}
           |""".stripMargin)
    }

    Seq(FileContent(codeGen.directory.resolve("opsTranslator.h"), code.toString))
  }
}
