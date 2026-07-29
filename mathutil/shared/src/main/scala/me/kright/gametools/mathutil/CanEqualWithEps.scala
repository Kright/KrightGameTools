package me.kright.gametools.mathutil

import scala.annotation.nowarn
import scala.quoted.*

/**
 * Typeclass for approximate equality with an explicit tolerance: `a.equalsWithEps(b, eps)` is true
 * iff the Chebyshev (L-infinity) distance over all `Double` components is within `eps`, i.e. every
 * component differs by at most `eps`.
 *
 * `derives CanEqualWithEps` works for case classes whose fields are `Double`s or (recursively) other
 * such case classes; the generated code is a flat `&&`-chain of per-component comparisons with no
 * allocations. Each comparison is `a == b || abs(a - b) <= eps`, so equal infinities compare equal
 * and NaN is never equal to anything (including itself).
 *
 * Note that the comparison is componentwise: projective types compare their homogeneous
 * representatives (not the geometric entities they encode), and a rotor is not equal to its
 * negation even though both encode the same rotation.
 */
trait CanEqualWithEps[T]:
  extension (a: T)
    def equalsWithEps(b: T, eps: Double): Boolean

object CanEqualWithEps:

  @nowarn
  inline def derived[T]: CanEqualWithEps[T] = new CanEqualWithEps[T]:
    extension (a: T)
      override def equalsWithEps(b: T, eps: Double): Boolean =
        CanEqualWithEps.componentwiseEquals[T](a, b, eps)

  given CanEqualWithEps[Double] = new CanEqualWithEps[Double]:
    extension (a: Double)
      def equalsWithEps(b: Double, eps: Double): Boolean = a == b || math.abs(a - b) <= eps

  /** true iff every (recursively nested) `Double` field of the case class differs by at most `eps` */
  inline def componentwiseEquals[T](a: T, b: T, eps: Double): Boolean = ${ equalsImpl[T]('a, 'b, 'eps) }

  private def equalsImpl[T: Type](a: Expr[T], b: Expr[T], eps: Expr[Double])(using Quotes): Expr[Boolean] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    if (!tpe.typeSymbol.flags.is(Flags.Case)) {
      report.errorAndAbort(s"Type ${Type.show[T]} must be a case class")
    }

    collectChecks(a.asTerm, b.asTerm, tpe, eps)
      .reduceOption((x, y) => '{ $x && $y })
      .getOrElse(Expr(true))

  private def collectChecks(using q: Quotes)(aTerm: q.reflect.Term,
                                             bTerm: q.reflect.Term,
                                             tpe: q.reflect.TypeRepr,
                                             eps: Expr[Double]): List[Expr[Boolean]] =
    import q.reflect.*

    if (tpe =:= TypeRepr.of[Double]) {
      val ae = aTerm.asExprOf[Double]
      val be = bTerm.asExprOf[Double]
      List('{ $ae == $be || math.abs($ae - $be) <= $eps })
    } else if (tpe.typeSymbol.flags.is(Flags.Case)) {
      tpe.typeSymbol.caseFields.flatMap { field =>
        collectChecks(Select(aTerm, field), Select(bTerm, field), tpe.memberType(field), eps)
      }
    } else {
      report.errorAndAbort(s"Field type ${tpe.show} must be Double or a case class with Double fields")
    }
