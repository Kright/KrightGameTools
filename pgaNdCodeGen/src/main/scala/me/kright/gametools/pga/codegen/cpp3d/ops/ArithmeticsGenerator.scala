package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import me.kright.gametools.symbolic.Sym

class ArithmeticsGenerator extends CppCodeGenerator:
  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(
      Seq(
        "#include <cmath>",
        s"#include \"${codeGen.Headers.types}\""
      ),
      code.generatorName(this)
    )

    code.namespace(codeGen.namespace) {
      plusMinus(code)
      code("")
      unaryMinus(code)
      code("")
      code("")
      multiplyOrDivideByScalar(code)
      code("")
      madd(code)
      code("")
      code("")
      equality(code)
    }

    Seq(FileContent(codeGen.directory.resolve("opsArithmetic.h"), code.toString))
  }

  private def multiplyOrDivideByScalar(code: CppCodeBuilder): Unit = {
    import me.kright.gametools.symbolic.Sym

    for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
      val a = cls.makeSymbolic("a")
      val result = a * Sym("d")
      val resultCls = CppSubclasses.findMatchingClass(result)
      code(s"[[nodiscard]] constexpr ${resultCls.name} operator*(const ${cls.name}& a, double d) noexcept { return ${resultCls.makeBracesInit(result, multiline = true)}; }")
      code(s"[[nodiscard]] constexpr ${resultCls.name} operator*(double d, const ${cls.name}& a) noexcept { return a * d; }")
      code(s"[[nodiscard]] constexpr ${resultCls.name} operator/(const ${cls.name}& a, double d) noexcept { return a * (1.0 / d); }")

      if (resultCls == cls) {
        code(s"constexpr ${resultCls.name}& operator*=(${cls.name}& a, double d) noexcept { a = a * d; return a; }")
        code(s"constexpr ${resultCls.name}& operator/=(${cls.name}& a, double d) noexcept { a = a / d; return a; }")
      }

      code("")
    }
  }

  private def unaryMinus(code: CppCodeBuilder): Unit = {
    for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
      val a = cls.makeSymbolic("a")
      val result = -a
      val resultCls = CppSubclasses.findMatchingClass(result)
      if (resultCls != CppSubclasses.zeroCls) {
        code(s"[[nodiscard]] constexpr ${resultCls.name} operator-(const ${cls.name}& a) noexcept { return ${resultCls.makeBracesInit(result)}; }")
      }
    }
  }

  private def plusMinus(code: CppCodeBuilder): Unit = {

    def makeMethod(left: CppSubclass, right: CppSubclass, operatorName: String, op: (MultiVector[Sym], MultiVector[Sym]) => MultiVector[Sym]): Unit = {
      val a = left.makeSymbolic("a")
      val b = right.makeSymbolic("b")
      val result = op(a, b)
      val resultCls = CppSubclasses.findMatchingClass(result)
      if (resultCls != CppSubclasses.zeroCls) {
        code(s"[[nodiscard]] constexpr ${resultCls.name} operator${operatorName}(const ${left.name}& a, const ${right.name}& b) noexcept { return ${resultCls.makeBracesInit(result, multiline = true)}; }")
      }
      if (resultCls == left) {
        code(s"constexpr ${resultCls.name}& operator${operatorName}=(${left.name}& a, const ${right.name}& b) noexcept { a = a ${operatorName} b; return a; }")
      }
    }

    val points = Set(CppSubclasses.projectivePoint, CppSubclasses.point, CppSubclasses.vector)
    val bivectors = Set(CppSubclasses.bivector, CppSubclasses.bivectorBulk, CppSubclasses.bivectorWeight)

    for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
      val self = cls.self

      if (points.contains(cls)) {
        for (pClass <- points) {
          makeMethod(cls, pClass, "+", _ + _)
          makeMethod(cls, pClass, "-", _ - _)
        }
      } else if (bivectors.contains(cls)) {
        for (pClass <- bivectors) {
          makeMethod(cls, pClass, "+", _ + _)
          makeMethod(cls, pClass, "-", _ - _)
        }
      } else {
        makeMethod(cls, cls, "+", _ + _)
        makeMethod(cls, cls, "-", _ - _)
      }
      code("")
    }
  }

  private def madd(code: CppCodeBuilder): Unit = {
    // Generate member method implementations using std::fma only when result type equals the class itself
    for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
      val selfMv = cls.self

      // Case 1: same-type madd
      val otherMvSame = cls.makeSymbolic("other")
      val trialSame = selfMv + otherMvSame * Sym("mult")
      val resultClsSame = CppSubclasses.findMatchingClass(trialSame)

      val pad = " " * 4

      if (resultClsSame == cls && cls.variableFields.nonEmpty) {
        // Build initialization list with fma for each variable field
        val selfGrouped = selfMv.mapValues(_.groupMultipliers())
        val otherGrouped = otherMvSame.mapValues(_.groupMultipliers())
        val fieldsInit = cls.variableFields.map { f =>
          s".${f.name} = std::fma(${otherGrouped(f.basisBladeWithSign)}, mult, ${selfGrouped(f.basisBladeWithSign)})"
        }.mkString(s"{\n$pad", s",\n$pad", "\n}")
        code(s"inline ${cls.name} ${cls.name}::madd(const ${cls.name}& other, double mult) const noexcept { return ${fieldsInit}; }")
      }

      // Case 2: Point + Vector * mult -> Point
      if (cls == CppSubclasses.point && cls.variableFields.nonEmpty) {
        val otherMvVec = CppSubclasses.vector.makeSymbolic("other")
        val trialPV = selfMv + otherMvVec * Sym("mult")
        val resultClsPV = CppSubclasses.findMatchingClass(trialPV)
        if (resultClsPV == CppSubclasses.point) {
          val selfGrouped = selfMv.mapValues(_.groupMultipliers())
          val otherGrouped = otherMvVec.mapValues(_.groupMultipliers())
          val fieldsInit = cls.variableFields.map { f =>
            s".${f.name} = std::fma(${otherGrouped(f.basisBladeWithSign)}, mult, ${selfGrouped(f.basisBladeWithSign)})"
          }.mkString(s"{\n$pad", s",\n$pad", "\n}")
          code(s"inline ${cls.name} ${cls.name}::madd(const ${CppSubclasses.vector.name}& other, double mult) const noexcept { return ${fieldsInit}; }")
        }
      }
    }
  }

  private def equality(code: CppCodeBuilder): Unit = {
    for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
      if (cls.variableFields.nonEmpty) {
        val equalityExpr = cls.variableFields.map(_.name).map(name => s"a.$name == b.$name").mkString(" && ")
        code(s"[[nodiscard]] constexpr bool operator==(const ${cls.name}& a, const ${cls.name}& b) noexcept { return $equalityExpr; }")
      }
    }
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    val a = cls.makeSymbolic("a")
    val b = cls.makeSymbolic("b")
    val trialSame = a + b * Sym("mult")
    val resultClsSame = CppSubclasses.findMatchingClass(trialSame)

    val sameTypeDecl = if (resultClsSame == cls && cls.variableFields.nonEmpty) s"[[nodiscard]] inline ${cls.name} madd(const ${cls.name}& other, double mult) const noexcept;" else ""

    // Special overload for Point + Vector * mult -> Point
    val pointVectorDecl =
      if (cls == CppSubclasses.point && cls.variableFields.nonEmpty) {
        val otherV = CppSubclasses.vector.makeSymbolic("b")
        val trialPV = a + otherV * Sym("mult")
        val resultPV = CppSubclasses.findMatchingClass(trialPV)
        if (resultPV == CppSubclasses.point) s"[[nodiscard]] inline ${cls.name} madd(const ${CppSubclasses.vector.name}& other, double mult) const noexcept;" else ""
      } else ""

    structBodyPart(Seq(sameTypeDecl, pointVectorDecl).filter(_.nonEmpty).mkString("\n"))
  }
