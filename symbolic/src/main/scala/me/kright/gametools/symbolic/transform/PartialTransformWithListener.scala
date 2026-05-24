package me.kright.gametools.symbolic.transform

import scala.util.Try

class PartialTransformWithListener[T](val wrapped: PartialTransform[T], logger: PartialTransformListener[T]) extends PartialTransform[T]:
  override def apply(value: T): Option[T] =
    val result = Try(wrapped(value))
    logger(wrapped, value, result)
    result.get

  override def withTransformedChildren(f: PartialTransform[T] => PartialTransform[T]): PartialTransform[T] =
    new PartialTransformWithListener[T](f(wrapped), logger)
