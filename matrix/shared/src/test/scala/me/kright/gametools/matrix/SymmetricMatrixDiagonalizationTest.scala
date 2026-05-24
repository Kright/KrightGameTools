package me.kright.gametools.matrix

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SymmetricMatrixDiagonalizationTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private def dist(a: Matrix, b: Matrix): Double =
    Math.sqrt((a - b).data.map(v => v * v).sum)

  test("test matrix 2x2 diagonalization corner cases") {
    val matrixSize = 2
    val rotationsMixed = 1

    def makeDiagMatrics(i00: Double, i11: Double): Matrix = {
      val m = Matrix(2, 2)
      m(0, 0) = i00
      m(1, 1) = i11
      m
    }

    def makeRotMatrics(angle: Double): Matrix = {
      val cos = Math.cos(angle)
      val sin = Math.sin(angle)
      val m = Matrix(2, 2)
      m(0, 0) = cos
      m(0, 1) = -sin
      m(1, 0) = sin
      m(1, 1) = cos
      m
    }

    def rotateDiag(diag: Matrix, rot: Matrix): Matrix =
      rot * diag * rot.transposedCopy()

    val diags = Seq(
      makeDiagMatrics(1.0, 1.0),
      makeDiagMatrics(1.0, 2.0),
      makeDiagMatrics(1.0, 1.0 + 1e-6),
      makeDiagMatrics(1.0, 1.0 + 1e-12),
    )

    def swap01(m: Matrix): Matrix = {
      val r = Matrix(2, 2)
      r(0, 0) = m(1, 1)
      r(1, 1) = m(0, 0)

      r(0, 1) = m(0, 1)
      r(1, 0) = m(1, 0)
      r
    }

    val smallValues = Seq(1e-1, 1e-3, 1e-6, 1e-9, 1e-12, 1e-15, 1e-50)
    val angles =
      for (
        offset <- Seq(0.0, Math.PI * 0.5, Math.PI * 0.25, Math.PI * 3 / 4, Math.PI);
        sign <- Seq(1.0, -1.0);
        e <- smallValues
      ) yield offset + e * sign

    for (diag <- diags;
         angle <- angles) {
      val rot = makeRotMatrics(angle)

      val rotated = rotateDiag(diag, rot)

      val (diag2, eig2) = Matrix.symmetricMatrixToDiagonalAndEigenvectors(rotated)

      def errMsg: String =
        s"""
           |diag = $diag
           |diag2 = $diag2
           |wtf = ${Math.sqrt((diag - diag2).data.map(v => v * v).sum)}
           |
           |angle = $angle
           |rot = $rot
           |rotated = $rotated
           |eig2 = $eig2
           |""".stripMargin

      val err = Math.min(dist(diag, diag2), dist(swap01(diag), diag2))
      assert(err < 1e-15, errMsg)
    }
  }

  test("test matrix 2x2 diagonalization ") {
    val matrixSize = 2
    val rotationsMixed = 1

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, rotationsMixed, 0.01, 1.0)) { input =>
      val (diagonal, eigenvectors) = Matrix.symmetricMatrixToDiagonalAndEigenvectors(input)
      val recreated = eigenvectors * diagonal * eigenvectors.transposedCopy()
      assert(dist(input, recreated) < 1e-14)
    }
  }

  test("test matrix 4x4 diagonalization") {
    val matrixSize = 4
    val rotationsMixed = 6

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, rotationsMixed, 0.01, 1.0)) { input =>
      val (diagonal, eigenvectors) = Matrix.symmetricMatrixToDiagonalAndEigenvectors(input)
      val recreated = eigenvectors * diagonal * eigenvectors.transposedCopy()
      assert(dist(input, recreated) < 1e-14)
    }

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, rotationsMixed, 0.01, 1.0)) { input =>
      val (diagonal, eigenvectors) = Matrix.symmetricMatrixToDiagonalAndEigenvectors(input)
      val recreated = eigenvectors * diagonal * eigenvectors.transposedCopy()
      assert(dist(input, recreated) < 1e-14)
    }
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
