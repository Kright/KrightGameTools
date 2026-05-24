package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.common.FileContent
import me.kright.gametools.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import TranslatorWithQuaternionGenerator.quaternionWithTranslator
import TranslatorWithQuaternionGenerator.translatorWithQuaternion

class MotorOpsGenerator extends CppCodeGenerator {
  // No declarations yet. Ready to add methods in the future.
  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls != CppSubclasses.motor) return Seq()

    val includes =
      if (cls == CppSubclasses.bivector) Seq("<utility>")
      else Seq()

    val code = new CppCodeBuilder()
    code(s"[[nodiscard]] static constexpr ${CppSubclasses.motor.name} id() noexcept { return { .s = 1.0 }; };")
    code("")
    code(s"[[nodiscard]] static constexpr ${CppSubclasses.motor.name} addVector(const ${CppSubclasses.vector.name}& v) noexcept;")
    code("")
    code(s"[[nodiscard]] inline ${CppSubclasses.bivector.name} log() const noexcept;")
    code(s"[[nodiscard]] inline ${CppSubclasses.motor.name} pow(double p) const noexcept;")
    code("")
    code(s"[[nodiscard]] constexpr ${quaternionWithTranslator} to${quaternionWithTranslator}() const noexcept;")
    code(s"[[nodiscard]] constexpr ${translatorWithQuaternion} to${translatorWithQuaternion}() const noexcept;")
    code("")
    code(s"[[nodiscard]] inline ${CppSubclasses.motor.name} renormalized() const noexcept;")
    code("")
    QuaternionAndMotorAxes.makeDeclaration(code, cls)

    structBodyPart(code.toString, includes)
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      Seq(
        s"#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        s"#include \"opsArithmetic.h\"",
      ),
      getClass.getName
    )

    code.namespace(codeGen.namespace) {
      code(
        s"""
           |[[nodiscard]] constexpr ${CppSubclasses.motor.name} ${CppSubclasses.motor.name}::addVector(const ${CppSubclasses.vector.name}& v) noexcept { return {.s = 1.0, .wx = v.x, .wy = v.y, .wz = v.z}; }""".stripMargin)

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.bivector.name} ${CppSubclasses.motor.name}::log() const noexcept {
           |    const double scalar = s;
           |    if (s < 0.0) return (-(*this)).log();
           |
           |    const double lenXYZ = std::sqrt(xy * xy + xz * xz + yz * yz);
           |    const double angle = std::atan2(lenXYZ, scalar);
           |
           |    // 1 / sin^2
           |    const double a = 1.0 / (1.0 - scalar * scalar);
           |
           |    // angle / sin(angle)
           |    const double b = (std::abs(angle) > 1e-5)
           |        ? (angle * std::sqrt(a))
           |        : (1.0 + angle * angle / 6.0);
           |
           |    const double c = (std::abs(angle) > 1e-5)
           |        ? (a * i * (1.0 - scalar * b))
           |        : ((1.0 + angle * angle / 2.0) * i / 3.0);
           |
           |    return ${CppSubclasses.bivector.name} {
           |        .wx = (b * wx + c * yz),
           |        .wy = (b * wy - c * xz),
           |        .wz = (b * wz + c * xy),
           |        .xy = b * xy,
           |        .xz = b * xz,
           |        .yz = b * yz,
           |    };
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.motor.name} ${CppSubclasses.motor.name}::pow(double p) const noexcept {
           |   return (log() * p).exp();
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] constexpr ${quaternionWithTranslator} ${CppSubclasses.motor.name}::to${quaternionWithTranslator}() const noexcept {
           |    return to${translatorWithQuaternion}().to${quaternionWithTranslator}();
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] constexpr ${translatorWithQuaternion} ${CppSubclasses.motor.name}::to${translatorWithQuaternion}() const noexcept {
           |    const Quaternion q = toQuaternionUnsafe();
           |    const Vector shift = sandwich(PointCenter{}).toPoint().toVectorUnsafe();
           |    const Translator t = Translator::addVector(shift);
           |    return { t, q };
           |}
           |""".stripMargin)

      code(
        s"""
           |/**
           | * see [[https://arxiv.org/abs/2206.07496]], page 14
           | * and [[https://https://bivector.net/PGAdyn.pdf.net/PGAdyn.pdf]], page 42
           | */
           |[[nodiscard]] inline ${CppSubclasses.motor.name} ${CppSubclasses.motor.name}::renormalized() const noexcept {
           |    const double a2 = 1.0 / (s * s + xy * xy + xz * xz + yz * yz);
           |    const double a = std::sqrt(a2);
           |    const double b = (s * i - wx * yz + wy * xz - wz * xy) * a * a2;
           |    return ${CppSubclasses.motor.name} {
           |        .s = a * s,
           |        .wx = a * wx + b * yz,
           |        .wy = a * wy - b * xz,
           |        .wz = a * wz + b * xy,
           |        .xy = a * xy,
           |        .xz = a * xz,
           |        .yz = a * yz,
           |        .i = a * i - b * s,
           |    };
           |}
           |""".stripMargin)

      code("")
      QuaternionAndMotorAxes.makeForMotor(code)
    }

    Seq(FileContent(codeGen.directory.resolve("opsMotor.h"), code.toString))
  }
}
