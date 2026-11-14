package me.kright.gametools.physics1d

import org.scalatest.funsuite.AnyFunSuiteLike

class OdeSolverCacheTest extends AnyFunSuiteLike:

  private def assertSeparateArrays(arrays: Array[Array[Double]], groupSize: Int, size: Int): Unit =
    assert(arrays.length == groupSize)
    for (i <- 0 until groupSize) {
      assert(arrays(i).length == size)
    }
    for (i <- 0 until groupSize; j <- 0 until groupSize if i != j) {
      assert(arrays(i) ne arrays(j))
    }

  private def shareAnArray(first: Array[Array[Double]], second: Array[Array[Double]]): Boolean =
    first.exists(a => second.exists(_ eq a))

  test("one call gives exactly the asked count of separate arrays of exactly the asked size") {
    for (cache <- Seq[OdeSolverCache | Null](OdeSolverCache(), null); (groupSize, size) <- Seq((6, 14), (3, 0), (0, 0), (0, 10), (9, 1))) {
      assertSeparateArrays(OdeSolverCache.getArrays(cache, groupSize, size), groupSize, size)
    }
  }

  test("a negative group size or size is rejected, with a cache and without") {
    for (cache <- Seq[OdeSolverCache | Null](OdeSolverCache(), null)) {
      assertThrows[IllegalArgumentException](OdeSolverCache.getArrays(cache, -1, 4))
      assertThrows[IllegalArgumentException](OdeSolverCache.getArrays(cache, 1, -1))
    }
  }

  test("a cache returns the same arrays for the same request") {
    val cache = OdeSolverCache()
    val first = cache.getArrays(6, 14)
    val firstArrays = first.toSeq

    val second = cache.getArrays(6, 14)
    assert(second eq first)
    assert(second.toSeq.zip(firstArrays).forall((a, b) => a eq b))
    assert(OdeSolverCache.getArrays(cache, 6, 14) eq first)
  }

  test("a cache replaces all the arrays when the group size or the size changes") {
    for ((groupSize, size) <- Seq((6, 15), (6, 13), (7, 14), (5, 14))) {
      val cache = OdeSolverCache()
      val before = cache.getArrays(6, 14)
      val after = cache.getArrays(groupSize, size)

      assertSeparateArrays(after, groupSize, size)
      assert(!shareAnArray(before, after))
      assert(cache.getArrays(groupSize, size) eq after)
    }
  }

  test("a cache does not clear the arrays between the calls") {
    val cache = OdeSolverCache()
    cache.getArrays(3, 4)(1)(2) = 42.0
    assert(cache.getArrays(3, 4)(1)(2) == 42.0)
  }

  test("separate caches share nothing") {
    assert(!shareAnArray(OdeSolverCache().getArrays(2, 8), OdeSolverCache().getArrays(2, 8)))
  }

  test("without a cache every call gives new zero-filled arrays") {
    val first = OdeSolverCache.getArrays(null, 6, 14)
    first.foreach(array => java.util.Arrays.fill(array, 42.0))

    val second = OdeSolverCache.getArrays(null, 6, 14)
    assert(second ne first)
    assert(!shareAnArray(first, second))
    assert(second.forall(_.forall(_ == 0.0)))
  }
