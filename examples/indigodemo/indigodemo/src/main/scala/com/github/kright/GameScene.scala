package com.github.kright

import indigo.*
import indigo.scenes.*
import indigo.shared.datatypes.RGBA

object GameScene extends Scene[StartUpData, Model, ViewModel]:

  type SceneModel = Model
  type SceneViewModel = ViewModel

  val name: SceneName =
    SceneName("game")

  val modelLens: Lens[Model, Model] =
    Lens.keepLatest

  val viewModelLens: Lens[ViewModel, ViewModel] =
    Lens.keepLatest

  val eventFilters: EventFilters =
    EventFilters.Permissive

  val subSystems: Set[SubSystem[Model]] =
    Set()

  def updateModel(
                   context: SceneContext[StartUpData],
                   model: SceneModel
                 ): GlobalEvent => Outcome[SceneModel] =
    _ =>
      Outcome(
        model.afterTime(context.frame.time.delta.toDouble * 5.0)
      )

  def updateViewModel(
                       context: SceneContext[StartUpData],
                       model: SceneModel,
                       viewModel: SceneViewModel
                     ): GlobalEvent => Outcome[SceneViewModel] =
    _ => Outcome(ViewModel(model))

  def present(
               context: SceneContext[StartUpData],
               model: SceneModel,
               viewModel: SceneViewModel
             ): Outcome[SceneUpdateFragment] = {
    val screenCenter = context.frame.viewport.center

    Outcome(
      SceneUpdateFragment(
        Shape
          .Circle(
            center = viewModel.firstBodyPos,
            radius = 10,
            Fill.LinearGradient(Point(0), RGBA.Magenta, Point(45), RGBA.Cyan)
          )
          .moveBy(screenCenter),
        Shape
          .Circle(
            center = viewModel.secondBodyPos,
            radius = 10,
            Fill.LinearGradient(Point(0), RGBA.Magenta, Point(45), RGBA.Cyan)
          )
          .moveBy(screenCenter),
        Shape
          .Line(viewModel.firstConnectionPoint, viewModel.firstBodyPos, Stroke(1, RGBA.White))
          .moveBy(screenCenter),
        Shape
          .Line(viewModel.secondConnectionPoint, viewModel.secondBodyPos, Stroke(1, RGBA.White))
          .moveBy(screenCenter),
        Shape
          .Line(viewModel.firstBodyPos, viewModel.secondBodyPos, Stroke(1, RGBA.White))
          .moveBy(screenCenter)
      )
    )
  }
