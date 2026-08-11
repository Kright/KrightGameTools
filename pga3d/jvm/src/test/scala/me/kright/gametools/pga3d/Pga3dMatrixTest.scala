package me.kright.gametools.pga3d

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}

import me.kright.gametools.matrix.*
import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dMatrixTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("matrix linear mapping is correct") {
    forAll(Pga3dGenerators.matrices(6, 6)) { matrix =>
      val mapping = Pga3dMatrix.linearMapping(b => matrixToBivector(matrix * bivectorToMatrix(b)))
      assert(Math.sqrt((matrix - mapping).data.map(v => v * v).sum) < 1e-15)
    }
  }

  test("matrix multiply with bivector") {
    forAll(Pga3dGenerators.matrices(6, 6), Pga3dGenerators.bivectors) { (matrix, b) =>
      val r1 = Pga3dMatrix.multiply(matrix, b)
      val r2 = matrixToBivector(matrix * bivectorToMatrix(b))
      assert((r1 - r2).norm < 1e-15)
    }
  }

  private def bivectorToMatrix(b: Pga3dBivector): ArrayView2dFlat[Double] =
    val m = ArrayView2dFlat[Double](6, 1)
    FlatDoubleSerializer.write(b, m.data, 0)
    m

  private def matrixToBivector(m: ArrayView2d[Double]): Pga3dBivector =
    require(m.h == 6, m.w == 1)
    FlatDoubleSerializer.read[Pga3dBivector](m.data, 0)

