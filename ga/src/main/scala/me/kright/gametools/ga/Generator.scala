package me.kright.gametools.ga

import me.kright.gametools.mathutil.Sign

case class Generator(number: Int, squareSign: Sign) extends Ordered[Generator]:

  val bitMask: Int = 1 << number

  override def compare(that: Generator): Int =
    (number, squareSign) compare(that.number, that.squareSign)


object Generator:
  // the signature guarantees a single squareSign per number, so ordering by number matches `compare` above
  given ordering: Ordering[Generator] = Ordering.by(_.number)

  def apply(number: Int, signature: Signature): Generator =
    new Generator(number, signature.getSign(number))

  def getAll(signature: Signature): Seq[Generator] =
    for (i <- 0 until signature.generatorsCount)
      yield new Generator(i, signature.getSign(i))
