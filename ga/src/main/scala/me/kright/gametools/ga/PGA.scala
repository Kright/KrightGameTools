package me.kright.gametools.ga

import me.kright.gametools.ga.PGA.CommonMethods

class PGA(representationConfig: GARepresentationConfig) extends GA(representationConfig):
  require(representationConfig.signature.zeros == 1)
  require(representationConfig.signature.negatives == 0)

object PGA extends CommonMethods:

  trait CommonMethods:
    /**
     * Plane defined by equation x * nx + y * ny + z * nz + d = 0
     * if plane contains point (px, py, pz), then d = px * nx + py * ny + pz * nz = -dot(p, n)
     */
    def zeroPoint[T](using ga: PGA, num: Numeric[T]): MultiVector[T] =
      MultiVector.makeNonNegative[T](
        "w" -> num.one,
      ).dual

    protected def half[T: Numeric] =
      summon[Numeric[T]].parseString("0.5").get

    /**
     * actually this is exp(line * 0.5)
     *
     * @return multivector which translates by sandwich product
     *         MultiVector(
     *         1 -> 1.0
     *         wx -> -0.5 * d.x
     *         wy -> -0.5 * d.y
     *         wz -> -0.5 * d.z
     *         )
     */
    def translator[T](srcPoint: MultiVector[T], dstPoint: MultiVector[T])(using ga: PGA, num: Numeric[T]): MultiVector[T] =
      translatorByIdealLine((dstPoint v srcPoint).bulk.dual)

    /**
     * actually this is exponentiation
     *
     * ideal line has (wx, wy, wz) components only
     *
     * @return multivector which translates by sandwich product
     *         MultiVector(
     *         1 -> 1.0
     *         wx -> -0.5 * d.x
     *         wy -> -0.5 * d.y
     *         wz -> -0.5 * d.z
     *         )
     */
    def translatorByIdealLine[T](idealLine: MultiVector[T])(using ga: PGA, num: Numeric[T]): MultiVector[T] =
      MultiVector.scalar[T](num.one) + idealLine * half

    def rotor(angle: Double, line: MultiVector[Double])(using ga: GA): MultiVector[Double] =
      expForLine(line * angle * 0.5)

    /**
     * simplified case of expForBivector when (bulk ⟑ weight) = 0.0
     */
    def expForLine(line: MultiVector[Double])(using ga: GA): MultiVector[Double] =
      val len = line.bulk.norm

      // (1 - len^2 / 3! + len^4 / 5!), but double has 15-17 significant digits and for small len result is just 1.0
      val sinDivLen = if (len > 1e-5) {
        Math.sin(len) / len
      } else 1.0 - (len * len) / 6.0

      MultiVector.scalar(Math.cos(len)) + line * sinDivLen

    /**
     * Made by carefully writing formulas on several sheets of paper
     *
     * bivector a = (a.wx, a.wy, a.wz, a.xy, a.xz, a.yz)
     *
     * len = math.sqrt(a.xy ** 2 + a.xz ** 2 + a.yz ** 2)
     * betta = (-2.0 * a.wy * a.xz + 2.0 * a.wx * a.yz + 2.0 * a.wz * a.xy)
     *
     * exp(bivector) = cos(len) + a * (sin(len) / len) + (a ⟑ I) betta (sin(len) / len - cos (len)) / (2 len**2)
     */
    def expForBivector(line: MultiVector[Double])(using ga: GA): MultiVector[Double] =
      require(ga.signature.positives <= 3 && ga.signature.zeros <= 1)
      // tested on works fine on PGA(3, 0 1), PGA(2, 0, 1), GA(3, 0, 0) and GA(2, 0, 0)
      // for GA(4, 0, 0) doesn't work because square of 4-vector != 0

      val bulk = line.bulk
      val weight = line.weight
      val len = bulk.norm
      val cos = Math.cos(len)

      // for small values sin(len) / len
      // (1 - len^2 / 3! + len^4 / 5! - ...), but double has 15-17 significant digits and while len ** 4 < 1e-17, simplified formula is accurate
      val sinDivLen = if (len > 1e-5) {
        Math.sin(len) / len
      } else 1.0 - (len * len) / 6.0

      val IBdiv2 = bulk ^ weight // = 0.5 of bulk * weight
      // (a ⟑ I) betta / 2
      val aIBettaDiv2 = line.geometric(IBdiv2)

      // and (sin(len) / len - cos(len)) / len**2 -> len**2 (1/2 - 1/6) + len ** 4 (1/4! - 1/5!) = (1 / 3) * (1 + 0.8 * len ** 2)
      // while len ** 4 < 1e-17, simplified formula is accurate, otherwise subtract sin and cos
      val sinMinusCosDivLen2 = if (len > 1e-5) {
        (sinDivLen - cos) / (len * len)
      } else (1.0 / 3.0) * (1.0 + 0.8 * len * len)

      MultiVector.scalar(cos) + (line + IBdiv2) * sinDivLen + aIBettaDiv2 * sinMinusCosDivLen2
