package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.common.FileContent
import me.kright.gametools.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

class ProjectionOpsGenerator extends CppCodeGenerator:
  private val pointClasses = Set(
    CppSubclasses.projectivePoint,
    CppSubclasses.point,
    CppSubclasses.pointCenter,
  )

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val code = CppCodeBuilder()

    if (cls == CppSubclasses.bivector) {
      val line = cls.self

      for (planeClass <- Seq(CppSubclasses.plane, CppSubclasses.planeIdeal)) {
        code(s"[[nodiscard]] constexpr ${cls.name} projectOntoPlane(const ${planeClass.name}& plane) const noexcept;")
      }
    }

    if (pointClasses.contains(cls)) {
      for (planeClass <- Seq(CppSubclasses.plane, CppSubclasses.planeIdeal)) {
        code(s"[[nodiscard]] constexpr ${CppSubclasses.projectivePoint.name} projectOntoPlane(const ${planeClass.name}& plane) const noexcept;")
      }

      code(s"[[nodiscard]] constexpr ${CppSubclasses.projectivePoint.name} projectOntoLine(const ${CppSubclasses.bivector.name}& line) const noexcept;")
    }

    structBodyPart(code.toString)
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(
      Seq(
        s"#include \"${codeGen.Headers.types}\"",
        s"#include \"opsGeometric.h\"",
        s"#include \"opsDot.h\"",
      ),
      getClass.getName,
    )

    code.namespace(codeGen.namespace) {
      for (planeClass <- Seq(CppSubclasses.plane, CppSubclasses.planeIdeal)) {
        code(s"[[nodiscard]] constexpr ${CppSubclasses.bivector.name} ${CppSubclasses.bivector.name}::projectOntoPlane(const ${planeClass.name}& plane) const noexcept { return -plane.dot(*this).geometric(plane).toBivectorUnsafe(); }")
      }

      for (pointClass <- pointClasses) {
        for (planeClass <- Seq(CppSubclasses.plane, CppSubclasses.planeIdeal)) {
          code(s"[[nodiscard]] constexpr ${CppSubclasses.projectivePoint.name} ${pointClass.name}::projectOntoPlane(const ${planeClass.name}& plane) const noexcept { return plane.dot(*this).geometric(plane).toProjectivePointUnsafe(); };")
        }

        code(s"[[nodiscard]] constexpr ${CppSubclasses.projectivePoint.name} ${pointClass.name}::projectOntoLine(const ${CppSubclasses.bivector.name}& line) const noexcept { return -line.dot(*this).geometric(line).toProjectivePointUnsafe(); };")
      }
    }

    Seq(FileContent(codeGen.directory.resolve("opsProject.h"), code.toString))
  }
