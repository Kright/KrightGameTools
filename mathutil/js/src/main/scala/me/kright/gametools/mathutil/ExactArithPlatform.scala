package me.kright.gametools.mathutil

private[mathutil] object ExactArithPlatform:
  inline def fma(a: Double, b: Double, c: Double): Double =
    ExactArith.fmaPortable(a, b, c)
