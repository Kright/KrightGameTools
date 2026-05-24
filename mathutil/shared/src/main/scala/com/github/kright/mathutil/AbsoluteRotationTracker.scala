package com.github.kright.mathutil

import com.github.kright.mathutil.AbsoluteRotationTracker.*

/**
 * Tracks rotations count
 * There should be no more than half of the turnover between calls to `update`
 *
 * Position of zero could be reset with [[resetAbsolute]]
 *
 * @param rotations       is stored in rotations, not in radians
 * @param rotationsOffset is stored in rotations in the interval [0, 1)
 */
class AbsoluteRotationTracker(var rotations: Double = 0.0,
                              var rotationsOffset: Double = 0.0):

  /**
   * @return angle in radians
   */
  def angle: Double =
    toRadians(rotations + rotationsOffset)

  /**
   * resets rotations and rotationsOffset, so the absolute becomes equal newAbsoluteInRadians
   */
  def angle_=(newAbsoluteInRadians: Double): Unit =
    setNewOffset(toRotations(newAbsoluteInRadians) - rotations)

  private def setNewOffset(newOffset: Double): Unit = {
    val floor = newOffset.floor
    rotationsOffset = newOffset - floor
    rotations += floor
  }

  /**
   * newAngleInRadians could have any value and will be wrapped into an interval of length 2 * PI
   */
  def update(newAngleInRadians: Double): Unit =
    val wrappedNewAngle = getWrapped(toRotations(newAngleInRadians))
    val wrappedOldAngle = getWrapped(rotations)

    // diff is in [-1, 1]
    val diff = wrappedNewAngle - wrappedOldAngle
    rotations += diff

    if (diff > 0.5) {
      rotations -= 1.0
    } else if (diff < -0.5) {
      rotations += 1.0
    }

  def deepCopy: AbsoluteRotationTracker =
    AbsoluteRotationTracker(rotations, rotationsOffset)


object AbsoluteRotationTracker:
  def toRotations(angleInRadians: Double): Double =
    angleInRadians * MathUtil.TauDiv

  def toRadians(angleInRotations: Double): Double =
    angleInRotations * MathUtil.Tau

  /**
   * @return wrapped angle in interval [0, 1)
   */
  private def getWrapped(angleInRotations: Double): Double =
    angleInRotations - angleInRotations.floor
