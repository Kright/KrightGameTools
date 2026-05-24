package com.github.kright.ga

import com.github.kright.symbolic.Sym
import com.github.kright.util.Sign


extension (obj: Sym.type)
  def multiVector(baseName: String)(using ga: GA): MultiVector[Sym] =
    new MultiVector[Sym](ga, ga.blades.map(b => b -> Sym(s"${baseName}.${ga.representation(b)}")).toMap)

  def multiVector(baseName: String, grade: Int)(using ga: GA): MultiVector[Sym] =
    new MultiVector[Sym](ga, ga.blades.filter(_.grade == grade).map(b => b -> Sym(s"${baseName}.${ga.representation(b)}")).toMap)

  def point(baseName: String)(using ga: PGA): MultiVector[Sym] =
    MultiVector[Sym](
      ga.generators.map { g =>
        val blade = BasisBlade(g, ga.signature)
        val symbol = if (g.squareSign == Sign.Zero) {
          Sym(1.0)
        } else {
          Sym(s"${baseName}.${ga.representation(g)}")
        }
        blade -> symbol
      }
    ).dual

  def idealPoint(baseName: String)(using ga: PGA): MultiVector[Sym] =
    point(baseName).weight

  def planeX(d: Sym = Sym.zero)(using ga: PGA3): MultiVector[Sym] =
    PGA3.plane(
      Sym.one,
      Sym.zero,
      Sym.zero,
      d,
    )

  def plane(baseName: String)(using ga: PGA3): MultiVector[Sym] =
    PGA3.plane(
      Sym(s"${baseName}.nx"),
      Sym(s"${baseName}.ny"),
      Sym(s"${baseName}.nz"),
      Sym(s"${baseName}.d"),
    )
