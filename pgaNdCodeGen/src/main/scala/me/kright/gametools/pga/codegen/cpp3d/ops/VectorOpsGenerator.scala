package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import scala.collection.immutable.ArraySeq

class VectorOpsGenerator extends CppCodeGenerator {

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls == CppSubclasses.vector) {
      structBodyPart(
        s"""[[nodiscard]] constexpr ${CppSubclasses.vector.name} min(const ${CppSubclasses.vector.name}& other) const noexcept;
           |[[nodiscard]] constexpr ${CppSubclasses.vector.name} max(const ${CppSubclasses.vector.name}& other) const noexcept;
           |[[nodiscard]] constexpr ${CppSubclasses.vector.name} clamp(const ${CppSubclasses.vector.name}& minV, const ${CppSubclasses.vector.name}& maxV) const noexcept;
           |
           |// classical cross product for a right-handed basis: crossRightHanded(x, y) == z.
           |// It is not an operation of geometric algebra and exists for convenience and for adapting code
           |// from other libraries. In GA terms it is the join antiWedge(a, b), an ideal line, read back
           |// as a vector by the commutator product with the origin.
           |[[nodiscard]] static constexpr ${CppSubclasses.vector.name} crossRightHanded(const ${CppSubclasses.vector.name}& a, const ${CppSubclasses.vector.name}& b) noexcept;
           |
           |// classical cross product for a left-handed basis: crossLeftHanded(x, y) == -z,
           |// the negation of crossRightHanded. It is not an operation of geometric algebra.
           |[[nodiscard]] static constexpr ${CppSubclasses.vector.name} crossLeftHanded(const ${CppSubclasses.vector.name}& a, const ${CppSubclasses.vector.name}& b) noexcept;""".stripMargin
      )
    } else ArraySeq()
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = new CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        "#include <algorithm>",
        s"#include \"Vector.h\"",
        s"#include \"BivectorWeight.h\"",
        s"#include \"PointCenter.h\"",
      ),
      code.generatorName(this))

    code.namespace(codeGen.namespace) {
      code(
        s"""
           |[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.vector.name}::min(const ${CppSubclasses.vector.name}& other) const noexcept {
           |    return ${CppSubclasses.vector.name}{
           |        .x = std::min(x, other.x),
           |        .y = std::min(y, other.y),
           |        .z = std::min(z, other.z),
           |    };
           |}
           |
           |[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.vector.name}::max(const ${CppSubclasses.vector.name}& other) const noexcept {
           |    return ${CppSubclasses.vector.name}{
           |        .x = std::max(x, other.x),
           |        .y = std::max(y, other.y),
           |        .z = std::max(z, other.z),
           |    };
           |}
           |
           |[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.vector.name}::clamp(const ${CppSubclasses.vector.name}& minV, const ${CppSubclasses.vector.name}& maxV) const noexcept {
           |    return ${CppSubclasses.vector.name}{
           |        .x = std::clamp(x, minV.x, maxV.x),
           |        .y = std::clamp(y, minV.y, maxV.y),
           |        .z = std::clamp(z, minV.z, maxV.z),
           |    };
           |}
           |
           |[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.vector.name}::crossRightHanded(const ${CppSubclasses.vector.name}& a, const ${CppSubclasses.vector.name}& b) noexcept {
           |    return ${CppSubclasses.pointCenter.name}{}.cross(a.antiWedge(b));
           |}
           |
           |[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.vector.name}::crossLeftHanded(const ${CppSubclasses.vector.name}& a, const ${CppSubclasses.vector.name}& b) noexcept {
           |    return crossRightHanded(b, a);
           |}
           |""".stripMargin)
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsVector.h"), code.toString))
  }
}
