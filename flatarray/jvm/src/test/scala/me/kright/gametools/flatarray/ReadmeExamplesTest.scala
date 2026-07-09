package me.kright.gametools.flatarray

import FlatView.*
import FlatMutableView.*
import org.scalatest.funsuite.AnyFunSuiteLike

/** Mirrors the "Example" code block in flatarray/README.md, so a README edit that breaks the
 * example fails a test instead of silently bit-rotting. */
private final case class Vec3dReadme(x: Double, y: Double, z: Double) derives FlatDoubleSerializer, CanEqual

class ReadmeExamplesTest extends AnyFunSuiteLike:
  test("README example compiles and behaves as documented") {
    val a = FlatArray[Vec3dReadme](3)
    val b = FlatArray[Vec3dReadme](3)
    a(0) = Vec3dReadme(1.0, 2.0, 3.0)
    b(0) = Vec3dReadme(0.5, 0.5, 0.5)

    val buffer = FlatBuffer[Vec3dReadme](sizeHint = 8)
    buffer += Vec3dReadme(4.0, 5.0, 6.0)
    buffer ++= a
    assert(buffer.size == 4)

    buffer.mapInPlace(v => Vec3dReadme(v.x * 2, v.y * 2, v.z * 2))
    assert(buffer(0) == Vec3dReadme(8.0, 10.0, 12.0))

    a.zipTo(b, a)((x, y) => Vec3dReadme(x.x + y.x, x.y + y.y, x.z + y.z))
    val first: Vec3dReadme = a(0)
    assert(first == Vec3dReadme(1.5, 2.5, 3.5))

    // collections interop (cold path)
    val plain: Array[Vec3dReadme] = a.to(Array)
    assert(plain.length == 3)

    val backToFlat: FlatArray[Vec3dReadme] = plain.to(FlatArray)
    assert(backToFlat.size == 3)
    for (i <- 0 until 3) {
      assert(backToFlat(i) == a(i))
    }
  }
