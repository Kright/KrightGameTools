package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class GeometricProductTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val eps = 1e-13

  test("geometric product is associative") {
    forAll(Pga3dGenerators.multivectors, Pga3dGenerators.multivectors, Pga3dGenerators.multivectors, MinSuccessful(1000)) { (a, b, c) =>
      val left = a.geometric(b).geometric(c)
      val right = a.geometric(b.geometric(c))
      assert((left - right).norm < eps)
    }
  }

  test("geometric product is distributive over addition") {
    forAll(Pga3dGenerators.multivectors, Pga3dGenerators.multivectors, Pga3dGenerators.multivectors, MinSuccessful(1000)) { (a, b, c) =>
      val left = a.geometric(b + c)
      val right = a.geometric(b) + a.geometric(c)
      assert((left - right).norm < eps)
    }
  }

  test("reverse of product is product of reverses in swapped order") {
    forAll(Pga3dGenerators.multivectors, Pga3dGenerators.multivectors, MinSuccessful(1000)) { (a, b) =>
      val left = a.geometric(b).reverse
      val right = b.reverse.geometric(a.reverse)
      assert((left - right).norm < eps)
    }
  }

  test("scalar is the unit of the geometric product") {
    forAll(Pga3dGenerators.multivectors) { a =>
      val one = Pga3dMultivector(s = 1.0)
      assert((one.geometric(a) - a).norm < eps)
      assert((a.geometric(one) - a).norm < eps)
    }
  }
