package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dRotor, Pga3dTranslator, Pga3dVector}
import me.kright.gametools.physics1d.{IwanElement, IwanFriction}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dIwanFrictionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  // the reference bushing pair of IwanFrictionTest: a stiff "micro" and a soft "macro" element
  private val micro = IwanElement(stiffness = 4e6, breakForce = 150.0) // breaks at 3.75e-5
  private val macro_ = IwanElement(stiffness = 1e6, breakForce = 250.0) // breaks at 2.5e-4
  private val maxForce = micro.breakForce + macro_.breakForce // 400
  private val origin = Pga3dPoint(0.0, 0.0, 0.0)

  /** friction with the starting point already latched at the origin */
  private def makeFriction(): Pga3dIwanFriction =
    val friction = Pga3dIwanFriction(micro, macro_)
    friction.advance(origin)
    friction

  private def make1d(): IwanFriction =
    val friction = IwanFriction(micro, macro_)
    friction.advance(0.0)
    friction

  private val bound = 0.01
  private val points: Gen[Pga3dPoint] =
    Pga3dPhysicsGenerators.pointIn(Pga3dPoint(-bound, -bound, -bound), Pga3dPoint(bound, bound, bound))
  private val paths: Gen[List[Pga3dPoint]] = Gen.listOf(points)
  private val scalars: Gen[List[Double]] = Gen.listOf(Gen.choose(-bound, bound))
  private val directions: Gen[Pga3dVector] =
    Pga3dPhysicsGenerators.vectorIn(Pga3dPoint(-1, -1, -1), Pga3dPoint(1, 1, 1))
      .filter(_.norm > 0.1).map(_.normalizedByNorm)

  test("constructor validation") {
    assertThrows[IllegalArgumentException](Pga3dIwanFriction())
  }

  test("the first advance only latches the point") {
    val friction = Pga3dIwanFriction(micro, macro_)
    val p = Pga3dPoint(1.0, 2.0, 3.0)
    assert(friction.forceAt(p) == Pga3dVector.zero) // no committed point yet
    friction.advance(p)
    assert(friction.forceAt(p) == Pga3dVector.zero)
    val q = p + Pga3dVector(1e-5, 0.0, 0.0)
    friction.advance(q)
    assert(friction.forceAt(q).x < 0 && friction.forceAt(q).y == 0.0 && friction.forceAt(q).z == 0.0)
  }

  test("back and forth along one line is equivalent to the 1d IwanFriction, trial points included") {
    forAll(directions, scalars, MinSuccessful(300)) { (u, positions) =>
      val friction3d = makeFriction()
      val friction1d = make1d()
      for (s <- positions) {
        val p = origin + u * s
        // a trial evaluation before the commit must agree as well, and must not disturb the state
        val trial = s + 3e-5
        assert((friction3d.forceAt(origin + u * trial) - u * friction1d.forceAt(trial)).norm <= 1e-12 * maxForce)
        friction3d.advance(p)
        friction1d.advance(s)
        val f3 = friction3d.forceAt(p)
        val f1 = friction1d.forceAt(s)
        assert((f3 - u * f1).norm <= 1e-12 * maxForce, s"3d $f3 vs 1d $f1 along $u at $s")
      }
    }
  }

  test("the virgin curve along a line: spot values of the 1d model") {
    val friction = makeFriction()
    val u = Pga3dVector(0.0, 1.0, 0.0)
    // -(4e6 + 1e6) * 1e-5 = -50; -(150 + 1e6 * 1e-4) = -250; saturation
    assert((friction.forceAt(origin + u * 1e-5) - u * -50.0).norm <= 1e-12 * maxForce)
    assert((friction.forceAt(origin + u * 1e-4) - u * -250.0).norm <= 1e-12 * maxForce)
    assert((friction.forceAt(origin + u * 1e-2) - u * -maxForce).norm <= 1e-12 * maxForce)
  }

  test("rate-independence: many substeps of one straight segment equal one big step") {
    forAll(directions, directions, directions, MinSuccessful(100)) { (u1, u2, u3) =>
      val oneStep = makeFriction()
      val substeps = makeFriction()
      // a non-trivial history with turns in it
      val a = origin + u1 * 6e-5
      val b = a + u2 * 1e-4
      for (f <- Seq(oneStep, substeps)) { f.advance(a); f.advance(b) }

      val dx = u3 * 4e-4
      val n = 100
      val end = b + dx
      oneStep.advance(end)
      for (i <- 1 to n) substeps.advance(b + dx * (i.toDouble / n))

      assert((oneStep.forceAt(end) - substeps.forceAt(end)).norm <= 1e-9 * maxForce)
    }
  }

  test("rotation invariance: the rotated path gives the rotated force") {
    forAll(directions, directions, paths, MinSuccessful(100)) { (from, to, positions) =>
      val rotor = Pga3dRotor.rotation(from, to)
      val friction = makeFriction()
      val rotated = makeFriction()
      for (p <- positions) {
        friction.advance(p)
        rotated.advance(rotor.sandwich(p).toPointUnsafe)
        val expected = rotor.sandwich(friction.forceAt(p))
        assert((rotated.forceAt(rotor.sandwich(p).toPointUnsafe) - expected).norm <= 1e-9 * maxForce)
      }
    }
  }

  test("odd symmetry: the mirrored path gives exactly the negated force") {
    forAll(paths, MinSuccessful(300)) { positions =>
      val friction = makeFriction()
      val mirrored = makeFriction()
      for (p <- positions) {
        val m = (origin - (p - origin))
        friction.advance(p)
        mirrored.advance(m)
        assert(mirrored.forceAt(m) == -friction.forceAt(p))
      }
    }
  }

  test("force stays bounded by maxForce on any path") {
    forAll(paths, MinSuccessful(300)) { positions =>
      val friction = makeFriction()
      for (p <- positions) {
        friction.advance(p)
        assert(friction.forceAt(p).norm <= maxForce * (1 + 1e-12))
      }
    }
  }

  test("forceAt is pure and consistent with advance, including trial turns") {
    val friction = makeFriction()
    val a = Pga3dPoint(1e-4, 0.0, 0.0)
    friction.advance(a)
    val committed = friction.forceAt(a)

    val forward = friction.forceAt(Pga3dPoint(2e-4, 0.0, 0.0))
    val sideways = friction.forceAt(Pga3dPoint(1e-4, 1e-4, 0.0)) // trial turn, must not mutate
    val backward = friction.forceAt(Pga3dPoint(0.5e-4, 0.0, 0.0))
    assert(backward.x > committed.x)
    assert(sideways.y < 0.0)
    assert(friction.forceAt(a) == committed)
    assert(friction.forceAt(Pga3dPoint(2e-4, 0.0, 0.0)) == forward)
    assert(friction.forceAt(Pga3dPoint(1e-4, 1e-4, 0.0)) == sideways)
    assert(friction.forceAt(Pga3dPoint(0.5e-4, 0.0, 0.0)) == backward)

    friction.advance(Pga3dPoint(1e-4, 1e-4, 0.0))
    assert(friction.forceAt(Pga3dPoint(1e-4, 1e-4, 0.0)) == sideways) // the committed value is exactly the stage evaluation
  }

  test("movedForce on vectors and the in-place scalar step agree, and the in-place step may overwrite its input") {
    forAll(points, points, points, MinSuccessful(300)) { (a, b, c) =>
      val force = (b - a) * 1e6 // up to ~2e4, well beyond every break force
      val dx = c - a
      for (element <- Seq(micro, macro_, IwanElement(1e6, 0.0))) {
        val expected = Pga3dIwanFriction.movedForce(force, dx, element)
        val inPlace = Array(0.0, force.x, force.y, force.z, 0.0)
        Pga3dIwanFriction.moveForce(inPlace(1), inPlace(2), inPlace(3), dx.x, dx.y, dx.z, element, inPlace, 1)
        assert(inPlace(0) == 0.0 && inPlace(4) == 0.0)
        assert(Pga3dVector(inPlace(1), inPlace(2), inPlace(3)) == expected)
        assert(expected.norm <= element.breakForce * (1 + 1e-12))
      }
    }
  }

  test("advancing to the same point keeps the state unchanged") {
    val friction = makeFriction()
    val a = Pga3dPoint(1e-4, 2e-5, 0.0)
    friction.advance(a)
    val before = friction.forceAt(a)
    friction.advance(a)
    assert(friction.forceAt(a) == before)
  }

  test("deepCopy is independent") {
    val friction = makeFriction()
    val a = Pga3dPoint(1e-4, 0.0, 0.0)
    friction.advance(a)
    val copy = friction.deepCopy()
    assert(copy.forceAt(a) == friction.forceAt(a))

    val b = Pga3dPoint(0.0, 1e-3, 0.0)
    friction.advance(b)
    assert(copy.forceAt(a) != friction.forceAt(b))

    copy.advance(Pga3dPoint(-1e-3, 0.0, 0.0))
    assert(copy.forceAt(Pga3dPoint(-1e-3, 0.0, 0.0)).x > 0 && friction.forceAt(b).y < 0)
  }

  /** the lag angle of a single element on a steady circle of the given radius, sampled with
   * stepsPerPeriod chords, after one period of transient; returns (angle, |force| / breakForce) */
  private def circleLag(element: IwanElement, radius: Double, stepsPerPeriod: Int): (Double, Double) =
    val friction = Pga3dIwanFriction(element)
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
    val cos = -(force antiDotI tangent(i)) / force.norm
    (Math.acos(cos), force.norm / element.breakForce)

  test("steady circle: a slipping element settles on its sphere lagging the motion by sin(theta) = breakDeformation / radius") {
    // the continuous model: dtheta/ds = -sin(theta) / breakDeformation + 1 / radius = 0 on the steady circle
    val element = macro_ // breakDeformation 2.5e-4
    val radius = 1e-3
    val expected = Math.asin(element.breakDeformation / radius) // 14.5 degrees

    val (coarse, coarseNorm) = circleLag(element, radius, stepsPerPeriod = 500)
    val (fine, fineNorm) = circleLag(element, radius, stepsPerPeriod = 5000)
    assert(Math.abs(coarseNorm - 1.0) <= 1e-9 && Math.abs(fineNorm - 1.0) <= 1e-9)
    assert(Math.abs(fine - expected) <= 1e-5, s"lag $fine vs $expected")

    // the chord-vs-arc error with a step travel (1.3e-5) well below breakDeformation is a midpoint-rule
    // error, second order in the step: 10x finer chords, ~100x smaller error
    val ratio = Math.abs(coarse - expected) / Math.abs(fine - expected)
    assert(ratio > 50.0 && ratio < 200.0, s"coarse error ${coarse - expected}, fine error ${fine - expected}, ratio $ratio")
  }

  test("steady circle with a step travel far beyond breakDeformation: the force snaps to the chord, first order") {
    val element = micro // breakDeformation 3.75e-5
    val radius = 1e-3
    val expected = Math.asin(element.breakDeformation / radius) // the continuous lag, 0.0375
    // 8 and 16 chords per period: travel 7.9e-4 and 3.9e-4 per step, 21 and 10 breakDeformations,
    // so the force relaxes fully onto the chord and lags the end tangent by half the step angle,
    // not by the continuous lag: an error proportional to the step
    val (coarse, _) = circleLag(element, radius, stepsPerPeriod = 8)
    val (fine, _) = circleLag(element, radius, stepsPerPeriod = 16)
    assert(Math.abs(coarse - Math.PI / 8) <= 1e-3, s"coarse lag $coarse vs continuous $expected")
    assert(Math.abs(fine - Math.PI / 16) <= 1e-3, s"fine lag $fine vs continuous $expected")
  }

  // ---- the integrator contract with the real RK4 solver: stage-consistent forceAt, advance per step ----

  /** a unit sphere body of mass 1 at start with the given velocity, driven by the constant external
   * force plus a central spring to the origin plus the friction; returns the final center position */
  private def simulateRK4(friction: Pga3dIwanFriction, start: Pga3dPoint, velocity: Pga3dVector,
                          externalForce: Pga3dVector, springK: Double, dt: Double, steps: Int): Pga3dPoint =
    val body = new Pga3dPhysicsBody(Pga3dInertia.sphere(mass = 1.0, r = 0.1),
      Pga3dTranslator.addVector(start - origin).toMotor, Pga3dVelocity.linear(velocity).toBivector)
    val bodies = Array(body)
    for (_ <- 1 to steps) {
      Pga3dPhysicsSolverRK4.step(bodies, dt, addForquesToBodies = { _ =>
        val p = body.globalCenterOfMass
        val force = externalForce + (origin - p) * springK + friction.forceAt(p)
        body.addGlobalForque(Pga3dForque.force(p, force))
      })
      friction.advance(body.globalCenterOfMass)
    }
    body.globalCenterOfMass

  private def order(errors: Seq[Double]): Seq[Double] =
    errors.sliding(2).map { case Seq(coarse, fine) => Math.log(coarse / fine) / Math.log(2.0) }.toSeq

  test("RK4 keeps the 4th order on a straight monotone path with slipping and stuck elements") {
    // committed state: moved 1e-4 along x, so the micro element slips (150) and the macro one is stuck at 100;
    // a constant push of 300 then oscillates the body around d = 5e-5 with omega = sqrt(1e6) = 1000,
    // x(t) = 1e-4 + d * (1 - cos(omega t)), monotone until t = pi / omega = 3.14e-3 and below the macro knee
    val u = Pga3dVector(1.0, 0.0, 0.0)
    val start = origin + u * 1e-4
    val t = 2.5e-3
    val exact = 1e-4 + 5e-5 * (1 - Math.cos(1000.0 * t))

    val errors = for (dt <- Seq(1e-4, 5e-5, 2.5e-5)) yield {
      val friction = makeFriction()
      friction.advance(start)
      val steps = Math.round(t / dt).toInt
      val end = simulateRK4(friction, start, Pga3dVector.zero, u * 300.0, springK = 0.0, dt, steps)
      assert(Math.abs(end.y) < 1e-15 && Math.abs(end.z) < 1e-15)
      Math.abs(end.x - exact)
    }
    val orders = order(errors)
    assert(orders.forall(o => o > 3.7 && o < 4.3), s"errors $errors, orders $orders")
  }

  test("RK4 loses its order on a reversal inside the interval: about 2nd order overall") {
    // the same oscillator, now past the reversal at t = 3.14e-3, where the unloading branch is 5x stiffer;
    // the error depends on where inside a step the reversal falls, so only the trend over 8x is checked
    val u = Pga3dVector(1.0, 0.0, 0.0)
    val start = origin + u * 1e-4
    val t = 4e-3
    val dts = Seq(1e-4, 5e-5, 2.5e-5, 1.25e-5)
    def run(dt: Double): Double =
      val friction = makeFriction()
      friction.advance(start)
      simulateRK4(friction, start, Pga3dVector.zero, u * 300.0, springK = 0.0, dt, Math.round(t / dt).toInt).x
    val reference = run(dts.last / 64)
    val errors = dts.map(dt => Math.abs(run(dt) - reference))
    val overall = Math.log(errors.head / errors.last) / Math.log(8.0)
    assert(overall > 1.5 && overall < 3.5, s"errors $errors, overall order $overall")
  }

  test("RK4 on a curved path while slipping: between 1st and 2nd order, the chord replaces the arc") {
    // all elements pre-saturated along the initial tangent; the central spring bends the path.
    // The step travel (4e-4 .. 5e-5) runs from above to about the break deformations (2.5e-4, 3.75e-5),
    // so the measured order climbs from the snapped-to-chord 1st order towards the midpoint-rule 2nd
    val radius = 2e-3
    val start = Pga3dPoint(radius, 0.0, 0.0)
    val tangent = Pga3dVector(0.0, 1.0, 0.0)
    val omega = 1000.0
    val t = 2e-3 // about 2 radians of turning
    val dts = Seq(2e-4, 1e-4, 5e-5, 2.5e-5)
    def run(dt: Double): Pga3dPoint =
      val friction = Pga3dIwanFriction(micro, macro_)
      friction.advance(start - tangent * 1e-2)
      friction.advance(start)
      simulateRK4(friction, start, tangent * (omega * radius), Pga3dVector.zero, springK = omega * omega, dt, Math.round(t / dt).toInt)
    val reference = run(dts.last / 64)
    val errors = dts.map(dt => (run(dt) - reference).norm)
    val orders = order(errors)
    assert(orders.forall(o => o > 1.0 && o < 2.5), s"errors $errors, orders $orders")
    assert(orders == orders.sorted, s"the order should climb with the refinement: $orders")
  }
