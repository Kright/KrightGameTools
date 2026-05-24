package me.kright.gametools.symbolic.transform

import scala.util.{Failure, Success, Try}

trait PartialTransformListener[T] extends ((PartialTransform[T], T, Try[Option[T]]) => Unit):
  def apply(partialTransform: PartialTransform[T], argument: T, result: Try[Option[T]]): Unit

object PartialTransformListener:
  def printAll[T](): PartialTransformListener[T] =
    (partialTransform: PartialTransform[T], argument: T, result: Try[Option[T]]) =>
      println(s"${partialTransform.getClass.getSimpleName}: ${argument} => ${result}")

  def printTransformed[T](): PartialTransformListener[T] =
    (partialTransform: PartialTransform[T], argument: T, result: Try[Option[T]]) =>
      result match
        case Failure(exception) =>
          println(s"ERROR ${partialTransform.getClass.getSimpleName}: ${argument} => ${exception}")
        case Success(option) =>
          option.foreach { r =>
            println(s"${partialTransform.getClass.getSimpleName}: ${argument} => ${r}")
          }
