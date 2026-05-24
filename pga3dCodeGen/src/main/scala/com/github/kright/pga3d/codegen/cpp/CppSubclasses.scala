package com.github.kright.pga3d.codegen.cpp

import com.github.kright.ga.{BasisBladeWithSign, MultiVector}
import com.github.kright.pga3d.codegen.common.MultivectorField
import com.github.kright.symbolic.Sym
import com.github.kright.util.Sign

object CppSubclasses:
  import Pga3dProvider.pga3

  private val genW = Pga3dProvider.pga3.generators.find(_.squareSign == Sign.Zero).get

  private val orderedFields =  Pga3dProvider.pga3.blades.map(b => MultivectorField(pga3.representation(b), BasisBladeWithSign(b)))
  private val orderedDualFields = orderedFields.zip(orderedFields.reverse).map { (n, r) =>
    val sign: Sign = Sign(MultiVector[Int](n.basisBlade).dual(r.basisBlade))
    MultivectorField(r.name, BasisBladeWithSign(n.basisBlade, sign))
  }

  private val fields = orderedFields.map(f => f.name -> f).toMap

  val multivector = CppSubclass("Multivector", orderedFields)

  val motor = CppSubclass("Motor", orderedFields.filter(b => Seq(0, 2, 4).contains(b.basisBlade.grade)))

  val scalar = CppSubclass("double", orderedFields.take(1), shouldBeGenerated = false)
  val plane = CppSubclass("Plane", orderedFields.filter(_.basisBlade.grade == 1).tail :+ orderedFields.filter(_.basisBlade.grade == 1).head)
  val bivector = CppSubclass("Bivector", orderedFields.filter(_.basisBlade.grade == 2))
  val projectivePoint = CppSubclass("ProjectivePoint", orderedDualFields.filter(_.basisBlade.grade == 3).take(3).reverse ++ orderedDualFields.filter(_.basisBlade.grade == 3).drop(3))
  val pseudoScalar = CppSubclass("PseudoScalar", orderedFields.takeRight(1))

  val quaternion = CppSubclass("Quaternion", motor.variableFields.filter(f => !f.basisBlade.contains(genW)))
  //  val quaternionDual = CppSubclass("QuaternionDual", motor.variableFields.filter(f => f.basisBlade.contains(genW)))
  val translator = CppSubclass("Translator", motor.variableFields.filter(f => f.basisBlade.grade == 2 && f.basisBlade.contains(genW)), Seq(scalar.variableFields.head -> 1.0))
  val projectiveTranslator = CppSubclass("ProjectiveTranslator", motor.variableFields.filter(f => f.basisBlade.grade == 0 || f.basisBlade.grade == 2 && f.basisBlade.contains(genW)))

  val vector = CppSubclass("Vector", projectivePoint.variableFields.filter(f => f.basisBlade.contains(genW)))
  val planeIdeal = CppSubclass("PlaneIdeal", plane.variableFields.filter(f => !f.basisBlade.contains(genW)))
  val point: CppSubclass = {
    val (weight, bulk) = projectivePoint.variableFields.partition(_.basisBlade.contains(genW))
    CppSubclass("Point", weight, bulk.map(f => (f, 1.0)))
  }

  val bivectorWeight = CppSubclass("BivectorWeight", bivector.variableFields.filter(f => f.basisBlade.contains(genW)))
  val bivectorBulk = CppSubclass("BivectorBulk", bivector.variableFields.filter(f => !f.basisBlade.contains(genW)))

  val pointCenter = CppSubclass("PointCenter", Seq(), projectivePoint.variableFields.map(f => (f, (if (f.basisBlade.contains(genW)) 0.0 else 1.0))))
  val zeroCls = CppSubclass("Zero", Seq(), shouldBeGenerated = false)

  val all = Seq[CppSubclass](
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

  def findMatchingClass(v: MultiVector[Sym]): CppSubclass =
    all.reverseIterator.find(_.isMatching(v)).get
