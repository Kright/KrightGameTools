package me.kright.gametools.ga

import org.scalacheck.Gen

object GAGenerator:
  def allGASeq: IndexedSeq[GA] =
    IndexedSeq(GA.ga2, GA.ga3, GA.ga4, GA.pga2, GA.pga3)

  def allGa: Gen[GA] =
    Gen.oneOf(allGASeq)

  def forAnyGA(body: GA ?=> Unit): Unit =
    for (basis <- allGASeq) {
      basis.use {
        body
      }
    }

  def basisBladesGen(using ga: GA): Gen[BasisBlade] =
    Gen.oneOf(ga.blades)

  def basisMultivectorsGen(using ga: GA): Gen[MultiVector[Double]] =
    basisBladesGen.map(MultiVector(ga, _))

  def multivectorsGen(using ga: GA): Gen[MultiVector[Double]] =
    Gen.containerOfN[Seq, Double](ga.signature.bladesCount, Gen.double).map { values =>
      require(values.length == ga.signature.bladesCount)
      MultiVector(ga.blades.zip(values))(using ga)
    }

  def bladesGen(grade: Int)(using ga: GA): Gen[MultiVector[Double]] =
    multivectorsGen.map(_.grade(grade))
