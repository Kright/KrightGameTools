package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BivectorExponentTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("exp for bivector") {
    forAll(Pga3dGenerators.bivectors) { bivector =>
      assert((bivector.exp() - bivector.exp(1.0)).norm < 1e-15)
      assert((bivector.bulk.exp() - bivector.bulk.exp(1.0)).norm < 1e-15)
      assert((bivector.weight.exp() - bivector.weight.exp(1.0)).norm < 1e-15)
    }
  }

  test("exp for full with t") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.double1) { (bivector, t) =>
      assert(((bivector * t).exp() - bivector.exp(t)).norm < 1e-15)
      assert(((bivector.bulk * t).exp() - bivector.bulk.exp(t)).norm < 1e-15)
      assert(((bivector.weight * t).exp() - bivector.weight.exp(t)).norm < 1e-15)
    }
  }

  test("log is inverse of exp") {
    forAll(Pga3dGenerators.bivectors) { bivector =>
      assert((bivector - bivector.exp().log()).norm < 1e-14)
      assert((bivector.bulk - bivector.bulk.exp().log()).norm < 1e-14)
      assert((bivector.weight - bivector.weight.exp().log()).norm < 1e-14)
    }
  }

//  test("log is inverse of exp failed") {
//    val b = Pga3dBivector(-0.26426518006043076, -0.8856240901960408, -0.17870331716302612, 0.9917926945307447, 0.936262951274583, 0.8114467770348108)
//
//    println(b)
//    println(b.exp().log())
//    println(b.bulk.exp().log())
//    println(b.weight.exp().log())
//
//    println(s"b.exp")
//    println(b.exp())
//    println(b.exp().log().exp())
//    println(b.bulk.exp().log().exp())
//    println(b.weight.exp().log().exp())
//  }

  test("bivector split") {
    forAll(Pga3dGenerators.bivectors.filter(_.bulkNorm > 1e-6)) { bivector =>
      val (line, shift) = bivector.split()

      assert(math.abs(line.weight.dual.dot(line.bulk)) < 1e-15)
    }
  }
