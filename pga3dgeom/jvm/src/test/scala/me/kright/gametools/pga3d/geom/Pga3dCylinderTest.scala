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
