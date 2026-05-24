package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.common.FileContent
import me.kright.gametools.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

class PointOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls == CppSubclasses.point) {
      structBodyPart(
        s"""[[nodiscard]] inline double distanceTo(const ${CppSubclasses.point.name}& other) const noexcept;
           |
           |[[nodiscard]] constexpr ${CppSubclasses.point.name} min(const ${CppSubclasses.point.name}& other) const noexcept;
           |[[nodiscard]] constexpr ${CppSubclasses.point.name} max(const ${CppSubclasses.point.name}& other) const noexcept;
           |[[nodiscard]] constexpr ${CppSubclasses.point.name} clamp(const ${CppSubclasses.point.name}& minV, const ${CppSubclasses.point.name}& maxV) const noexcept;""".stripMargin
      )
    } else Seq()
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      Seq(
        "#include <algorithm>",
        s"#include \"${codeGen.Headers.types}\"",
        "#include \"opsArithmetic.h\"",
        "#include \"opsNorm.h\"",
      ),
      getClass.getName
    )

    code.namespace(codeGen.namespace) {
      code(
        s"""
           |[[nodiscard]] inline double ${CppSubclasses.point.name}::distanceTo(const ${CppSubclasses.point.name}& other) const noexcept {
           |    return ((*this) - other).norm();
           |}
           |
           |[[nodiscard]] constexpr ${CppSubclasses.point.name} ${CppSubclasses.point.name}::min(const ${CppSubclasses.point.name}& other) const noexcept {
           |    return ${CppSubclasses.point.name}{
           |        .x = std::min(x, other.x),
           |        .y = std::min(y, other.y),
           |        .z = std::min(z, other.z),
           |    };
           |}
           |
           |[[nodiscard]] constexpr ${CppSubclasses.point.name} ${CppSubclasses.point.name}::max(const ${CppSubclasses.point.name}& other) const noexcept {
           |    return ${CppSubclasses.point.name}{
           |        .x = std::max(x, other.x),
           |        .y = std::max(y, other.y),
           |        .z = std::max(z, other.z),
           |    };
           |}
           |
           |[[nodiscard]] constexpr ${CppSubclasses.point.name} ${CppSubclasses.point.name}::clamp(const ${CppSubclasses.point.name}& minV, const ${CppSubclasses.point.name}& maxV) const noexcept {
           |    return ${CppSubclasses.point.name}{
           |        .x = std::clamp(x, minV.x, maxV.x),
           |        .y = std::clamp(y, minV.y, maxV.y),
           |        .z = std::clamp(z, minV.z, maxV.z),
           |    };
           |}
           |""".stripMargin)
    }

    Seq(FileContent(codeGen.directory.resolve("opsPoint.h"), code.toString))
  }
}
