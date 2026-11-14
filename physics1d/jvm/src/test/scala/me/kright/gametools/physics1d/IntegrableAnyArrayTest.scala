package me.kright.gametools.physics1d

import org.scalatest.funsuite.AnyFunSuiteLike

class IntegrableAnyArrayTest extends AnyFunSuiteLike:
  private val marker = -777.0

  private def markedArray(size: Int): Array[Double] =
    Array.fill(size)(marker)

  test("the states are packed one after another in the order of the parts") {
    val integrableArray = IntegrableAnyArray(TestOscillator(1.0, 1.0, 2.0), TestTriple(3.0, 4.0, 5.0), TestOscillator(1.0, 6.0, 7.0))

    assert(integrableArray.stateSize == 7)
    assert((0 until 3).map(integrableArray.offsetOf) == Seq(0, 2, 5))

    val dst = markedArray(11)
    integrableArray.saveState(dst, 2)
    assert(dst.toSeq == Seq(marker, marker, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, marker, marker))
  }

  test("loadState is the inverse of saveState and reads only its own run of doubles") {
    val a = TestOscillator(1.0, 0.0, 0.0)
    val b = TestTriple(0.0, 0.0, 0.0)
    val integrableArray = IntegrableAnyArray(a, b)

    integrableArray.loadState(Array(marker, 1.0, 2.0, 3.0, 4.0, 5.0, marker), 1)
    assert((a.x, a.v) == (1.0, 2.0))
    assert((b.a, b.b, b.c) == (3.0, 4.0, 5.0))
  }

  test("the derivatives are in the layout of the states") {
    val integrableArray = IntegrableAnyArray(TestOscillator(10.0, 1.0, 2.0), TestTriple(3.0, 4.0, 5.0))

    val dst = markedArray(7)
    integrableArray.saveDerivative(dst, 1)
    assert(dst.toSeq == Seq(marker, 2.0, -10.0, -3.0, -8.0, -15.0, marker))
  }

  test("resetForces reaches every part, nested ones too") {
    val a = TestOscillator(1.0, 0.0, 0.0)
    val b = TestOscillator(1.0, 0.0, 0.0)
    val integrableArray = IntegrableAnyArray(a, IntegrableAnyArray(b))

    integrableArray.resetForces()
    integrableArray.resetForces()
    assert(a.resetsCount == 2)
    assert(b.resetsCount == 2)
  }

  test("a nested array has the layout of the flattened one") {
    def parts() = (TestOscillator(1.0, 1.0, 2.0), TestTriple(3.0, 4.0, 5.0), TestOscillator(2.0, 6.0, 7.0), TestTriple(8.0, 9.0, 10.0))

    val (a, b, c, d) = parts()
    val flat = IntegrableAnyArray(a, b, c, d)
    val (na, nb, nc, nd) = parts()
    val nested = IntegrableAnyArray(na, IntegrableAnyArray(nb, IntegrableAnyArray(nc)), nd)

    assert(nested.stateSize == flat.stateSize)

    val flatState, nestedState, flatDerivative, nestedDerivative = new Array[Double](flat.stateSize)
    flat.saveState(flatState, 0)
    nested.saveState(nestedState, 0)
    flat.saveDerivative(flatDerivative, 0)
    nested.saveDerivative(nestedDerivative, 0)

    assert(nestedState.toSeq == flatState.toSeq)
    assert(nestedDerivative.toSeq == flatDerivative.toSeq)
  }

  test("an empty array has no state and touches nothing") {
    for (empty <- Seq(IntegrableAnyArray(), IntegrableAnyArray(IntegrableAnyArray(), IntegrableAnyArray()), Integrable.empty)) {
      assert(empty.stateSize == 0)

      val array = markedArray(3)
      val offsetRightAfterTheEnd = array.length
      empty.saveState(array, 1)
      empty.saveDerivative(array, offsetRightAfterTheEnd)
      empty.loadState(array, 0)
      empty.resetForces()
      assert(array.forall(_ == marker))
    }
  }

  test("IntegrableAnyArray and IntegrableUniformArray are both an IntegrableArray of their element type") {
    def lastElementStart(integrableArray: IntegrableArray[TestOscillator]): (Double, Int) =
      val last = integrableArray.elements.length - 1
      (integrableArray.elements(last).x, integrableArray.offsetOf(last))

    val elements = Array(TestOscillator(1.0, 1.0, 2.0), TestOscillator(1.0, 10.0, 20.0), TestOscillator(1.0, 100.0, 200.0))

    assert(lastElementStart(IntegrableAnyArray(elements)) == (100.0, 4))
    assert(lastElementStart(Integrable.uniformElementSize(elements, 2)) == (100.0, 4))
    assert(lastElementStart(IntegrableAnyArray(TestOscillator(1.0, 3.0, 0.0))) == (3.0, 0))
  }

  test("IntegrableAnyArray uses the given array itself, without a copy") {
    val source: Array[TestOscillator] = Array(TestOscillator(1.0, 1.0, 2.0))
    val integrableArray = IntegrableAnyArray(source)
    assert(integrableArray.elements eq source)

    source(0) = TestOscillator(1.0, 100.0, 200.0)

    val dst = new Array[Double](2)
    integrableArray.saveState(dst, 0)
    assert(dst.toSeq == Seq(100.0, 200.0))
  }

  test("the factories of Integrable give the same arrays as the factories of the classes") {
    def state(integrable: Integrable): Seq[Double] =
      val dst = new Array[Double](integrable.stateSize)
      integrable.saveState(dst, 0)
      dst.toSeq

    def makeElements(): Array[TestOscillator] =
      Array(TestOscillator(1.0, 1.0, 2.0), TestOscillator(1.0, 10.0, 20.0))

    val elements = makeElements()
    assert(Integrable(elements).elements eq elements)
    assert(state(Integrable(makeElements())) == state(IntegrableAnyArray(makeElements())))
    assert(state(Integrable(TestOscillator(1.0, 3.0, 4.0), TestTriple(5.0, 6.0, 7.0))) == Seq(3.0, 4.0, 5.0, 6.0, 7.0))

    val uniform = Integrable.uniformElementSize(elements, 2)
    assert(uniform.elements eq elements)
    assert(uniform.elementSize == 2)
    assert(state(uniform) == state(IntegrableUniformArray(makeElements(), 2)))

    val mixed: Array[Integrable] = Array(TestOscillator(1.0, 0.0, 0.0), TestTriple(1.0, 2.0, 3.0))
    assertThrows[IllegalArgumentException](Integrable.uniformElementSize(mixed, 2))
    assert(Integrable.uniformElementSize(mixed, 2, checkElementSizes = false).stateSize == 4)
  }
