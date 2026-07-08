package me.kright.gametools.pga.codegen.scala.pga2d

import me.kright.gametools.ga.*
import me.kright.gametools.mathutil.Sign
import me.kright.gametools.pga.codegen.common.MultivectorField
import me.kright.gametools.pga.codegen.scala.common.*
import me.kright.gametools.pga.codegen.scala.common.ops.*
import me.kright.gametools.pga.codegen.scala.pga2d.ops.*
import me.kright.gametools.symbolic.Sym

object Pga2dScalaAlgebra extends ScalaPgaAlgebra:

  given ScalaPgaAlgebra = this

  private def representationConfig = GARepresentationConfig(
    Signature.pga2,
    generatorNames = "wxy",
    namePrefix = "",
    overrideScalar = Option("s"),
    overridePseudoScalar = Option("i"),
  )

  override given pga: PGA2 = PGA2(representationConfig)

  private val genW = pga.generators.find(_.squareSign == Sign.Zero).get

  private val orderedFields = pga.blades.map(b => MultivectorField(pga.representation(b), BasisBladeWithSign(b)))
  private val orderedDualFields = orderedFields.zip(orderedFields.reverse).map { (n, r) =>
    val sign: Sign = Sign(MultiVector[Int](n.basisBlade).dual(r.basisBlade))
    MultivectorField(r.name, BasisBladeWithSign(n.basisBlade, sign))
  }

  private val fields = orderedFields.map(f => f.name -> f).toMap

  override val typeNamePrefix: String = "Pga2d"

  override val targetPackage: String = "me.kright.gametools.pga2d"

  override val generatorMainFqcn: String = "me.kright.gametools.pga.codegen.scala.pga2d.runScalaCodeGen"

  override val generatedComment: String = "me.kright.gametools.pga.codegen.scala.pga2d.ScalaMultivectorSubClass"

  override val hyperplaneElementName: String = "line"

  override val multivector = ScalaMultivectorSubClass("Pga2dMultivector", orderedFields,
    description = "A generic multivector of 2d PGA with all 8 components, used when no specialized class fits the value.")
  override val motor = ScalaMultivectorSubClass("Pga2dMotor", orderedFields.filter(b => Seq(0, 2).contains(b.basisBlade.grade)),
    description = "A motor: a rigid transformation of the 2d plane (combined rotation and translation),\nthe even-graded (0, 2) element of 2d PGA. Applied with motor.sandwich(obj).\nA motor is the exponent of a grade-2 element (projectivePoint.exp()), and motor.log() returns that element back.")
  override val scalar = ScalaMultivectorSubClass("Double", orderedFields.take(1), shouldBeGenerated = false)
  val line = ScalaMultivectorSubClass("Pga2dLine", orderedFields.filter(_.basisBlade.grade == 1).tail :+ orderedFields.filter(_.basisBlade.grade == 1).head,
    description = "A line ax + by + c = 0 with the coefficients (a, b, c) stored in the fields (x, y, w);\nthe grade-1 element of 2d PGA. The 2d sibling of Pga3dPlane.")
  override val projectivePoint = ScalaMultivectorSubClass("Pga2dProjectivePoint", orderedDualFields.filter(_.basisBlade.grade == 2).take(2).reverse ++ orderedDualFields.filter(_.basisBlade.grade == 2).drop(2),
    description = "A point with three homogeneous coordinates: (x/w, y/w) when w != 0, or an ideal point (a direction) when w == 0.\nThe grade-2 element of 2d PGA, stored in dual representation.\nprojectivePoint.exp() is a Pga2dMotor (rotation around the point), and motor.log() is a Pga2dProjectivePoint.")
  override val pseudoScalar = ScalaMultivectorSubClass("Pga2dPseudoScalar", orderedFields.takeRight(1),
    description = "The pseudoscalar, the grade-3 element of 2d PGA with the single component i = wxy.")

  override val rotor = ScalaMultivectorSubClass("Pga2dRotor", motor.variableFields.filter(f => !f.basisBlade.contains(genW)),
    description = "A rotor: rotation around the center of coordinates, applied with rotor.sandwich(obj).\nThe fields s and xy hold the cosine and sine of the half-angle. The 2d analog of Pga3dQuaternion;\na rotor is the exponent of a grade-2 element concentrated at the origin (the xy blade).")
  override val translator = ScalaMultivectorSubClass("Pga2dTranslator", motor.variableFields.filter(f => f.basisBlade.grade == 2 && f.basisBlade.contains(genW)), Seq(scalar.variableFields.head -> 1.0),
    description = "A translator: translation of the 2d plane, applied with translator.sandwich(obj). Moves points but not vectors.\nA translator is the exponent of a Pga2dVector (vector.exp()), and translator.log() returns that vector back.")
  override val projectiveTranslator = ScalaMultivectorSubClass("Pga2dProjectiveTranslator", motor.variableFields.filter(f => f.basisBlade.grade == 0 || f.basisBlade.grade == 2 && f.basisBlade.contains(genW)),
    description = "A translator with an explicit (not necessarily 1.0) scalar part: an unnormalized version of Pga2dTranslator.")

  override val vector = ScalaMultivectorSubClass("Pga2dVector", projectivePoint.variableFields.filter(f => f.basisBlade.contains(genW)),
    description = "A vector: the difference between two points, a direction with magnitude; an ideal point with w = 0.\nStored in dual representation with fields x, y. Translators move points but do not change vectors.")
  val lineIdeal = ScalaMultivectorSubClass("Pga2dLineIdeal", line.variableFields.filter(f => !f.basisBlade.contains(genW)),
    description = "A line ax + by = 0 passing through the center of coordinates: a Pga2dLine with w = 0.\nDual to Pga2dVector.")
  override val point = {
    val (weight, bulk) = projectivePoint.variableFields.partition(_.basisBlade.contains(genW))
    ScalaMultivectorSubClass("Pga2dPoint", weight, bulk.map(f => (f, 1.0)),
      description = "A point on the 2d plane, stored in dual representation with human-friendly fields x, y and constant w = 1.")
  }

  override val pointCenter = ScalaMultivectorSubClass("Pga2dPointCenter", Seq(), projectivePoint.variableFields.map(f => (f, (if (f.basisBlade.contains(genW)) 0.0 else 1.0))),
    description = "The center of coordinates as a singleton object: a Pga2dPoint with x = y = 0 and w = 1.")
  override val zeroCls = ScalaMultivectorSubClass("Pga2dZero", Seq(), shouldBeGenerated = false)

  override val pgaClasses = Seq(
    multivector, motor,
    line, projectivePoint,
    rotor, projectiveTranslator, translator,
    vector, point, lineIdeal,
    scalar, pseudoScalar,
    pointCenter, zeroCls,
  )

  override val additionGroups: Seq[Set[ScalaMultivectorSubClass]] = Seq(
    Set(projectivePoint, point, vector),
  )

  override val unaryOperations = Seq(
    DefConstAndDualFields(),
    DefToString(),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "dual", pga.operations.dual(v))),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "weight", pga.operations.weight(v))),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "bulk", pga.operations.bulk(v))),
    MultivectorUnaryOp((cls, s) => GeneratedValue(cls, "unary_- ", -s, "unaryMinus")),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "reverse", pga.operations.reverse(v))),
    MultivectorUnaryOp((cls, v) => GeneratedValue(cls, "antiReverse", pga.operations.antiReverse(v))),
    DefRenormalizedForMotor(),
    DefMotorToRotorAndTranslator(),
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
    DefInterpolation(),
    DefConvertTo(),
    DefProjection(),
  )

  override val binaryOperations = Seq(
    MultivectorBinaryOp(Seq("geometric"), pga.operations.multiplication.geometric(_, _)),
    MultivectorBinaryOp(Seq("dot"), pga.operations.multiplication.dot(_, _)),
    MultivectorBinaryOp(Seq("wedge", "^", "meet"), pga.operations.multiplication.wedge(_, _)),

    MultivectorBinaryOp(Seq("antiGeometric"), pga.operations.anti.geometric(_, _)),
    MultivectorBinaryOp(Seq("antiDot"), pga.operations.anti.dot(_, _)),
    MultivectorBinaryOp.option(Seq("antiDotI"), (a, b) => Option(pga.operations.anti.dot(a, b).dual).filter(findMatchingClass(_) == scalar)),
    MultivectorBinaryOp(Seq("antiWedge", "v", "join"), pga.operations.anti.wedge(_, _)),

    MultivectorBinaryOp(Seq("sandwich"), (a, b) => a.sandwich(b)),
    MultivectorBinaryOp(Seq("reverseSandwich"), (a, b) => a.reverse.sandwich(b)),
    MultivectorBinaryOp(Seq("cross"), (a, b) => a.crossX2(b) * Sym(0.5)),
  )

  override val companionObjectOperations = Seq(
    DefVariablesComponentsCount(),
    DefZeroObjectMethods(),
    DefMethodsIfAnyPoint(),
    DefObjectMethodsForTranslator(),
    DefObjectMethodsForRotor(),
    DefObjectMethodsForMotor(),
  )
