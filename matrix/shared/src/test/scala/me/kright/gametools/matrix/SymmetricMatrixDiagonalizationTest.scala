package me.kright.gametools.matrix

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.collection.immutable.ArraySeq

class SymmetricMatrixDiagonalizationTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private def dist(a: ArrayView2d[Double], b: ArrayView2d[Double]): Double =
    Math.sqrt((a - b).data.map(v => v * v).sum)

  test("test matrix 2x2 diagonalization corner cases") {
    val matrixSize = 2
    val rotationsMixed = 1

    def makeDiagMatrics(i00: Double, i11: Double): ArrayView2dFlat[Double] = {
      val m = ArrayView2dFlat[Double](2, 2)
      m(0, 0) = i00
      m(1, 1) = i11
      m
    }

    def makeRotMatrics(angle: Double): ArrayView2dFlat[Double] = {
      val cos = Math.cos(angle)
      val sin = Math.sin(angle)
      val m = ArrayView2dFlat[Double](2, 2)
      m(0, 0) = cos
      m(0, 1) = -sin
      m(1, 0) = sin
      m(1, 1) = cos
      m
    }

    def rotateDiag(diag: ArrayView2d[Double], rot: ArrayView2d[Double]): ArrayView2dFlat[Double] =
      rot * diag * rot.transposed

    val diags = ArraySeq(
      makeDiagMatrics(1.0, 1.0),
      makeDiagMatrics(1.0, 2.0),
      makeDiagMatrics(1.0, 1.0 + 1e-6),
      makeDiagMatrics(1.0, 1.0 + 1e-12),
    )

    val smallValues = ArraySeq(1e-1, 1e-3, 1e-6, 1e-9, 1e-12, 1e-15, 1e-50)
    val angles =
      for (
        offset <- ArraySeq(0.0, Math.PI * 0.5, Math.PI * 0.25, Math.PI * 3 / 4, Math.PI);
        sign <- ArraySeq(1.0, -1.0);
        e <- smallValues
      ) yield offset + e * sign

    for (diag <- diags;
         angle <- angles) {
      val rot = makeRotMatrics(angle)

      val rotated = rotateDiag(diag, rot)

      val Eigen(values, eig2) = SymmetricMatrixDiagonalization.eigen(rotated)

      def errMsg: String =
        s"""
           |diag = $diag
           |values = ${values.data.mkString(", ")}
           |
           |angle = $angle
           |rot = $rot
           |rotated = $rotated
           |eig2 = $eig2
           |""".stripMargin

      def valuesError(v0: Double, v1: Double): Double =
        Math.hypot(values(0) - v0, values(1) - v1)

      // the eigenvalues may come out in either order
      val err = Math.min(valuesError(diag(0, 0), diag(1, 1)), valuesError(diag(1, 1), diag(0, 0)))
      assert(err < 1e-15, errMsg)
    }
  }

  test("test matrix 2x2 diagonalization ") {
    val matrixSize = 2
    val rotationsMixed = 1

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, rotationsMixed, 0.01, 1.0)) { input =>
      val Eigen(diagonal, eigenvectors) = SymmetricMatrixDiagonalization.eigen(input)
      val recreated = eigenvectors * diagonalMatrix(diagonal) * eigenvectors.transposed
      assert(dist(input, recreated) < 1e-14)
    }
  }

  test("test matrix 4x4 diagonalization") {
    val matrixSize = 4
    val rotationsMixed = 6

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, rotationsMixed, 0.01, 1.0)) { input =>
      val Eigen(diagonal, eigenvectors) = SymmetricMatrixDiagonalization.eigen(input)
      val recreated = eigenvectors * diagonalMatrix(diagonal) * eigenvectors.transposed
      assert(dist(input, recreated) < 1e-14)
    }

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, rotationsMixed, 0.01, 1.0)) { input =>
      val Eigen(diagonal, eigenvectors) = SymmetricMatrixDiagonalization.eigen(input)
      val recreated = eigenvectors * diagonalMatrix(diagonal) * eigenvectors.transposed
      assert(dist(input, recreated) < 1e-14)
    }
  }

  test("a NaN input terminates with a best-effort result instead of hanging") {
    val m = matrixFromValues(3, 3)(
      1.0, 2.0, 3.0,
      2.0, Double.NaN, 4.0,
      3.0, 4.0, 5.0,
    )
    val Eigen(diagonal, _) = SymmetricMatrixDiagonalization.eigen(m)
    assert(diagonal.data.exists(_.isNaN))
  }

  test("matrix multiplication is associative") {
    val size = 4
    forAll(
      MatrixGenerators.matrixUniform1(size, size),
      MatrixGenerators.matrixUniform1(size, size),
      MatrixGenerators.matrixUniform1(size, size),
    ) { (a, b, c) =>
      val r1 = (a * b) * c
      val r2 = a * (b * c)
      assert(dist(r1, r2) < 1e-14)
    }
  }
