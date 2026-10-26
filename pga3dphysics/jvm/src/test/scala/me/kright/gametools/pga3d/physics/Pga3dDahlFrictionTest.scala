package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dTranslator, Pga3dVector}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dDahlFrictionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val maxForce = 100.0
  private val saturationTravel = 1e-4
  private val origin = Pga3dPoint(0.0, 0.0, 0.0)

  private def makeFriction(): Pga3dDahlFriction =
    val friction = new Pga3dDahlFriction(maxForce, saturationTravel)
    friction.advance(origin)
    friction

  private val bound = 0.01
  private val paths: Gen[List[Pga3dPoint]] =
    Gen.listOf(Pga3dPhysicsGenerators.pointIn(Pga3dPoint(-bound, -bound, -bound), Pga3dPoint(bound, bound, bound)))

  test("constructor validation") {
    assertThrows[IllegalArgumentException](new Pga3dDahlFriction(-1.0, 1e-4))
    assertThrows[IllegalArgumentException](new Pga3dDahlFriction(1.0, 0.0))
  }

  test("the virgin curve along a line saturates exponentially") {
    val friction = makeFriction()
    val u = Pga3dVector(0.0, 0.0, 1.0)
    for (s <- Seq(1e-5, 1e-4, 5e-4, 1e-2)) {
      val expected = u * (-maxForce * (1 - Math.exp(-s / saturationTravel)))
      assert((friction.forceAt(origin + u * s) - expected).norm <= 1e-12 * maxForce, s"at $s")
    }
  }

  test("tiny moves from the virgin state follow the Dahl curve to full precision") {
    val friction = makeFriction()
    val u = Pga3dVector(0.3, -0.5, 0.8).normalizedByNorm
    for (eps <- Seq(1e-6, 1e-8, 1e-10, 1e-12, 1e-14, 1e-16, 1e-20, 1e-40)) {
      val expected = u * (maxForce * Math.expm1(-eps / saturationTravel)) // -maxForce * (1 - exp(-eps / d))
      val force = friction.forceAt(origin + u * eps)
      assert((force - expected).norm <= 1e-12 * expected.norm, s"eps $eps: $force vs $expected")
    }
  }

  test("passivity: the energy the friction returns never exceeds the stored |F|^2 / (2 sigma)") {
    // the element force F = -forceAt; work done by the point on the element = sum F . dx;
    // storage V = |F|^2 / (2 sigma), sigma = maxForce / saturationTravel; passivity: work >= V(end) - V(start)
    val sigma = maxForce / saturationTravel
    forAll(paths, MinSuccessful(300)) { positions =>
      val friction = makeFriction()
      var committed = origin // the last committed point
      var work = 0.0
      var travel = 0.0
      for (p <- positions) {
        // rate-independence lets us refine each segment freely; the trapezoid is then nearly exact
        val n = 50
        val from = committed
        for (i <- 1 to n) {
          val next = from + (p - from) * (i.toDouble / n)
          val dx = next - committed
          val before = -friction.forceAt(committed)
          friction.advance(next)
          committed = next
          val after = -friction.forceAt(next)
          work += ((before + after) * 0.5) antiDotI dx
          travel += dx.norm
        }
      }
      val stored = friction.forceAt(committed).normSquare / (2 * sigma)
      assert(work >= stored - 1e-6 * maxForce * travel, s"work $work < stored $stored")
    }
  }

  test("steady circle: the force settles below saturation with the soft-slider lag") {
    // in the rotating frame of a circle of radius A: |F| = maxForce / sqrt(1 + (d / A)^2), tan(lag) = d / A
    for (radius <- Seq(1e-4, 1e-3)) {
      val friction = new Pga3dDahlFriction(maxForce, saturationTravel)
      val stepsPerPeriod = 20000
      def position(i: Int): Pga3dPoint =
        val phi = 2 * Math.PI * i / stepsPerPeriod
        Pga3dPoint(radius * Math.cos(phi), radius * Math.sin(phi), 0.0)
      def tangent(i: Int): Pga3dVector =
        val phi = 2 * Math.PI * i / stepsPerPeriod
        Pga3dVector(-Math.sin(phi), Math.cos(phi), 0.0)
      friction.advance(position(0))
      for (i <- 1 to 2 * stepsPerPeriod) friction.advance(position(i))
      val i = 2 * stepsPerPeriod
      val force = friction.forceAt(position(i))
      val ratio = saturationTravel / radius
      val expectedNorm = maxForce / Math.sqrt(1 + ratio * ratio)
      val expectedLag = Math.atan(ratio)
      val lag = Math.acos(-(force antiDotI tangent(i)) / force.norm)
      assert(Math.abs(force.norm - expectedNorm) <= 1e-3 * maxForce, s"radius $radius: |F| ${force.norm} vs $expectedNorm")
      assert(Math.abs(lag - expectedLag) <= 1e-3, s"radius $radius: lag $lag vs $expectedLag")
    }
  }

  test("RK4 keeps the 4th order on a straight monotone path (the force is smooth there)") {
    // a push of 60 below the saturation 100 from the virgin state: the body creeps forward, decelerating
    // as the force grows; monotone within the measured interval
    val u = Pga3dVector(1.0, 0.0, 0.0)
    val t = 2e-3
    val dts = Seq(1e-4, 5e-5, 2.5e-5)
    def run(dt: Double): Double =
      val friction = makeFriction()
      val body = new Pga3dPhysicsBody(Pga3dInertia.sphere(mass = 1.0, r = 0.1),
        Pga3dTranslator.addVector(Pga3dVector.zero).toMotor, Pga3dVelocity.linear(Pga3dVector.zero).toBivector)
      val bodies = Array(body)
      for (_ <- 1 to Math.round(t / dt).toInt) {
        Pga3dPhysicsSolverRK4.step(bodies, dt, addForquesToBodies = { _ =>
          val p = body.globalCenterOfMass
          body.addGlobalForque(Pga3dForque.force(p, u * 60.0 + friction.forceAt(p)))
        })
        friction.advance(body.globalCenterOfMass)
      }
      body.globalCenterOfMass.x
    val reference = run(dts.last / 64)
    val errors = dts.map(dt => Math.abs(run(dt) - reference))
    val orders = errors.sliding(2).map { case Seq(coarse, fine) => Math.log(coarse / fine) / Math.log(2.0) }.toSeq
    assert(orders.forall(o => o > 3.7 && o < 4.3), s"errors $errors, orders $orders")
  }
