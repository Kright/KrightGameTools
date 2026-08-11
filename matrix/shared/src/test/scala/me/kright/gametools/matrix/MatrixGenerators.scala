package me.kright.gametools.matrix

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}
import org.scalacheck.Gen

object MatrixGenerators:
  def diagonalPositiveMatrix(size: Int, min: Double, max: Double): Gen[ArrayView2dFlat[Double]] =
    Gen.containerOfN[Array, Double](size, Gen.double.map(v => min + v * (max - min))).map { arr =>
      val m = ArrayView2dFlat[Double](size, size)
      arr.zipWithIndex.foreach { (v, i) =>
        m(i, i) = v
      }
      m
    }

  def singleAxisRotationMatrix(size: Int): Gen[ArrayView2dFlat[Double]] =
    for {
      i <- Gen.oneOf(1 until size)
      j <- Gen.oneOf(0 until i)
      angle <- Gen.double.map(_ * Math.PI * 2)
    } yield {
      val m = ArrayView2dFlat[Double](size, size)
      m.setIdentity()
      val cos = Math.cos(angle)
      val sin = Math.sin(angle)
      m(i, i) = cos
      m(j, j) = cos
      m(i, j) = -sin
      m(j, i) = sin
      m
    }

  def rotationMatrix(size: Int, repeats: Int): Gen[ArrayView2dFlat[Double]] =
    Gen.containerOfN[Array, ArrayView2dFlat[Double]](repeats, singleAxisRotationMatrix(size)).map { arr =>
      arr.reduce(_ * _)
    }

  def matrixUniform1(h: Int, w: Int): Gen[ArrayView2dFlat[Double]] =
    Gen.containerOfN[Array, Double](w * h, Gen.double.map(v => v * 2.0 - 1.0)).map { arr =>
      ArrayView2dFlat(arr, h, w)
    }

  def rotatedDiagonal(size: Int, repeats: Int, min: Double, max: Double): Gen[ArrayView2dFlat[Double]] =
    for {
      diagonal <- diagonalPositiveMatrix(size, min, max)
      rotation <- rotationMatrix(size, repeats)
    } yield rotation * diagonal * rotation.transposed
