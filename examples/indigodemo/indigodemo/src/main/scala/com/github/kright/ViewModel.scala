package com.github.kright

import me.kright.gametools.pga3d.Pga3dPoint
import indigo.shared.datatypes.Point

final case class ViewModel(firstConnectionPoint: Point,
                           secondConnectionPoint: Point,
                           firstBodyPos: Point,
                           secondBodyPos: Point)

object ViewModel:
  def apply(model: Model): ViewModel =
    val firstConnectionPoint = Model.firstBodyConnectionPoint
    val secondConnectionPoint = Model.secondBodyConnectionPoint

    val firstBodyCenter = model.system.state(0).globalCenter
    val secondBodyCenter = model.system.state(1).globalCenter

    ViewModel(
      toPoint(firstConnectionPoint),
      toPoint(secondConnectionPoint),
      toPoint(firstBodyCenter),
      toPoint(secondBodyCenter)
    )

  private def toPoint(pga3dPoint: Pga3dPoint): Point = {
    val scale = 5.0
    Point((pga3dPoint.x * scale).toInt, -(pga3dPoint.y * scale).toInt)
  }
