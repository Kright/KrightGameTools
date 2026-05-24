package me.kright.gametools.ga

import scala.language.implicitConversions
import scala.util.chaining.*

case class GA(representationConfig: GARepresentationConfig):
  val signature = representationConfig.signature

  val generators: IndexedSeq[Generator] = (0 until signature.generatorsCount).map(i => Generator(i, signature))
  val blades: IndexedSeq[BasisBlade] = (0 until signature.bladesCount).map(b => BasisBlade(b, signature)).sortBy(_.generators).sortBy(_.grade)

  val operations = GAOperations(signature)
  val representation = GARepresentation(representationConfig, operations, generators, blades)

  def scalarBlade: BasisBlade = blades.head
  def pseudoScalarBlade: BasisBlade = blades.last


def ga[T <: GA](using b: T): T = b


object GA:
  def ga2: GA = GA(GARepresentationConfig.wxyz(Signature.ga2))
  def ga3: GA = GA(GARepresentationConfig.wxyz(Signature.ga3))
  def ga4: GA = GA(GARepresentationConfig.wxyz(Signature.ga4))

  def pga2: PGA2 = PGA2(GARepresentationConfig.wxyz(Signature.pga2))
  def pga3: PGA3 = PGA3(GARepresentationConfig.wxyz(Signature.pga3))

  extension [B <: GA](ga: B)
    def use[T](f: B ?=> T): T =
      given b: B = ga

      f
