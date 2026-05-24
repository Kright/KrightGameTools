package me.kright.gametools.ga

import org.scalatest.funsuite.*

class PermutationTest extends AnyFunSuite:
  private def check(arr: Array[Int], expected: Boolean): Unit =
    assert(Permutation.parity(arr.clone()) == expected)
    check(arr, arr.sorted, expected)

  private def check(left: Array[Int], right: Array[Int], expected: Boolean) =
    assert(Permutation.parity(left, right) == expected)
    assert(Permutation.parity(right, left) == expected)

  test("parity") {
    check(Array(0, 1, 2), true)
    check(Array(0, 1, 2, 3), true)
    check(Array(0, 1, 3, 2), false)
    check(Array(1, 0, 3, 2), true)
    check(Array(1, 3, 0, 2), false)
    check(Array(3, 1, 0, 2), true)
  }

  test("parity with repetition") {
    check(Array(0, 1, 1, 2), Array(0, 1, 1, 2), true)
    check(Array(0, 1, 1, 2), Array(0, 1, 2, 1), false)
    check(Array(0, 1, 1, 2), Array(0, 2, 1, 1), true)
  }
