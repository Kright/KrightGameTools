package me.kright.gametools.pga.codegen.cpp3d

import me.kright.gametools.ga.{GARepresentationConfig, PGA3, Signature}

object Pga3dProvider:
  private val representationConfig = GARepresentationConfig(
    Signature.pga3,
    generatorNames = "wxyz",
    namePrefix = "",
    overrideScalar = Option("s"),
    overridePseudoScalar = Option("i"),
  )

  given pga3: PGA3 = PGA3(representationConfig)
