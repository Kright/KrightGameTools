package me.kright.gametools.pga3d.physics

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.matrix.*
import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.physics.Pga3dInertiaSummable.sandwich
import scala.collection.immutable.ArraySeq


/**
 * The inertia as the first and second moments of the mass distribution: inertias of separate
 * bodies simply add up, and apply/invert are the fastest of the general representations
 * (see InertiaBenchmark).
 *
 * Precision hint: the second moments carry parallel-axis terms ~ mass * R^2 for a center of
 * mass at distance R from the origin, and their near-cancellation costs invert, getKineticEnergy
 * and getAcceleration ~1e-16 * R^2 of relative error (apply loses only ~1e-16 * R). Far from
 * the origin prefer [[Pga3dInertiaMovedLocal]]; numbers in Pga3dInertiaPrecisionTest.
 */
final case class Pga3dInertiaSummable(ww: Double,
                                      wx: Double,
                                      wy: Double,
                                      wz: Double,
                                      xx: Double,
                                      yy: Double,
                                      zz: Double,
                                      xy: Double,
                                      yz: Double,
                                      xz: Double) extends Pga3dInertia derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  /** lazy cache for the conversions */
  private var inertiaMovedLocalOrNull: Pga3dInertiaMovedLocal | Null = null

  /** lazy cache for [[invert]] */
  private var inverseOrNull: Pga3dInertiaSummableInverse | Null = null

  /** the lazily cached inverse momentum map (see [[Pga3dInertiaSummableInverse]]) */
  def inverse: Pga3dInertiaSummableInverse =
    val cached = inverseOrNull
    if (cached ne null) return cached

    val computed = Pga3dInertiaSummableInverse(this)
    inverseOrNull = computed
    computed

  override def toInertiaMovedLocal: Pga3dInertiaMovedLocal =
    val inertiaOrNull = inertiaMovedLocalOrNull
    if (inertiaOrNull ne null) return inertiaOrNull

    val newInertia = toInertiaMovedLocalImpl
    inertiaMovedLocalOrNull = newInertia
    newInertia

  def mass: Double =
    ww

  def centerOfMass: Pga3dPoint =
    val inv = 1.0 / mass
    Pga3dPoint(wx * inv, wy * inv, wz * inv)

  def centerOfMassProjective: Pga3dProjectivePoint =
    Pga3dProjectivePoint(wx, wy, wz, ww)

  def +(v: Pga3dInertiaSummable): Pga3dInertiaSummable =
    Pga3dInertiaSummable(
      ww = v.ww + ww,
      wx = v.wx + wx,
      wy = v.wy + wy,
      wz = v.wz + wz,
      xx = v.xx + xx,
      yy = v.yy + yy,
      zz = v.zz + zz,
      xy = v.xy + xy,
      yz = v.yz + yz,
      xz = v.xz + xz
    )

  def -(v: Pga3dInertiaSummable): Pga3dInertiaSummable =
    Pga3dInertiaSummable(
      ww = ww - v.ww,
      wx = wx - v.wx,
      wy = wy - v.wy,
      wz = wz - v.wz,
      xx = xx - v.xx,
      yy = yy - v.yy,
      zz = zz - v.zz,
      xy = xy - v.xy,
      yz = yz - v.yz,
      xz = xz - v.xz
    )

  def *(s: Double): Pga3dInertiaSummable =
    Pga3dInertiaSummable(
      ww = ww * s,
      wx = wx * s,
      wy = wy * s,
      wz = wz * s,
      xx = xx * s,
      yy = yy * s,
      zz = zz * s,
      xy = xy * s,
      yz = yz * s,
      xz = xz * s,
    )

  def normSquare: Double =
    ww * ww +
      wx * wx +
      wy * wy +
      wz * wz +
      xx * xx +
      yy * yy +
      zz * zz +
      xy * xy +
      yz * yz +
      xz * xz

  def norm: Double =
    Math.sqrt(normSquare)

  /**
   * It looks like there is a problem with precision, it's quite low, about 1e-8
   */
  private def toInertiaMovedLocalImpl: Pga3dInertiaMovedLocal =
    val shift = centerOfMass.toVectorUnsafe

    val shifted = Pga3dTranslator.addVector(-shift).sandwich(this)
    val i = shifted.toMatrixXYZ
    val rotor = Pga3dInertiaSummable.diagonalizeSymmetricInplace(i)
    val ixx = i(0, 0)
    val iyy = i(1, 1)
    val izz = i(2, 2)

    Pga3dInertiaMovedLocal(
      localToGlobal = Pga3dTranslator.addVector(shift).geometric(rotor),
      localInertia = Pga3dInertiaLocal(mass, iyy + izz, ixx + izz, ixx + iyy)
    )

  def apply(b: Pga3dBivector): Pga3dBivector =
    Pga3dBivector(
      wx = +(yy + zz) * b.yz + xy * b.xz - xz * b.xy - wz * b.wy + wy * b.wz,
      wy = -xy * b.yz - (xx + zz) * b.xz - yz * b.xy - wx * b.wz + wz * b.wx,
      wz = -xz * b.yz + yz * b.xz + (xx + yy) * b.xy + wx * b.wy - wy * b.wx,
      xy = ww * b.wz + wx * b.xz + wy * b.yz,
      xz = -ww * b.wy - wx * b.xy + wz * b.yz,
      yz = ww * b.wx - wy * b.xy - wz * b.xz,
    )

  /**
   * the inverse of [[apply]], solved in closed form on the 3x3 blocks of the momentum map by
   * the lazily cached [[Pga3dInertiaSummableInverse]]: about as cheap as [[apply]] after the
   * first call, and unlike the [[toInertiaMovedLocal]] route it needs no eigendecomposition
   */
  override def invert(localInertia: Pga3dBivector): Pga3dBivector =
    inverse(localInertia)

  def toMatrixXYZ: ArrayView2dFlat[Double] =
    matrixFromValues(3, 3)(
      xx, xy, xz,
      xy, yy, yz,
      xz, yz, zz,
    )

  def toMatrixXYZW: ArrayView2dFlat[Double] =
    matrixFromValues(4, 4)(
      xx, xy, xz, wx,
      xy, yy, yz, wy,
      xz, yz, zz, wz,
      wx, wy, wz, ww,
    )

  def toMatrixWXYZ: ArrayView2dFlat[Double] =
    matrixFromValues(4, 4)(
      ww, wx, wy, wz,
      wx, xx, xy, xz,
      wy, xy, yy, yz,
      wz, xz, yz, zz,
    )

  def withXYZ(m: ArrayView2d[Double]): Pga3dInertiaSummable = {
    copy(
      xx = m(0, 0),
      xy = m(0, 1),
      xz = m(0, 2),
      yy = m(1, 1),
      yz = m(1, 2),
      zz = m(2, 2),
    )
  }

  def movedBy(translator: Pga3dTranslator): Pga3dInertiaSummable =
    translator.sandwich(this)

  def movedBy(motor: Pga3dMotor): Pga3dInertiaSummable =
    motor.sandwich(this)

  def movedBy(rotor: Pga3dRotor): Pga3dInertiaSummable =
    rotor.sandwich(this)

  override def toString: String =
    s"Pga3dInertiaSummable(ww=$ww, wx=$wx, wy=$wy, wz=$wz, xx=$xx, yy=$yy, zz=$zz, xy=$xy, yz=$yz, xz=$xz)"

  override def toSummable: Pga3dInertiaSummable =
    this


object Pga3dInertiaSummable:
  inline val componentsCount = 10

  val zero: Pga3dInertiaSummable =
    Pga3dInertiaSummable(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

  def symmetricProduct(u: Pga3dProjectivePoint, v: Pga3dProjectivePoint): Pga3dInertiaSummable =
    Pga3dInertiaSummable(
      ww = (u.w * v.w + v.w * u.w) * 0.5,
      wx = (u.w * v.x + v.w * u.x) * 0.5,
      wy = (u.w * v.y + v.w * u.y) * 0.5,
      wz = (u.w * v.z + v.w * u.z) * 0.5,
      xx = (u.x * v.x + v.x * u.x) * 0.5,
      yy = (u.y * v.y + v.y * u.y) * 0.5,
      zz = (u.z * v.z + v.z * u.z) * 0.5,
      xy = (u.x * v.y + v.x * u.y) * 0.5,
      yz = (u.y * v.z + v.y * u.z) * 0.5,
      xz = (u.x * v.z + v.x * u.z) * 0.5,
    )

  // inertia of a mass in a point = symmetricProduct(p.toProjectivePoint, p * mass)
  def point(p: Pga3dPoint, mass: Double): Pga3dInertiaSummable =
    Pga3dInertiaSummable(
      ww = mass,
      wx = p.x * mass,
      wy = p.y * mass,
      wz = p.z * mass,
      xx = p.x * p.x * mass,
      yy = p.y * p.y * mass,
      zz = p.z * p.z * mass,
      xy = p.x * p.y * mass,
      yz = p.y * p.z * mass,
      xz = p.x * p.z * mass,
    )

  private inline def sandwichImpl(b: Pga3dInertiaSummable, sandwichF: Pga3dProjectivePoint => Pga3dProjectivePoint): Pga3dInertiaSummable =
    val cx = sandwichF(Pga3dProjectivePoint(b.xx, b.xy, b.xz, b.wx))
    val cy = sandwichF(Pga3dProjectivePoint(b.xy, b.yy, b.yz, b.wy))
    val cz = sandwichF(Pga3dProjectivePoint(b.xz, b.yz, b.zz, b.wz))
    val cw = sandwichF(Pga3dProjectivePoint(b.wx, b.wy, b.wz, b.ww))

    val rx = sandwichF(Pga3dProjectivePoint(cx.x, cy.x, cz.x, cw.x))
    val ry = sandwichF(Pga3dProjectivePoint(cx.y, cy.y, cz.y, cw.y))
    val rz = sandwichF(Pga3dProjectivePoint(cx.z, cy.z, cz.z, cw.z))
    val rw = sandwichF(Pga3dProjectivePoint(cx.w, cy.w, cz.w, cw.w))

    Pga3dInertiaSummable(rw.w, rw.x, rw.y, rw.z, rx.x, ry.y, rz.z, rx.y, ry.z, rx.z)

  extension (motor: Pga3dMotor)
    def sandwich(b: Pga3dInertiaSummable): Pga3dInertiaSummable =
      // 8 point sandwiches per call: caching the operator once beats motor.sandwich per point;
      // the projective transform matches motor.sandwich for any motor, normalized or not
      sandwichImpl(b, Pga3dProjectiveTransform(motor).sandwich)

  extension (t: Pga3dTranslator)
    def sandwich(b: Pga3dInertiaSummable): Pga3dInertiaSummable =
      sandwichImpl(b, t.sandwich)

  extension (q: Pga3dRotor)
    def sandwich(b: Pga3dInertiaSummable): Pga3dInertiaSummable =
      sandwichImpl(b, q.sandwich)

  private val planesByAxis = ArraySeq(
    Pga3dPlaneCentral(1, 0, 0),
    Pga3dPlaneCentral(0, 1, 0),
    Pga3dPlaneCentral(0, 0, 1),
  )

  /** the shared Jacobi loop, accumulating the eigenvector basis as a rotor instead of a matrix */
  private def diagonalizeSymmetricInplace(i: ArrayView2d[Double]): Pga3dRotor = {
    var rotor = Pga3dRotor.id
    SymmetricMatrixDiagonalization.diagonalizeSymmetricInplace(i, (p, q, sin, cos) => {
      val planeP = planesByAxis(p)
      val planeQ = planesByAxis(q)
      rotor = rotor.geometric(Pga3dRotor.rotation(planeP, planeP * cos + planeQ * sin))
    })
    rotor
  }
