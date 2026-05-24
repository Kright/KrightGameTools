package me.kright.gametools.symbolic

import org.scalatest.funsuite.AnyFunSuiteLike

class SymbolicTest extends AnyFunSuiteLike:
  test("isFunc or isSymbol") {
    def assertIsSymbol[F, S](v: Symbolic[F, S], isSymbol: Boolean): Unit = {
      assert(v.isSymbol == isSymbol)
      assert(v.isFunc == !isSymbol)
    }

    assertIsSymbol(Symbolic.Func[String, Double]("f", Seq()), false)
    assertIsSymbol(Symbolic.Func[String, Double]("f", Seq(Symbolic.Symbol(0.0))), false)
    assertIsSymbol(Symbolic.Symbol[Double](1.0), true)
  }
