package me.kright.gametools.mathutil

/**
 * Standard range boxes int indexes into Integer, and in some hot loops this causes performance problems.
 * For example, I measured x15 slowdown on code with triple nested loops for multiplying matrices 4x4
 * This range is a replacement, aimed to maximum performance.
 * In addition, because of inlining there is no problem with `return` invocation from inner code
 *
 * If you use explicit import FastRange.*, it shadows standard Range
 * Code like for(i <- 0 until 5){ println(i) } will work with FastRange instead
 */
object FastRange:
  inline def apply(endExclusive: Int) = new FastRange(endExclusive)

  inline def apply(inline zero: 0, endExclusive: Int) = new FastRange(endExclusive)

  inline def apply(start: Int, endExclusive: Int) = new FastRangeWithStart(start, endExclusive)

  inline def cfor(start: Int, shouldContinue: Int => Boolean, inc: Int => Int)(inline body: Int => Unit): Unit =
    var i = start
    while (shouldContinue(i)) {
      body(i)
      i = inc(i)
    }

  extension (t: Int) inline infix def until(v: Int): FastRangeWithStart = FastRange(t, v)

  extension (inline zero: 0) inline infix def until(v: Int): FastRange = FastRange(0, v)

  extension (t: Int) inline infix def to(v: Int): FastRangeWithStart = FastRange(t, v + 1)

  extension (inline zero: 0) inline infix def to(v: Int): FastRange = FastRange(0, v + 1)


final class FastRange(val endExclusive: Int):
  inline def foreach(inline body: Int => Unit): Unit = {
    var i = 0
    while (i < endExclusive) {
      body(i)
      i += 1
    }
  }


final class FastRangeWithStart(val start: Int,
                               val endExclusive: Int):
  inline def foreach(inline body: Int => Unit): Unit = {
    var i = start
    while (i < endExclusive) {
      body(i)
      i += 1
    }
  }
