package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import scala.collection.immutable.ArraySeq

class BivectorOpsGenerator extends CppCodeGenerator {

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        s"#include \"opsArithmetic.h\"",
        s"#include \"opsNorm.h\""
      ),
      code.generatorName(this)
    )

    code.namespace(codeGen.namespace) {
      val bivector = CppSubclasses.bivector
      val bivectorWeight = CppSubclasses.bivectorWeight

      code(
        s"""
           |inline std::pair<${bivector.name}, ${bivectorWeight.name}> ${bivector.name}::split() const noexcept {
           |    const double div = bulkNormSquare();
           |    if (div < 1e-100) {
           |        return {
           |            ${bivector.name}{ .wx = 0.0, .wy = 0.0, .wz = 0.0, .xy = xy, .xz = xz, .yz = yz },
           |            ${bivectorWeight.name}{ .wx = wx, .wy = wy, .wz = wz }
           |        };
           |    }
           |
           |    const double pseudoScalar = (wy * xz - wx * yz - wz * xy) / div;
           |    const ${bivectorWeight.name} shiftAlongLine{
           |        .wx = -pseudoScalar * yz,
           |        .wy = pseudoScalar * xz,
           |        .wz = -pseudoScalar * xy
           |    };
           |
           |    const ${bivector.name} line = (*this) - shiftAlongLine;
           |    return {line, shiftAlongLine};
           |}
           |
           |inline ${CppSubclasses.motor.name} ${bivector.name}::exp() const noexcept {
           |    const double len = bulkNorm();
           |    const double cos = std::cos(len);
           |
           |    // sin(x)/x = 1 - x^2/6 + x^4/120 - ...; at x <= 1e-5 the dropped x^4/120 <= 8.4e-23
           |    // relative term is far below 1e-17, so the second-order form is exact in double
           |    const double sinDivLen = (len > 1e-5) ?
           |        (std::sin(len) / len) :
           |        (1.0 - (len * len) / 6.0);
           |
           |    // (sin(x)/x - cos(x)) / x^2, step by step:
           |    //   sin(x)   = x - x^3/6 + x^5/120 - x^7/5040 + ...
           |    //   sin(x)/x = 1 - x^2/6 + x^4/120 - x^6/5040 + ...
           |    //   cos(x)   = 1 - x^2/2 + x^4/24  - x^6/720  + ...
           |    //   sin(x)/x - cos(x) = (1/2 - 1/6)*x^2 + (1/120 - 1/24)*x^4 + (1/720 - 1/5040)*x^6 + ...
           |    //                     = x^2/3 - x^4/30 + x^6/840 - ...
           |    //   divide by x^2:      1/3   - x^2/30 + x^4/840 - ...
           |    // at x <= 1e-5 the dropped x^4/840 <= 1.2e-23 is relatively far below 1e-17,
           |    // so the second-order form is exact in double
           |    const double sinMinusCosDivLen2 = (len > 1e-5) ?
           |        (sinDivLen - cos) / (len * len) :
           |        (1.0 / 3.0 - (len * len) / 30.0);
           |
           |    return ${CppSubclasses.motor.name} {
           |      .s = cos,
           |      .wx = (sinDivLen * wx + sinMinusCosDivLen2 * yz * (wy * xz - wx * yz - wz * xy)),
           |      .wy = (sinDivLen * wy + sinMinusCosDivLen2 * xz * (wx * yz + wz * xy - wy * xz)),
           |      .wz = (sinDivLen * wz + sinMinusCosDivLen2 * xy * (wy * xz - wx * yz - wz * xy)),
           |      .xy = sinDivLen * xy,
           |      .xz = sinDivLen * xz,
           |      .yz = sinDivLen * yz,
           |      .i = sinDivLen * (wx * yz + wz * xy - wy * xz),
           |    };
           |}
           |""".stripMargin
      )
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsBivector.h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val includes = if (cls == CppSubclasses.bivector) {
      ArraySeq("<utility>")
    } else {
      Seq.empty
    }

    val code = if (cls == CppSubclasses.bivector) {
      s"""[[nodiscard]] inline std::pair<${cls.name}, ${CppSubclasses.bivectorWeight.name}> split() const noexcept;
         |
         |[[nodiscard]] inline ${CppSubclasses.motor.name} exp() const noexcept;""".stripMargin
    } else ""
    
    structBodyPart(code, includes)
  }
}
