package com.github.kright.matrix

import me.kright.gametools.mathutil.FastRange


/**
 * helper methods for matrix diagonalization
 */
object SymmetricMatrixDiagonalization:
  def toDiagonalAndEigenvectors(m: Matrix): (Matrix, Matrix) = {
    require(m.isSquare)
    val i = m.copy()
    val v = Matrix(m.h, m.w)
    v.setIdt()

    diagonalizeSymmetricInplace(i, (p, q, sin, cos) => rotateEigenvectors(v, p, q, sin, cos))

    (i, v)
  }

  /** first index is bigger */
  def findBiggestOffDiagonalElementByAbs(m: Matrix): (Int, Int) =
    require(m.isSquare)
    var maxAbs: Double = 0.0
    var maxX: Int = 1
    var maxY: Int = 0

    for (y <- FastRange(1, m.h);
         x <- FastRange(0, y)) {
      val v = m(y, x)
      val abs = Math.abs(v)
      if (abs > maxAbs) {
        maxAbs = abs
        maxX = x
        maxY = y
      }
    }

    (maxY, maxX)

  def findSinCosAtan(iPP: Double, iPQ: Double, iQQ: Double): (Double, Double) =
    val angle2 = {
      val r = Math.atan2(2.0 * iPQ, iPP - iQQ)
      if (r <= Math.PI / 2) {
        r
      } else {
        r - Math.PI
      }
    }
    val angle = angle2 * 0.5
    val angleCos = Math.cos(angle)
    val angleSin = Math.sin(angle)

    (angleSin, angleCos)

  def findSinCosTau(iPP: Double, iPQ: Double, iQQ: Double): (Double, Double) =
    val tau = (iPP - iQQ) / (2.0 * iPQ)

    val t =
      if (tau >= 0) 1.0 / (tau + Math.sqrt(1.0 + tau * tau))
      else -1.0 / (-tau + Math.sqrt(1.0 + tau * tau))

    val cos = 1.0 / Math.sqrt(t * t + 1.0)
    val sin = t * cos

    (sin, cos)

  def rotateEigenvectors(v: Matrix, p: Int, q: Int, sin: Double, cos: Double): Unit =
    for (k <- 0 until v.w) {
      val vKP = v(k, p)
      val vKQ = v(k, q)
      v(k, p) = cos * vKP + sin * vKQ
      v(k, q) = -sin * vKP + cos * vKQ
    }

  def sandwichRotSymmetricMatrix(i: Matrix, p: Int, q: Int, sin: Double, cos: Double): Unit = {
    val cos2 = cos * cos
    val sin2 = sin * sin
    val sincos = sin * cos

    val iPP = i(p, p)
    val iPQ = i(p, q)
    val iQQ = i(q, q)

    i(p, p) = cos2 * iPP + sin2 * iQQ + 2.0 * sincos * iPQ
    i(q, q) = cos2 * iQQ + sin2 * iPP - 2.0 * sincos * iPQ
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

  def diagonalizeSymmetricInplace(i: Matrix, doStep: (Int, Int, Double, Double) => Unit): Unit =
    require(i.isSquare)
    while (true) {
      val (p, q) = findBiggestOffDiagonalElementByAbs(i)
      val iPP = i(p, p)
      val iPQ = i(p, q)
      val iQQ = i(q, q)

      if (Math.abs(iPQ * 1e16) < Math.abs(iPP) + Math.abs(iQQ)) return
      val (sin, cos) = findSinCosTau(iPP, iPQ, iQQ)
      sandwichRotSymmetricMatrix(i, p, q, sin, cos)
      doStep(p, q, sin, cos)
    }
