package me.kright.gametools.pga.codegen.scala.common

import me.kright.gametools.ga.{MultiVector, PGA}
import me.kright.gametools.symbolic.Sym

/**
 * Per-dimension configuration for the shared Scala generation framework: implemented once per algebra
 * (Pga3dScalaAlgebra, Pga2dScalaAlgebra) and injected into the single [[ScalaMultivectorSubClass]] class
 * and into the op generators in `scala.common.ops` that are written once against this interface.
 */
trait ScalaPgaAlgebra:
  given pga: PGA

  /** the class-name prefix, e.g. "Pga3d" / "Pga2d" */
  def typeNamePrefix: String

  /** package of the generated Scala files, e.g. "me.kright.gametools.pga3d" */
  def targetPackage: String

  /** FQCN of the `runScalaCodeGen` main embedded in generated header comments */
  def generatorMainFqcn: String

  /** FQCN embedded in the default (non-overridden) generateClassDoc() comment */
  def generatedComment: String

  /** lowercase name of the grade-1 (hyperplane) element, used only in operations.md prose: "plane" for 3d, "line" for 2d */
  def hyperplaneElementName: String

  def multivector: ScalaMultivectorSubClass
  def motor: ScalaMultivectorSubClass
  def scalar: ScalaMultivectorSubClass
  def projectivePoint: ScalaMultivectorSubClass
  def pseudoScalar: ScalaMultivectorSubClass
  def rotor: ScalaMultivectorSubClass
  def translator: ScalaMultivectorSubClass
  def projectiveTranslator: ScalaMultivectorSubClass
  def vector: ScalaMultivectorSubClass
  def point: ScalaMultivectorSubClass
  def pointCenter: ScalaMultivectorSubClass
  def zeroCls: ScalaMultivectorSubClass

  def pgaClasses: Seq[ScalaMultivectorSubClass]

  /**
   * groups of classes that cross-combine with `+`, `-` and `madd` (e.g. the point family, or in 3d the
   * bivector family); a class not covered by any group only combines with itself. Used by DefPlusMinusMadd.
   */
  def additionGroups: Seq[Set[ScalaMultivectorSubClass]]

  def unaryOperations: Seq[MultivectorUnaryOp]
  def binaryOperations: Seq[MultivectorBinaryOp]
  def companionObjectOperations: Seq[MultivectorUnaryOp]

  final def findMatchingClass(v: MultiVector[Sym]): ScalaMultivectorSubClass =
    pgaClasses.reverseIterator.find(_.isMatching(v)).get
