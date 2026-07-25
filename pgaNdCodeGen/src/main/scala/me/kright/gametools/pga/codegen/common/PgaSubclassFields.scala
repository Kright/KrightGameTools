package me.kright.gametools.pga.codegen.common

import me.kright.gametools.ga.{BasisBladeWithSign, MultiVector, PGA}
import me.kright.gametools.mathutil.Sign

import scala.collection.immutable.ArraySeq

/**
 * The shared derivation of the field structure of every generated subclass, for a PGA of
 * any dimension. Consumed by the Scala 2d/3d algebra objects and the C++ class list, so
 * the taxonomies cannot drift apart; the consumers add only names and descriptions.
 */
class PgaSubclassFields(using pga: PGA):
  val genW = pga.generators.find(_.squareSign == Sign.Zero).get

  val orderedFields: Seq[MultivectorField] =
    pga.blades.map(b => MultivectorField(pga.representation(b), BasisBladeWithSign(b)))

  val orderedDualFields: Seq[MultivectorField] =
    orderedFields.zip(orderedFields.reverse).map { (n, r) =>
      val sign: Sign = Sign(MultiVector[Int](n.basisBlade).dual(r.basisBlade))
      MultivectorField(r.name, BasisBladeWithSign(n.basisBlade, sign))
    }

  /** the grade of a point: 2 in 2d, 3 in 3d */
  private val pointGrade = pga.generators.size - 1

  val multivector: Seq[MultivectorField] = orderedFields
  val motor: Seq[MultivectorField] = orderedFields.filter(_.basisBlade.grade % 2 == 0)
  val scalar: Seq[MultivectorField] = orderedFields.take(1)
  val pseudoScalar: Seq[MultivectorField] = orderedFields.takeRight(1)

  /** grade-1 fields rotated so that the w (offset) coefficient comes last */
  val hyperplane: Seq[MultivectorField] =
    val grade1 = orderedFields.filter(_.basisBlade.grade == 1)
    grade1.tail :+ grade1.head

  val hyperplaneCentral: Seq[MultivectorField] = hyperplane.filter(f => !f.basisBlade.contains(genW))

  /** dual-representation point fields: the weight coordinates first, the constant-candidate bulk blade last */
  val projectivePoint: Seq[MultivectorField] =
    val dualPoint = orderedDualFields.filter(_.basisBlade.grade == pointGrade)
    dualPoint.take(pointGrade).reverse ++ dualPoint.drop(pointGrade)

  val vector: Seq[MultivectorField] = projectivePoint.filter(f => f.basisBlade.contains(genW))
  val pointVariable: Seq[MultivectorField] = projectivePoint.filter(f => f.basisBlade.contains(genW))
  val pointConstants: Seq[(MultivectorField, Double)] = projectivePoint.filterNot(f => f.basisBlade.contains(genW)).map(f => (f, 1.0))
  val pointCenterConstants: Seq[(MultivectorField, Double)] = projectivePoint.map(f => (f, if (f.basisBlade.contains(genW)) 0.0 else 1.0))

  val rotor: Seq[MultivectorField] = motor.filter(f => !f.basisBlade.contains(genW))
  val translatorVariable: Seq[MultivectorField] = motor.filter(f => f.basisBlade.grade == 2 && f.basisBlade.contains(genW))
  val translatorConstants: Seq[(MultivectorField, Double)] = ArraySeq(scalar.head -> 1.0)
  val projectiveTranslator: Seq[MultivectorField] = motor.filter(f => f.basisBlade.grade == 0 || f.basisBlade.grade == 2 && f.basisBlade.contains(genW))

  // the grade-2 (bivector) family exists as separate classes only in 3d
  val bivector: Seq[MultivectorField] = orderedFields.filter(_.basisBlade.grade == 2)
  val bivectorWeight: Seq[MultivectorField] = bivector.filter(f => f.basisBlade.contains(genW))
  val bivectorBulk: Seq[MultivectorField] = bivector.filter(f => !f.basisBlade.contains(genW))
