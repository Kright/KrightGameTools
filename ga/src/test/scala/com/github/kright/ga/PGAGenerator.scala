package com.github.kright.ga

import com.github.kright.math.VectorMathGenerators
import com.github.kright.mathutil.Sign
import org.scalacheck.Gen

object PGAGenerator:
  def allPGASeq: IndexedSeq[PGA] =
    IndexedSeq(GA.pga2, GA.pga3)

  def forAnyPGA(body: PGA ?=> Unit): Unit =
    for (basis <- allPGASeq) {
      basis.use {
        body
      }
    }

  def planeGen(using ga: PGA): Gen[MultiVector[Double]] =
    val vectors = ga.generators
    for (lst <- Gen.containerOfN[Seq, Double](vectors.size, VectorMathGenerators.double1))
      yield MultiVector(
        lst.zip(vectors).map { (value, vec) =>
          BasisBlade(vec, ga.signature) -> value
        }
      )

  def pointGen(using ga: PGA): Gen[MultiVector[Double]] =
    val vectors = ga.generators
    for (lst <- Gen.containerOfN[Seq, Double](vectors.size, VectorMathGenerators.double1))
      yield MultiVector(
        lst.zip(vectors).map { (value, vec) =>
          BasisBlade(vec, ga.signature) -> (if (vec.squareSign == Sign.Zero) 1.0 else value)
        }
      ).dual