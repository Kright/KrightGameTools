package com.github.kright.pga3d.codegen.cpp.ops

import com.github.kright.ga.MultiVector
import com.github.kright.math.Sign.Positive
import com.github.kright.pga3d.codegen.common.FileContent
import com.github.kright.pga3d.codegen.cpp.*
import com.github.kright.symbolic.Sym
import com.github.kright.symbolic.Sym.given_Numeric_Sym.mkNumericOps
import com.github.kright.symbolic.transform.simplifiers.ReplaceSumOrProduct

class GeometricOpsGenerator extends CppCodeGeneratorSum(
  Seq(
    new GeometricOpGenerator(),
    new AntiGeometricOpGenerator(),

    new DotOpGenerator(),
    new AntiDotOpGenerator(),

    new WedgeOpGenerator(),
    new AntiWedgeOpGenerator(),

    new SandwichOpGenerator(),
    new SandwichAsMatrix(),

    new CrossOpGenerator(),
  )
)


class GeometricOpGenerator extends BinaryMethodCodeGen(
  methodName = "geometric",
  fileName = "opsGeometric.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.geometric(b)
)

class AntiGeometricOpGenerator extends BinaryMethodCodeGen(
  methodName = "antiGeometric",
  fileName = "opsAntiGeometric.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.antiGeometric(b)
)


class DotOpGenerator extends BinaryMethodCodeGen(
  methodName = "dot",
  fileName = "opsDot.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.dot(b)
)

class AntiDotOpGenerator extends BinaryMethodCodeGen(
  methodName = "antiDot",
  fileName = "opsAntiDot.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.antiDot(b)
)


class WedgeOpGenerator extends BinaryMethodCodeGen(
  methodName = "wedge",
  alternativeNames = Seq("meet"),
  fileName = "opsWedge.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.wedge(b)
)

class AntiWedgeOpGenerator extends BinaryMethodCodeGen(
  methodName = "antiWedge",
  alternativeNames = Seq("join"),
  fileName = "opsAntiWedge.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.antiWedge(b)
)


class SandwichOpGenerator extends BinaryMethodCodeGen(
  methodName = "sandwich",
  fileName = "opsSandwich.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.sandwich(b),
  makeCustomBody = Some(
    new CustomFuncBody {
      override def apply(left: CppSubclass, leftS: MultiVector[Sym], right: CppSubclass, rightS: MultiVector[Sym], target: CppSubclass, resultS: MultiVector[Sym]): String = {
        val code = CppCodeBuilder()

        val simplifications: Seq[(Sym, Sym)] =
          (for ((fx, i) <- left.variableFields.zipWithIndex;
                (fy, j) <- left.variableFields.zipWithIndex if i <= j)
          yield Sym(s"${fx.name}M${fy.name}") -> leftS(fx.basisBladeWithSign) * leftS(fy.basisBladeWithSign))

        var rr = resultS
        for ((simple, expr) <- simplifications) {
          val replacer = ReplaceSumOrProduct(expr.symbol, simple.symbol)
          val newRR = rr.mapValues(v => v.map(replacer))

          if ((getSize(newRR) + 1 < getSize(rr))) {
            rr = newRR
            code(s"const double ${simple.toString} = ${expr.toString};")
          }
        }

        code(s"return ${target.makeBracesInit(rr, multiline = true)};")

        code.toString
      }

      private def getSize(a: MultiVector[Sym]): Int =
        a.values.map((b, s) => s.size).sum
    }
  ),
  comment = Option(
    """/**
      | * Compiler can reuse constants between calls, where the same object used several times,
      | * So in the code below all that constants `const double sMs = s * s` are reused and the code is fast.
      | * It is crucial to keep method constexpr or inline to achieve this
      | * On my machine this trick speed up `Motor.sandwich(Bivector)` from 9 ns to 3 ns
      | * @code
      | * const pga3d::Motor& motor = ...
      | * for(pga3d::Vector& v: array) {
      | *     v = motor.sandwich(v);
      | * }
      | * @endcode
      | */
      |""".stripMargin
  )
)


class CrossOpGenerator extends BinaryMethodCodeGen(
  methodName = "cross",
  fileName = "opsCross.h",
  op = (a: MultiVector[Sym], b: MultiVector[Sym]) => a.crossX2(b) * Sym(0.5)
)


private trait CustomFuncBody {
  def apply(left: CppSubclass, leftS: MultiVector[Sym], right: CppSubclass, rightS: MultiVector[Sym], target: CppSubclass, resultS: MultiVector[Sym]): String
}


/**
 * Generalized binary-method code generator for PGA3D C++ bindings.
 * Reuses the same emission pattern for methods like geometric and dot.
 *
 * Simplifications:
 *  - Skip scalar and zero classes entirely
 *  - Multivector interacts only with Multivector
 *  - No X op Multivector for X != Multivector
 *  - Skip emission when result resolves to Zero class
 */
private class BinaryMethodCodeGen(val methodName: String,
                                  val fileName: String,
                                  val op: (MultiVector[Sym], MultiVector[Sym]) => MultiVector[Sym],
                                  val alternativeNames: Seq[String] = Seq(),
                                  val makeCustomBody: Option[CustomFuncBody] = None,
                                  val comment: Option[String] = None) extends CppCodeGenerator:

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] =
    val code = CppCodeBuilder()

    code.myHeader(Seq(s"#include \"${codeGen.Headers.types}\""), getClass.getName)

    comment.foreach(code(_))

    code.namespace(codeGen.namespace) {
      for (left <- CppSubclasses.all if left.shouldBeGenerated) {
        for (right <- CppSubclasses.all if right.shouldBeGenerated) {
          val skipRight = right == CppSubclasses.scalar || right == CppSubclasses.zeroCls
          val skipLeft = left == CppSubclasses.scalar || left == CppSubclasses.zeroCls
          if !skipLeft && !skipRight then
            val pairAllowed =
              if left == CppSubclasses.multivector then right == CppSubclasses.multivector
              else right != CppSubclasses.multivector

            if (pairAllowed) {
              val leftS = left.makeSymbolic("a")
              val rightS = right.makeSymbolic("b")
              val resultS = op(leftS, rightS)
              val target = CppSubclasses.findMatchingClass(resultS)
              if (target != CppSubclasses.zeroCls) {

                makeCustomBody match {
                  case Some(makeBody) => {
                    code(s"[[nodiscard]] constexpr ${target.name} ${methodName}(const ${left.name}&a, const ${right.name}& b) noexcept {")
                    code.block {
                      code(makeBody(left, leftS, right, rightS, target, resultS))
                    }
                    code("}")
                  }
                  case None => {
                    code(s"[[nodiscard]] constexpr ${target.name} ${methodName}(const ${left.name}& a,const ${right.name}& b) noexcept { return ${target.makeBracesInit(resultS, multiline = true)}; }")
                  }
                }

                alternativeNames.foreach { altName =>
                  code(s"[[nodiscard]] constexpr ${target.name} ${altName}(const ${left.name}& a,const ${right.name}& b) noexcept { return ${methodName}(a, b); }")
                }

                (Seq(methodName) ++ alternativeNames).foreach { altName =>
                  code(s"constexpr ${target.name} ${left.name}::${altName}(const ${right.name}& b) const noexcept { return pga3d::${methodName}(*this, b); }")
                }
                code("")
              }
            }
        }
        code("")
      }
    }

    Seq(FileContent(codeGen.directory.resolve(fileName), code.toString))

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] =
    val decls =
      for
        right <- CppSubclasses.all
        if right.shouldBeGenerated
        if right != CppSubclasses.scalar && right != CppSubclasses.zeroCls
        if (if cls == CppSubclasses.multivector then right == CppSubclasses.multivector else right != CppSubclasses.multivector)
      yield
        val result = op(cls.self, right.makeSymbolic("b"))
        val target = CppSubclasses.findMatchingClass(result)
        if target == CppSubclasses.zeroCls then ""
        else {
          (Seq(methodName) ++ alternativeNames)
            .map(name => s"[[nodiscard]] constexpr ${target.name} ${name}(const ${right.name}& b) const noexcept;")
            .mkString("\n")
        }

    val result = decls.filter(_.nonEmpty).mkString("\n")

    structBodyPart(result)


class SandwichAsMatrix extends CppCodeGenerator {

  private def motorClasses: Seq[CppSubclass] =
    Seq(CppSubclasses.motor, CppSubclasses.quaternion)

  private def elemClasses: Seq[CppSubclass] =
    Seq(CppSubclasses.projectivePoint, CppSubclasses.vector, CppSubclasses.bivector, CppSubclasses.plane)

  private def generateCode(anyMotor: CppSubclass, element: CppSubclass): DeclarationWithImplementation = {
    val decl = CppCodeBuilder()
    val impl = CppCodeBuilder()

    val result = anyMotor.self.sandwich(element.self)
    val resultType = CppSubclasses.findMatchingClass(result)
    require(resultType == element)

    val size = element.variableFields.size
    val matrixSize = size * size

    import Pga3dProvider.pga3

    val probes = element.variableFields.map { f =>
      MultiVector[Sym](
        Seq(
          (f.basisBlade, if (f.sign == Positive) Sym(1) else Sym(-1))
        )
      )
    }

    val mappedProbes = probes.map { p => anyMotor.self.sandwich(p) }

    val arrayElements = element.variableFields.flatMap { f =>
      mappedProbes.map { p =>
        p.get(f.basisBladeWithSign).getOrElse(Sym(0.0))
      }
    }

    decl(s"[[nodiscard]] constexpr std::array<double, $matrixSize> sandwichAsMatrixFor${element.name}() const noexcept;")
    impl(
      s"""constexpr std::array<double, $matrixSize> ${anyMotor.name}::sandwichAsMatrixFor${element.name}() const noexcept {
         |    return {
         |        ${arrayElements.grouped(size).map(_.mkString(", ")).mkString(",\n" + " " * 8)}
         |    };
         |}
         |""".stripMargin)

    DeclarationWithImplementation(decl.toString, impl.toString)
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (!Set(CppSubclasses.motor, CppSubclasses.quaternion).contains(cls)) return Seq()

    val code = CppCodeBuilder()

    for (elem <- elemClasses) {
      code(generateCode(cls, elem).declaration)
    }

    structBodyPart(code.toString, includes = Seq("<array>"))
  }

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(
      Seq(
        s"\"${CppSubclasses.motor.name}.h\"",
        s"\"${CppSubclasses.quaternion.name}.h\"",
      ),
      this.getClass.getName
    )

    code.namespace(codeGen.namespace) {
      val impls = motorClasses.flatMap { motor =>
        elemClasses.map { elem =>
          generateCode(motor, elem).implementation
        }
      }

      code(impls.mkString("\n\n"))
    }

    Seq(FileContent(codeGen.directory.resolve("opsSandwichAsMatrix.h"), code.toString))
  }
}

private class DeclarationWithImplementation(val declaration: String, val implementation: String)