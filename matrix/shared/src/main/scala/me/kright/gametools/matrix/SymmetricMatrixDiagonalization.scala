package me.kright.gametools.matrix

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}

import me.kright.gametools.mathutil.FastRange


/**
 * The eigendecomposition of a symmetric matrix by the classical (greedy) Jacobi method:
 * on every rotation the largest off-diagonal element is zeroed by a two-sided Givens rotation.
 * The method of choice for the small matrices this library works with (3x3 inertias and the
 * like): backward stable, high relative accuracy of the eigenvalues, quadratic convergence.
 */
object SymmetricMatrixDiagonalization:

  def eigen(m: ArrayView2d[Double]): Eigen = {
    require(m.isSquare)
    val work = m.copy
    val vectors = ArrayView2dFlat[Double](m.h, m.w)
    vectors.setIdentity()

    diagonalizeSymmetricInplace(work, (p, q, sin, cos) => rotateEigenvectors(vectors, p, q, sin, cos))

    Eigen(work.diagonal.copy, vectors)
  }

  /**
   * Diagonalizes the symmetric matrix in place; `doStep(p, q, sin, cos)` is called after every
   * rotation so the caller can accumulate the eigenvectors in its own representation (a matrix,
   * a rotor, ...).
   *
   * Rotations stop when the largest off-diagonal element is negligible relative to its two
   * diagonal entries. A zeroed element is re-polluted by later rotations sharing its row or
   * column, so the count is not n*(n-1)/2: the convergence is quadratic, and because a rotation
   * never mixes the diagonal back into the off-diagonal, the off-diagonal keeps shrinking
   * multiplicatively below the machine epsilon (measured on random matrices: ~3-4 sweeps to the
   * threshold, ~6-7 sweeps to an exactly zero off-diagonal). The internal rotation cap is a
   * far-above-that backstop, not a tunable: it turns a NaN input (the exit comparison is always
   * false for NaN) from an infinite loop into a best-effort return - the caller sees the NaN in
   * the result.
   */
  def diagonalizeSymmetricInplace(i: ArrayView2d[Double], doStep: (Int, Int, Double, Double) => Unit): Unit =
    require(i.isSquare)
    val maxRotations = 30 * i.h * (i.h - 1) / 2
    for (_ <- FastRange(maxRotations)) {
      val (p, q) = findBiggestOffDiagonalElementByAbs(i)
      val iPP = i(p, p)
      val iPQ = i(p, q)
      val iQQ = i(q, q)

      // <= and not <: for an all-zero submatrix (a point mass at the origin) both sides are 0,
      // and rotating with iPQ == 0 would produce tau = 0/0 = NaN
      if (Math.abs(iPQ * 1e16) <= Math.abs(iPP) + Math.abs(iQQ)) return
      val (sin, cos) = findSinCos(iPP, iPQ, iQQ)
      sandwichRotSymmetricMatrix(i, p, q, sin, cos)
      doStep(p, q, sin, cos)
    }

  /** the largest |element| below the diagonal; the first index is bigger */
  private def findBiggestOffDiagonalElementByAbs(m: ArrayView2d[Double]): (Int, Int) =
    var maxAbs: Double = 0.0
    var maxX: Int = 1
    var maxY: Int = 0

    for (y <- FastRange(1, m.h);
         x <- FastRange(0, y)) {
      val abs = Math.abs(m(y, x))
      if (abs > maxAbs) {
        maxAbs = abs
        maxX = x
        maxY = y
      }
    }

    (maxY, maxX)

  /**
   * the Jacobi rotation zeroing the (p, q) element (Rutishauser's stable form): the smaller of
   * the two candidate angles, |angle| <= pi/4, computed without trigonometric functions
   */
  private def findSinCos(iPP: Double, iPQ: Double, iQQ: Double): (Double, Double) =
    val tau = (iPP - iQQ) / (2.0 * iPQ)

    val t =
      if (tau >= 0) 1.0 / (tau + Math.sqrt(1.0 + tau * tau))
      else -1.0 / (-tau + Math.sqrt(1.0 + tau * tau))

    val cos = 1.0 / Math.sqrt(t * t + 1.0)
    val sin = t * cos

    (sin, cos)

  private def rotateEigenvectors(v: ArrayView2d[Double], p: Int, q: Int, sin: Double, cos: Double): Unit =
    for (k <- 0 until v.w) {
      val vKP = v(k, p)
      val vKQ = v(k, q)
      v(k, p) = cos * vKP + sin * vKQ
      v(k, q) = -sin * vKP + cos * vKQ
    }

  /**
   * applies the two-sided rotation J^T * i * J in place, for the (sin, cos) of the Jacobi
   * rotation zeroing the (p, q) element - the diagonal update uses Rutishauser's cancellation-free
   * form a_pp + t * a_pq, which is valid only for that zeroing angle
   */
  private def sandwichRotSymmetricMatrix(i: ArrayView2d[Double], p: Int, q: Int, sin: Double, cos: Double): Unit = {
    val t = sin / cos
    val iPQ = i(p, q)

    i(p, p) = i(p, p) + t * iPQ
    i(q, q) = i(q, q) - t * iPQ
    i(p, q) = 0.0
    i(q, p) = 0.0

    for (j <- FastRange(i.w)) {
      if (j != p && j != q) {
        val iJp = i(j, p)
        val iJq = i(j, q)
        val iJPnew = cos * iJp + sin * iJq
        val iJQnew = -sin * iJp + cos * iJq
        i(j, p) = iJPnew
        i(p, j) = iJPnew
        i(j, q) = iJQnew
        i(q, j) = iJQnew
      }
    }
  }
