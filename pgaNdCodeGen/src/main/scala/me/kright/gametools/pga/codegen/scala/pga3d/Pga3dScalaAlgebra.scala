package me.kright.gametools.pga.codegen.scala.pga3d

import me.kright.gametools.ga.*
import me.kright.gametools.mathutil.Sign
import me.kright.gametools.pga.codegen.common.MultivectorField
import me.kright.gametools.pga.codegen.scala.common.*
import me.kright.gametools.pga.codegen.scala.common.ops.*
import me.kright.gametools.pga.codegen.scala.pga3d.ops.*
import me.kright.gametools.symbolic.Sym

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

  private val genW = pga.generators.find(_.squareSign == Sign.Zero).get

  private val orderedFields = pga.blades.map(b => MultivectorField(pga.representation(b), BasisBladeWithSign(b)))
  private val orderedDualFields = orderedFields.zip(orderedFields.reverse).map { (n, r) =>
    val sign: Sign = Sign(MultiVector[Int](n.basisBlade).dual(r.basisBlade))
    MultivectorField(r.name, BasisBladeWithSign(n.basisBlade, sign))
  }

  private val fields = orderedFields.map(f => f.name -> f).toMap

  override val typeNamePrefix: String = "Pga3d"

  override val targetPackage: String = "me.kright.gametools.pga3d"

  override val generatorMainFqcn: String = "me.kright.gametools.pga.codegen.scala.pga3d.runScalaCodeGen"

  override val generatedComment: String = "me.kright.gametools.pga.codegen.scala.pga3d.ScalaMultivectorSubClass"

  override val hyperplaneElementName: String = "plane"

  override val multivector = ScalaMultivectorSubClass("Pga3dMultivector", orderedFields,
    description = "A generic multivector of 3d PGA with all 16 components, used when no specialized class fits the value.")

  override val motor = ScalaMultivectorSubClass("Pga3dMotor", orderedFields.filter(b => Seq(0, 2, 4).contains(b.basisBlade.grade)),
    description = "A motor: a rigid transformation of 3d space (combined rotation and translation),\nthe even-graded (0, 2, 4) element of 3d PGA. Applied with motor.sandwich(obj).\nA motor is the exponent of a Pga3dBivector (bivector.exp()), and motor.log() returns that bivector back.")

  override val scalar = ScalaMultivectorSubClass("Double", orderedFields.take(1), shouldBeGenerated = false)
  val plane = ScalaMultivectorSubClass("Pga3dPlane", orderedFields.filter(_.basisBlade.grade == 1).tail :+ orderedFields.filter(_.basisBlade.grade == 1).head,
    description = "A plane ax + by + cz + d = 0 with the coefficients (a, b, c, d) stored in the fields (x, y, z, w);\nthe grade-1 element of 3d PGA.")
  val bivector = ScalaMultivectorSubClass("Pga3dBivector", orderedFields.filter(_.basisBlade.grade == 2),
    description = "A bivector, the grade-2 element of 3d PGA: an unnormalized line in 3d, also used for rates of motion\n(angular and linear velocity) in physics. The sum of a Pga3dBivectorBulk and a Pga3dBivectorWeight part.\nbivector.exp() is a Pga3dMotor, and motor.log() is a Pga3dBivector.")
  override val projectivePoint = ScalaMultivectorSubClass("Pga3dProjectivePoint", orderedDualFields.filter(_.basisBlade.grade == 3).take(3).reverse ++ orderedDualFields.filter(_.basisBlade.grade == 3).drop(3),
    description = "A point with four homogeneous coordinates: (x/w, y/w, z/w) when w != 0, or an ideal point (a direction) when w == 0.\nThe grade-3 element of 3d PGA, stored in dual representation.")
  override val pseudoScalar = ScalaMultivectorSubClass("Pga3dPseudoScalar", orderedFields.takeRight(1),
    description = "The pseudoscalar, the grade-4 element of 3d PGA with the single component i = wxyz.")

  override val rotor = ScalaMultivectorSubClass("Pga3dRotor", motor.variableFields.filter(f => !f.basisBlade.contains(genW)),
    description = "A rotor: rotation around an axis passing through the origin, applied with rotor.sandwich(obj).\nA rotor is the exponent of a Pga3dBivectorBulk (bivectorBulk.exp()), and rotor.log() returns that bivector back.")
  //  val rotorDual = MultivectorSubClass("RotorDual", motor.variableFields.filter(f => f.basisBlade.contains(genW)))
  override val translator = ScalaMultivectorSubClass("Pga3dTranslator", motor.variableFields.filter(f => f.basisBlade.grade == 2 && f.basisBlade.contains(genW)), Seq(scalar.variableFields.head -> 1.0),
    description = "A translator: translation of 3d space, applied with translator.sandwich(obj). Moves points but not vectors.\nA translator is the exponent of a Pga3dBivectorWeight (bivectorWeight.exp()), and translator.log() returns that bivector back.")
  override val projectiveTranslator = ScalaMultivectorSubClass("Pga3dProjectiveTranslator", motor.variableFields.filter(f => f.basisBlade.grade == 0 || f.basisBlade.grade == 2 && f.basisBlade.contains(genW)),
    description = "A translator with an explicit (not necessarily 1.0) scalar part: an unnormalized version of Pga3dTranslator.")

  override val vector = ScalaMultivectorSubClass("Pga3dVector", projectivePoint.variableFields.filter(f => f.basisBlade.contains(genW)),
    description = "A vector: the difference between two points, a direction with magnitude; an ideal point with w = 0.\nStored in dual representation with fields x, y, z. Translators move points but do not change vectors.")
  val planeIdeal = ScalaMultivectorSubClass("Pga3dPlaneIdeal", plane.variableFields.filter(f => !f.basisBlade.contains(genW)),
    description = "A plane ax + by + cz = 0 passing through the center of coordinates: a Pga3dPlane with w = 0.\nDual to Pga3dVector.")
  override val point = {
    val (weight, bulk) = projectivePoint.variableFields.partition(_.basisBlade.contains(genW))
    ScalaMultivectorSubClass("Pga3dPoint", weight, bulk.map(f => (f, 1.0)),
      description = "A point in 3d space, stored in dual representation with human-friendly fields x, y, z and constant w = 1.")
  }

  val bivectorWeight = ScalaMultivectorSubClass("Pga3dBivectorWeight", bivector.variableFields.filter(f => f.basisBlade.contains(genW)),
    description = "The weight part (wx, wy, wz) of a Pga3dBivector: an ideal line, or the linear part of a rate of motion.\nbivectorWeight.exp() is a Pga3dTranslator.")
  val bivectorBulk = ScalaMultivectorSubClass("Pga3dBivectorBulk", bivector.variableFields.filter(f => !f.basisBlade.contains(genW)),
    description = "The bulk part (xy, xz, yz) of a Pga3dBivector: a line passing through the center of coordinates,\nor the angular part of a rate of motion. bivectorBulk.exp() is a Pga3dRotor.")

  override val pointCenter = ScalaMultivectorSubClass("Pga3dPointCenter", Seq(), projectivePoint.variableFields.map(f => (f, (if (f.basisBlade.contains(genW)) 0.0 else 1.0))),
    description = "The center of coordinates as a singleton object: a Pga3dPoint with x = y = z = 0 and w = 1.")
  override val zeroCls = ScalaMultivectorSubClass("Pga3dZero", Seq(), shouldBeGenerated = false)

  override val pgaClasses = Seq(
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
    planeIdeal,

    bivectorBulk,
    bivectorWeight,

    scalar, // blade 0
    pseudoScalar, // blade 4

    pointCenter,
    zeroCls, // no fields
  )

  override val additionGroups: Seq[Set[ScalaMultivectorSubClass]] = Seq(
    Set(projectivePoint, point, vector),
    Set(bivector, bivectorBulk, bivectorWeight),
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
    DefBivectorSplit(),
    DefConvertTo(),
    DefProjection(),
    DefMotorAndRotorAxices(),
    DefRotorProjectToRotationInPlane(),
    DefInterpolation(),
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
