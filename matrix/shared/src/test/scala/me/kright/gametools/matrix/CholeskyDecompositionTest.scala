package me.kright.gametools.matrix

import me.kright.arrayview.ArrayView2dFlat

import me.kright.arrayview.ArrayView2d

import me.kright.gametools.mathutil.FastRange
import org.scalactic.anyvals.PosInt
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.collection.mutable.ArrayBuffer

class CholeskyDecompositionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private val matrixSize = 4

  test("decomposition") {
    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, repeats = 10, 0.1, 10.0), MinSuccessful(100)) { matrix =>
      val L = CholeskyDecomposition(matrix)
      val restoredMatrix = L * L.transposed
      assert((matrix - restoredMatrix).frobeniusNorm < 1e-14)
    }
  }

  test("lower triangular matrix inversion") {
    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, repeats = 10, 0.1, 10.0), MinSuccessful(100)) { matrix =>
      val L = CholeskyDecomposition(matrix)
      val Linv = CholeskyDecomposition.invertedLowerTriangularMatrix(L)

      val mult = L * Linv

      assert((mult - identityMatrix(4)).frobeniusNorm < 2e-14)
    }
  }

  test("matrix inversion") {
    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, repeats = 10, 0.1, 10.0), MinSuccessful(100)) { matrix =>
      val inverted = CholeskyDecomposition.inverted(matrix)
      assert((matrix * inverted - identityMatrix(matrixSize)).frobeniusNorm < 1e-14)
    }
  }

  test("tiny decomposition text") {
    val matrices = new ArrayBuffer[ArrayView2d[Double]]()
    val result = new ArrayBuffer[ArrayView2d[Double]]()
    val repeats = 10000

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, repeats = 10, 0.1, 10.0), MinSuccessful(PosInt.from(repeats).get)) { matrix =>
      matrices += matrix
    }

    for (i <- FastRange(5)) {
      result.clear()
      val start = System.nanoTime()

      for (m <- matrices) {
        result += CholeskyDecomposition.inverted(m)
      }

      val end = System.nanoTime()

//      println(s"${(end - start) / repeats} ns")
//      println(s"max = ${result.map(_.frobeniusNorm).max}")
    }
  }

  test("tiny multiplication text") {
    val matrices = new ArrayBuffer[ArrayView2d[Double]]()
    val result = new ArrayBuffer[ArrayView2d[Double]]()
    val repeats = 10000

    forAll(MatrixGenerators.rotatedDiagonal(matrixSize, repeats = 10, 0.1, 10.0), MinSuccessful(PosInt.from(repeats).get)) { matrix =>
      matrices += matrix
    }

    for (i <- FastRange(5)) {
      result.clear()
      val start = System.nanoTime()

      for (m <- matrices) {
        result += m * m
      }

      val end = System.nanoTime()

//      println(s"${(end - start) / repeats} ns")
//      println(s"max = ${result.map(_.frobeniusNorm).max}")
    }
  }
