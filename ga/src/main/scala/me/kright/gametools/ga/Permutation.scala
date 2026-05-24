package me.kright.gametools.ga

import me.kright.gametools.mathutil.MathUtil.swap

import scala.collection.mutable

object Permutation:
  /**
   * moves values in array!
   *
   * @param arr array with N unique integers 0..(n-1) in some random order
   * @return true if parity is even
   */
  def parity(arr: Array[Int]): Boolean =
    // https://codeforces.com/blog/entry/97849?#comment-1019437
    var result: Boolean = true
    var pos = 0
    while (pos < arr.length) {
      val cur = arr(pos)
      if (cur == pos) {
        pos += 1
      } else {
        require(pos < cur)
        result = !result
        arr.swap(pos, cur)
      }
    }
    result

  def parity[T](left: Seq[T], right: Seq[T]): Boolean =
    require(left.size == right.size)

    val rightPositions = new mutable.HashMap[T, mutable.ArrayDeque[Int]]().withDefault(_ => new mutable.ArrayDeque[Int]())

    for ((r, i) <- right.zipWithIndex) {
      rightPositions(r) = rightPositions(r).append(i)
    }

    val arr = new Array[Int](left.size)
    for ((l, i) <- left.zipWithIndex) {
      arr(i) = rightPositions(l).removeHead()
    }

    parity(arr)
