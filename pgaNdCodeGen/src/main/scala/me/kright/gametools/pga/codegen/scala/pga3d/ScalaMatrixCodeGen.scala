package me.kright.gametools.pga.codegen.scala.pga3d

import me.kright.gametools.pga.codegen.scala.common.ScalaCodeGenClass

class ScalaMatrixCodeGen extends ScalaCodeGenClass:

  override def name: String =
    "Pga3dMatrix"

  override def isObject: Boolean =
    true

  override def typeNamePrefix: String = "Pga3d"

  override def generatedComment: String = "me.kright.gametools.pga.codegen.scala.pga3d.ScalaMatrixCodeGen"

  override def targetPackage: String = "me.kright.gametools.pga3d"

  override def generateImports(): String =
    s"""import me.kright.gametools.matrix.Matrix
       |import me.kright.gametools.flatarray.FlatDoubleSerializer
       |""".stripMargin

  override def generateCode(): String =
    s"""object Pga3dMatrix:
       |  private val bivectors = Seq[Pga3dBivector](
       |    Pga3dBivector(1, 0, 0, 0, 0, 0),
       |    Pga3dBivector(0, 1, 0, 0, 0, 0),
       |    Pga3dBivector(0, 0, 1, 0, 0, 0),
       |    Pga3dBivector(0, 0, 0, 1, 0, 0),
       |    Pga3dBivector(0, 0, 0, 0, 1, 0),
       |    Pga3dBivector(0, 0, 0, 0, 0, 1),
       |  )
       |
       |  def multiply(matrix: Matrix, b: Pga3dBivector): Pga3dBivector =
       |    val m = matrix.data
       |
       |    inline def dot(offset: Int): Double =
       |      b.wx * m(offset) +
       |        b.wy * m(offset + 1) +
       |        b.wz * m(offset + 2) +
       |        b.xy * m(offset + 3) +
       |        b.xz * m(offset + 4) +
       |        b.yz * m(offset + 5)
       |
       |    Pga3dBivector(
       |      wx = dot(0),
       |      wy = dot(6),
       |      wz = dot(12),
       |      xy = dot(18),
       |      xz = dot(24),
       |      yz = dot(30),
       |    )
       |
       |  def linearMapping(map: Pga3dBivector => Pga3dBivector): Matrix =
       |    val matrix = Matrix(6, 6)
       |    for ((b, i) <- bivectors.zipWithIndex) {
       |      val mappedB = map(b)
       |      FlatDoubleSerializer.write(mappedB, matrix.data, i * 6)
       |    }
       |    matrix.transposeInplace()
       |    matrix
       |
       |""".stripMargin
