package me.kright.gametools.symbolic.transform

class TransformSeq[T](val rules: Seq[PartialTransform[T]]) extends PartialTransform[T]:
  override def apply(symbolic: T): Option[T] =
    var current = symbolic
    var anyUpdated = false
    for (r <- rules) {
      r(current) match
        case Some(next) => {
          current = next
          anyUpdated = true
        }
        case None =>
    }
    if (anyUpdated) Option(current) else None

  override def withTransformedChildren(f: PartialTransform[T] => PartialTransform[T]): PartialTransform[T] =
    new TransformSeq[T](rules.map(f))  
