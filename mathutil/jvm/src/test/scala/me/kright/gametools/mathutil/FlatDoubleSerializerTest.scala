package me.kright.gametools.mathutil

import me.kright.gametools.mathutil.FlatDoubleSerializer
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

private final case class Vector3d(x: Double, y: Double, z: Double)

private final case class Pga3dBivector(wx: Double = 0.0,
                                       wy: Double = 0.0,
                                       wz: Double = 0.0,
                                       xy: Double = 0.0,
                                       xz: Double = 0.0,
                                       yz: Double = 0.0)

class FlatDoubleSerializerTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val vector3dGen: Gen[Vector3d] = for {
    x <- Gen.double
    y <- Gen.double
    z <- Gen.double
  } yield Vector3d(x, y, z)

  private val pga3dBivectorGen: Gen[Pga3dBivector] = for {
    wx <- Gen.double
    wy <- Gen.double
    wz <- Gen.double
    xy <- Gen.double
    xz <- Gen.double
    yz <- Gen.double
  } yield Pga3dBivector(wx, wy, wz, xy, xz, yz)
  test("getSize should return correct number of fields") {
    assert(FlatDoubleSerializer.getSize[Vector3d] == 3)
    assert(FlatDoubleSerializer.getSize[Pga3dBivector] == 6)
  }

  test("serialize and deserialize should work for Vector3d") {
    val vec = Vector3d(1.0, 2.0, 3.0)
    val arr = new Array[Double](3)

    FlatDoubleSerializer.write(vec, arr, 0)
    assert(arr(0) == 1.0)
    assert(arr(1) == 2.0)
    assert(arr(2) == 3.0)

    val restored = FlatDoubleSerializer.read[Vector3d](arr, 0)
    assert(restored.x == 1.0)
    assert(restored.y == 2.0)
    assert(restored.z == 3.0)
  }

  test("serialize and deserialize should work for Pga3dBivector") {
    val bivector = Pga3dBivector(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    val arr = new Array[Double](6)

    FlatDoubleSerializer.write(bivector, arr, 0)
    assert(arr.toSeq == Seq(1.0, 2.0, 3.0, 4.0, 5.0, 6.0))

    val restored = FlatDoubleSerializer.read[Pga3dBivector](arr, 0)
    assert(restored.wx == 1.0)
    assert(restored.wy == 2.0)
    assert(restored.wz == 3.0)
    assert(restored.xy == 4.0)
    assert(restored.xz == 5.0)
    assert(restored.yz == 6.0)
  }

  test("serialize and deserialize should work with offset") {
    val vec = Vector3d(1.0, 2.0, 3.0)
    val arr = new Array[Double](5)

    FlatDoubleSerializer.write(vec, arr, 2)
    assert(arr(2) == 1.0)
    assert(arr(3) == 2.0)
    assert(arr(4) == 3.0)

    val restored = FlatDoubleSerializer.read[Vector3d](arr, 2)
    assert(restored.x == 1.0)
    assert(restored.y == 2.0)
    assert(restored.z == 3.0)
  }

  test("property: Vector3d serialization/deserialization roundtrip") {
    forAll(vector3dGen) { vec =>
      val arr = new Array[Double](3)
      FlatDoubleSerializer.write(vec, arr, 0)
      val restored = FlatDoubleSerializer.read[Vector3d](arr, 0)
      assert(restored.x == vec.x)
      assert(restored.y == vec.y)
      assert(restored.z == vec.z)
    }
  }

  test("property: Pga3dBivector serialization/deserialization roundtrip") {
    forAll(pga3dBivectorGen) { bivector =>
      val arr = new Array[Double](6)
      FlatDoubleSerializer.write(bivector, arr, 0)
      val restored = FlatDoubleSerializer.read[Pga3dBivector](arr, 0)
      assert(restored.wx == bivector.wx)
      assert(restored.wy == bivector.wy)
      assert(restored.wz == bivector.wz)
      assert(restored.xy == bivector.xy)
      assert(restored.xz == bivector.xz)
      assert(restored.yz == bivector.yz)
    }
  }

  test("property: serialization/deserialization with random offset") {
    forAll(vector3dGen, Gen.choose(0, 10)) { (vec, offset) =>
      val arr = new Array[Double](offset + 3)
      FlatDoubleSerializer.write(vec, arr, offset)
      val restored = FlatDoubleSerializer.read[Vector3d](arr, offset)
      assert(restored.x == vec.x)
      assert(restored.y == vec.y)
      assert(restored.z == vec.z)
    }
  }

  test("derived serializer should work") {
    val serializer = FlatDoubleSerializer.derived[Vector3d]
    val vec = Vector3d(1.0, 2.0, 3.0)
    val arr = new Array[Double](3)
    serializer.write(vec, arr, 0)
    assert(arr(0) == 1.0)
    assert(arr(1) == 2.0)
    assert(arr(2) == 3.0)
    val restored = serializer.read(arr, 0)
    assert(restored.x == vec.x)
    assert(restored.y == vec.y)
    assert(restored.z == vec.z)
  }
