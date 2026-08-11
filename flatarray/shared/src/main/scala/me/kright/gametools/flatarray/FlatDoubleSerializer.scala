package me.kright.gametools.flatarray

import scala.annotation.nowarn
import scala.quoted.*
import scala.reflect.ClassTag


trait FlatDoubleSerializer[T]:
  /** the number of doubles one serialized T occupies (flat containers use it as the element stride) */
  val size: Int
  def classTag: ClassTag[T]

  def write(elem: T, array: Array[Double], offset: Int): Unit

  def read(array: Array[Double], offset: Int): T


object FlatDoubleSerializer:

  @nowarn
  inline def derived[T]: FlatDoubleSerializer[T] = new FlatDoubleSerializer[T]:
    override val size: Int = FlatDoubleSerializer.getSize[T]
    override val classTag: ClassTag[T] = scala.compiletime.summonInline[ClassTag[T]]

    override def write(value: T, array: Array[Double], offset: Int): Unit =
      FlatDoubleSerializer.write[T](value, array, offset)

    override def read(array: Array[Double], offset: Int): T =
      FlatDoubleSerializer.read[T](array, offset)


  //* count of double values in case class, recursing into nested case classes */
  inline def getSize[T]: Int = ${ getSizeImpl[T] }

  private def getSizeImpl[T: Type](using Quotes): Expr[Int] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    if (!tpe.typeSymbol.flags.is(Flags.Case)) {
      report.errorAndAbort(s"Type ${Type.show[T]} must be a case class")
    }

    Expr(countDoubles(tpe))

  private def countDoubles(using q: Quotes)(tpe: q.reflect.TypeRepr): Int =
    import q.reflect.*

    if (tpe =:= TypeRepr.of[Double])
      1
    else if (tpe.typeSymbol.flags.is(Flags.Case))
      tpe.typeSymbol.caseFields.map(field => countDoubles(tpe.memberType(field))).sum
    else
      report.errorAndAbort(s"Field type ${tpe.show} must be Double or a case class with Double fields")

  /** write case class with double fields (or nested case classes of doubles) into the array of doubles at specific offset */
  inline def write[T](elem: T, array: Array[Double], offset: Int): Unit = ${ writeImpl[T]('elem, 'array, 'offset) }

  private def writeImpl[T: Type](elem: Expr[T], array: Expr[Array[Double]], offset: Expr[Int])(using Quotes): Expr[Unit] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    if (!tpe.typeSymbol.flags.is(Flags.Case)) {
      report.errorAndAbort(s"Type ${Type.show[T]} must be a case class")
    }

    val (assignments, _) = collectWrites(elem.asTerm, tpe, 0, array, offset)

    assignments.foldLeft[Expr[Unit]]('{ () })((acc, expr) =>
      '{ $acc ; $expr }
    )

  private def collectWrites(using q: Quotes)(term: q.reflect.Term,
                                             tpe: q.reflect.TypeRepr,
                                             startIdx: Int,
                                             array: Expr[Array[Double]],
                                             offset: Expr[Int]): (List[Expr[Unit]], Int) =
    import q.reflect.*

    if (tpe =:= TypeRepr.of[Double]) {
      val arrayIdx = Expr(startIdx)
      val fieldValue = term.asExprOf[Double]
      (List('{ $array($offset + $arrayIdx) = $fieldValue }), startIdx + 1)
    } else if (tpe.typeSymbol.flags.is(Flags.Case)) {
      tpe.typeSymbol.caseFields.foldLeft((List.empty[Expr[Unit]], startIdx)) { case ((acc, idx), field) =>
        val (writes, nextIdx) = collectWrites(Select(term, field), tpe.memberType(field), idx, array, offset)
        (acc ++ writes, nextIdx)
      }
    } else {
      report.errorAndAbort(s"Field type ${tpe.show} must be Double or a case class with Double fields")
    }

  /** restore case class back from the array at specific offset */
  inline def read[T](array: Array[Double], offset: Int): T = ${ readImpl[T]('array, 'offset) }

  private def readImpl[T: Type](array: Expr[Array[Double]], offset: Expr[Int])(using Quotes): Expr[T] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    if (!tpe.typeSymbol.flags.is(Flags.Case)) {
      report.errorAndAbort(s"Type ${Type.show[T]} must be a case class")
    }

    val (result, _) = buildRead(tpe, 0, array, offset)
    result.asExprOf[T]

  private def buildRead(using q: Quotes)(tpe: q.reflect.TypeRepr,
                                         startIdx: Int,
                                         array: Expr[Array[Double]],
                                         offset: Expr[Int]): (q.reflect.Term, Int) =
    import q.reflect.*

    if (tpe =:= TypeRepr.of[Double]) {
      val arrayIdx = Expr(startIdx)
      ('{ $array($offset + $arrayIdx) }.asTerm, startIdx + 1)
    } else if (tpe.typeSymbol.flags.is(Flags.Case)) {
      val symbol = tpe.typeSymbol
      val (args, nextIdx) = symbol.caseFields.foldLeft((List.empty[Term], startIdx)) { case ((acc, idx), field) =>
        val (arg, next) = buildRead(tpe.memberType(field), idx, array, offset)
        (acc :+ arg, next)
      }
      (Apply(Select(New(Inferred(tpe)), symbol.primaryConstructor), args), nextIdx)
    } else {
      report.errorAndAbort(s"Field type ${tpe.show} must be Double or a case class with Double fields")
    }
