package me.kright.gametools.ga

case class BasisBlade(bits: Int, signature: Signature) derives CanEqual:
  require(bits >= 0 && bits < signature.bladesCount)

  def contains(v: Generator): Boolean =
    (bits & v.bitMask) != 0

  def generators: Seq[Generator] =
    Generator.getAll(signature).filter(contains)

  def grade: Int =
    Integer.bitCount(bits)

  def antiGrade: Int =
    signature.generatorsCount - grade

  def hasCommonBasisVectors(other: BasisBlade): Boolean =
    (bits & other.bits) != 0

  def getCommon(other: BasisBlade): BasisBlade =
    BasisBlade(bits & other.bits, signature)

  def isContraction(other: BasisBlade): Boolean =
    isLeftContraction(other) || isRightContraction(other)

  def isLeftContraction(other: BasisBlade): Boolean =
    (bits | other.bits) == other.bits

  def isRightContraction(other: BasisBlade): Boolean =
    other.isLeftContraction(this)

  def unsignedComplement: BasisBlade =
    BasisBlade(bits ^ signature.bitMask, signature)


object BasisBlade:
  def apply(generator: Generator, signature: Signature): BasisBlade =
    BasisBlade(generator.bitMask, signature)

  def scalar(signature: Signature): BasisBlade =
    BasisBlade(0, signature)

  def pseudoScalar(signature: Signature): BasisBlade =
    BasisBlade(signature.bitMask, signature)