package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import scala.language.unsafeNulls

class OpWithMultivectorConsistencyTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private val eps = 1e-15

  private val classes = Seq(
    classOf[java.lang.Double],
    classOf[Pga2dLine],
    classOf[Pga2dLineIdeal],
    classOf[Pga2dMotor],
    classOf[Pga2dMultivector],
    classOf[Pga2dProjectivePoint],
    classOf[Pga2dPoint],
    classOf[Pga2dProjectiveTranslator],
    classOf[Pga2dPseudoScalar],
    classOf[Pga2dRotor],
    classOf[Pga2dTranslator],
    classOf[Pga2dVector],
  )

  def genInstance(cls: Class[?]): Gen[AnyRef] = {
    val constructor = cls.getConstructors.nn.head.nn
    val arity = constructor.getParameterCount
    // Exclude exact 0.0 per component: normalizedByNorm/normalizedByBulk are x / norm, and at zero norm the
    // generic Pga2dMultivector yields NaN while the typed reps keep structural zeros as 0.0, so the consistency
    // check would spuriously fail at that singularity. Non-zero components keep every norm non-zero.
    Gen.listOfN(arity, Gen.double.suchThat(_ != 0.0)).map { args =>
      constructor.newInstance(args.map(_.asInstanceOf[AnyRef]) *).asInstanceOf[AnyRef]
    }
  }

  private def toMultivector(instance: AnyRef): Pga2dMultivector = {
    val cls = instance.getClass
    (if (cls eq classOf[Pga2dMultivector]) {
      instance
    } else if (cls eq classOf[java.lang.Double]) {
      Pga2dMultivector(s = instance.asInstanceOf[Double])
    } else {
      cls.getMethod("toMultivector").nn.invoke(instance)
    }).asInstanceOf[Pga2dMultivector]
  }

  private def call(first: AnyRef, methodName: String, second: AnyRef): AnyRef = {
    val cls = first.getClass
    val method = cls.getMethod(methodName, second.getClass)
    method.nn.invoke(first, second)
  }

  private def call(first: AnyRef, methodName: String): AnyRef = {
    val cls = first.getClass
    val method = cls.getMethod(methodName)
    method.invoke(first)
  }

  private def testBinop(methodName: String): Unit = {
    for (cls <- classes) {
      for (method <- cls.getMethods.filter(_.getName == methodName)) {
        val parameterTypes = method.getParameterTypes
        require(method.getParameterTypes.size == 1)
        val otherType = parameterTypes.head

        if (classes.contains(otherType) || (otherType eq classOf[Double])) {
          val pairs = for (a <- genInstance(cls); b <- genInstance(otherType)) yield (a, b)
          forAll(pairs, MinSuccessful(10)) { case (first, second) =>
            val result = toMultivector(method.invoke(first, second))
            val result2 = call(toMultivector(first), methodName, toMultivector(second)).asInstanceOf[Pga2dMultivector]

            val diff = (result - result2).norm

            assert(diff < eps, s"diff = $diff, result1 = {$result}, result2 = {$result2}, first = {$first}, second = {$second}, methodName = $methodName, otherType = $otherType")
          }
        }
      }
    }
  }

  private def testUnOp(methodName: String): Unit = {
    for (cls <- classes) {
      for (method <- cls.getMethods.filter(_.getName == methodName)) {
        val returnType = method.getReturnType

        forAll(genInstance(cls), MinSuccessful(10)) { instance =>
          val result = toMultivector(method.invoke(instance))
          val result2 = toMultivector(call(toMultivector(instance), methodName))

          val diff = (result - result2).norm
          assert(diff < eps, s"diff = $diff, result1 = {$result}, result2 = {$result2}, first = {$instance}, methodName = $methodName, returnType = $returnType")
        }
      }
    }
  }

  test("binaryOperations") {
    val methodNames = Seq(
      // generated +/- carry @targetName("plus")/("minus"), so reflection sees those names
      "plus",
      "minus",
      "multiplyElementwise",
      "geometric",
      "dot",
      "wedge",
      "antiGeometric",
      "antiDot",
      "antiWedge",
      "sandwich",
      "reverseSandwich",
    )

    methodNames.foreach(testBinop)
  }

  test("unaryOperations") {
    val methodNames = Seq(
      "dual",
      "weight",
      "bulk",
      "unaryMinus", // @targetName of unary_-
      "reverse",
      "antiReverse",
      "bulkNormSquare",
      "bulkNorm",
      "normalizedByBulk",
      "normSquare",
      "norm",
      "normalizedByNorm",
    )

    methodNames.foreach(testUnOp)
  }
