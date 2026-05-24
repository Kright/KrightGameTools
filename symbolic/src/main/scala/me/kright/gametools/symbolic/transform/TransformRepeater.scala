package me.kright.gametools.symbolic.transform

import scala.annotation.tailrec

class TransformRepeater[T](rule: PartialTransform[T], maxRepeatCount: Int) extends PartialTransform[T]:
  require(maxRepeatCount > 0)

  override def apply(symbolic: T): Option[T] =
    rule(symbolic).map(repeatRule(_, maxRepeatCount - 1))

  override def withTransformedChildren(f: PartialTransform[T] => PartialTransform[T]): PartialTransform[T] =
    new TransformRepeater[T](f(rule), maxRepeatCount)

  @tailrec
  private def repeatRule(current: T, remainingRetires: Int): T =
    if (remainingRetires > 0) {
      rule(current) match
        case None => current
        case Some(next) => repeatRule(next, remainingRetires - 1)
    }
    else current
    