package com.github.kright.mathutil

import com.github.kright.mathutil.EqualityEps

import scala.annotation.targetName

trait IEqualsWithEps[T]:
  def isEquals(other: T, eps: Double): Boolean

  @targetName("isEquals")
  def ===(other: T)(using eps: EqualityEps): Boolean =
    isEquals(other, eps.value)
