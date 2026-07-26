package me.kright.gametools.pga.codegen.scalagen.pga3d

import me.kright.gametools.ga.*
import me.kright.gametools.mathutil.Sign
import me.kright.gametools.pga.codegen.common.{MultivectorField, NormSymbolics, PgaSubclassFields}
import me.kright.gametools.pga.codegen.scalagen.common.*
import me.kright.gametools.pga.codegen.scalagen.common.ops.*
import me.kright.gametools.pga.codegen.scalagen.pga3d.ops.*
import me.kright.gametools.symbolic.Sym

import scala.collection.immutable.ListSet
import scala.collection.immutable.ArraySeq

object Pga3dScalaAlgebra extends ScalaPgaAlgebra:

  given ScalaPgaAlgebra = this

  private def representationConfig = GARepresentationConfig(
    Signature.pga3,
    generatorNames = "wxyz",
    namePrefix = "",
    overrideScalar = Option("s"),
    overridePseudoScalar = Option("i"),
  )

  override given pga: PGA3 = PGA3(representationConfig)

  private val structure = PgaSubclassFields(using pga)

  override val typeNamePrefix: String = "Pga3d"

  override val targetPackage: String = "me.kright.gametools.pga3d"

  override val generatorMainFqcn: String = "me.kright.gametools.pga.codegen.scalagen.pga3d.runScalaCodeGen"


  override val hyperplaneElementName: String = "plane"

  override val multivector = ScalaMultivectorSubClass("Pga3dMultivector", structure.multivector,
    description = "A generic multivector of 3d PGA with all 16 components, used when no specialized class fits the value.")

  override val motor = ScalaMultivectorSubClass("Pga3dMotor", structure.motor,
    description = "A motor: a rigid transformation of 3d space (combined rotation and translation),\nthe even-graded (0, 2, 4) element of 3d PGA. Applied with motor.sandwich(obj).\nA motor is the exponent of a Pga3dBivector (bivector.exp), and motor.log returns that bivector back.")

  override val scalar = ScalaMultivectorSubClass("Double", structure.scalar, shouldBeGenerated = false)
  val plane = ScalaMultivectorSubClass("Pga3dPlane", structure.hyperplane,
    description = "A plane ax + by + cz + d = 0 with the coefficients (a, b, c, d) stored in the fields (x, y, z, w);\nthe grade-1 element of 3d PGA.")
  val bivector = ScalaMultivectorSubClass("Pga3dBivector", structure.bivector,
    description = "A bivector, the grade-2 element of 3d PGA: an unnormalized line in 3d, also used for rates of motion\n(angular and linear velocity) in physics. The sum of a Pga3dBivectorBulk and a Pga3dBivectorWeight part.\nbivector.exp is a Pga3dMotor, and motor.log is a Pga3dBivector.")
  override val projectivePoint = ScalaMultivectorSubClass("Pga3dProjectivePoint", structure.projectivePoint,
    description = "A point with four homogeneous coordinates: (x/w, y/w, z/w) when w != 0, or an ideal point (a direction) when w == 0.\nThe grade-3 element of 3d PGA, stored in dual representation.")
  override val pseudoScalar = ScalaMultivectorSubClass("Pga3dPseudoScalar", structure.pseudoScalar,
    description = "The pseudoscalar, the grade-4 element of 3d PGA with the single component i = wxyz.")

  override val rotor = ScalaMultivectorSubClass("Pga3dRotor", structure.rotor,
    description = "A rotor: rotation around an axis passing through the origin, applied with rotor.sandwich(obj).\nA rotor is the exponent of a Pga3dBivectorBulk (bivectorBulk.exp), and rotor.log returns that bivector back.")
  //  val rotorDual = MultivectorSubClass("RotorDual", motor.variableFields.filter(f => f.basisBlade.contains(genW)))
  override val translator = ScalaMultivectorSubClass("Pga3dTranslator", structure.translatorVariable, structure.translatorConstants,
    description = "A translator: translation of 3d space, applied with translator.sandwich(obj). Moves points but not vectors.\nA translator is the exponent of a Pga3dBivectorWeight (bivectorWeight.exp), and translator.log returns that bivector back.")
  override val projectiveTranslator = ScalaMultivectorSubClass("Pga3dProjectiveTranslator", structure.projectiveTranslator,
    description = "A translator with an explicit (not necessarily 1.0) scalar part: an unnormalized version of Pga3dTranslator.")

  override val vector = ScalaMultivectorSubClass("Pga3dVector", structure.vector,
    description = "A vector: the difference between two points, a direction with magnitude; an ideal point with w = 0.\nStored in dual representation with fields x, y, z. Translators move points but do not change vectors.")
  val planeCentral = ScalaMultivectorSubClass("Pga3dPlaneCentral", structure.hyperplaneCentral,
    description = "A plane ax + by + cz = 0 passing through the center of coordinates: a Pga3dPlane with w = 0.\nDual to Pga3dVector.")
  override val point = ScalaMultivectorSubClass("Pga3dPoint", structure.pointVariable, structure.pointConstants,
    description = "A point in 3d space, stored in dual representation with human-friendly fields x, y, z and constant w = 1.")

  val bivectorWeight = ScalaMultivectorSubClass("Pga3dBivectorWeight", structure.bivectorWeight,
    description = "The weight part (wx, wy, wz) of a Pga3dBivector: an ideal line, or the linear part of a rate of motion.\nbivectorWeight.exp is a Pga3dTranslator.")
  val bivectorBulk = ScalaMultivectorSubClass("Pga3dBivectorBulk", structure.bivectorBulk,
    description = "The bulk part (xy, xz, yz) of a Pga3dBivector: a line passing through the center of coordinates,\nor the angular part of a rate of motion. bivectorBulk.exp is a Pga3dRotor.")

  override val pointCenter = ScalaMultivectorSubClass("Pga3dPointCenter", ArraySeq(), structure.pointCenterConstants,
    description = "The center of coordinates as a singleton object: a Pga3dPoint with x = y = z = 0 and w = 1.")
  override val zeroCls = ScalaMultivectorSubClass("Pga3dZero", ArraySeq(), shouldBeGenerated = false)

  override val pgaClasses = ArraySeq(
    multivector, // all
    motor, // blade 0 + 2 + 4

    plane, // blade 1
    bivector, // blade 2
    projectivePoint, // blade 3

    rotor,
    //    rotorDual,
    projectiveTranslator,
    translator,

    vector,
    point,
    planeCentral,

    bivectorBulk,
    bivectorWeight,

    scalar, // blade 0
    pseudoScalar, // blade 4

    pointCenter,
    zeroCls, // no fields
  )

  override val additionGroups: Seq[ListSet[ScalaMultivectorSubClass]] = ArraySeq(
    ListSet(projectivePoint, point, vector),
    ListSet(bivector, bivectorBulk, bivectorWeight),
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
    DefBivectorSplit(),
    DefConvertTo(),
    DefProjection(),
    DefMotorAndRotorAxes(),
    DefRotorProjectToRotationInPlane(),
    DefInterpolation(),
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
    DefObjectMethodsForVector(),
  )
