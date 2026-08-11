package me.kright.gametools.pga2d

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}

import me.kright.gametools.matrix.*
import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dMatrixTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("matrix linear mapping is correct") {
    forAll(Pga2dGenerators.matrices(3, 3)) { matrix =>
      val mapping = Pga2dMatrix.linearMapping(p => matrixToPoint(matrix * pointToMatrix(p)))
      assert(Math.sqrt((matrix - mapping).data.map(v => v * v).sum) < 1e-15)
    }
  }

  test("matrix multiply with projective point") {
    forAll(Pga2dGenerators.matrices(3, 3), Pga2dGenerators.projectivePoints) { (matrix, p) =>
      val r1 = Pga2dMatrix.multiply(matrix, p)
      val r2 = matrixToPoint(matrix * pointToMatrix(p))
      assert((r1 - r2).norm < 1e-15)
    }
  }

  private def pointToMatrix(p: Pga2dProjectivePoint): ArrayView2dFlat[Double] =
    val m = ArrayView2dFlat[Double](3, 1)
    FlatDoubleSerializer.write(p, m.data, 0)
    m

  private def matrixToPoint(m: ArrayView2d[Double]): Pga2dProjectivePoint =
    require(m.h == 3, m.w == 1)
    FlatDoubleSerializer.read[Pga2dProjectivePoint](m.data, 0)
