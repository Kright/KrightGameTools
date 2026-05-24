package me.kright.gametools.matrix

import org.scalacheck.Gen

object MatrixGenerators:
  def diagonalPositiveMatrix(size: Int, min: Double, max: Double): Gen[Matrix] =
    Gen.containerOfN[Array, Double](size, Gen.double.map(v => min + v * (max - min))).map { arr =>
      val m = Matrix(size, size)
      arr.zipWithIndex.foreach { (v, i) =>
        m(i, i) = v
      }
      m
    }

  def singleAxisRotationMatrix(size: Int): Gen[Matrix] =
    for {
      i <- Gen.oneOf(1 until size)
      j <- Gen.oneOf(0 until i)
      angle <- Gen.double.map(_ * Math.PI * 2)
    } yield {
      val m = Matrix(size, size)
      m.setIdt()
      val cos = Math.cos(angle)
      val sin = Math.sin(angle)
      m(i, i) = cos
      m(j, j) = cos
      m(i, j) = -sin
      m(j, i) = sin
      m
    }

  def rotationMatrix(size: Int, repeats: Int): Gen[Matrix] =
    Gen.containerOfN[Array, Matrix](repeats, singleAxisRotationMatrix(size)).map { arr =>
      arr.reduce(_ * _)
    }

  def matrixUniform1(h: Int, w: Int): Gen[Matrix] =
    Gen.containerOfN[Array, Double](w * h, Gen.double.map(v => v * 2.0 - 1.0)).map { arr =>
      Matrix(h, w, arr)
    }

  def rotatedDiagonal(size: Int, repeats: Int, min: Double, max: Double): Gen[Matrix] =
    for {
      diagonal <- diagonalPositiveMatrix(size, min, max)
      rotation <- rotationMatrix(size, repeats)
    } yield rotation * diagonal * rotation.transposedCopy()