package com.github.kright.util

import com.github.kright.util.EqualityEps

import scala.annotation.targetName

trait IEqualsWithEps[T]:
  def isEquals(other: T, eps: Double): Boolean

  @targetName("isEquals")
  def ===(other: T)(using eps: EqualityEps): Boolean =
    isEquals(other, eps.value)
