package me.kright.gametools.ga

case class GARepresentationConfig(signature: Signature,
                                  generatorNames: Seq[Char],
                                  namePrefix: String,
                                  overrideScalar: Option[String],
                                  overridePseudoScalar: Option[String]):
  val char2Index: Map[Char, Int] = generatorNames.zipWithIndex.toMap

object GARepresentationConfig:
  private def checkDimensions(signature: Signature): Unit =
    require(signature.negatives == 0)
    require(signature.positives <= 4)
    require(signature.zeros <= 1)

  def wxyz(signature: Signature): GARepresentationConfig =
    checkDimensions(signature)

    GARepresentationConfig(
      signature,
      generatorNames = "w".take(signature.zeros) ++ "xyzu".take(signature.positives),
      namePrefix = "",
      overrideScalar = Option("1"),
      overridePseudoScalar = Option("I"),
    )

  def e0123(signature: Signature): GARepresentationConfig =
    checkDimensions(signature)

    GARepresentationConfig(
      signature,
      generatorNames = "0".take(signature.zeros) ++ "1234".take(signature.positives),
      namePrefix = "e",
      overrideScalar = Option("1"),
      overridePseudoScalar = None,
    )
