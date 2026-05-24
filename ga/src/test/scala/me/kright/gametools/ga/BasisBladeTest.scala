package me.kright.gametools.ga

import me.kright.gametools.ga.GAGenerator.forAnyGA
import com.github.kright.mathutil.Sign
import org.scalatest.funsuite.AnyFunSuite

class BasisBladeTest extends AnyFunSuite:
  test("multiplication with unsigned complement is pseudo scalar with some sign") {
    forAnyGA {
      for (blade <- ga.blades) {
        val unsignedComplement = blade.unsignedComplement
        assert(ga.operations.multiplication.geometric(blade, unsignedComplement).basisBlade == ga.pseudoScalarBlade,
          s"${blade} geometric ${unsignedComplement} = ${ga.operations.multiplication.geometric(blade, unsignedComplement)}")
      }
    }
  }

  test("right and left complement") {
    forAnyGA {
      for (blade <- ga.blades) {
        val bladeWithSign = BasisBladeWithSign(blade)
        assert(ga.operations.multiplication.geometric(bladeWithSign, ga.operations.complement.right(blade)) == BasisBladeWithSign(ga.pseudoScalarBlade, Sign.Positive))
        assert(ga.operations.multiplication.geometric(ga.operations.complement.left(blade), bladeWithSign) == BasisBladeWithSign(ga.pseudoScalarBlade, Sign.Positive))
      }
    }
  }
