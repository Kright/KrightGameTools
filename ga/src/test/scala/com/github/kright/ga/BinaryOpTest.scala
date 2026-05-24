package com.github.kright.ga

import com.github.kright.ga.GAGenerator.forAnyGA
import com.github.kright.util.Sign
import org.scalatest.funsuite.AnyFunSuite

class BinaryOpTest extends AnyFunSuite:

  test("dot is symmetric for vectors") {
    forAnyGA {
      val dot = ga.operations.multiplication.dot

      for (left <- ga.blades.filter(_.grade == 1);
           right <- ga.blades.filter(_.grade == 1) if left.bits <= right.bits) {
        assert(dot(left, right) == dot(right, left),
          s"""left = ${left},
             |right = ${right},
             |left dot right = ${dot(left, right)}
             |right dot left = ${dot(right, left)}
             |basis = ${ga}
             |""".stripMargin)
      }
    }
  }

  test("wedge is anti-symmetric") {
    forAnyGA {
      val wedge = ga.operations.multiplication.wedge

      for (left <- ga.blades.filter(_.grade == 1);
           right <- ga.blades.filter(_.grade == 1) if left.bits <= right.bits) {
        val w1 = wedge(left, right)
        val w2 = wedge(right, left)

        assert(w1.sign == -w2.sign && w1.basisBlade == w2.basisBlade)
      }
    }
  }

  test("geometric is a sum of dot and wedge") {
    forAnyGA {
      for (left <- ga.blades.filter(_.grade == 1);
           right <- ga.blades.filter(_.grade == 1) if left.bits <= right.bits) {

        val m = ga.operations.multiplication

        val w = m.wedge(left, right)
        val b = m.dot(left, right)
        val g = m.geometric(left, right)

        assert(g == w && b.sign == Sign.Zero || g == b && w.sign == Sign.Zero)
      }
    }
  }

  test("wedge grade is sums of grades or zero") {
    forAnyGA {
      for (left <- ga.blades;
           right <- ga.blades) {

        val w = ga.operations.multiplication.wedge(left, right)

        assert(w.basisBlade.grade == 0 || w.basisBlade.grade == left.grade + right.grade)
      }
    }
  }

  test("dot grade is sub of grades or zero") {
    forAnyGA {
      for (left <- ga.blades;
           right <- ga.blades) {

        val w = ga.operations.multiplication.dot(left, right)

        assert(w.basisBlade.grade == 0 || w.basisBlade.grade == math.abs(left.grade - right.grade))
      }
    }
  }
