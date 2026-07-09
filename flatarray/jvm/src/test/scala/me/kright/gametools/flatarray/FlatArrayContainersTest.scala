package me.kright.gametools.flatarray

import FlatView.*
import FlatMutableView.*
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

private final case class Vec2d(x: Double, y: Double) derives FlatDoubleSerializer, CanEqual

private final case class Vec3d(x: Double, y: Double, z: Double) derives FlatDoubleSerializer, CanEqual

private final case class Bivector6(wx: Double = 0.0,
                                    wy: Double = 0.0,
                                    wz: Double = 0.0,
                                    xy: Double = 0.0,
                                    xz: Double = 0.0,
                                    yz: Double = 0.0) derives FlatDoubleSerializer, CanEqual


class FlatArrayContainersTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val vec2dGen: Gen[Vec2d] = for {
    x <- Gen.double
    y <- Gen.double
  } yield Vec2d(x, y)

  private val vec3dGen: Gen[Vec3d] = for {
    x <- Gen.double
    y <- Gen.double
    z <- Gen.double
  } yield Vec3d(x, y, z)

  private val bivector6Gen: Gen[Bivector6] = for {
    wx <- Gen.double
    wy <- Gen.double
    wz <- Gen.double
    xy <- Gen.double
    xz <- Gen.double
    yz <- Gen.double
  } yield Bivector6(wx, wy, wz, xy, xz, yz)

  // --- read/write roundtrip through FlatArray, for several field counts ---

  test("FlatArray roundtrip for Vec2d (2 fields)") {
    forAll(Gen.listOf(vec2dGen)) { elems =>
      val arr = FlatArray[Vec2d](elems.size)
      for (i <- elems.indices) {
        arr(i) = elems(i)
      }
      for (i <- elems.indices) {
        assert(arr(i) == elems(i))
      }
    }
  }

  test("FlatArray roundtrip for Vec3d (3 fields)") {
    forAll(Gen.listOf(vec3dGen)) { elems =>
      val arr = FlatArray[Vec3d](elems.size)
      for (i <- elems.indices) {
        arr(i) = elems(i)
      }
      for (i <- elems.indices) {
        assert(arr(i) == elems(i))
      }
    }
  }

  test("FlatArray roundtrip for Bivector6 (6 fields)") {
    forAll(Gen.listOf(bivector6Gen)) { elems =>
      val arr = FlatArray[Bivector6](elems.size)
      for (i <- elems.indices) {
        arr(i) = elems(i)
      }
      for (i <- elems.indices) {
        assert(arr(i) == elems(i))
      }
    }
  }

  test("FlatArray.apply allocates exactly size * stride doubles") {
    val arr = FlatArray[Vec3d](5)
    assert(arr.size == 5)
    assert(arr.array.length == 5 * 3)
  }

  test("FlatArray.from copies elements out of an existing view") {
    val src = FlatArray[Vec3d](3)
    src(0) = Vec3d(1.0, 2.0, 3.0)
    src(1) = Vec3d(4.0, 5.0, 6.0)
    src(2) = Vec3d(7.0, 8.0, 9.0)

    val copy = FlatArray.from(src)
    assert(copy.size == 3)
    assert(!(copy.array eq src.array))
    for (i <- 0 until 3) {
      assert(copy(i) == src(i))
    }
  }

  // --- foreach / foreachWithIndex ---

  test("foreach visits every element in order") {
    val arr = FlatArray[Vec3d](3)
    arr(0) = Vec3d(1.0, 2.0, 3.0)
    arr(1) = Vec3d(4.0, 5.0, 6.0)
    arr(2) = Vec3d(7.0, 8.0, 9.0)

    val seen = collection.mutable.ArrayBuffer.empty[Vec3d]
    arr.foreach(v => seen += v)
    assert(seen.toList == List(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0)))
  }

  test("foreachWithIndex reports correct indices") {
    val arr = FlatArray[Vec3d](3)
    arr(0) = Vec3d(1.0, 2.0, 3.0)
    arr(1) = Vec3d(4.0, 5.0, 6.0)
    arr(2) = Vec3d(7.0, 8.0, 9.0)

    val seen = collection.mutable.ArrayBuffer.empty[(Vec3d, Int)]
    arr.foreachWithIndex((v, i) => seen += ((v, i)))
    assert(seen.toList == List((Vec3d(1.0, 2.0, 3.0), 0), (Vec3d(4.0, 5.0, 6.0), 1), (Vec3d(7.0, 8.0, 9.0), 2)))
  }

  // --- mapTo / zipTo, including dst aliasing a source ---

  test("mapTo writes mapped elements into a distinct destination") {
    val src = FlatArray[Vec3d](3)
    src(0) = Vec3d(1.0, 2.0, 3.0)
    src(1) = Vec3d(4.0, 5.0, 6.0)
    src(2) = Vec3d(7.0, 8.0, 9.0)

    val dst = FlatArray[Vec3d](3)
    src.mapTo(dst)(v => Vec3d(v.x * 2, v.y * 2, v.z * 2))

    assert(dst(0) == Vec3d(2.0, 4.0, 6.0))
    assert(dst(1) == Vec3d(8.0, 10.0, 12.0))
    assert(dst(2) == Vec3d(14.0, 16.0, 18.0))
  }

  test("mapTo aliasing the same view (dst eq src) computes correctly") {
    val arr = FlatArray[Vec3d](3)
    arr(0) = Vec3d(1.0, 2.0, 3.0)
    arr(1) = Vec3d(4.0, 5.0, 6.0)
    arr(2) = Vec3d(7.0, 8.0, 9.0)

    arr.mapTo(arr)(v => Vec3d(v.x * 2, v.y * 2, v.z * 2))

    assert(arr(0) == Vec3d(2.0, 4.0, 6.0))
    assert(arr(1) == Vec3d(8.0, 10.0, 12.0))
    assert(arr(2) == Vec3d(14.0, 16.0, 18.0))
  }

  test("zipTo combines two views into a distinct destination") {
    val a = FlatArray[Vec3d](3)
    val b = FlatArray[Vec3d](3)
    for (i <- 0 until 3) {
      a(i) = Vec3d(i.toDouble, i.toDouble, i.toDouble)
      b(i) = Vec3d(1.0, 2.0, 3.0)
    }
    val dst = FlatArray[Vec3d](3)
    a.zipTo(b, dst)((x, y) => Vec3d(x.x + y.x, x.y + y.y, x.z + y.z))

    for (i <- 0 until 3) {
      assert(dst(i) == Vec3d(i + 1.0, i + 2.0, i + 3.0))
    }
  }

  test("zipTo with dst aliasing the first source (a.zipTo(b, a))") {
    val a = FlatArray[Vec3d](3)
    val b = FlatArray[Vec3d](3)
    for (i <- 0 until 3) {
      a(i) = Vec3d(i.toDouble, i.toDouble, i.toDouble)
      b(i) = Vec3d(1.0, 2.0, 3.0)
    }
    val expected = (0 until 3).map(i => Vec3d(i + 1.0, i + 2.0, i + 3.0))

    a.zipTo(b, a)((x, y) => Vec3d(x.x + y.x, x.y + y.y, x.z + y.z))

    for (i <- 0 until 3) {
      assert(a(i) == expected(i))
    }
  }

  test("zipTo with dst aliasing the second source (a.zipTo(b, b))") {
    val a = FlatArray[Vec3d](3)
    val b = FlatArray[Vec3d](3)
    for (i <- 0 until 3) {
      a(i) = Vec3d(i.toDouble, i.toDouble, i.toDouble)
      b(i) = Vec3d(1.0, 2.0, 3.0)
    }
    val expected = (0 until 3).map(i => Vec3d(i + 1.0, i + 2.0, i + 3.0))

    a.zipTo(b, b)((x, y) => Vec3d(x.x + y.x, x.y + y.y, x.z + y.z))

    for (i <- 0 until 3) {
      assert(b(i) == expected(i))
    }
  }

  // --- mapInPlace / fill ---

  test("mapInPlace updates every element") {
    val arr = FlatArray[Vec3d](3)
    arr(0) = Vec3d(1.0, 2.0, 3.0)
    arr(1) = Vec3d(4.0, 5.0, 6.0)
    arr(2) = Vec3d(7.0, 8.0, 9.0)

    arr.mapInPlace(v => Vec3d(v.x * 2, v.y * 2, v.z * 2))

    assert(arr(0) == Vec3d(2.0, 4.0, 6.0))
    assert(arr(1) == Vec3d(8.0, 10.0, 12.0))
    assert(arr(2) == Vec3d(14.0, 16.0, 18.0))
  }

  test("fill sets every element to the same value") {
    val arr = FlatArray[Vec3d](4)
    arr.fill(Vec3d(1.0, 2.0, 3.0))
    for (i <- 0 until 4) {
      assert(arr(i) == Vec3d(1.0, 2.0, 3.0))
    }
  }

  // --- toArray (cold interop) ---

  test("toArray materializes elements") {
    val arr = FlatArray[Vec3d](2)
    arr(0) = Vec3d(1.0, 2.0, 3.0)
    arr(1) = Vec3d(4.0, 5.0, 6.0)

    val result = arr.toArray
    assert(result.length == 2)
    assert(result(0) == Vec3d(1.0, 2.0, 3.0))
    assert(result(1) == Vec3d(4.0, 5.0, 6.0))
  }

  // --- FlatBuffer growth and view-validity-across-growth ---

  test("FlatBuffer grows and all elements read back correctly") {
    val buffer = FlatBuffer[Vec3d]()
    val expected = (0 until 5000).map(i => Vec3d(i.toDouble, i.toDouble * 2, i.toDouble * 3))

    for (v <- expected) {
      buffer += v
    }

    assert(buffer.size == 5000)
    for (i <- expected.indices) {
      assert(buffer(i) == expected(i))
    }
  }

  test("a FlatView handle onto a FlatBuffer sees data added after reallocation (view-validity-across-growth)") {
    val buffer = FlatBuffer[Vec3d]()
    // a thin wrapper that always re-reads buffer.array / buffer.size through `def`, like FlatBuffer itself
    val handle: FlatView[Vec3d] = new FlatView[Vec3d] {
      def array: Array[Double] = buffer.array
      def size: Int = buffer.size
    }

    buffer += Vec3d(1.0, 2.0, 3.0)
    assert(handle(0) == Vec3d(1.0, 2.0, 3.0))

    // force many reallocations
    for (i <- 1 until 2000) {
      buffer += Vec3d(i.toDouble, i.toDouble, i.toDouble)
    }

    assert(handle.size == 2000)
    assert(handle(0) == Vec3d(1.0, 2.0, 3.0))
    assert(handle(1999) == Vec3d(1999.0, 1999.0, 1999.0))
  }

  test("FlatBuffer.clear resets size but the array remains usable for further additions") {
    val buffer = FlatBuffer[Vec3d]()
    buffer += Vec3d(1.0, 2.0, 3.0)
    buffer += Vec3d(4.0, 5.0, 6.0)
    buffer.clear()
    assert(buffer.size == 0)

    buffer += Vec3d(9.0, 9.0, 9.0)
    assert(buffer.size == 1)
    assert(buffer(0) == Vec3d(9.0, 9.0, 9.0))
  }

  test("FlatBuffer.ensureCapacity grows backing without changing size") {
    val buffer = FlatBuffer[Vec3d]()
    buffer.ensureCapacity(100)
    assert(buffer.size == 0)
    assert(buffer.array.length >= 100 * 3)
  }

  // --- bounds checks ---

  test("apply out of range throws IndexOutOfBoundsException") {
    val arr = FlatArray[Vec3d](3)
    assertThrows[IndexOutOfBoundsException](arr(-1))
    assertThrows[IndexOutOfBoundsException](arr(3))
  }

  test("update out of range throws IndexOutOfBoundsException") {
    val arr = FlatArray[Vec3d](3)
    assertThrows[IndexOutOfBoundsException](arr(-1) = Vec3d(0.0, 0.0, 0.0))
    assertThrows[IndexOutOfBoundsException](arr(3) = Vec3d(0.0, 0.0, 0.0))
  }

  test("FlatBuffer apply/update reject an index within backing capacity but >= size") {
    val buffer = FlatBuffer[Vec3d]()
    buffer.ensureCapacity(10) // backing has room for 10 elements, but size is still 0
    assert(buffer.size == 0)
    assert(buffer.array.length >= 10 * 3)
    assertThrows[IndexOutOfBoundsException](buffer(0))
    assertThrows[IndexOutOfBoundsException](buffer(0) = Vec3d(0.0, 0.0, 0.0))

    buffer += Vec3d(1.0, 1.0, 1.0)
    assert(buffer(0) == Vec3d(1.0, 1.0, 1.0))
    assertThrows[IndexOutOfBoundsException](buffer(1))
  }

  // --- := ---

  test(":= copies from another view of the same size") {
    val a = FlatArray[Vec3d](3)
    a(0) = Vec3d(1.0, 2.0, 3.0)
    a(1) = Vec3d(4.0, 5.0, 6.0)
    a(2) = Vec3d(7.0, 8.0, 9.0)

    val b = FlatArray[Vec3d](3)
    b := a

    for (i <- 0 until 3) {
      assert(b(i) == a(i))
    }
    assert(!(b.array eq a.array))
  }

  test(":= throws IllegalArgumentException on size mismatch") {
    val a = FlatArray[Vec3d](3)
    val b = FlatArray[Vec3d](4)
    assertThrows[IllegalArgumentException](b := a)
  }

  // --- FlatBuffer.apply(sizeHint) ---

  test("FlatBuffer.apply() default sizeHint is 8") {
    val buffer = FlatBuffer[Vec3d]()
    assert(buffer.size == 0)
    assert(buffer.array.length == 8 * 3)
  }

  test("FlatBuffer.apply(sizeHint) allocates room for exactly sizeHint elements") {
    val buffer = FlatBuffer[Vec3d](20)
    assert(buffer.size == 0)
    assert(buffer.array.length == 20 * 3)
  }

  test("FlatBuffer content is correct after appends that don't trigger growth") {
    val buffer = FlatBuffer[Vec3d](10)
    val expected = (0 until 5).map(i => Vec3d(i.toDouble, i.toDouble * 2, i.toDouble * 3))
    for (v <- expected) {
      buffer += v
    }
    assert(buffer.size == 5)
    for (i <- expected.indices) {
      assert(buffer(i) == expected(i))
    }
  }

  test("FlatBuffer content is correct after appends that do trigger growth") {
    val buffer = FlatBuffer[Vec3d](2)
    val expected = (0 until 500).map(i => Vec3d(i.toDouble, i.toDouble * 2, i.toDouble * 3))
    for (v <- expected) {
      buffer += v
    }
    assert(buffer.size == 500)
    for (i <- expected.indices) {
      assert(buffer(i) == expected(i))
    }
  }

  test("FlatArray.newBuilder accumulates elements through internal growth") {
    val builder = FlatArray.newBuilder[Vec3d]
    builder.sizeHint(10)  // small hint to force growth
    val count = 300
    val expected = (0 until count).map(i => Vec3d(i.toDouble, i.toDouble * 2, i.toDouble * 3))

    for (i <- 0 until count) {
      builder += expected(i)
    }

    val result = builder.result()
    assert(result.size == count)
    for (i <- 0 until count) {
      assert(result(i) == expected(i))
    }
  }

  // --- FlatBuffer.ensureCapacity / grow ---

  test("ensureCapacity jumps straight to the requested capacity, preserving content, without changing size") {
    val buffer = FlatBuffer[Vec3d](2)
    buffer += Vec3d(1.0, 2.0, 3.0)
    buffer += Vec3d(4.0, 5.0, 6.0)

    buffer.ensureCapacity(1000)

    assert(buffer.size == 2)
    assert(buffer.array.length == 1000 * 3)
    assert(buffer(0) == Vec3d(1.0, 2.0, 3.0))
    assert(buffer(1) == Vec3d(4.0, 5.0, 6.0))
  }

  test("ensureCapacity is a no-op when capacity is already sufficient") {
    val buffer = FlatBuffer[Vec3d](100)
    val before = buffer.array
    buffer.ensureCapacity(10)
    assert(buffer.array eq before)
    assert(buffer.array.length == 100 * 3)
  }

  // --- FlatBuffer.++= (FlatView fast path) ---

  test("++= (FlatView) appends all elements from another FlatArray via a single arraycopy") {
    val src = FlatArray[Vec3d](3)
    src(0) = Vec3d(1.0, 2.0, 3.0)
    src(1) = Vec3d(4.0, 5.0, 6.0)
    src(2) = Vec3d(7.0, 8.0, 9.0)

    val buffer = FlatBuffer[Vec3d](1)
    buffer += Vec3d(0.0, 0.0, 0.0)
    buffer ++= src

    assert(buffer.size == 4)
    assert(buffer(0) == Vec3d(0.0, 0.0, 0.0))
    assert(buffer(1) == Vec3d(1.0, 2.0, 3.0))
    assert(buffer(2) == Vec3d(4.0, 5.0, 6.0))
    assert(buffer(3) == Vec3d(7.0, 8.0, 9.0))
  }

  test("++= (FlatView) appends all elements from another FlatBuffer, forcing growth") {
    val src = FlatBuffer[Vec3d](2)
    src += Vec3d(1.0, 1.0, 1.0)
    src += Vec3d(2.0, 2.0, 2.0)
    src += Vec3d(3.0, 3.0, 3.0)

    val dst = FlatBuffer[Vec3d](1)
    dst ++= src

    assert(dst.size == 3)
    for (i <- 0 until 3) {
      assert(dst(i) == src(i))
    }
  }

  // --- FlatBuffer.++= (IterableOnce cold path) ---

  test("++= (IterableOnce) appends all elements from a List (knownSize)") {
    val elems = List(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0))
    val buffer = FlatBuffer[Vec3d]()
    buffer ++= elems

    assert(buffer.size == elems.size)
    for (i <- elems.indices) {
      assert(buffer(i) == elems(i))
    }
  }

  test("++= (IterableOnce) appends all elements from an Iterator (no knownSize)") {
    val elems = List(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0))
    val buffer = FlatBuffer[Vec3d]()
    buffer ++= elems.iterator.filter(_ => true) // filtered iterator has unknown size

    assert(buffer.size == elems.size)
    for (i <- elems.indices) {
      assert(buffer(i) == elems(i))
    }
  }

  test("++= (IterableOnce) from a large Iterator with no knownSize forces growth and preserves content") {
    val elems = (0 until 500).map(i => Vec3d(i.toDouble, i.toDouble * 2, i.toDouble * 3)).toList
    val buffer = FlatBuffer[Vec3d]()
    buffer ++= elems.iterator.filter(_ => true)

    assert(buffer.size == elems.size)
    for (i <- elems.indices) {
      assert(buffer(i) == elems(i))
    }
  }

  // --- standard-collections interop: FlatArray as an EvidenceIterableFactory, FlatView.to/iterator ---

  test("Vector(...).to(FlatArray) roundtrips to a compact FlatArray with the same elements") {
    val elems = Vector(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0))
    val flat = elems.to(FlatArray)

    assert(flat.size == elems.size)
    assert(flat.array.length == elems.size * 3)
    for (i <- elems.indices) {
      assert(flat(i) == elems(i))
    }
  }

  test("flatArray.to(IndexedSeq) roundtrips back to the original elements") {
    val elems = Vector(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0))
    val flat = elems.to(FlatArray)
    val back = flat.to(IndexedSeq)

    assert(back == elems)
  }

  test("FlatArray.from(list) produces a compact array with correct content") {
    val elems = List(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0), Vec3d(10.0, 11.0, 12.0))
    val flat = FlatArray.from(elems)

    assert(flat.size == elems.size)
    assert(flat.array.length == elems.size * 3)
    for (i <- elems.indices) {
      assert(flat(i) == elems(i))
    }
  }

  test("FlatArray.from(iterator without knownSize) produces a compact array with correct content") {
    val elems = List(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0), Vec3d(10.0, 11.0, 12.0))
    val flat = FlatArray.from(elems.iterator.filter(_ => true))

    assert(flat.size == elems.size)
    assert(flat.array.length == elems.size * 3)
    for (i <- elems.indices) {
      assert(flat(i) == elems(i))
    }
  }

  test("iterator visits every element in order") {
    val arr = FlatArray[Vec3d](3)
    arr(0) = Vec3d(1.0, 2.0, 3.0)
    arr(1) = Vec3d(4.0, 5.0, 6.0)
    arr(2) = Vec3d(7.0, 8.0, 9.0)

    assert(arr.iterator.toList == List(Vec3d(1.0, 2.0, 3.0), Vec3d(4.0, 5.0, 6.0), Vec3d(7.0, 8.0, 9.0)))
  }
