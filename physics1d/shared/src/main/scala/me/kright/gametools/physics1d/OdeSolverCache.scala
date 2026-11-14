package me.kright.gametools.physics1d

/**
 * Not thread-safe, and a step must not start another step with the same instance (e.g. from
 * its force callback): both would write the same arrays.
 */
final class OdeSolverCache:
  private var arrays: Array[Array[Double]] = new Array[Array[Double]](0)
  private var arraySize: Int = 0

  /** the arrays are reused between the calls and never cleared: their content is whatever the previous call left */
  def getArrays(groupSize: Int, size: Int): Array[Array[Double]] =
    require(groupSize >= 0 && size >= 0)

    if (groupSize != arrays.length || size != arraySize) {
      arrays = Array.fill(groupSize)(new Array[Double](size))
      arraySize = size
    }
    arrays


object OdeSolverCache:
  given CanEqual[OdeSolverCache | Null, Null] = CanEqual.derived

  def getArrays(cache: OdeSolverCache | Null, groupSize: Int, size: Int): Array[Array[Double]] =
    if (cache == null) {
      require(groupSize >= 0 && size >= 0)
      return Array.fill(groupSize)(new Array[Double](size))
    }
    cache.getArrays(groupSize, size)
