package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scala.language.unsafeNulls

class OpWithMultivectorConsistencyTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private val eps = 1e-15

  private val classes = Seq(
    classOf[java.lang.Double],
    classOf[Pga3dBivector],
    classOf[Pga3dBivectorBulk],
    classOf[Pga3dBivectorWeight],
    classOf[Pga3dMotor],
    classOf[Pga3dMultivector],
    classOf[Pga3dPlane],
    classOf[Pga3dPlaneIdeal],
    classOf[Pga3dProjectivePoint],
    classOf[Pga3dPoint],
    classOf[Pga3dPseudoScalar],
    classOf[Pga3dQuaternion],
    classOf[Pga3dTranslator],
    classOf[Pga3dVector],
  )

  def makeRandom(cls: Class[?]): AnyRef = {
    val constructor = cls.getConstructors.nn.head.nn
    val args = constructor.getParameterTypes.map(_ => scala.util.Random.nextDouble())
    constructor.newInstance(args *)
  }

  private def toMultivector(instance: AnyRef): Pga3dMultivector = {
    val cls = instance.getClass
    (if (cls == classOf[Pga3dMultivector]) {
      instance
    } else if (cls == classOf[java.lang.Double]) {
      Pga3dMultivector(s = instance.asInstanceOf[Double])
    } else {
      cls.getMethod("toMultivector").nn.invoke(instance)
    }).asInstanceOf[Pga3dMultivector]
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

        if (classes.contains(otherType) || otherType == classOf[Double]) {
          val otherCls = makeRandom(otherType)

          val first = makeRandom(cls)
          val second = makeRandom(otherType)

          val result = toMultivector(method.invoke(first, second))
          val result2 = call(toMultivector(first), methodName, toMultivector(second)).asInstanceOf[Pga3dMultivector]

          val diff = (result - result2).norm

          assert(diff < eps, s"diff = $diff, result1 = {$result}, result2 = {$result2}, first = {$first}, second = {$second}, methodName = $methodName, otherType = $otherType")
        }
      }
    }
  }

  private def testUnOp(methodName: String): Unit = {
    for (cls <- classes) {
      for (method <- cls.getMethods.filter(_.getName == methodName)) {
        val returnType = method.getReturnType

        val otherCls = makeRandom(cls)
        val instance = makeRandom(cls)

        val result = toMultivector(method.invoke(instance))
        val result2 = toMultivector(call(toMultivector(instance), methodName))

        val diff = (result - result2).norm
        assert(diff < eps, s"diff = $diff, result1 = {$result}, result2 = {$result2}, first = {$instance}, methodName = $methodName, returnType = $returnType")
      }
    }
  }

  test("binaryOperations") {
    val methodNames = Seq(
      "$plus",
      "$minus",
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
      "unary_-",
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
