package me.kright.gametools.pga3d

import me.kright.gametools.matrix.Matrix
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

  private def bivectorToMatrix(b: Pga3dBivector): Matrix =
    val m = Matrix(6, 1)
    FlatDoubleSerializer.write(b, m.data, 0)
    m

  private def matrixToBivector(m: Matrix): Pga3dBivector =
    require(m.h == 6, m.w == 1)
    FlatDoubleSerializer.read[Pga3dBivector](m.data, 0)

