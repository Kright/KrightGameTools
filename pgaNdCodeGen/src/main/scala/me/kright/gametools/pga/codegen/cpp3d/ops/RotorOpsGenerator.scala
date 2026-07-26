package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.{FileContent, FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import me.kright.gametools.symbolic.Sym
import scala.collection.immutable.ArraySeq

class RotorOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val code = new CppCodeBuilder()

    if (CppSubclasses.rotor == cls) {
      code(s"[[nodiscard]] static inline ${cls.name} rotation(const ${CppSubclasses.vector.name}& from, const ${CppSubclasses.vector.name}& to) noexcept;")
      code(s"[[nodiscard]] static inline ${cls.name} rotation(const ${CppSubclasses.planeCentral.name}& from, const ${CppSubclasses.planeCentral.name}& to) noexcept;")
      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.bivectorBulk.name} log() const noexcept;")
      code(s"[[nodiscard]] inline ${CppSubclasses.rotor.name} pow(double p) const noexcept;")
      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.rotor.name} projectToRotationInPlane(const ${CppSubclasses.planeCentral.name}& plane) const noexcept;")
      code(s"[[nodiscard]] inline double restoreRotationInPlane(const ${CppSubclasses.planeCentral.name}& plane) const noexcept;")
      code(s"[[nodiscard]] inline double restoreRotationInPlaneX() const noexcept;")
      code(s"[[nodiscard]] inline double restoreRotationInPlaneY() const noexcept;")
      code(s"[[nodiscard]] inline double restoreRotationInPlaneZ() const noexcept;")
      code("")
      RotorAndMotorAxes.makeDeclaration(code, cls)
    }

    structBodyPart(code.toString)
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        "#include \"opsNorm.h\"",
        "#include \"opsArithmetic.h\"",
        "#include \"opsGeometric.h\"",
      ),
      code.generatorName(this)
    )
    
    val cls = CppSubclasses.rotor

    code.namespace(codeGen.namespace) {
      code(
        s"""namespace detail {
           |// a * b - c * d with a few ulp of relative error even when the products cancel almost exactly;
           |// std::fma(a, b, -p) extracts the exact rounding error of the product p = a * b
           |[[nodiscard]] inline double diffOfProducts(double a, double b, double c, double d) noexcept {
           |    const double p1 = a * b;
           |    const double p2 = c * d;
           |    return (p1 - p2) + (std::fma(a, b, -p1) - std::fma(c, d, -p2));
           |}
           |} // namespace detail
           |""".stripMargin)

      code(
        s"""[[nodiscard]] inline ${cls.name} ${cls.name}::rotation(const ${CppSubclasses.vector.name}& from, const ${CppSubclasses.vector.name}& to) noexcept {
           |    return rotation(from.dual(), to.dual());
           |}""".stripMargin)

      code("")
      code(s"[[nodiscard]] inline ${cls.name} ${cls.name}::rotation(const ${CppSubclasses.planeCentral.name}& from, const ${CppSubclasses.planeCentral.name}& to) noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.rotorRotation))
      }
      code("}")

      code(s"[[nodiscard]] inline ${CppSubclasses.bivectorBulk.name} ${cls.name}::log() const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.rotorLog))
        code("")
        code(s"return ${CppSubclasses.bivectorBulk.name} ${CppSubclasses.bivectorBulk.makeBracesInit(SharedFormulas.rotorLogResult(cls.self), multiline = true)};")
      }
      code("}")
      code("")

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.rotor.name} ${CppSubclasses.rotor.name}::pow(double p) const noexcept {
           |   return (log() * p).exp();
           |}
           |""".stripMargin)

      code(s"[[nodiscard]] inline ${CppSubclasses.rotor.name} ${CppSubclasses.rotor.name}::projectToRotationInPlane(const ${CppSubclasses.planeCentral.name}& plane) const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.rotorProjectToRotationInPlane))
      }
      code("}")
      code("")

      code(s"[[nodiscard]] inline double ${CppSubclasses.rotor.name}::restoreRotationInPlane(const ${CppSubclasses.planeCentral.name}& plane) const noexcept {")
      code.block {
        code(FormulaTemplate.renderCpp(SharedFormulas.rotorRestoreRotationInPlane))
      }
      code("}")
      code("")

      code(
        s"""[[nodiscard]] inline double ${CppSubclasses.rotor.name}::restoreRotationInPlaneX() const noexcept { return restoreRotationInPlane(${CppSubclasses.planeCentral.name}(1, 0, 0)); }
           |[[nodiscard]] inline double ${CppSubclasses.rotor.name}::restoreRotationInPlaneY() const noexcept { return restoreRotationInPlane(${CppSubclasses.planeCentral.name}(0, 1, 0)); }
           |[[nodiscard]] inline double ${CppSubclasses.rotor.name}::restoreRotationInPlaneZ() const noexcept { return restoreRotationInPlane(${CppSubclasses.planeCentral.name}(0, 0, 1)); }
           |""".stripMargin)

      code("")
      RotorAndMotorAxes.makeForRotor(code)
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsRotor.h"), code.toString))
  }
}


