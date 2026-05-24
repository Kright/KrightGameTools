package me.kright.gametools.ga

import com.github.kright.mathutil.Sign

case class Signature(positives: Int,
                     negatives: Int,
                     zeros: Int):
  require(positives >= 0)
  require(negatives >= 0)
  require(zeros >= 0)

  val generatorsCount: Int = positives + negatives + zeros
  val bladesCount: Int = 1 << generatorsCount
  val bitMask: Int = (1 << generatorsCount) - 1

  def getSign(position: Int): Sign =
    require(position >= 0 && position < generatorsCount, s"position ${position} should be in 0 >= and ${generatorsCount}")
    if (position < zeros) Sign.Zero
    else if (position < zeros + positives) Sign.Positive
    else Sign.Negative


object Signature:
  def ga2: Signature = Signature(2, 0, 0)
  def ga3: Signature = Signature(3, 0, 0)
  def ga4: Signature = Signature(4, 0, 0)

  def pga2: Signature = Signature(2, 0, 1)
  def pga3: Signature = Signature(3, 0, 1)
