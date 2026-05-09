package com.github.kright.math

import org.scalatest.funsuite.AnyFunSuiteLike

private final case class Vector3dTest(x: Double, y: Double, z: Double) derives CanEqual

class FlatDoubleViewTest extends AnyFunSuiteLike:
  implicit val serializer: FlatDoubleSerializer[Vector3dTest] = FlatDoubleSerializer.derived[Vector3dTest]

  test("FlatDoubleView should behave like an array of Vector3dTest") {
    val arr = new Array[Double](9)
    val view = FlatDoubleView[Vector3dTest](arr)

    assert(view.size == 3)

    view(0) = Vector3dTest(1.0, 2.0, 3.0)
    view(1) = Vector3dTest(4.0, 5.0, 6.0)
    view(2) = Vector3dTest(7.0, 8.0, 9.0)

    assert(view(0) == Vector3dTest(1.0, 2.0, 3.0))
    assert(view(1) == Vector3dTest(4.0, 5.0, 6.0))
    assert(view(2) == Vector3dTest(7.0, 8.0, 9.0))

    assert(arr.toSeq == Seq(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
  }

  test("FlatDoubleView should respect offset and count") {
    val arr = new Array[Double](10)
    // view over indices 1 to 2 (2 elements of size 3 each = 6 doubles)
    // starting at index 1 in the array
    val view = new FlatDoubleView[Vector3dTest](arr, offset = 1, length = 2)

    assert(view.size == 2)

    view(0) = Vector3dTest(1.0, 2.0, 3.0)
    view(1) = Vector3dTest(4.0, 5.0, 6.0)

    assert(arr.toSeq == Seq(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 0.0, 0.0, 0.0))
  }

  test("FlatDoubleView foreach should iterate correctly") {
    val arr = Array(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    val view = FlatDoubleView[Vector3dTest](arr)

    var count = 0
    view.foreach { v =>
      if (count == 0) assert(v == Vector3dTest(1.0, 2.0, 3.0))
      if (count == 1) assert(v == Vector3dTest(4.0, 5.0, 6.0))
      count += 1
    }
    assert(count == 2)
  }

  test("FlatDoubleView mapInPlace should update elements in-place") {
    val arr = Array(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    val view = FlatDoubleView[Vector3dTest](arr)

    view.mapInPlace(v => Vector3dTest(v.x * 2, v.y * 2, v.z * 2))

    assert(arr.toSeq == Seq(2.0, 4.0, 6.0, 8.0, 10.0, 12.0))
  }

  test("FlatDoubleView slice should return a zero-copy subview") {
    val arr = Array(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
    val view = FlatDoubleView[Vector3dTest](arr)

    val sliced = view.slice(1, 3)

    assert(sliced.size == 2)
    assert(sliced(0) == Vector3dTest(4.0, 5.0, 6.0))
    assert(sliced(1) == Vector3dTest(7.0, 8.0, 9.0))

    sliced(0) = Vector3dTest(10.0, 11.0, 12.0)
    assert(arr(3) == 10.0) // modifies original array
  }

  test("FlatDoubleView toArray should materialize elements") {
    val arr = Array(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    val view = FlatDoubleView[Vector3dTest](arr)

    val result = view.toArray

    assert(result.length == 2)
    assert(result(0) == Vector3dTest(1.0, 2.0, 3.0))
    assert(result(1) == Vector3dTest(4.0, 5.0, 6.0))
  }

  test("FlatDoubleView appended should reuse array when capacity allows") {
    val arr = new Array[Double](12) // capacity for 4 elements
    val view = FlatDoubleView[Vector3dTest](arr).slice(0, 2)

    view(0) = Vector3dTest(1.0, 2.0, 3.0)
    view(1) = Vector3dTest(4.0, 5.0, 6.0)

    val view2 = view :+ Vector3dTest(7.0, 8.0, 9.0)

    assert(view2.size == 3)
    assert(view2.array eq arr) // тот же массив
    assert(view2(2) == Vector3dTest(7.0, 8.0, 9.0))
  }

  test("FlatDoubleView appended should grow array when capacity exceeded") {
    val arr = Array(1.0, 2.0, 3.0, 4.0, 5.0, 6.0) // ровно 2 элемента, места нет
    val view = FlatDoubleView[Vector3dTest](arr)

    val view2 = view :+ Vector3dTest(7.0, 8.0, 9.0)

    assert(view2.size == 3)
    assert(!(view2.array eq arr)) // новый массив
    assert(view2.offset == 0)
    assert(view2.array.length == (3 * 3 / 2 + 8) * 3)
    assert(view2(0) == Vector3dTest(1.0, 2.0, 3.0))
    assert(view2(1) == Vector3dTest(4.0, 5.0, 6.0))
    assert(view2(2) == Vector3dTest(7.0, 8.0, 9.0))
  }

  test("FlatDoubleView toCompact should create independent minimal view") {
    val arr = Array(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 0.0, 0.0)
    val view = new FlatDoubleView[Vector3dTest](arr, offset = 1, length = 2)

    val compact = view.toCompact

    assert(compact.offset == 0)
    assert(compact.size == 2)
    assert(compact.array.length == 6)
    assert(compact(0) == Vector3dTest(1.0, 2.0, 3.0))
    assert(compact(1) == Vector3dTest(4.0, 5.0, 6.0))

    compact(0) = Vector3dTest(99.0, 99.0, 99.0)
    assert(arr(1) == 1.0) // оригинальный массив не тронут
  }

  test("FlatDoubleView toIndexedSeq should materialize elements") {
    val arr = Array(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    val view = FlatDoubleView[Vector3dTest](arr)

    val result = view.toIndexedSeq

    assert(result == IndexedSeq(Vector3dTest(1.0, 2.0, 3.0), Vector3dTest(4.0, 5.0, 6.0)))
  }

  test("FlatDoubleView.from should build view from empty collection") {
    val view = List.empty[Vector3dTest].to(FlatDoubleView)

    assert(view.size == 0)
    assert(view.length == 0)
    assert(view.offset == 0)
  }

  test("FlatDoubleView.from should build view from non-empty collection") {
    val source = List(
      Vector3dTest(1.0, 2.0, 3.0),
      Vector3dTest(4.0, 5.0, 6.0),
      Vector3dTest(7.0, 8.0, 9.0),
    )

    val view = source.to(FlatDoubleView)

    assert(view.size == 3)
    assert(view(0) == Vector3dTest(1.0, 2.0, 3.0))
    assert(view(1) == Vector3dTest(4.0, 5.0, 6.0))
    assert(view(2) == Vector3dTest(7.0, 8.0, 9.0))
  }
