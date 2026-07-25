package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import me.kright.gametools.symbolic.Sym

class RotorOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val code = new CppCodeBuilder()

    if (CppSubclasses.rotor == cls) {
      code(s"[[nodiscard]] static inline ${cls.name} rotation(const ${CppSubclasses.vector.name}& from, const ${CppSubclasses.vector.name}& to) noexcept;")
      code(s"[[nodiscard]] static inline ${cls.name} rotation(const ${CppSubclasses.planeIdeal.name}& from, const ${CppSubclasses.planeIdeal.name}& to) noexcept;")
      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.bivectorBulk.name} log() const noexcept;")
      code(s"[[nodiscard]] inline ${CppSubclasses.rotor.name} pow(double p) const noexcept;")
      code("")
      code(s"[[nodiscard]] inline ${CppSubclasses.rotor.name} projectToRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept;")
      code(s"[[nodiscard]] inline double restoreRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept;")
      code("")
      RotorAndMotorAxes.makeDeclaration(code, cls)
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

      code(
        s"""
           |[[nodiscard]] inline ${cls.name} ${cls.name}::rotation(const ${CppSubclasses.planeIdeal.name}& from, const ${CppSubclasses.planeIdeal.name}& to) noexcept {
           |    // not std::sqrt(from.normSquare() * to.normSquare()): the product overflows/underflows
           |    // for extreme magnitudes (~1e100 or ~1e-100) where each norm alone is still fine
           |    const double norm = from.norm() * to.norm();
           |    const Rotor q2a = to.geometric(from) / norm;
           |    const double dot = q2a.s;
           |
           |    // the -0.9 threshold keeps (1.0 + dot) >= 0.1, so the half-angle branch loses
           |    // at most ~2e-15 relative to the dot rounding; angles closer to pi take the
           |    // exact-wedge branch below, which stays ~1e-15 all the way to pi
           |    if (dot > -0.9) {
           |        const double newCos = std::sqrt((1.0 + dot) / 2);
           |        const double newSinDivSin2 = 0.5 / newCos;
           |        return Rotor(newCos, q2a.xy * newSinDivSin2, q2a.xz * newSinDivSin2, q2a.yz * newSinDivSin2);
           |    }
           |
           |    // near pi the wedge components of q2a cancel catastrophically (~1e-17 absolute
           |    // noise, which would tilt the axis by ~1e-17/sin2a), so the axis is recomputed
           |    // with error-free products
           |    const double invNorm = 1.0 / norm;
           |    const double bxy = detail::diffOfProducts(from.y, to.x, from.x, to.y) * invNorm;
           |    const double bxz = detail::diffOfProducts(from.z, to.x, from.x, to.z) * invNorm;
           |    const double byz = detail::diffOfProducts(from.z, to.y, from.y, to.z) * invNorm;
           |    const double sin2a = std::sqrt(bxy * bxy + bxz * bxz + byz * byz);
           |
           |    if (sin2a > 0.0) {
           |        // rotation by (pi - eps): the dot guard bounds sin2a <= sin(acos(0.9)) ~ 0.44,
           |        // where asin is well-conditioned - unlike atan2 near pi, whose ~ulp(pi)
           |        // absolute error would be ~1e-16/eps relative in s
           |        const double eps = std::asin(sin2a);
           |        const double axisMult = std::cos(eps * 0.5) / sin2a;
           |        return Rotor(std::sin(eps * 0.5), bxy * axisMult, bxz * axisMult, byz * axisMult);
           |    }
           |
           |    // exactly antipodal inputs: the axis is any direction orthogonal to from
           |    const PlaneIdeal orthogonalPlane =
           |        (std::abs(from.x) > std::abs(from.z)) ? PlaneIdeal{-from.y, from.x, 0} : PlaneIdeal{0, -from.z, from.y};
           |
           |    return Rotor(0, orthogonalPlane.z, -orthogonalPlane.y, orthogonalPlane.x).normalizedByNorm();
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
           |    // for a normalized rotor sin(angle) = lenXYZ, so this is angle / sin(angle);
           |    // dividing by lenXYZ directly avoids the catastrophic cancellation that the
           |    // equivalent sqrt(1.0 - scalar * scalar) form has for small angles. The series branch:
           |    // x/sin(x) = 1 / (sin(x)/x) = 1 / (1 - x^2/6 + x^4/120 - ...);
           |    // substitute v = x^2/6 - x^4/120 + ... into 1/(1 - v) = 1 + v + v^2 + ...:
           |    //   x/sin(x) = 1 + x^2/6 + (1/36 - 1/120)*x^4 + ...
           |    //            = 1 + x^2/6 + 7*x^4/360 + ...
           |    // at x <= 1e-5 the dropped 7*x^4/360 <= 2e-22 relative term is far below 1e-17,
           |    // so the second-order form is exact in double
           |    const double b = (std::abs(angle) > 1e-5) ? (angle / lenXYZ) : (1.0 + angle * angle / 6.0);
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
           |[[nodiscard]] inline ${CppSubclasses.rotor.name} ${CppSubclasses.rotor.name}::pow(double p) const noexcept {
           |   return (log() * p).exp();
           |}
           |""".stripMargin)

      code(
        s"""
           |[[nodiscard]] inline ${CppSubclasses.rotor.name} ${CppSubclasses.rotor.name}::projectToRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept {
           |    const ${CppSubclasses.rotor.name} q = normalizedByNorm();
           |    const ${CppSubclasses.rotor.name} qPart = ${CppSubclasses.rotor.name}::rotation(q.sandwich(plane), plane);
           |    return qPart.geometric(q);
           |}
           |
           |[[nodiscard]] inline double ${CppSubclasses.rotor.name}::restoreRotationInPlane(const ${CppSubclasses.planeIdeal.name}& plane) const noexcept {
           |    const ${CppSubclasses.rotor.name} q0 = projectToRotationInPlane(plane);
           |    const ${CppSubclasses.bivectorWeight.name} logDual = q0.log().dual();
           |    const double currentAngle = 2.0 * (logDual.wx * plane.x + logDual.wy * plane.y + logDual.wz * plane.z) / plane.norm();
           |    return currentAngle;
           |}
           |""".stripMargin)

      code("")
      RotorAndMotorAxes.makeForRotor(code)
    }

    Seq(FileContent(codeGen.directory.resolve("opsRotor.h"), code.toString))
  }
}


