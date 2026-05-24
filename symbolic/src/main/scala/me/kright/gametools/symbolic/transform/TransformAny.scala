package me.kright.gametools.symbolic.transform

class TransformAny[T](val rules: Seq[PartialTransform[T]]) extends PartialTransform[T]:
  override def apply(value: T): Option[T] =
    rules.view.flatMap(r => r(value)).headOption

  override def withTransformedChildren(f: PartialTransform[T] => PartialTransform[T]): PartialTransform[T] =
    new TransformAny[T](rules.map(f))
