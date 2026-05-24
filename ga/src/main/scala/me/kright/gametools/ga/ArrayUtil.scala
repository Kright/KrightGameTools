package me.kright.gametools.ga

extension [T <: AnyRef](arr: Array[T | Null])
  private[ga] inline def getOrElseUpdate(i: Int, inline update: => T): T =
    val result = arr(i)
    if (result != null) {
      result.nn
    } else {
      val newValue = update
      arr(i) = newValue
      newValue
    }