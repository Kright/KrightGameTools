package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.{FileContent, FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import TranslatorWithRotorGenerator.rotorWithTranslator
import TranslatorWithRotorGenerator.translatorWithRotor
import scala.collection.immutable.ArraySeq

class MotorOpsGenerator extends CppCodeGenerator {
  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls != CppSubclasses.motor) return ArraySeq()

    val code = new CppCodeBuilder()
    code(s"[[nodiscard]] static constexpr ${CppSubclasses.motor.name} id() noexcept { return { .s = 1.0 }; };")
    code("")
    code(s"[[nodiscard]] static constexpr ${CppSubclasses.motor.name} addVector(const ${CppSubclasses.vector.name}& v) noexcept;")
    code("")
    code(s"[[nodiscard]] inline ${CppSubclasses.bivector.name} log() const noexcept;")
    code(s"[[nodiscard]] inline ${CppSubclasses.motor.name} pow(double p) const noexcept;")
    code("")
    code(s"[[nodiscard]] constexpr ${rotorWithTranslator} to${rotorWithTranslator}() const noexcept;")
    code(s"[[nodiscard]] constexpr ${translatorWithRotor} to${translatorWithRotor}() const noexcept;")
    code("")
    code(s"[[nodiscard]] inline ${CppSubclasses.motor.name} renormalized() const noexcept;")
    code("")
    RotorAndMotorAxes.makeDeclaration(code, cls)

    structBodyPart(code.toString, ArraySeq())
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        s"#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        s"#include \"opsArithmetic.h\"",
      ),
      code.generatorName(this)
    )

    code.namespace(codeGen.namespace) {
      code(
        s"""
           |[[nodiscard]] constexpr ${CppSubclasses.motor.name} ${CppSubclasses.motor.name}::addVector(const ${CppSubclasses.vector.name}& v) noexcept { return {.s = 1.0, .wx = v.x, .wy = v.y, .wz = v.z}; }""".stripMargin)

      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.bivector.name} ${CppSubclasses.motor.name}::log() const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.motorLog))
        code("")
        code(s"return ${CppSubclasses.bivector.name} ${CppSubclasses.bivector.makeBracesInit(SharedFormulas.motorLogResult(CppSubclasses.motor.self), multiline = true)};")
      }
      code("}")
      code("")

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.motor.name} ${CppSubclasses.motor.name}::pow(double p) const noexcept {
           |   return (log() * p).exp();
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] constexpr ${rotorWithTranslator} ${CppSubclasses.motor.name}::to${rotorWithTranslator}() const noexcept {
           |    return to${translatorWithRotor}().to${rotorWithTranslator}();
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] constexpr ${translatorWithRotor} ${CppSubclasses.motor.name}::to${translatorWithRotor}() const noexcept {
           |    const Rotor q = toRotorUnsafe();
           |    const Vector shift = sandwich(PointCenter{}).toPoint().toVectorUnsafe();
           |    const Translator t = Translator::addVector(shift);
           |    return { t, q };
           |}
           |""".stripMargin)

      code(
        s"""
           |/**
           | * see [[https://arxiv.org/abs/2206.07496]], page 14
           | * and [[https://bivector.net/PGAdyn.pdf]], page 42
           | */""".stripMargin)
      code(s"[[nodiscard]] inline ${CppSubclasses.motor.name} ${CppSubclasses.motor.name}::renormalized() const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.motorRenormalized))
        code(s"return ${CppSubclasses.motor.name} ${CppSubclasses.motor.makeBracesInit(SharedFormulas.motorRenormalizedResult(CppSubclasses.motor.self), multiline = true)};")
      }
      code("}")
      code("")

      code("")
      RotorAndMotorAxes.makeForMotor(code)
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsMotor.h"), code.toString))
  }
}
