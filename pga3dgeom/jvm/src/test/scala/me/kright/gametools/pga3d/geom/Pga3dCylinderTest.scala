package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dCylinderTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  
  test("contains correctly identifies points inside the cylinder") {
    val a = Pga3dPoint(0, 0, 0)
    val b = Pga3dPoint(0, 0, 10)
    val r = 2.0
    val cylinder = Pga3dCylinder(a, b, r)
    
    // Points inside
    assert(cylinder.contains(Pga3dPoint(0, 0, 5))) // On the axis
    assert(cylinder.contains(Pga3dPoint(1, 0, 5))) // Inside, off axis
    assert(cylinder.contains(Pga3dPoint(0, 1.9, 5))) // Near the edge  
    
    // Points outside
    assert(!cylinder.contains(Pga3dPoint(0, 0, -1))) // Outside along axis
    assert(!cylinder.contains(Pga3dPoint(0, 0, 11))) // Outside along axis
    assert(!cylinder.contains(Pga3dPoint(2.1, 0, 5))) // Outside radially
    assert(!cylinder.contains(Pga3dPoint(10, 10, 10))) // Far outside
  }
  
  test("intersects correctly identifies edges that intersect the cylinder") {
    val a = Pga3dPoint(0, 0, 0)
    val b = Pga3dPoint(0, 0, 10)
    val r = 2.0
    val cylinder = Pga3dCylinder(a, b, r)
    
    // Edge passing through the cylinder
    val edge1 = Pga3dEdge(Pga3dPoint(-5, 0, 5), Pga3dPoint(5, 0, 5))
    assert(cylinder.intersects(edge1))
    
    // Edge touching the cylinder
    val edge2 = Pga3dEdge(Pga3dPoint(2, 0, 5), Pga3dPoint(5, 0, 5))
    assert(cylinder.intersects(edge2))
    
    // Edge not intersecting the cylinder
    val edge3 = Pga3dEdge(Pga3dPoint(3, 0, 5), Pga3dPoint(5, 0, 5))
    assert(!cylinder.intersects(edge3))
    
    // Edge with one endpoint inside the cylinder
    val edge4 = Pga3dEdge(Pga3dPoint(1, 0, 5), Pga3dPoint(5, 0, 5))
    assert(cylinder.intersects(edge4))
  }

  test("intersects rejects a separated edge whose axis projection partially overlaps the axis range") {
    val cylinder = Pga3dCylinder(Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 10), r = 2.0)

    // crosses the near cap plane far from the axis
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(5, 0, -5), Pga3dPoint(5, 0, 5))))
    // crosses the far cap plane far from the axis
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(5, 0, 5), Pga3dPoint(5, 0, 15))))
    // the near twins of the same edges do intersect
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(1, 0, -5), Pga3dPoint(1, 0, 5))))
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(1, 0, 5), Pga3dPoint(1, 0, 15))))
  }

  test("intersects clamps the edge exactly at the cap planes") {
    val cylinder = Pga3dCylinder(Pga3dPoint(0, 0, 0), Pga3dPoint(0, 0, 10), r = 2.0)

    // edge.a is beyond the far cap; the edge reaches radius ~1.93 < r exactly at the far cap plane
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(0.3, 0, 12), Pga3dPoint(6, 0, 5))))
    // the mirrored case: edge.b is below the near cap
    assert(cylinder.intersects(Pga3dEdge(Pga3dPoint(6, 0, 5), Pga3dPoint(0.3, 0, -2))))
    // the same two edges shifted outwards (radius ~4 at the cap plane) do not intersect
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(2.5, 0, 12), Pga3dPoint(8, 0, 5))))
    assert(!cylinder.intersects(Pga3dEdge(Pga3dPoint(8, 0, 5), Pga3dPoint(2.5, 0, -2))))
  }
