package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
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
      code.generatorName(this)
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
           |    const double lenXYZ2 = xy * xy + xz * xz + yz * yz;
           |    const double lenXYZ = std::sqrt(lenXYZ2);
           |    const double angle = std::atan2(lenXYZ, scalar);
           |
           |    // 1 / sin^2 for a normalized motor; (1.0 - scalar * scalar) is the same value,
           |    // but cancels catastrophically for small angles (relative error ~eps / angle^2)
           |    const double a = 1.0 / lenXYZ2;
           |
           |    // for a normalized motor sin(angle) = lenXYZ, so this is angle / sin(angle); the series branch:
           |    // x/sin(x) = 1 / (sin(x)/x) = 1 / (1 - x^2/6 + x^4/120 - ...);
           |    // substitute v = x^2/6 - x^4/120 + ... into 1/(1 - v) = 1 + v + v^2 + ...:
           |    //   x/sin(x) = 1 + x^2/6 + (1/36 - 1/120)*x^4 + ...
           |    //            = 1 + x^2/6 + 7*x^4/360 + ...
           |    // at x <= 1e-5 the dropped 7*x^4/360 <= 2e-22 relative term is far below 1e-17,
           |    // so the second-order form is exact in double
           |    const double b = (std::abs(angle) > 1e-5)
           |        ? (angle / lenXYZ)
           |        : (1.0 + angle * angle / 6.0);
           |
           |    // c = a * i * (1 - scalar * b); for a normalized motor scalar = cos(x), lenXYZ = sin(x),
           |    // a = 1/sin(x)^2 and b = x/sin(x), so c = i * (1 - cos(x)*b) / sin(x)^2. Step by step:
           |    //   cos(x)*b = (1 - x^2/2 + x^4/24 - ...) * (1 + x^2/6 + 7*x^4/360 + ...)
           |    //            = 1 + (1/6 - 1/2)*x^2 + (7/360 - 1/12 + 1/24)*x^4 + ...
           |    //            = 1 - x^2/3 - x^4/45 - ...
           |    //   1 - cos(x)*b = x^2/3 + x^4/45 + ...
           |    //   sin(x)^2 = (x - x^3/6 + ...)^2 = x^2 - x^4/3 + ... = x^2 * (1 - x^2/3 + ...)
           |    //   1/sin(x)^2 = (1 + x^2/3 + ...) / x^2   (again via 1/(1 - v) = 1 + v + ...)
           |    //   c/i = (x^2/3 + x^4/45 + ...) * (1 + x^2/3 + ...) / x^2
           |    //       = 1/3 + (1/9 + 1/45)*x^2 + ... = 1/3 + 2*x^2/15 + ...
           |    // carrying the x^4 terms through the same steps gives the dropped term 2*x^4/63;
           |    // at x <= 1e-5 it is <= 3.2e-22, relatively far below 1e-17, so the second-order
           |    // form is exact in double
           |    const double c = (std::abs(angle) > 1e-5)
           |        ? (a * i * (1.0 - scalar * b))
           |        : ((1.0 / 3.0 + angle * angle * (2.0 / 15.0)) * i);
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
