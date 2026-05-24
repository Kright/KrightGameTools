package me.kright.gametools.ga

import me.kright.gametools.mathutil.Sign
import scala.math.Numeric.Implicits.infixNumericOps

case class MultiVector[Value](ga: GA, values: Map[BasisBlade, Value]):

  def scalar(using num: Numeric[Value]): Value = get(ga.scalarBlade).getOrElse(num.zero)

  def pseudoScalar(using num: Numeric[Value]): Value = get(ga.pseudoScalarBlade).getOrElse(num.zero)

  def apply(b: BasisBlade)(using num: Numeric[Value]): Value =
    values.getOrElse(b, num.zero)

  def apply(b: BasisBladeWithSign)(using num: Numeric[Value]): Value =
    b.sign match
      case Sign.Positive => apply(b.basisBlade)
      case Sign.Negative => -apply(b.basisBlade)
      case Sign.Zero => ???

  def apply(b: String)(using num: Numeric[Value]): Value =
    apply(ga.representation.basisBladeWithSign(b))

  def get(b: BasisBlade)(using num: Numeric[Value]): Option[Value] =
    values.get(b)

  def get(b: BasisBladeWithSign)(using num: Numeric[Value]): Option[Value] =
    b.sign match
      case Sign.Positive => values.get(b.basisBlade)
      case Sign.Negative => values.get(b.basisBlade).map(v => -v)
      case Sign.Zero => ???

  def grade(grade: Int): MultiVector[Value] =
    filter((b, _) => b.grade == grade)

  def map[V2](f: (BasisBlade, Value) => V2): MultiVector[V2] =
    MultiVector[V2](ga, values.map((b, v) => b -> f(b, v)))

  def mapValues[V2](f: Value => V2): MultiVector[V2] =
    MultiVector[V2](ga, values.map((b, v) => b -> f(v)))

  def filter(f: (BasisBlade, Value) => Boolean): MultiVector[Value] =
    MultiVector[Value](ga, values.filter(f(_, _)))

  def updated(setValues: IterableOnce[(BasisBlade, Value)]): MultiVector[Value] =
    MultiVector[Value](ga, values ++ setValues)

  override def toString: String = ga.representation(this)

  def toMultilineString: String = ga.representation.toMultilineString(this)

  def toMultilineString(f: Value => String)(using num: Numeric[Value]): String = ga.representation.toMultilineString(this, f)

object MultiVector:
  def apply[T](ga: GA, values: Map[BasisBlade, T]): MultiVector[T] =
    new MultiVector(ga, values)

  def apply[T: Numeric](ga: GA, b: BasisBlade): MultiVector[T] =
    MultiVector(ga, Map(b -> summon[Numeric[T]].one))

  def apply[T: Numeric](ga: GA, b: BasisBladeWithSign): MultiVector[T] =
    MultiVector(ga, Map(b.basisBlade -> summon[Numeric[T]].fromInt(b.sign.toInt)))

  def apply[T](ga: GA, bladeString: String)(using num: Numeric[T]): MultiVector[T] =
    val bs = ga.representation.basisBladeWithSign(bladeString)
    MultiVector(ga, bs)

  def apply[T: Numeric](ga: GA, values: (String, T)*): MultiVector[T] =
    MultiVector[T](ga, values.map((s, v) =>
      val bs = ga.representation.basisBladeWithSign(s)
      bs.sign match
        case Sign.Positive => bs.basisBlade -> v
        case Sign.Negative => bs.basisBlade -> -v
        case Sign.Zero => ???
    ).toMap)

  def makeNonNegative[T](ga: GA, values: (String, T)*): MultiVector[T] =
    MultiVector[T](ga, values.map((s, v) =>
      val bs = ga.representation.basisBladeWithSign(s)
      bs.sign match
        case Sign.Positive => bs.basisBlade -> v
        case _ => throw new IllegalArgumentException(s"sign should be positive, got ${values.mkString(", ")}")
    ).toMap)

  def scalar[T](ga: GA, value: T): MultiVector[T] =
    new MultiVector(ga, Map(ga.scalarBlade -> value))

  def pseudoScalar[T](ga: GA, value: T): MultiVector[T] =
    new MultiVector(ga, Map(ga.pseudoScalarBlade -> value))

  inline def zero[T](using ga: GA): MultiVector[T] =
    new MultiVector(ga, Map.empty[BasisBlade, T])

  // with GA from implicits:
  inline def apply[T: Numeric](b: BasisBlade)(using ga: GA): MultiVector[T] = MultiVector(ga, b)

  inline def apply[T: Numeric](b: BasisBladeWithSign)(using ga: GA): MultiVector[Double] = MultiVector(ga, b)

  inline def apply[T: Numeric](bladeString: String)(using ga: GA): MultiVector[T] = MultiVector(ga, bladeString)

  inline def apply[T](values: IterableOnce[(BasisBlade, T)])(using ga: GA): MultiVector[T] = MultiVector[T](ga, values.iterator.toMap)

  inline def apply[T: Numeric](values: (String, T)*)(using ga: GA): MultiVector[T] = MultiVector[T](ga, values *)

  inline def makeNonNegative[T](values: (String, T)*)(using ga: GA): MultiVector[T] = MultiVector.makeNonNegative[T](ga, values *)

  inline def scalar[T](value: T)(using ga: GA): MultiVector[T] = MultiVector.scalar(ga, value)

  inline def pseudoScalar[T](value: T)(using ga: GA): MultiVector[T] = MultiVector.pseudoScalar[T](ga, value)

  extension [T](left: MultiVector[T])(using num: Numeric[T])
    infix def geometric(right: MultiVector[T]): MultiVector[T] = left.ga.operations.multiplication.geometric(left, right)
    infix def wedge(right: MultiVector[T]): MultiVector[T] = left.ga.operations.multiplication.wedge(left, right)
    infix def dot(right: MultiVector[T]): MultiVector[T] = left.ga.operations.multiplication.dot(left, right)

    infix def antiGeometric(right: MultiVector[T]): MultiVector[T] = left.ga.operations.anti.geometric(left, right)
    infix def antiWedge(right: MultiVector[T]): MultiVector[T] = left.ga.operations.anti.wedge(left, right)
    infix def antiDot(right: MultiVector[T]): MultiVector[T] = left.ga.operations.anti.dot(left, right)

    def sandwich(middle: MultiVector[T]): MultiVector[T] = left.geometric(middle).geometric(left.reverse)
    def antiSandwich(middle: MultiVector[T]): MultiVector[T] = left.antiGeometric(middle).antiGeometric(left.antiReverse)

    /** don't forget to div by 2 */
    def crossX2(right: MultiVector[T]): MultiVector[T] = left.geometric(right) - right.geometric(left)

    def rightComplement: MultiVector[T] = left.ga.operations.complement.right(left)
    def leftComplement: MultiVector[T] = left.ga.operations.complement.left(left)
    def bulk: MultiVector[T] = left.ga.operations.bulk(left)
    def weight: MultiVector[T] = left.ga.operations.weight(left)
    def reverse: MultiVector[T] = left.ga.operations.reverse(left)
    def antiReverse: MultiVector[T] = left.ga.operations.antiReverse(left)
    def dual: MultiVector[T] = left.ga.operations.dual(left)

    // unicode symbols: https://projectivegeometricalgebra.org/
    inline infix def ⟑(right: MultiVector[T]): MultiVector[T] = geometric(right)
    inline infix def ∧(right: MultiVector[T]): MultiVector[T] = wedge(right)
    inline infix def ⋅(right: MultiVector[T]): MultiVector[T] = dot(right)

    inline infix def ⟇(right: MultiVector[T]): MultiVector[T] = antiGeometric(right)
    inline infix def ∨(right: MultiVector[T]): MultiVector[T] = antiWedge(right)
    inline infix def ◦(right: MultiVector[T]): MultiVector[T] = antiDot(right)

    inline infix def ^(right: MultiVector[T]): MultiVector[T] = wedge(right)
    inline infix def v(right: MultiVector[T]): MultiVector[T] = antiWedge(right)

    infix def +(right: MultiVector[T]): MultiVector[T] =
      MultiVector[T]((left.values.keySet ++ right.values.keySet).toSeq.map { b =>
        b -> (left(b) + right(b))
      })(using left.ga)

    infix def -(right: MultiVector[T]): MultiVector[T] =
      MultiVector[T]((left.values.keySet ++ right.values.keySet).toSeq.map { b =>
        b -> (left(b) - right(b))
      })(using left.ga)

    def unary_- : MultiVector[T] =
      left.map((b, v) => -v)

    infix def *(scalarMultiplier: T): MultiVector[T] =
      left.map((b, t) => t * scalarMultiplier)

    def multiplyElementwise(right: MultiVector[T]): MultiVector[T] =
      MultiVector[T]((left.values.keySet.intersect(right.values.keySet)).toSeq.map(b =>
        b -> (left(b) * right(b))
      ))(using left.ga)

    def apply(right: MultiVector[T]): MultiVector[T] = geometric(right)

    def getSqrDist(right: MultiVector[T]): T =
      (left.values.keySet ++ right.values.keySet).view.map { b =>
        val d = left(b) - right(b)
        d * d
      }.sum

    def squareMagnitude: T =
      left.values.values.map(v => v * v).sum

    def withoutZeros: MultiVector[T] =
      left.filter((_, v) => v != num.zero)

  extension (left: MultiVector[Double])
    def magnitude: Double =
      math.sqrt(left.squareMagnitude)

    def /(scalarDivider: Double): MultiVector[Double] =
      left * (1.0 / scalarDivider)

    def norm: Double =
      math.sqrt(left.squareMagnitude)

    def normalizedByWeight: MultiVector[Double] =
      left / left.weight.norm

    def normalizedByBulk: MultiVector[Double] =
      left / left.bulk.norm

    def normalizedByNorm: MultiVector[Double] =
      left / left.norm

    def withoutZeroEps(eps: Double): MultiVector[Double] =
      left.filter((_, v) => Math.abs(v) > eps)

    def cross(right: MultiVector[Double]): MultiVector[Double] =
      left.crossX2(right) / 2.0

    /**
     * @return infinite series 1 + x + x^2 / 2! + x^3 / 3! + ...
     */
    def exponentSeries: LazyList[MultiVector[Double]] =
      left.ga.use {
        def loop(v: MultiVector[Double], n: Int): LazyList[MultiVector[Double]] =
          v #:: loop((v geometric left) / n, n + 1)

        loop(MultiVector.scalar(1.0), 1)
      }

    /**
     * Very straightforward but inefficient code
     * Works well for values less then 1, otherwise requires many steps
     */
    def exponentBySeriesSum(thresholdNorm: Double, maxSteps: Int = 50): MultiVector[Double] =
      val thresholdSquaredNorm = thresholdNorm * thresholdNorm
      val series = exponentSeries.take(maxSteps)
      series.tail.takeWhile(_.squareMagnitude > thresholdSquaredNorm).fold(series.head)(_ + _)
