package com.github.kright.mathutil

import scala.annotation.nowarn
import scala.quoted.*
import scala.reflect.ClassTag


trait FlatDoubleSerializer[T]:
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

  
  //* count of double values in case class */
  inline def getSize[T]: Int = ${ getSizeImpl[T] }

  private def getSizeImpl[T: Type](using Quotes): Expr[Int] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val symbol = tpe.typeSymbol

    if (!symbol.flags.is(Flags.Case)) {
      report.errorAndAbort(s"Type ${Type.show[T]} must be a case class")
    }

    val fields = tpe.typeSymbol.caseFields
    val doubleFields = fields.filter(field => TypeRepr.of[Double] =:= tpe.memberType(field))

    if (doubleFields.length != fields.length) {
      report.errorAndAbort(s"All fields in ${Type.show[T]} must be of type Double")
    }

    Expr(fields.length)

  /** write case class with double fields into the array of doubles at specific offset */
  inline def write[T](elem: T, array: Array[Double], offset: Int): Unit = ${ writeImpl[T]('elem, 'array, 'offset) }

  private def writeImpl[T: Type](elem: Expr[T], array: Expr[Array[Double]], offset: Expr[Int])(using Quotes): Expr[Unit] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val symbol = tpe.typeSymbol

    if (!symbol.flags.is(Flags.Case)) {
      report.errorAndAbort(s"Type ${Type.show[T]} must be a case class")
    }

    val fields = tpe.typeSymbol.caseFields
    val doubleFields = fields.filter(field => TypeRepr.of[Double] =:= tpe.memberType(field))

    if (doubleFields.length != fields.length) {
      report.errorAndAbort(s"All fields in ${Type.show[T]} must be of type Double")
    }

    val assignments = fields.zipWithIndex.map { (field, idx) =>
      val fieldSelect = Select(elem.asTerm, field).asExprOf[Double]
      val arrayIdx = Expr(idx)
      '{ $array($offset + $arrayIdx) = $fieldSelect }
    }

    val result = assignments.foldLeft[Expr[Unit]]('{ () })((acc, expr) =>
      '{ $acc ; $expr }
    )
    result

  /** restore case class back from the array at specific offset */
  inline def read[T](array: Array[Double], offset: Int): T = ${ readImpl[T]('array, 'offset) }

  private def readImpl[T: Type](array: Expr[Array[Double]], offset: Expr[Int])(using Quotes): Expr[T] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val symbol = tpe.typeSymbol

    if (!symbol.flags.is(Flags.Case)) {
      report.errorAndAbort(s"Type ${Type.show[T]} must be a case class")
    }

    val fields = tpe.typeSymbol.caseFields
    val doubleFields = fields.filter(field => TypeRepr.of[Double] =:= tpe.memberType(field))

    if (doubleFields.length != fields.length) {
      report.errorAndAbort(s"All fields in ${Type.show[T]} must be of type Double")
    }

    val fieldValues = fields.zipWithIndex.map { (field, idx) =>
      val arrayIdx = Expr(idx)
      '{ $array($offset + $arrayIdx) }
    }

    val newExpr = New(TypeTree.of[T])
    val constructor = symbol.primaryConstructor
    val newInstance = Apply(Select(newExpr, constructor), fieldValues.map(_.asTerm)).asExprOf[T]
    newInstance
