package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.pga.codegen.common.{FileContent, NormSymbolics}
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import me.kright.gametools.symbolic.Sym

import scala.collection.immutable.ArraySeq

class NormOpGenerator extends CppCodeGenerator {
  private case class Family(squareName: String, normName: String, normalizedName: String, square: MultiVector[Sym]) {
    def isConstantOne: Boolean = NormSymbolics.isConstantOne(square)
  }

  private def families(cls: CppSubclass): Seq[Family] = {
    val self = cls.self
    ArraySeq(
      Family("normSquare", "norm", "normalizedByNorm", NormSymbolics.fullSquare(self)),
      Family("bulkNormSquare", "bulkNorm", "normalizedByBulk", NormSymbolics.bulkSquare(self)),
      Family("weightNormSquare", "weightNorm", "normalizedByWeight", NormSymbolics.weightSquare(self)),
    ).filter(_.square.values.nonEmpty)
  }

  private def normalizedResultCls(cls: CppSubclass): CppSubclass =
    CppSubclasses.findMatchingClass(cls.makeSymbolic("a") * Sym("b"))

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\"",
        s"#include \"opsArithmetic.h\"",
      ),
      code.generatorName(this),
    )

    code.namespace(codeGen.namespace) {
      for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
        val resultCls = normalizedResultCls(cls)

        for (f <- families(cls)) {
          if (f.isConstantOne) {
            code(s"constexpr double ${cls.name}::${f.squareName}() const noexcept { return 1.0; }")
            code(s"inline double ${cls.name}::${f.normName}() const noexcept { return 1.0; }")
            code(s"inline ${cls.name} ${cls.name}::${f.normalizedName}() const noexcept { return *this; }")
          } else {
            val expr = f.square.values.values.head
            code(s"constexpr double ${cls.name}::${f.squareName}() const noexcept { return ${expr.toString}; }")
            code(s"inline double ${cls.name}::${f.normName}() const noexcept { return std::sqrt(${f.squareName}()); }")
            if (resultCls != CppSubclasses.zeroCls) {
              code(s"inline ${resultCls.name} ${cls.name}::${f.normalizedName}() const noexcept { return *this / ${f.normName}(); }")
            }
          }
          code("")
        }
      }
    }

    ArraySeq(FileContent(codeGen.directory.resolve("opsNorm.h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val resultCls = normalizedResultCls(cls)

    val parts = families(cls).flatMap { f =>
      val normalizedDecl =
        if (f.isConstantOne) Some(s"[[nodiscard]] inline ${cls.name} ${f.normalizedName}() const noexcept;")
        else if (resultCls != CppSubclasses.zeroCls) Some(s"[[nodiscard]] inline ${resultCls.name} ${f.normalizedName}() const noexcept;")
        else None

      ArraySeq(
        Some(s"[[nodiscard]] constexpr double ${f.squareName}() const noexcept;"),
        Some(s"[[nodiscard]] inline double ${f.normName}() const noexcept;"),
        normalizedDecl,
      ).flatten
    }

    structBodyPart(parts.mkString("\n"))
  }
}
