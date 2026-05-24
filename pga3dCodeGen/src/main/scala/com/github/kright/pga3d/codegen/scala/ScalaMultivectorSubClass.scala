package com.github.kright.pga3d.codegen.scala

import com.github.kright.ga.*
import ScalaMultivectorSubClass.*
import com.github.kright.pga3d.codegen.common.{MultivectorField, MultivectorSubClass}
import com.github.kright.pga3d.codegen.scala.ops.*
import com.github.kright.symbolic.Sym
import com.github.kright.symbolic.transform.simplifiers.ReplaceSumOrProduct
import com.github.kright.mathutil.Sign

import scala.math.Numeric.Implicits.infixNumericOps
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps

class ScalaMultivectorSubClass(name: String,
                               variableFields: Seq[MultivectorField],
                               constantFields: Seq[(MultivectorField, Double)] = Seq(),
                               shouldBeGenerated: Boolean = true)(using pga3: PGA3) extends MultivectorSubClass(name, variableFields, constantFields, shouldBeGenerated) with ScalaCodeGenClass:

  override def isObject: Boolean = variableFields.isEmpty

  override def generateImports(): String =
    """import scala.annotation.targetName
      |import com.github.kright.mathutil.FlatDoubleSerializer
      |
      |""".stripMargin

  def makeSymbolic(instanceName: String): MultiVector[Sym] =
    if (name == "Double") {
      MultiVector.scalar(Sym(instanceName))
    } else {
      MultiVector[Sym](
        variableFields.map(f => f.basisBlade -> Sym(s"${instanceName}.${f.name}").pipe(e => if (f.sign == Sign.Positive) e else -e)).toMap ++
          constantFields.map((f, v) => f.basisBlade -> Sym(v).pipe(e => if (f.sign == Sign.Positive) e else -e))
      )
    }

  val self: MultiVector[Sym] =
    if (name == "Double") {
      MultiVector.scalar(Sym("this"))
    } else {
      MultiVector[Sym](
        variableFields.map(f => f.basisBlade -> Sym(f.name).pipe(e => if (f.sign == Sign.Positive) e else -e)).toMap ++
          constantFields.map((f, v) => f.basisBlade -> Sym(v).pipe(e => if (f.sign == Sign.Positive) e else -e))
      )
    }

  def makeConstructor(value: MultiVector[Sym]): String =
    val groupedResult = value.mapValues(_.groupMultipliers())
    if (name == "Double") {
      val expr = groupedResult.get(variableFields.head.basisBlade).getOrElse(Sym.zero)
      return expr.toString
    }
    if (isObject) {
      return name
    }

    val code = ScalaCodeBuilder()
    code(name + "(")
    code.block {
      for (f <- variableFields) {
        val expr: Sym = groupedResult.get(f.basisBlade).getOrElse(Sym.zero)

        val exprString: String =
          if (f.sign == Sign.Positive) expr.toString
          else (-expr).groupMultipliers().toString

        require(!exprString.startsWith("--"), s"expString = '${exprString}',\n-expr = ${(-expr).symbol},\nexpr = ${expr.symbol}")

        code(s"${f.name} = ${exprString},")
      }
    }
    code(")")
    code.toString


  override def generateCode(): String =
    generateCode(unaryOperations, binaryOperations)

  private def generateConstructorCode(code: ScalaCodeBuilder): Unit = {
    if (isObject) {
      code(s"object ${name}:")
    } else {
      require(variableFields.nonEmpty)

      val start = s"final case class ${name}("
      val pad = " ".repeat(start.length)

      code(variableFields
        .map(f => s"${f.name}: Double = 0.0")
        .zipWithIndex
        .map((s, i) => if (i == 0) s else pad + s)
        .mkString(start, ",\n", ") derives CanEqual, FlatDoubleSerializer:"))
    }
  }

  private def generateCode(unaryOps: Seq[MultivectorUnaryOp],
                           binaryOps: Seq[MultivectorBinaryOp]): String = {
    val code = ScalaCodeBuilder()

    generateConstructorCode(code)

    code.block {
      if (isObject) {
        DefVariablesComponentsCount()(using pga3).apply(this, self).foreach { lines =>
          code(lines)
        }
      }

      for (unaryOp <- unaryOps) {
        unaryOp(this, self).foreach { lines =>
          code(lines)
        }
      }

      for (binaryOp <- binaryOps) {
        for (rightCls <- pgaClasses
             if (rightCls != zeroCls)
               && (rightCls != scalar)
               && ((this == ScalaMultivectorSubClass.multivector) == (rightCls == ScalaMultivectorSubClass.multivector))) {
          val v = rightCls.makeSymbolic("v")
          binaryOp(self, v) match {
            case None => ()
            case Some(result) => {
              val resultCls = findMatchingClass(result)
              if (resultCls != zeroCls) {
                code(s"\ninfix def ${binaryOp.name}(v: ${rightCls.typeName}): ${resultCls.typeName} =")

                code.block {
                  if (resultCls == this && result == self) {
                    code("this")
                  } else if (resultCls == this && result == -self) {
                    code("-this")
                  } else if (resultCls == rightCls && result == v) {
                    code("v")
                  } else if (resultCls == rightCls && result == -v) {
                    code("-v")
                  } else {
                    binaryOp.name match
                      case "sandwich" | "reverseSandwich" => code(makeConstructorOptimized(result, resultCls))
                      case _ => code(resultCls.makeConstructor(result))
                  }
                }

                for (opName <- binaryOp.names.tail) {
                  code(s"\ninline infix def ${opName}(v: ${rightCls.typeName}): ${resultCls.typeName} = ${binaryOp.name}(v)")
                }
              }
            }
          }
        }
      }
    }

    if (!isObject) {
      code(
        s"""
           |
           |object ${typeName}:""".stripMargin)
      code.block {

        for (unaryOp <- companionObjectOperations) {
          unaryOp(this, self).foreach { lines =>
            code(lines)
          }
        }

      }
    }

    code.toString
  }

  def makeConstructorOptimized(result: MultiVector[Sym], resultCls: ScalaMultivectorSubClass): String = {
    val code = ScalaCodeBuilder()

    val simplifications: Seq[(Sym, Sym)] =
      (for ((fx, i) <- variableFields.zipWithIndex;
            (fy, j) <- variableFields.zipWithIndex if i <= j)
      yield Sym(s"${fx.name}M${fy.name}") -> self(fx.basisBladeWithSign) * self(fy.basisBladeWithSign))

    var rr = result
    for ((simple, expr) <- simplifications) {
      val replacer = ReplaceSumOrProduct(expr.symbol, simple.symbol)
      val newRR = rr.mapValues(v => v.map(replacer))

      if ((getSize(newRR) + 1 < getSize(rr))) {
        rr = newRR
        code(s"val ${simple.toString} = ${expr.toString}")
      }
    }

    val constructorCode = resultCls.makeConstructor(rr)
    code(constructorCode)
    code.toString
  }

  private def getSize(a: MultiVector[Sym]): Int =
    a.values.map((b, s) => s.size).sum


object ScalaMultivectorSubClass:
  private def representationConfig = GARepresentationConfig(
    Signature.pga3,
    generatorNames = "wxyz",
    namePrefix = "",
    overrideScalar = Option("s"),
    overridePseudoScalar = Option("i"),
  )

  given pga3: PGA3 = PGA3(representationConfig)

  private val genW = pga3.generators.find(_.squareSign == Sign.Zero).get

  private val orderedFields = pga3.blades.map(b => MultivectorField(pga3.representation(b), BasisBladeWithSign(b)))
  private val orderedDualFields = orderedFields.zip(orderedFields.reverse).map { (n, r) =>
    val sign: Sign = Sign(MultiVector[Int](n.basisBlade).dual(r.basisBlade))
    MultivectorField(r.name, BasisBladeWithSign(n.basisBlade, sign))
  }

  private val fields = orderedFields.map(f => f.name -> f).toMap

  val multivector = ScalaMultivectorSubClass("Pga3dMultivector", orderedFields)

  val motor = ScalaMultivectorSubClass("Pga3dMotor", orderedFields.filter(b => Seq(0, 2, 4).contains(b.basisBlade.grade)))

  val scalar = ScalaMultivectorSubClass("Double", orderedFields.take(1), shouldBeGenerated = false)
  val plane = ScalaMultivectorSubClass("Pga3dPlane", orderedFields.filter(_.basisBlade.grade == 1).tail :+ orderedFields.filter(_.basisBlade.grade == 1).head)
  val bivector = ScalaMultivectorSubClass("Pga3dBivector", orderedFields.filter(_.basisBlade.grade == 2))
  val projectivePoint = ScalaMultivectorSubClass("Pga3dProjectivePoint", orderedDualFields.filter(_.basisBlade.grade == 3).take(3).reverse ++ orderedDualFields.filter(_.basisBlade.grade == 3).drop(3))
  val pseudoScalar = ScalaMultivectorSubClass("Pga3dPseudoScalar", orderedFields.takeRight(1))

  val quaternion = ScalaMultivectorSubClass("Pga3dQuaternion", motor.variableFields.filter(f => !f.basisBlade.contains(genW)))
  //  val quaternionDual = MultivectorSubClass("QuaternionDual", motor.variableFields.filter(f => f.basisBlade.contains(genW)))
  val translator = ScalaMultivectorSubClass("Pga3dTranslator", motor.variableFields.filter(f => f.basisBlade.grade == 2 && f.basisBlade.contains(genW)), Seq(scalar.variableFields.head -> 1.0))
  val projectiveTranslator = ScalaMultivectorSubClass("Pga3dProjectiveTranslator", motor.variableFields.filter(f => f.basisBlade.grade == 0 || f.basisBlade.grade == 2 && f.basisBlade.contains(genW)))

  val vector = ScalaMultivectorSubClass("Pga3dVector", projectivePoint.variableFields.filter(f => f.basisBlade.contains(genW)))
  val planeIdeal = ScalaMultivectorSubClass("Pga3dPlaneIdeal", plane.variableFields.filter(f => !f.basisBlade.contains(genW)))
  val point = {
    val (weight, bulk) = projectivePoint.variableFields.partition(_.basisBlade.contains(genW))
    ScalaMultivectorSubClass("Pga3dPoint", weight, bulk.map(f => (f, 1.0)))
  }

  val bivectorWeight = ScalaMultivectorSubClass("Pga3dBivectorWeight", bivector.variableFields.filter(f => f.basisBlade.contains(genW)))
  val bivectorBulk = ScalaMultivectorSubClass("Pga3dBivectorBulk", bivector.variableFields.filter(f => !f.basisBlade.contains(genW)))

  val pointCenter = ScalaMultivectorSubClass("Pga3dPointCenter", Seq(), projectivePoint.variableFields.map(f => (f, (if (f.basisBlade.contains(genW)) 0.0 else 1.0))))
  val zeroCls = ScalaMultivectorSubClass("Pga3dZero", Seq(), shouldBeGenerated = false)

  val pgaClasses = Seq(
    multivector, // all
    motor, // blade 0 + 2 + 4

    plane, // blade 1
    bivector, // blade 2
    projectivePoint, // blade 3

    quaternion,
    //    quaternionDual,
    projectiveTranslator,
    translator,

    vector,
    point,
    planeIdeal,

    bivectorBulk,
    bivectorWeight,

    scalar, // blade 0
    pseudoScalar, // blade 4

    pointCenter,
    zeroCls, // no fields
  )

  def findMatchingClass(v: MultiVector[Sym]): ScalaMultivectorSubClass =
    pgaClasses.reverseIterator.find(_.isMatching(v)).get

  val unaryOperations = Seq(
    DefConstAndDualFields(),
    DefToString(),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "dual", pga3.operations.dual(v))),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "weight", pga3.operations.weight(v))),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "bulk", pga3.operations.bulk(v))),
    MultivectorUnaryOp((cls, s) => GeneratedValue(cls, "unary_- ", -s, "unaryMinus")),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "reverse", pga3.operations.reverse(v))),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "antiReverse", pga3.operations.antiReverse(v))),
    DefRenormalizedForMotor(),
    DefMotorToQuaternionAndTranslator(),
    DefNorm("bulkNormSquare", "bulkNorm", "normalizedByBulk", s => s.geometric(s.reverse).grade(0)),
    DefNorm("weightNormSquare", "weightNorm", "normalizedByWeight", s => s.dual.geometric(s.dual.reverse).grade(0)),
    DefNorm("normSquare", "norm", "normalizedByNorm", s => s.geometric(s.reverse).grade(0) + s.dual.geometric(s.dual.reverse).grade(0)),
    DefMultiplyToScalar(),
    DefDivideByScalar(),
    DefMinMaxForPointOrVector(),
    DefDistanceToPoint(),
    DefPlusMinusMadd(),
    DefExpForBivector(),
    DefLogForMotor(),
    DefBivectorSplit(),
    DefConvertTo(),
    DefProjection(),
    DefMotorAndQuaternionAxices(),
    DefQuaternionProjectToRotationInPlane(),
  )

  val binaryOperations = Seq(
    MultivectorBinaryOp(Seq("geometric"), pga3.operations.multiplication.geometric(_, _)),
    MultivectorBinaryOp(Seq("dot"), pga3.operations.multiplication.dot(_, _)),
    MultivectorBinaryOp(Seq("wedge", "^", "meet"), pga3.operations.multiplication.wedge(_, _)),

    MultivectorBinaryOp(Seq("antiGeometric"), pga3.operations.anti.geometric(_, _)),
    MultivectorBinaryOp(Seq("antiDot"), pga3.operations.anti.dot(_, _)),
    MultivectorBinaryOp.option(Seq("antiDotI"), (a, b) => Option(pga3.operations.anti.dot(a, b).dual).filter(findMatchingClass(_) == scalar)),
    MultivectorBinaryOp(Seq("antiWedge", "v", "join"), pga3.operations.anti.wedge(_, _)),

    MultivectorBinaryOp(Seq("sandwich"), (a, b) => a.sandwich(b)),
    MultivectorBinaryOp(Seq("reverseSandwich"), (a, b) => a.reverse.sandwich(b)),
    MultivectorBinaryOp(Seq("cross"), (a, b) => a.crossX2(b) * Sym(0.5)),
  )

  val companionObjectOperations = Seq(
    DefVariablesComponentsCount(),
    DefZeroObjectMethods(),
    DefMethodsIfAnyPoint(),
    DefObjectMethodsForTranslator(),
    DefObjectMethodsForQuaternion(),
    DefObjectMethodsForMotor(),
  )
