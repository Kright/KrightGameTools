package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import me.kright.gametools.symbolic.Sym

class QuaternionOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val code = new CppCodeBuilder()

    if (CppSubclasses.quaternion == cls) {
      code(s"[[nodiscard]] static inline ${cls.name} rotation(const ${CppSubclasses.vector.name}& from, const ${CppSubclasses.vector.name}& to) noexcept;")
      code(s"[[nodiscard]] static inline ${cls.name} rotation(const ${CppSubclasses.planeIdeal.name}& from, const ${CppSubclasses.planeIdeal.name}& to) noexcept;")
      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.bivectorBulk.name} log() const noexcept;")
      code(s"[[nodiscard]] inline ${CppSubclasses.quaternion.name} pow(double p) const noexcept;")
      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.quaternion.name} projectToRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept;")
      code(s"[[nodiscard]] inline double restoreRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept;")
      code("")
      QuaternionAndMotorAxes.makeDeclaration(code, cls)
    }

    structBodyPart(code.toString)
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      Seq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        "#include \"opsNorm.h\"",
        "#include \"opsArithmetic.h\"",
        "#include \"opsGeometric.h\"",
      ),
      code.generatorName(this)
    )
    
    val cls = CppSubclasses.quaternion

    code.namespace(codeGen.namespace) {
      code(
        s"""[[nodiscard]] inline ${cls.name} ${cls.name}::rotation(const ${CppSubclasses.vector.name}& from, const ${CppSubclasses.vector.name}& to) noexcept {
           |    return rotation(from.dual(), to.dual());
           |}""".stripMargin)

      code(
        s"""
           |[[nodiscard]] inline ${cls.name} ${cls.name}::rotation(const ${CppSubclasses.planeIdeal.name}& from, const ${CppSubclasses.planeIdeal.name}& to) noexcept {
           |    const double norm = std::sqrt(from.normSquare() * to.normSquare());
           |    const Quaternion q2a = to.geometric(from) / norm;
           |    const double dot = q2a.s;
           |
           |    if (dot > -1.0 + 1e-6) {
           |        const double newCos = std::sqrt((1.0 + dot) / 2);
           |        const double newSinDivSin2 = 0.5 / newCos;
           |        return Quaternion(newCos, q2a.xy * newSinDivSin2, q2a.xz * newSinDivSin2, q2a.yz * newSinDivSin2);
           |    }
           |
           |    const double sin2a = std::sqrt(q2a.xy * q2a.xy + q2a.xz * q2a.xz + q2a.yz * q2a.yz);
           |
           |    if (sin2a > 1e-8) {
           |        const double angle2 = std::atan2(sin2a, q2a.s);
           |        const double propAngle = angle2 * 0.5;
           |        const double mult = std::sin(propAngle) / sin2a;
           |        return Quaternion(std::cos(propAngle), q2a.xy * mult, q2a.xz * mult, q2a.yz * mult).normalizedByNorm();
           |    }
           |
           |    // choose any axis
           |    const PlaneIdeal orthogonalPlane =
           |        (std::abs(from.x) > std::abs(from.z)) ? PlaneIdeal{-from.y, from.x, 0} : PlaneIdeal{0, -from.z, from.y};
           |
           |    return Quaternion(0, orthogonalPlane.z, -orthogonalPlane.y, orthogonalPlane.x).normalizedByNorm();
           |}""".stripMargin)

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.bivectorBulk.name} ${cls.name}::log() const noexcept {
           |    const double scalar = s;
           |    if (s < 0.0) return (-(*this)).log();
           |
           |    const double lenXYZ = std::sqrt(xy * xy + xz * xz + yz * yz);
           |    const double angle = std::atan2(lenXYZ, scalar);
           |
           |     // 1 / sin^2
           |    const double a = 1.0 / (1.0 - scalar * scalar);
           |
           |    // angle / sin(angle)
           |    const double b = (std::abs(angle) > 1e-5) ? angle * std::sqrt(a) : (1.0 + angle * angle / 6.0);
           |
           |    return ${CppSubclasses.bivectorBulk.name} {
           |        .xy = b * xy,
           |        .xz = b * xz,
           |        .yz = b * yz,
           |    };
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.quaternion.name} ${CppSubclasses.quaternion.name}::pow(double p) const noexcept {
           |   return (log() * p).exp();
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.quaternion.name} ${CppSubclasses.quaternion.name}::projectToRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept {
           |    const ${CppSubclasses.quaternion.name} q = normalizedByNorm();
           |    const ${CppSubclasses.quaternion.name} qPart = ${CppSubclasses.quaternion.name}::rotation(q.sandwich(plane), plane);
           |    return qPart.geometric(q);
           |}
           |
           |[[nodiscard]] inline double ${CppSubclasses.quaternion.name}::restoreRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept {
           |    const ${CppSubclasses.quaternion.name} q0 = projectToRotationInPlane(plane);
           |    const ${CppSubclasses.bivectorWeight.name} logDual = q0.log().dual();
           |    const double currentAngle = 2.0 * (logDual.wx * plane.x + logDual.wy * plane.y + logDual.wz * plane.z) / plane.norm();
           |    return currentAngle;
           |}
           |""".stripMargin)

      code("")
      QuaternionAndMotorAxes.makeForQuaternion(code)
    }

    Seq(FileContent(codeGen.directory.resolve("opsQuaternion.h"), code.toString))
  }
}


