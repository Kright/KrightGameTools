package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.cpp.Pga3dProvider.pga3
import me.kright.gametools.pga3d.codegen.cpp.*

class StructFieldsGenerator extends CppCodeGenerator:
  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] =
    val code = new CppCodeBuilder()

    cls.variableFields.foreach { field =>
      code(s"double ${field.name} = 0.0;")
    }

    code("")
    code(s"static constexpr size_t componentsCount = ${cls.variableFields.size};")

    if (cls.constantFields.nonEmpty) {
      code("")
      cls.constantFields.foreach { (field, constValue) =>
        code(s"[[nodiscard]] constexpr double ${field.name}() const noexcept { return ${constValue}; }")
      }
    }

    if (Set(CppSubclasses.vector, CppSubclasses.point, CppSubclasses.projectivePoint, CppSubclasses.pointCenter).contains(cls)) {
      code("")

      cls.self.values.foreach { (b, sym) =>
        val fName = s"${pga3.representation(b)}"

        code(s"[[nodiscard]] constexpr double ${fName}() const noexcept { return ${sym}; }")
      }
    }

    code("")
    code(s"[[nodiscard]] constexpr std::array<double, componentsCount> toArray() const noexcept { return { ${cls.variableFields.map(_.name).mkString(", ")} }; }")
    code(s"[[nodiscard]] static constexpr ${cls.name} from(const std::array<double, componentsCount>& values) noexcept { return { ${cls.variableFields.zipWithIndex.map((f, i) => s".${f.name} = values[$i]").mkString(", ")} }; }")
    code(s"[[nodiscard]] static constexpr ${cls.name} from(const std::span<double, componentsCount>& values) noexcept { return { ${cls.variableFields.zipWithIndex.map((f, i) => s".${f.name} = values[$i]").mkString(", ")} }; }")

    val includes =
      if (cls.variableFields.nonEmpty) Seq("<array>", "<span>")
      else Seq()

    structBodyPart(
      code.toString,
      includes,
    )
