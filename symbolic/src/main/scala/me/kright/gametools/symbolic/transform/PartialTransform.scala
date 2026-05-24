package me.kright.gametools.symbolic.transform

import me.kright.gametools.symbolic.Symbolic

trait PartialTransform[T] extends (T => Option[T]):
  override def apply(value: T): Option[T]

  def transform(value: T): T =
    apply(value).getOrElse(value)

  def repeat(maxRepeatCount: Int): TransformRepeater[T] =
    TransformRepeater(this, maxRepeatCount)

  def combine(after: PartialTransform[T]): PartialTransform[T] =
    PartialTransform(this, after)

  /** used for logging and testing */
  def withListener(logger: PartialTransformListener[T]): PartialTransform[T] =
    PartialTransformWithListener[T](this.withTransformedChildren(_.withListener(logger)), logger)

  /** override in transforms which combine other transforms */
  def withTransformedChildren(f: PartialTransform[T] => PartialTransform[T]): PartialTransform[T] = this

object PartialTransform:
  def any[T](transforms: PartialTransform[T]*): TransformAny[T] =
    TransformAny[T](transforms)

  def apply[T](transforms: PartialTransform[T]*): TransformSeq[T] =
    TransformSeq(transforms)

  extension [F, S](rule: PartialTransform[Symbolic[F, S]])
    def apply(values: Seq[Symbolic[F, S]]): Option[Seq[Symbolic[F, S]]] =
      val newElems = values.map(apply)
      if (newElems.forall(_.isEmpty)) return None
      Option(newElems.zip(values).map((next, prev) => next.getOrElse(prev)))
