package me.kright.gametools.mathutil

/**
 * Error-free transformations for code that would otherwise lose all precision to
 * catastrophic cancellation, e.g. cross products of nearly (anti)parallel vectors.
 *
 * Full precision holds while the products and their rounding errors stay in the normal
 * double range: |a * b| in [~1e-292, 1.8e308]. fmaPortable additionally requires
 * |a|, |b| <= ~1.3e291 (Veltkamp splitting overflows above). Outside these ranges the
 * results degrade gracefully towards naive arithmetic.
 */
object ExactArith:
  /**
   * a * b + c with a single rounding: the Math.fma intrinsic on JVM, fmaPortable on Scala.js.
   * The key property: fma(a, b, -(a * b)) is the exact rounding error of the product.
   */
  inline def fma(a: Double, b: Double, c: Double): Double =
    ExactArithPlatform.fma(a, b, c)

  /** a * b - c * d with a few ulp of relative error even when the products cancel almost exactly */
  def diffOfProducts(a: Double, b: Double, c: Double, d: Double): Double =
    val p1 = a * b
    val p2 = c * d
    (p1 - p2) + (fma(a, b, -p1) - fma(c, d, -p2))

  private inline val VeltkampSplit = 134217729.0 // 2^27 + 1

  /**
   * Dekker-based fma emulation for platforms without Math.fma (Scala.js).
   * Exact whenever the true a * b + c is representable (in particular for the
   * product-error case fma(a, b, -(a * b))), and within 1 ulp of the true value
   * otherwise - a hardware fma is always within 0.5 ulp.
   */
  def fmaPortable(a: Double, b: Double, c: Double): Double =
    val p = a * b
    val ta = VeltkampSplit * a
    val ah = ta - (ta - a)
    val al = a - ah
    val tb = VeltkampSplit * b
    val bh = tb - (tb - b)
    val bl = b - bh
    val productLow = ((ah * bh - p) + ah * bl + al * bh) + al * bl
    val sum = p + c
    val sumLow = if (Math.abs(p) >= Math.abs(c)) (p - sum) + c else (c - sum) + p
    sum + (productLow + sumLow)
