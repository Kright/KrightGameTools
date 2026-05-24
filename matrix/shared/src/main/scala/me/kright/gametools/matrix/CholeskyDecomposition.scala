package me.kright.gametools.matrix

import me.kright.gametools.mathutil.FastRange
import scala.annotation.targetName

/**
 * Cholesky decomposition is defined for symmetrical positive-definite matrix.
 *
 * Matrix M = L * L.transposed()
 * inverted(M) = inverted(L).transposed() * inverted(L)
 *
 * L is a lower diagonal matrix
 *
 * [[https://en.wikipedia.org/wiki/Cholesky_decomposition]]
 */
object CholeskyDecomposition:

  /**
   * @return low triangular matrix L, so L * L.transposed = m
   */
  @targetName("invoke")
  def apply(m: Matrix): Matrix =
    require(m.isSquare)

    val L = Matrix(m.h, m.w)
    val size = m.h

    for (i <- FastRange(size)) {
      val Lii = {
        var summ = 0.0
        for (p <- FastRange(0, i)) {
          summ += square(L(i, p))
        }
        Math.sqrt(m(i, i) - summ)
      }

      L(i, i) = Lii

      for (j <- FastRange(i + 1, size)) {
        var summ = 0.0
        for (p <- FastRange(0, i)) {
          summ += L(i, p) * L(j, p)
        }
        L(j, i) = (m(j, i) - summ) / Lii
      }
    }

    L

  private def square(a: Double): Double = a * a

  def inplaceInvertLowerTriangularMatrix(L: Matrix): Unit =
    require(L.isSquare)

    val size = L.w

    for (i <- FastRange(size)) {
      L(i, i) = 1.0 / L(i, i)
    }

    for (i <- FastRange(1, size)) {
      for (j <- FastRange(0, i)) {
        var sum = 0.0
        for (k <- j until i) {
          sum += L(i, k) * L(k, j)
        }
        L(i, j) = -sum * L(i, i)
      }
    }

  def invertedLowerTriangularMatrix(L: Matrix): Matrix =
    val result = L.copy()
    inplaceInvertLowerTriangularMatrix(result)
    result

  def inverted(m: Matrix): Matrix =
    val L = apply(m)
    inplaceInvertLowerTriangularMatrix(L)
    L.transposedCopy() * L
