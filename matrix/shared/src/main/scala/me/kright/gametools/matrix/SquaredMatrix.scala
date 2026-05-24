package me.kright.gametools.matrix

import scala.util.chaining.scalaUtilChainingOps

trait SquaredMatrix[S <: SquaredMatrix] {
  self: Matrix =>

  def det(): Double
}
