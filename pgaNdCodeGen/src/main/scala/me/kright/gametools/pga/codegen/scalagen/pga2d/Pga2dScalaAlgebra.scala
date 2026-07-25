package me.kright.gametools.pga.codegen.scalagen.pga2d

import me.kright.gametools.ga.*
import me.kright.gametools.mathutil.Sign
import me.kright.gametools.pga.codegen.common.{MultivectorField, NormSymbolics, PgaSubclassFields}
import me.kright.gametools.pga.codegen.scalagen.common.*
import me.kright.gametools.pga.codegen.scalagen.common.ops.*
import me.kright.gametools.pga.codegen.scalagen.pga2d.ops.*
import me.kright.gametools.symbolic.Sym

import scala.collection.immutable.ListSet
import scala.collection.immutable.ArraySeq

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

  private val structure = PgaSubclassFields(using pga)

  override val typeNamePrefix: String = "Pga2d"

  override val targetPackage: String = "me.kright.gametools.pga2d"

  override val generatorMainFqcn: String = "me.kright.gametools.pga.codegen.scalagen.pga2d.runScalaCodeGen"


  override val hyperplaneElementName: String = "line"

  override val multivector = ScalaMultivectorSubClass("Pga2dMultivector", structure.multivector,
    description = "A generic multivector of 2d PGA with all 8 components, used when no specialized class fits the value.")
  override val motor = ScalaMultivectorSubClass("Pga2dMotor", structure.motor,
    description = "A motor: a rigid transformation of the 2d plane (combined rotation and translation),\nthe even-graded (0, 2) element of 2d PGA. Applied with motor.sandwich(obj).\nA motor is the exponent of a grade-2 element (projectivePoint.exp), and motor.log returns that element back.")
  override val scalar = ScalaMultivectorSubClass("Double", structure.scalar, shouldBeGenerated = false)
  val line = ScalaMultivectorSubClass("Pga2dLine", structure.hyperplane,
    description = "A line ax + by + c = 0 with the coefficients (a, b, c) stored in the fields (x, y, w);\nthe grade-1 element of 2d PGA. The 2d sibling of Pga3dPlane.")
  override val projectivePoint = ScalaMultivectorSubClass("Pga2dProjectivePoint", structure.projectivePoint,
    description = "A point with three homogeneous coordinates: (x/w, y/w) when w != 0, or an ideal point (a direction) when w == 0.\nThe grade-2 element of 2d PGA, stored in dual representation.\nprojectivePoint.exp is a Pga2dMotor (rotation around the point), and motor.log is a Pga2dProjectivePoint.")
  override val pseudoScalar = ScalaMultivectorSubClass("Pga2dPseudoScalar", structure.pseudoScalar,
    description = "The pseudoscalar, the grade-3 element of 2d PGA with the single component i = wxy.")

  override val rotor = ScalaMultivectorSubClass("Pga2dRotor", structure.rotor,
    description = "A rotor: rotation around the center of coordinates, applied with rotor.sandwich(obj).\nThe fields s and xy hold the cosine and sine of the half-angle. The 2d analog of Pga3dRotor;\na rotor is the exponent of a grade-2 element concentrated at the origin (the xy blade).")
  override val translator = ScalaMultivectorSubClass("Pga2dTranslator", structure.translatorVariable, structure.translatorConstants,
    description = "A translator: translation of the 2d plane, applied with translator.sandwich(obj). Moves points but not vectors.\nA translator is the exponent of a Pga2dVector (vector.exp), and translator.log returns that vector back.")
  override val projectiveTranslator = ScalaMultivectorSubClass("Pga2dProjectiveTranslator", structure.projectiveTranslator,
    description = "A translator with an explicit (not necessarily 1.0) scalar part: an unnormalized version of Pga2dTranslator.")

  override val vector = ScalaMultivectorSubClass("Pga2dVector", structure.vector,
    description = "A vector: the difference between two points, a direction with magnitude; an ideal point with w = 0.\nStored in dual representation with fields x, y. Translators move points but do not change vectors.")
  val lineCentral = ScalaMultivectorSubClass("Pga2dLineCentral", structure.hyperplaneCentral,
    description = "A line ax + by = 0 passing through the center of coordinates: a Pga2dLine with w = 0.\nDual to Pga2dVector.")
  override val point = ScalaMultivectorSubClass("Pga2dPoint", structure.pointVariable, structure.pointConstants,
    description = "A point on the 2d plane, stored in dual representation with human-friendly fields x, y and constant w = 1.")

  override val pointCenter = ScalaMultivectorSubClass("Pga2dPointCenter", ArraySeq(), structure.pointCenterConstants,
    description = "The center of coordinates as a singleton object: a Pga2dPoint with x = y = 0 and w = 1.")
  override val zeroCls = ScalaMultivectorSubClass("Pga2dZero", ArraySeq(), shouldBeGenerated = false)

  override val pgaClasses = ArraySeq(
    multivector, motor,
    line, projectivePoint,
    rotor, projectiveTranslator, translator,
    vector, point, lineCentral,
    scalar, pseudoScalar,
    pointCenter, zeroCls,
  )

  override val additionGroups: Seq[ListSet[ScalaMultivectorSubClass]] = ArraySeq(
    ListSet(projectivePoint, point, vector),
  )

  override val unaryOperations = ArraySeq(
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
    DefNorm("bulkNormSquare", "bulkNorm", "normalizedByBulk", NormSymbolics.bulkSquare),
    DefNorm("weightNormSquare", "weightNorm", "normalizedByWeight", NormSymbolics.weightSquare),
    DefNorm("normSquare", "norm", "normalizedByNorm", NormSymbolics.fullSquare),
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
    DefMotorAndRotorAxes(),
  )

  override val binaryOperations = ArraySeq(
    MultivectorBinaryOp(ArraySeq("geometric"), pga.operations.multiplication.geometric(_, _)),
    MultivectorBinaryOp(ArraySeq("dot"), pga.operations.multiplication.dot(_, _)),
    MultivectorBinaryOp(ArraySeq("wedge", "^", "meet"), pga.operations.multiplication.wedge(_, _)),

    MultivectorBinaryOp(ArraySeq("antiGeometric"), pga.operations.anti.geometric(_, _)),
    MultivectorBinaryOp(ArraySeq("antiDot"), pga.operations.anti.dot(_, _)),
    MultivectorBinaryOp.option(ArraySeq("antiDotI"), (a, b) => Option(pga.operations.anti.dot(a, b).dual).filter(findMatchingClass(_) == scalar)),
    MultivectorBinaryOp(ArraySeq("antiWedge", "v", "join"), pga.operations.anti.wedge(_, _)),

    MultivectorBinaryOp(ArraySeq("sandwich"), (a, b) => a.sandwich(b)),
    MultivectorBinaryOp(ArraySeq("reverseSandwich"), (a, b) => a.reverse.sandwich(b)),
    MultivectorBinaryOp(ArraySeq("cross"), (a, b) => a.crossX2(b) * Sym(0.5)),
  )

  override val companionObjectOperations = ArraySeq(
    DefVariablesComponentsCount(),
    DefZeroObjectMethods(),
    DefMethodsIfAnyPoint(),
    DefObjectMethodsForTranslator(),
    DefObjectMethodsForRotor(),
    DefObjectMethodsForMotor(),
  )
