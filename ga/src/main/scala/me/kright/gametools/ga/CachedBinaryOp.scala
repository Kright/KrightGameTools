package me.kright.gametools.ga

class CachedBinaryOp(val signature: Signature,
                     val binaryOp: BinaryOp) extends BinaryOp:
  private val vectorsCount = signature.generatorsCount
  private val data = new Array[BasisBladeWithSign | Null](1 << (vectorsCount * 2))

  override def apply(left: BasisBlade, right: BasisBlade): BasisBladeWithSign =
    val pos = (left.bits << vectorsCount) + right.bits
    data.getOrElseUpdate(pos, binaryOp(left, right))
