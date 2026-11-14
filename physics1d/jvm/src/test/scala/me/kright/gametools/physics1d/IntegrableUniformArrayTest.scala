package me.kright.gametools.physics1d

import org.scalatest.funsuite.AnyFunSuiteLike
import scala.collection.immutable.ArraySeq

class IntegrableUniformArrayTest extends AnyFunSuiteLike:
  private val marker = -777.0

  private def makeElements(): Array[TestOscillator] =
    Array.tabulate(4)(i => TestOscillator(k = 10.0 + i, x = 1.0 + i, v = -0.5 * i))

  test("a uniform array has the layout of an IntegrableAnyArray of the same elements") {
    val elements = makeElements()
    val uniform = IntegrableUniformArray[TestOscillator](elements, 2)
    val general = Integrable(elements)

    assert(uniform.elementSize == 2)
    assert(uniform.stateSize == general.stateSize)
    assert(elements.indices.map(uniform.offsetOf) == elements.indices.map(general.offsetOf))

    val uniformState, generalState, uniformDerivative, generalDerivative = Array.fill(uniform.stateSize + 3)(marker)
    uniform.saveState(uniformState, 2)
    general.saveState(generalState, 2)
    uniform.saveDerivative(uniformDerivative, 2)
    general.saveDerivative(generalDerivative, 2)

    assert(uniformState.toSeq == generalState.toSeq)
    assert(uniformDerivative.toSeq == generalDerivative.toSeq)
  }

  test("the size check can be switched off") {
    val mixed: Array[Integrable] = Array(TestOscillator(1.0, 0.0, 0.0), TestTriple(1.0, 2.0, 3.0))
    assert(IntegrableUniformArray[Integrable](mixed, 2, checkElementSizes = false).stateSize == 4)
    assertThrows[IllegalArgumentException](IntegrableUniformArray[Integrable](mixed, 2))
  }

  test("the elements and their size are known to a user of the uniform array") {
    def firstX(integrableArray: IntegrableUniformArray[TestOscillator]): Double =
      integrableArray.elements(0).x

    val elements = makeElements()
    val integrableArray = IntegrableUniformArray[TestOscillator](elements, 2)
    assert(integrableArray.elementSize == 2)
    assert(integrableArray.stateSize == elements.length * 2)
    assert(firstX(integrableArray) == elements(0).x)
  }

  test("loadState gives every element its own run of doubles") {
    val source = IntegrableUniformArray[TestOscillator](makeElements(), 2)
    val saved = Array.fill(source.stateSize + 5)(marker)
    source.saveState(saved, 5)

    val target = Array.fill(4)(TestOscillator(1.0, 0.0, 0.0))
    IntegrableUniformArray[TestOscillator](target, 2).loadState(saved, 5)

    assert(target.map(e => (e.x, e.v)).toSeq == source.elements.map(e => (e.x, e.v)).toSeq)
  }

  test("resetAccum reaches every element") {
    val elements = makeElements()
    val integrableArray = IntegrableUniformArray[TestOscillator](elements, 2)
    integrableArray.resetAccum()
    integrableArray.resetAccum()
    assert(elements.forall(_.resetsCount == 2))
  }

  test("elements of different state sizes are rejected") {
    val mixed: Array[Integrable] = Array(TestOscillator(1.0, 0.0, 0.0), TestTriple(1.0, 2.0, 3.0))
    assertThrows[IllegalArgumentException](IntegrableUniformArray[Integrable](mixed, 2))
  }

  test("an empty uniform array has no state") {
    val empty = IntegrableUniformArray[TestOscillator](Array.empty[TestOscillator], 2)
    assert(empty.stateSize == 0)

    val array = Array.fill(3)(marker)
    empty.saveState(array, 1)
    empty.saveDerivative(array, 1)
    empty.loadState(array, 1)
    empty.resetAccum()
    assert(array.forall(_ == marker))
  }

  test("the uniform array uses the given array itself, without a copy") {
    val elements = makeElements()
    val integrableArray = IntegrableUniformArray[TestOscillator](elements, 2)
    assert(integrableArray.elements eq elements)

    val replacement = TestOscillator(1.0, 100.0, 200.0)
    elements(0) = replacement

    val state = new Array[Double](integrableArray.stateSize)
    integrableArray.saveState(state, 0)
    assert(state.take(2).toSeq == ArraySeq(100.0, 200.0))
  }
