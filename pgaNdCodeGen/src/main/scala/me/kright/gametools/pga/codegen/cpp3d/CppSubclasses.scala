package me.kright.gametools.pga.codegen.cpp3d

import me.kright.gametools.ga.{BasisBladeWithSign, MultiVector}
import me.kright.gametools.pga.codegen.common.{MultivectorField, PgaSubclassFields}
import me.kright.gametools.symbolic.Sym
import me.kright.gametools.mathutil.Sign
import scala.collection.immutable.ArraySeq

object CppSubclasses:
  import Pga3dProvider.pga3

  private val structure = PgaSubclassFields(using pga3)

  val multivector = CppSubclass("Multivector", structure.multivector)

  val motor = CppSubclass("Motor", structure.motor)

  val scalar = CppSubclass("double", structure.scalar, shouldBeGenerated = false)
  val plane = CppSubclass("Plane", structure.hyperplane)
  val bivector = CppSubclass("Bivector", structure.bivector)
  val projectivePoint = CppSubclass("ProjectivePoint", structure.projectivePoint)
  val pseudoScalar = CppSubclass("PseudoScalar", structure.pseudoScalar)

  val rotor = CppSubclass("Rotor", structure.rotor)
  //  val rotorDual = CppSubclass("RotorDual", motor.variableFields.filter(f => f.basisBlade.contains(genW)))
  val translator = CppSubclass("Translator", structure.translatorVariable, structure.translatorConstants)
  val projectiveTranslator = CppSubclass("ProjectiveTranslator", structure.projectiveTranslator)

  val vector = CppSubclass("Vector", structure.vector)
  val planeCentral = CppSubclass("PlaneCentral", structure.hyperplaneCentral)
  val point: CppSubclass = CppSubclass("Point", structure.pointVariable, structure.pointConstants)

  val bivectorWeight = CppSubclass("BivectorWeight", structure.bivectorWeight)
  val bivectorBulk = CppSubclass("BivectorBulk", structure.bivectorBulk)

  val pointCenter = CppSubclass("PointCenter", ArraySeq(), structure.pointCenterConstants)
  val zeroCls = CppSubclass("Zero", ArraySeq(), shouldBeGenerated = false)

  val all = ArraySeq[CppSubclass](
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

  def findMatchingClass(v: MultiVector[Sym]): CppSubclass =
    all.reverseIterator.find(_.isMatching(v)).get
