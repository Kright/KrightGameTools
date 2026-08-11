package me.kright.gametools.pga3d.physics

import me.kright.gametools.ga.{GARepresentationConfig, MultiVector, PGA3, Signature}
import me.kright.gametools.pga3d.*
import org.scalatest.funsuite.AnyFunSuiteLike

/**
 * Measures how the accuracy of Pga3dInertiaMovedLocal and its Pga3dInertiaPrecomputed form
 * degrades as the center of mass moves away from the origin.
 *
 * The reference is computed with BigDecimal (DECIMAL128, ~34 significant digits) through the
 * slow generic sandwich of the ga module and the exact diagonal formulas of Pga3dInertiaLocal,
 * on the same double inputs, so the only difference measured is the rounding of the implementations.
 *
 * The inputs are physically bounded local twists and forques of the body (the solver regime):
 * their global images have the near-cancelling structure weight ~ -t x bulk, which is what
 * actually exposes the conditioning of the representations. For uniformly random global
 * bivectors both representations stay flat at ~3e-16 for any offset (the norm of the result
 * grows together with the absolute error), which hides the problem.
 *
 * The expected picture: the moved-local path loses digits linearly with the offset on every
 * operation. The precomputed 6x6 matrices bake the parallel-axis terms (~ mass * offset^2)
 * into their entries; on apply/invert the norm of the result grows together with the error and
 * the relative loss stays linear too, but the kinetic energy and the acceleration go through
 * the near-cancellation of the matrix terms and lose ~1e-16 * offset^2 - the price of the
 * fast matrix path, documented in the pga3dphysics README.
 */
class Pga3dInertiaPrecisionTest extends AnyFunSuiteLike:

  // the same representation the code generator uses, so the blade names match the generated fields
  private given pga3: PGA3 = PGA3(GARepresentationConfig(
    Signature.pga3,
    generatorNames = "wxyz",
    namePrefix = "",
    overrideScalar = Option("s"),
    overridePseudoScalar = Option("i"),
  ))

  /** the exact BigDecimal image of the double (not the shortest decimal representation) */
  private def big(d: Double): BigDecimal =
    BigDecimal(new java.math.BigDecimal(d))

  private def toGa(b: Pga3dBivector): MultiVector[BigDecimal] =
    MultiVector(
      "wx" -> big(b.wx), "wy" -> big(b.wy), "wz" -> big(b.wz),
      "xy" -> big(b.xy), "xz" -> big(b.xz), "yz" -> big(b.yz),
    )

  private def toGa(m: Pga3dMotor): MultiVector[BigDecimal] =
    MultiVector(
      "s" -> big(m.s),
      "wx" -> big(m.wx), "wy" -> big(m.wy), "wz" -> big(m.wz),
      "xy" -> big(m.xy), "xz" -> big(m.xz), "yz" -> big(m.yz),
      "i" -> big(m.i),
    )

  /** Pga3dInertiaLocal.apply in BigDecimal */
  private def applyLocalRef(inertia: Pga3dInertiaLocal, b: MultiVector[BigDecimal]): MultiVector[BigDecimal] =
    MultiVector(
      "wx" -> b("yz") * big(inertia.mryz),
      "wy" -> -b("xz") * big(inertia.mrxz),
      "wz" -> b("xy") * big(inertia.mrxy),
      "xy" -> b("wz") * big(inertia.mass),
      "xz" -> -b("wy") * big(inertia.mass),
      "yz" -> b("wx") * big(inertia.mass),
    )

  /** Pga3dInertiaLocal.invert in BigDecimal */
  private def invertLocalRef(inertia: Pga3dInertiaLocal, b: MultiVector[BigDecimal]): MultiVector[BigDecimal] =
    MultiVector(
      "wx" -> b("yz") / big(inertia.mass),
      "wy" -> -b("xz") / big(inertia.mass),
      "wz" -> b("xy") / big(inertia.mass),
      "xy" -> b("wz") / big(inertia.mrxy),
      "xz" -> -b("wy") / big(inertia.mrxz),
      "yz" -> b("wx") / big(inertia.mryz),
    )

  private val bivectorBlades = Seq("wx", "wy", "wz", "xy", "xz", "yz")

  private def relError(actual: Pga3dBivector, ref: MultiVector[BigDecimal]): Double =
    val actualByBlade = Map(
      "wx" -> actual.wx, "wy" -> actual.wy, "wz" -> actual.wz,
      "xy" -> actual.xy, "xz" -> actual.xz, "yz" -> actual.yz)
    val diffNorm = Math.sqrt(bivectorBlades.map { blade =>
      val d = (big(actualByBlade(blade)) - ref(blade)).toDouble
      d * d
    }.sum)
    val refNorm = Math.sqrt(bivectorBlades.map(blade => Math.pow(ref(blade).toDouble, 2)).sum)
    if (refNorm > 0.0) diffNorm / refNorm else diffNorm

  private def relError(actual: Double, ref: BigDecimal): Double =
    val refD = ref.toDouble
    if (refD != 0.0) Math.abs((big(actual) - ref).toDouble / refD) else Math.abs(actual)

  test("accuracy of the moved and the precomputed inertia vs the center of mass offset") {
    val localInertia = Pga3dInertiaLocal(mass = 2.0, mryz = 3.0, mrxz = 2.0, mrxy = 1.5)
    val rotor = Pga3dBivectorBulk(0.3, -0.4, 0.5).exp
    val direction = Pga3dVector(0.6, -0.64, 0.48)
    val random = new java.util.Random(42)
    def rnd(): Double = random.nextDouble() * 2.0 - 1.0

    val samples = 20
    val half = big(0.5)

    println("max relative error over random bounded local twists, by center of mass offset:")
    println("    offset |    apply     |    invert    |    energy    | acceleration |   (moved | precomputed)")

    for (offset <- Seq(0.0, 1.0, 10.0, 100.0, 1e3, 1e4, 1e5, 1e6)) {
      val motor = Pga3dTranslator.addVector(direction * offset).geometric(rotor)
      val moved = Pga3dInertia.moved(motor, localInertia)
      val precomputed = moved.toPrecomputed
      val mRef = toGa(motor)

      var movedApply, preApply, movedInvert, preInvert = 0.0
      var movedEnergy, preEnergy, movedAccel, preAccel = 0.0

      for (_ <- 0 until samples) {
        // a bounded twist and forque of the body, in the global frame (the solver regime)
        val localB = Pga3dBivector(rnd(), rnd(), rnd(), rnd(), rnd(), rnd())
        val globalB = motor.sandwich(localB)
        val globalForque = motor.sandwich(Pga3dBivector(rnd(), rnd(), rnd(), rnd(), rnd(), rnd()))

        val localBRef = mRef.reverse.sandwich(toGa(globalB))
        val localFRef = mRef.reverse.sandwich(toGa(globalForque))
        val localLRef = applyLocalRef(localInertia, localBRef)

        val applyRef = mRef.sandwich(localLRef)
        movedApply = Math.max(movedApply, relError(moved(globalB), applyRef))
        preApply = Math.max(preApply, relError(precomputed(globalB), applyRef))

        val invertRef = mRef.sandwich(invertLocalRef(localInertia, mRef.reverse.sandwich(toGa(globalB))))
        movedInvert = Math.max(movedInvert, relError(moved.invert(globalB), invertRef))
        preInvert = Math.max(preInvert, relError(precomputed.invert(globalB), invertRef))

        val energyRef = localBRef.antiWedge(localLRef).scalar * half
        movedEnergy = Math.max(movedEnergy, relError(moved.getKineticEnergy(globalB), energyRef))
        preEnergy = Math.max(preEnergy, relError(precomputed.getKineticEnergy(globalB), energyRef))

        // invert(localB cross apply(localB) + localForque), transformed to the global frame
        val crossRef = localBRef.crossX2(localLRef).mapValues(_ * half)
        val accelRef = mRef.sandwich(invertLocalRef(localInertia, crossRef + localFRef))
        movedAccel = Math.max(movedAccel, relError(moved.getAcceleration(globalB, globalForque), accelRef))
        preAccel = Math.max(preAccel, relError(precomputed.getAcceleration(globalB, globalForque), accelRef))
      }

      println(f"$offset%10.0f | $movedApply%5.1e | $preApply%5.1e | $movedInvert%5.1e | $preInvert%5.1e | $movedEnergy%5.1e | $preEnergy%5.1e | $movedAccel%5.1e | $preAccel%5.1e")

      // the moved-local path loses digits ~linearly with the offset; the energy and the
      // acceleration of the precomputed form go through the matrices and lose ~quadratically;
      // the bounds are far above the observed errors so the test does not flake
      val linear = 1e-13 * (1.0 + offset)
      val quadratic = 1e-13 * (1.0 + offset) * (1.0 + offset)
      assert(movedApply < linear && movedInvert < linear && movedEnergy < linear && movedAccel < linear, s"offset = $offset")
      assert(preApply < linear && preInvert < linear, s"offset = $offset")
      assert(preEnergy < quadratic && preAccel < quadratic, s"offset = $offset")
    }
  }
