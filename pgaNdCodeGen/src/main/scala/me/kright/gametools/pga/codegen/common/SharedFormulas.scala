package me.kright.gametools.pga.codegen.common

import me.kright.gametools.ga.{GA, MultiVector}
import me.kright.gametools.symbolic.Sym
import me.kright.gametools.symbolic.Sym.given_Numeric_Sym.mkNumericOps

/**
 * The single source of the hand-written 3d numerical formulas, rendered by FormulaTemplate
 * into both the Scala and the C++ generators. The scalar preambles live here as templates;
 * the constructors of the results are built per language from the symbolic results below.
 */
object SharedFormulas:

  /** the body of Rotor.rotation(from: PlaneCentral, to: PlaneCentral): Rotor */
  val rotorRotation: String =
    """// not sqrt(from.normSquare * to.normSquare): the product overflows/underflows
      |// for extreme magnitudes (~1e100 or ~1e-100) where each norm alone is still fine
      |@norm = from.norm * to.norm
      |@q2a: {Rotor} = to.geometric(from) / norm
      |@dot = q2a.s
      |
      |// the -0.9 threshold keeps (1.0 + dot) >= 0.1, so the half-angle branch loses
      |// at most ~2e-15 relative to the dot rounding; angles closer to pi take the
      |// exact-wedge branch below, which stays ~1e-15 all the way to pi
      |if (dot > -0.9) {
      |  @newCos = sqrt((1.0 + dot) / 2)
      |  @newSinDivSin2 = 0.5 / newCos
      |  return {Rotor}(newCos, q2a.xy * newSinDivSin2, q2a.xz * newSinDivSin2, q2a.yz * newSinDivSin2)
      |}
      |
      |// near pi the wedge components of q2a cancel catastrophically (~1e-17 absolute
      |// noise, which would tilt the axis by ~1e-17/sin2a), so the axis is recomputed
      |// with error-free products
      |@invNorm = 1.0 / norm
      |@bxy = diffOfProducts(from.y, to.x, from.x, to.y) * invNorm
      |@bxz = diffOfProducts(from.z, to.x, from.x, to.z) * invNorm
      |@byz = diffOfProducts(from.z, to.y, from.y, to.z) * invNorm
      |@sin2a = sqrt(bxy * bxy + bxz * bxz + byz * byz)
      |
      |if (sin2a > 0.0) {
      |  // rotation by (pi - eps): the dot guard bounds sin2a <= sin(acos(0.9)) ~ 0.44,
      |  // where asin is well-conditioned - unlike atan2 near pi, whose ~ulp(pi)
      |  // absolute error would be ~1e-16/eps relative in s
      |  @eps = asin(sin2a)
      |  @axisMult = cos(eps * 0.5) / sin2a
      |  return {Rotor}(sin(eps * 0.5), bxy * axisMult, bxz * axisMult, byz * axisMult)
      |}
      |
      |// exactly antipodal inputs: the axis is any direction orthogonal to from
      |if (abs(from.x) > abs(from.z)) {
      |  return {Rotor}(0.0, 0.0, -from.x, -from.y).normalizedByNorm
      |}
      |return {Rotor}(0.0, from.y, from.z, 0.0).normalizedByNorm""".stripMargin

  private def logBSeriesComment(versor: String): String =
    s"""// for a normalized $versor sin(angle) = lenXYZ, so this is angle / sin(angle); the series branch:
      |// x/sin(x) = 1 / (sin(x)/x) = 1 / (1 - x^2/6 + x^4/120 - ...);
      |// substitute v = x^2/6 - x^4/120 + ... into 1/(1 - v) = 1 + v + v^2 + ...:
      |//   x/sin(x) = 1 + x^2/6 + (1/36 - 1/120)*x^4 + ...
      |//            = 1 + x^2/6 + 7*x^4/360 + ...
      |// at x <= 1e-5 the dropped 7*x^4/360 <= 2e-22 relative term is far below 1e-17,
      |// so the second-order form is exact in double""".stripMargin

  /** the scalar preamble of Motor.log; the constructor comes from motorLogResult */
  val motorLog: String =
    s"""@scalar = s
       |if (s < 0.0) {
       |  return (-this).log
       |}
       |
       |@lenXYZ2 = xy * xy + xz * xz + yz * yz
       |@lenXYZ = sqrt(lenXYZ2)
       |@angle = atan2(lenXYZ, scalar)
       |
       |// 1 / sin^2 for a normalized motor; (1.0 - scalar * scalar) is the same value,
       |// but cancels catastrophically for small angles (relative error ~eps / angle^2)
       |@a = 1.0 / lenXYZ2
       |
       |${logBSeriesComment("motor")}
       |@b = IF abs(angle) > 1e-5 THEN angle / lenXYZ ELSE 1.0 + angle * angle / 6.0
       |
       |// c = a * i * (1 - scalar * b); for a normalized motor scalar = cos(x), lenXYZ = sin(x),
       |// a = 1/sin(x)^2 and b = x/sin(x), so c = i * (1 - cos(x)*b) / sin(x)^2. Step by step:
       |//   cos(x)*b = (1 - x^2/2 + x^4/24 - ...) * (1 + x^2/6 + 7*x^4/360 + ...)
       |//            = 1 + (1/6 - 1/2)*x^2 + (7/360 - 1/12 + 1/24)*x^4 + ...
       |//            = 1 - x^2/3 - x^4/45 - ...
       |//   1 - cos(x)*b = x^2/3 + x^4/45 + ...
       |//   sin(x)^2 = (x - x^3/6 + ...)^2 = x^2 - x^4/3 + ... = x^2 * (1 - x^2/3 + ...)
       |//   1/sin(x)^2 = (1 + x^2/3 + ...) / x^2   (again via 1/(1 - v) = 1 + v + ...)
       |//   c/i = (x^2/3 + x^4/45 + ...) * (1 + x^2/3 + ...) / x^2
       |//       = 1/3 + (1/9 + 1/45)*x^2 + ... = 1/3 + 2*x^2/15 + ...
       |// carrying the x^4 terms through the same steps gives the dropped term 2*x^4/63;
       |// at x <= 1e-5 it is <= 3.2e-22, relatively far below 1e-17, so the second-order
       |// form is exact in double
       |@c = IF abs(angle) > 1e-5 THEN a * i * (1.0 - scalar * b) ELSE (1.0 / 3.0 + angle * angle * (2.0 / 15.0)) * i""".stripMargin

  def motorLogResult(self: MultiVector[Sym]): MultiVector[Sym] =
    val vb = self.grade(2)
    vb * Sym("b") + vb.bulk.dual * Sym("c")

  /** the scalar preamble of Rotor.log; the constructor comes from rotorLogResult */
  val rotorLog: String =
    s"""@scalar = s
       |if (s < 0.0) {
       |  return (-this).log
       |}
       |
       |@lenXYZ = sqrt(xy * xy + xz * xz + yz * yz)
       |@angle = atan2(lenXYZ, scalar)
       |
       |${logBSeriesComment("rotor")}
       |@b = IF abs(angle) > 1e-5 THEN angle / lenXYZ ELSE 1.0 + angle * angle / 6.0""".stripMargin

  def rotorLogResult(self: MultiVector[Sym]): MultiVector[Sym] =
    self.grade(2) * Sym("b")

  /** the scalar preamble of Motor.renormalized; the constructor comes from motorRenormalizedResult */
  val motorRenormalized: String =
    """@a2 = 1.0 / (s * s + xy * xy + xz * xz + yz * yz)
      |@a = sqrt(a2)
      |@b = (s * i - wx * yz + wy * xz - wz * xy) * a * a2""".stripMargin

  def motorRenormalizedResult(self: MultiVector[Sym]): MultiVector[Sym] =
    self * Sym("a") + (self.grade(2).bulk.dual - self.grade(0).dual) * Sym("b")

  /** the sinDivLen preamble of the exp family; lenExpr is e.g. "bulkNorm" or "bulkNorm * abs(t)" */
  def expSinDivLen(lenExpr: String): String =
    s"""@len = $lenExpr
       |@cos = cos(len)
       |
       |// sin(x)/x = 1 - x^2/6 + x^4/120 - ...; at x <= 1e-5 the dropped x^4/120 <= 8.4e-23
       |// relative term is far below 1e-17, so the second-order form is exact in double
       |@sinDivLen = IF len > 1e-5 THEN sin(len) / len ELSE 1.0 - (len * len) / 6.0""".stripMargin

  val expSinMinusCos: String =
    """// (sin(x)/x - cos(x)) / x^2, step by step:
      |//   sin(x)   = x - x^3/6 + x^5/120 - x^7/5040 + ...
      |//   sin(x)/x = 1 - x^2/6 + x^4/120 - x^6/5040 + ...
      |//   cos(x)   = 1 - x^2/2 + x^4/24  - x^6/720  + ...
      |//   sin(x)/x - cos(x) = (1/2 - 1/6)*x^2 + (1/120 - 1/24)*x^4 + (1/720 - 1/5040)*x^6 + ...
      |//                     = x^2/3 - x^4/30 + x^6/840 - ...
      |//   divide by x^2:      1/3   - x^2/30 + x^4/840 - ...
      |// at x <= 1e-5 the dropped x^4/840 <= 1.2e-23 is relatively far below 1e-17,
      |// so the second-order form is exact in double
      |@sinMinusCosDivLen2 = IF len > 1e-5 THEN (sinDivLen - cos) / (len * len) ELSE 1.0 / 3.0 - (len * len) / 30.0""".stripMargin

  /** exp of a (possibly t-scaled) grade-2 element; also valid for the pure-bulk case where the correction vanishes */
  def bivectorExpResult(b: MultiVector[Sym])(using GA): MultiVector[Sym] =
    val IBdiv2 = b.bulk ^ b.weight
    val aIBettaDiv2 = b.geometric(IBdiv2)
    MultiVector.scalar(Sym("cos")) + (b + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

  def weightExpResult(b: MultiVector[Sym])(using GA): MultiVector[Sym] =
    MultiVector.scalar(Sym(1.0)) + b

  /** the guard of Bivector.split; the early return inside is emitted per language */
  val bivectorSplitGuard: String =
    """@div = bulkNormSquare
      |if (div < 1e-100) {""".stripMargin

  def bivectorSplitPseudoScalar(self: MultiVector[Sym]): String =
    s"""// shiftAlongLine = this.geometric((this ^ this.reverse) / div / 2.0)
       |@pseudoScalar = ${(self ^ self.reverse).pseudoScalar * Sym(0.5)} / div""".stripMargin

  def bivectorSplitShift(self: MultiVector[Sym])(using GA): MultiVector[Sym] =
    self.geometric(MultiVector("i" -> Sym("pseudoScalar")))

  /** the body of Rotor.projectToRotationInPlane(plane: PlaneCentral): Rotor */
  val rotorProjectToRotationInPlane: String =
    """@q: {Rotor} = {this}.normalizedByNorm
      |@qPart: {Rotor} = {Rotor}::rotation(q.sandwich(plane), plane)
      |return qPart.geometric(q)""".stripMargin

  /** the body of Rotor.restoreRotationInPlane(plane: PlaneCentral): Double */
  val rotorRestoreRotationInPlane: String =
    """@q0: {Rotor} = {this}.projectToRotationInPlane(plane)
      |@logDual: {BivectorWeight} = q0.log.dual
      |return 2.0 * (logDual.wx * plane.x + logDual.wy * plane.y + logDual.wz * plane.z) / plane.norm""".stripMargin
