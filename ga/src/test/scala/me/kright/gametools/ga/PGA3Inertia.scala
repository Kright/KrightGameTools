package me.kright.gametools.ga


case class PGA3Inertia[T](mass: MultiVector[T],
                          invertedMass: MultiVector[T]):
  def apply(localB: MultiVector[T])(using num: Numeric[T]): MultiVector[T] =
    localB.multiplyElementwise(mass).dual

  def invert(localInertia: MultiVector[T])(using num: Numeric[T]): MultiVector[T] =
    localInertia.dual.multiplyElementwise(invertedMass)

  def mapValues[U](f: T => U): PGA3Inertia[U] =
    PGA3Inertia(
      mass.mapValues(f),
      invertedMass.mapValues(f)
    )

  def linearMass(using num: Numeric[T]): T =
    mass("wx")


object PGA3Inertia:
  def apply(m: Double, mryz: Double, mrxz: Double, mrxy: Double)(using ga: PGA3): PGA3Inertia[Double] =
    val inertia = MultiVector[Double](
      "wx" -> m,
      "wy" -> m,
      "wz" -> m,
      "yz" -> mryz,
      "xz" -> mrxz,
      "xy" -> mrxy,
    )

    PGA3Inertia(
      inertia,
      inertia.mapValues(d => 1.0 / d)
    )

  def cube(m: Double, rx: Double, ry: Double, rz: Double)(using ga: PGA3): PGA3Inertia[Double] =
    val rx2 = rx * rx
    val ry2 = ry * ry
    val rz2 = rz * rz
    apply(
      m = m,
      mryz = (ry2 + rz2) * m / 3,
      mrxz = (rx2 + rz2) * m / 3,
      mrxy = (rx2 + ry2) * m / 3,
    )
