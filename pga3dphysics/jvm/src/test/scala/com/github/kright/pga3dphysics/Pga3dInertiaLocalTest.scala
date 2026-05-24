package com.github.kright.pga3dphysics

import com.github.kright.pga3d.*
import com.github.kright.mathutil.{EqualityEps, FastRange}
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dInertiaLocalTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val eps = 1e-12

  private given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)

  private given equalityEps: EqualityEps = EqualityEps(eps)

  test("getAcceleration is equal to apply invert") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors, Pga3dInertiaGenerators.inertiaLocal(0.1, 10.0, 0.1, 10.0), MinSuccessful(1000)) { (forque, b, inertia) =>

      val a0 = inertia.getAcceleration(b, forque)
      val a1 = inertia.invert(b.cross(inertia(b)) + forque)

      assert((a0 - a1).norm < 1e-12, s"a0 = $a0, a1 = $a1")
    }
  }

  test("performance compare with C++") {
    assume(false, "skip test")
    for (i <- FastRange(100)) {
      val system = Pga3dPhysicsSystemForTest(
        state = Seq(0.0, 1e-2, 1e-1, 1.0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6).map { d =>
          Pga3dPhysicsSystemForTest.simpleBody(Pga3dTranslator.addVector(Pga3dVector(d, d, d)).toMotor)
        }.toArray,
        solver = Pga3dPhysicsSolverRK4
      )

      val stepsCount = 1000
      val dt = 0.01

      val start = System.nanoTime()
      for (step <- FastRange(stepsCount)) {
        system.doStep(dt, _ => ())
      }
      val end = System.nanoTime()

//      println(s"${end - start} ns for ${stepsCount} steps, error = ${system.getError()}")
    }
  }

  test("calculate free rotation body precession for RK4 for different centers") {
    // Up to 1'000 everything is ok, but for higher distance precision is decreasing

    val expectedMaxError = ErrorOfEnergyAndMomentum(6.0E-11,2.0E-9)

    val centersAndErrors = Seq[Pga3dPoint](
      Pga3dPoint(0, 0, 0),
      Pga3dPoint(1, 1, 1),
      Pga3dPoint(1e1, 1e1, 1e1),
      Pga3dPoint(1e2, 1e2, 1e2),
      Pga3dPoint(1e3, 1e3, 1e3),
      Pga3dPoint(1e4, 1e4, 1e4),
      Pga3dPoint(1e5, 1e5, 1e5),
      Pga3dPoint(1e6, 1e6, 1e6),
    )

    for (center <- centersAndErrors) {
      val stepsCount = 1000
      val dt = 0.01
      val system = Pga3dPhysicsSystemForTest(Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dTranslator.addVector(center.toVectorUnsafe).toMotor)), Pga3dPhysicsSolverRK4)

      assert(system.initialE == 3.0)
      assert(system.initialL == Pga3dBivector(wx = 3.0, wy = 2.0, wz = 1.0))

      val errors = for (step <- 0 until stepsCount)
        yield {
          system.doStep(dt, _ => ())
          system.getError()
        }

      val maxError = errors.reduce(_ max _)
      println(s"center = $center, maxError = $maxError")

      assert(maxError < expectedMaxError, s"center = $center, maxError = $maxError, but expected $expectedMaxError")
    }
  }

  test("calculate free rotation body precession for RK4 for different orientation") {
    val stepsCount = 1000
    val dt = 0.01
    val forque = Pga3dBivector()

    val motors = Seq(
      Pga3dMotor.id,
      Pga3dTranslator.addVector(Pga3dVector(1, 0, 0)).toMotor,
      Pga3dTranslator.addVector(Pga3dVector(0, 1, 0)).toMotor,
      Pga3dTranslator.addVector(Pga3dVector(0, 0, 1)).toMotor,
      Pga3dTranslator.addVector(Pga3dVector(1, 2, 3)).toMotor,
      Pga3dQuaternion.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 0)).toMotor,
      Pga3dQuaternion.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 1)).toMotor,
      Pga3dQuaternion.rotation(Pga3dVector(1, 0, 0), Pga3dVector(0, 1, 1)).geometric(Pga3dTranslator.addVector(Pga3dVector(1, 2, 3)).toMotor),
    )

    for (motor <- motors) {
      val body = Pga3dPhysicsSystemForTest.simpleBody(motor)
      val system = Pga3dPhysicsSystemForTest(Array(body), Pga3dPhysicsSolverRK4)

      val maxError = (for (_ <- 0 until stepsCount) yield {
        system.doStep(dt, _ => ())
        system.getError()
      }).reduce(_ max _)

      assert(maxError < ErrorOfEnergyAndMomentum(errorE = 6e-11, errorL = 1e-9))
    }
  }

  test("getAcceleration test") {
    val localB = Pga3dBivector(0.1, 0.2, 0.3, 0.4, 0.5, 0.6)
    val localForque = Pga3dBivector(0.7, 0.8, 0.9, 1.0, 1.1, 1.2)
    val body = Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)
    val system = Pga3dPhysicsSystemForTest(Array(body), Pga3dPhysicsSolverRK4)
    val acc = body.inertia.getAcceleration(localB, localForque)
    val accNaive = body.inertia.invert(localB.cross(body.inertia(localB)) + localForque)
    val err = (acc - accNaive).norm
    assert(err < 2e-16)
  }

  test("calculate performance") {
    assume(false, "skip test")
    val stepsCount = 10_000_000

    val body = Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)
    val system = Pga3dPhysicsSystemForTest(Array(body), Pga3dPhysicsSolverRK4)

    assert(system.initialE == 3.0)
    assert(system.initialL == Pga3dBivector(wx = 3.0, wy = 2.0, wz = 1.0))

    val start = System.nanoTime()
    for (_ <- 0 until stepsCount) {
      system.doStep(dt = 0.001, _ => ())
    }
    val end = System.nanoTime()
    println(s"dt = ${(end - start) / stepsCount}ns")
    println(system.state.head)
  }

  test("calculate energy accumulation for linear force during rotation") {
    val dt = 0.01
    val stepsCount = 100

    val forceDirection = Pga3dVector(1, 0, 0)
    val body = Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)
    val system = Pga3dPhysicsSystemForTest(Array(body), Pga3dPhysicsSolverRK4)

    assert(system.initialE == 3.0)
    assert(system.initialL == Pga3dBivector(wx = 3.0, wy = 2.0, wz = 1.0))

    var accumulatedL = system.initialL

    val errors = for (step <- 1 to stepsCount) yield {
      val t = step * dt

      val bodyCenter = system.state.head.globalCenter
      val globalForque = Pga3dForque.force(bodyCenter, forceDirection)

      system.doStep(dt, _ => system.state.head.addGlobalForque(globalForque))

      val expectedEnergy = system.initialE + 0.5 * forceDirection.normSquare * t * t / body.inertia.mass
      accumulatedL += globalForque * dt

      val err = ErrorOfEnergyAndMomentum(errorE = math.abs(expectedEnergy - system.getKineticEnergy()), errorL = (accumulatedL - system.getL()).norm)
      err
    }

    val maxError = errors.reduce(_ max _)
    assert(maxError < ErrorOfEnergyAndMomentum(2e-9, 4e-9))

    val endPoint = system.state.head.globalCenter
    val expectedPoint = Pga3dPlane(w = 1.0, x = 0.5).dual

    assert((endPoint - expectedPoint).norm < 2e-9)
  }

  test("calculate energy accumulation for linear spring") {
    val dt = 0.01
    val stepsCount = 1000
    val mass = 10.0

    val body = Pga3dPhysicsBody.motionless(
      Pga3dInertiaLocal(mass, 1.0, 1.0, 1.0),
      Pga3dMotor.id,
    )

    val system = Pga3dPhysicsSystem(Array(body), Pga3dPhysicsSolverRK4)

    val springCenter = Pga3dPoint(3.0, 4.0, 0.0) // len 5
    val springK = 20

    def getEnergy(): Double =
      (system.state.head.globalCenter - springCenter).normSquare * 0.5 * springK + system.getKineticEnergy()

    val initialEnergy = getEnergy()

    for (_ <- 1 to stepsCount) {
      system.doStep(dt, _ => {
        val globalForque = (system.state.head.globalCenter v springCenter) * springK
        system.state.head.addGlobalForque(globalForque)
      })

      val expectedPos = springCenter - (springCenter.weight * Math.cos(system.time * Math.sqrt(springK / mass))).toProjectivePoint

      val dE = math.abs(initialEnergy - getEnergy()) / initialEnergy
      val dPos = (expectedPos - system.state.head.globalCenter).norm

      assert(dE <= 1.2e-10)
      assert(dPos <= 2.4e-8)
    }
  }

  /**
   *
   * mass = 1.0, k=1000000.0, freq = 159, dt = 1.0E-4
   * Pga3dPhysicsSolverRK4: dE = 1.3870565822646022E-4, dPos = 0.49975484053489333
   * Pga3dPhysicsSolverGaussLegendre(iterations=1): dE = 0.07970029940243602, dPos = 0.5447876372350485
   * Pga3dPhysicsSolverGaussLegendre(iterations=2): dE = 1.3870565821245314E-4, dPos = 0.49975484053489305
   * Pga3dPhysicsSolverGaussLegendre(iterations=3): dE = 5.7822137475013735E-8, dPos = 0.49978223112059206
   * Pga3dPhysicsSolverGaussLegendre(iterations=4): dE = 5.782212808728218E-8, dPos = 0.4997822311205916
   * Pga3dPhysicsSolverGaussLegendre(iterations=5): dE = 5.7725794315338135E-8, dPos = 0.49978225394608755
   * Pga3dPhysicsSolverGaussLegendre(iterations=6): dE = 1.4452233910560607E-10, dPos = 0.4997822540077615
   * Pga3dPhysicsSolverGaussLegendre(iterations=7): dE = 4.8086047172546384E-11, dPos = 0.4997822539697158
   * Pga3dPhysicsSolverGaussLegendre(iterations=8): dE = 8.717179298400879E-14, dPos = 0.4997822539696614
   * Pga3dPhysicsSolverGaussLegendre(iterations=9): dE = 6.705522537231445E-15, dPos = 0.49978225396968057
   * Pga3dPhysicsSolverGaussLegendre(iterations=10): dE = 8.940696716308593E-15, dPos = 0.49978225396968384
   *
   *
   * mass = 1.0, k=1000000.0, freq = 159, dt = 2.0E-4
   * Pga3dPhysicsSolverRK4: dE = 0.00441246054211855, dPos = 0.9979386803683491
   * Pga3dPhysicsSolverGaussLegendre(iterations=1): dE = 0.48202124378590694, dPos = 1.7622203978621156
   * Pga3dPhysicsSolverGaussLegendre(iterations=2): dE = 0.004412460542118996, dPos = 0.9979386803683494
   * Pga3dPhysicsSolverGaussLegendre(iterations=3): dE = 7.382688797712326E-6, dPos = 0.9982036622268402
   * Pga3dPhysicsSolverGaussLegendre(iterations=4): dE = 7.382688812464476E-6, dPos = 0.9982036622268418
   * Pga3dPhysicsSolverGaussLegendre(iterations=5): dE = 7.333525105416775E-6, dPos = 0.9982045455067081
   * Pga3dPhysicsSolverGaussLegendre(iterations=6): dE = 7.374513953924179E-8, dPos = 0.998204555476723
   * Pga3dPhysicsSolverGaussLegendre(iterations=7): dE = 2.436296746134758E-8, dPos = 0.9982045495881882
   * Pga3dPhysicsSolverGaussLegendre(iterations=8): dE = 1.6379207372665406E-10, dPos = 0.9982045495549573
   * Pga3dPhysicsSolverGaussLegendre(iterations=9): dE = 2.8476119041442874E-13, dPos = 0.9982045495647688
   * Pga3dPhysicsSolverGaussLegendre(iterations=10): dE = 2.82973051071167E-13, dPos = 0.9982045495647699
   *
   *
   * mass = 1.0, k=1000000.0, freq = 159, dt = 4.0E-4
   * Pga3dPhysicsSolverRK4: dE = 0.13010404214322804, dPos = 1.9754320977289879
   * Pga3dPhysicsSolverGaussLegendre(iterations=1): dE = 0.9936162145014202, dPos = 4.878961178067868
   * Pga3dPhysicsSolverGaussLegendre(iterations=2): dE = 0.13010404214322835, dPos = 1.975432097728989
   * Pga3dPhysicsSolverGaussLegendre(iterations=3): dE = 9.350688982291519E-4, dPos = 1.9846108352545107
   * Pga3dPhysicsSolverGaussLegendre(iterations=4): dE = 9.350688982385397E-4, dPos = 1.9846108352545118
   * Pga3dPhysicsSolverGaussLegendre(iterations=5): dE = 9.109761101138592E-4, dPos = 1.9847410352875607
   * Pga3dPhysicsSolverGaussLegendre(iterations=6): dE = 3.7254718262255194E-5, dPos = 1.9847462043134818
   * Pga3dPhysicsSolverGaussLegendre(iterations=7): dE = 1.197443572178483E-5, dPos = 1.9847427323028435
   * Pga3dPhysicsSolverGaussLegendre(iterations=8): dE = 3.3040764674544335E-7, dPos = 1.9847426633828016
   * Pga3dPhysicsSolverGaussLegendre(iterations=9): dE = 2.2175052762031556E-9, dPos = 1.9847426865295354
   * Pga3dPhysicsSolverGaussLegendre(iterations=10): dE = 2.2175079584121703E-9, dPos = 1.984742686529534
   *
   *
   * mass = 1.0, k=1.0E7, freq = 503, dt = 1.0E-4
   * Pga3dPhysicsSolverRK4: dE = 0.12816380137387334, dPos = 1.5658543883969738
   * Pga3dPhysicsSolverGaussLegendre(iterations=1): dE = 0.9996836944552729, dPos = 4.979924238609978
   * Pga3dPhysicsSolverGaussLegendre(iterations=2): dE = 0.12816380137389755, dPos = 1.565854388396974
   * Pga3dPhysicsSolverGaussLegendre(iterations=3): dE = 5.737165510194301E-4, dPos = 1.572866073566761
   * Pga3dPhysicsSolverGaussLegendre(iterations=4): dE = 5.737165509964228E-4, dPos = 1.5728660735667626
   * Pga3dPhysicsSolverGaussLegendre(iterations=5): dE = 5.644760622814894E-4, dPos = 1.5729338072491357
   * Pga3dPhysicsSolverGaussLegendre(iterations=6): dE = 1.4307287155389785E-5, dPos = 1.5729355511030128
   * Pga3dPhysicsSolverGaussLegendre(iterations=7): dE = 4.662776381731034E-6, dPos = 1.5729344222069894
   * Pga3dPhysicsSolverGaussLegendre(iterations=8): dE = 7.937367510795593E-8, dPos = 1.5729344076748943
   * Pga3dPhysicsSolverGaussLegendre(iterations=9): dE = 3.321239948272705E-10, dPos = 1.5729344123786266
   * Pga3dPhysicsSolverGaussLegendre(iterations=10): dE = 3.3212459087371827E-10, dPos = 1.5729344123786266
   *
   *
   * mass = 1.0, k=1.0E7, freq = 503, dt = 2.0E-4
   * Pga3dPhysicsSolverRK4: dE = 0.9853601196384955, dPos = 5.668760675145409
   * Pga3dPhysicsSolverGaussLegendre(iterations=1): dE = 1.0, dPos = 5.1864494892298785
   * Pga3dPhysicsSolverGaussLegendre(iterations=2): dE = 0.9853601196384963, dPos = 5.668760675145405
   * Pga3dPhysicsSolverGaussLegendre(iterations=3): dE = 0.06910189172049236, dPos = 3.107374769717493
   * Pga3dPhysicsSolverGaussLegendre(iterations=4): dE = 0.06910189172050082, dPos = 3.107374769717494
   * Pga3dPhysicsSolverGaussLegendre(iterations=5): dE = 0.0691175328411194, dPos = 3.107709791994586
   * Pga3dPhysicsSolverGaussLegendre(iterations=6): dE = 0.007106333114624262, dPos = 3.1077432812034886
   * Pga3dPhysicsSolverGaussLegendre(iterations=7): dE = 0.002145922075793147, dPos = 3.1077209461770057
   * Pga3dPhysicsSolverGaussLegendre(iterations=8): dE = 1.5646360285377503E-4, dPos = 3.1077198298845987
   * Pga3dPhysicsSolverGaussLegendre(iterations=9): dE = 2.6521294847726823E-6, dPos = 3.1077202021346713
   * Pga3dPhysicsSolverGaussLegendre(iterations=10): dE = 2.652129468560219E-6, dPos = 3.107720202134671
   */
  test("mass on spring precistion") {
    assume(false, "skip test")

    val t = 1.0 // one second
    val dt = 0.0004 // step
    val stepsCount = Math.round(t / dt).toInt

    val mass = 1.0
    val k = 1e6 // stiff spring

    val freq = 1.0 / Math.PI / 2.0 * Math.sqrt(k / mass)
    val springCenter = Pga3dPoint(3.0, 4.0, 0.0) // len 5

    //    val solver = Pga3dPhysicsSolverRK4
    val solvers = Seq(
      Pga3dPhysicsSolverRK4,
      Pga3dPhysicsSolverGaussLegendre(iterations = 1),
      Pga3dPhysicsSolverGaussLegendre(iterations = 2),
      Pga3dPhysicsSolverGaussLegendre(iterations = 3),
      Pga3dPhysicsSolverGaussLegendre(iterations = 4),
      Pga3dPhysicsSolverGaussLegendre(iterations = 5),
      Pga3dPhysicsSolverGaussLegendre(iterations = 6),
      Pga3dPhysicsSolverGaussLegendre(iterations = 7),
      Pga3dPhysicsSolverGaussLegendre(iterations = 8),
      Pga3dPhysicsSolverGaussLegendre(iterations = 9),
      Pga3dPhysicsSolverGaussLegendre(iterations = 10),
    )

    println(s"mass = ${mass}, k=${k}, freq = ${Math.round(freq)}, dt = ${dt}")

    for (solver <- solvers) {
      val body = Pga3dPhysicsBody.motionless(Pga3dInertiaLocal(mass, 1.0, 1.0, 1.0), Pga3dMotor.id)
      val system = Pga3dPhysicsSystemForTest(Array(body), solver)

      def getEnergy(): Double =
        (system.state.head.globalCenter - springCenter).normSquare * 0.5 * k + system.getKineticEnergy()

      val initialEnergy = getEnergy()

      var maxDe = 0.0
      var maxDpos = 0.0

      for (step <- 0 until stepsCount) {
        val t = step * dt

        system.doStep(dt, _ => {
          val globalForque = (system.state.head.globalCenter v springCenter) * k
          system.state.head.addGlobalForque(globalForque)
        })

        val expectedPos = springCenter - (springCenter.weight * Math.cos(t * Math.sqrt(k / mass))).toProjectivePoint

        val dE = math.abs(initialEnergy - getEnergy()) / initialEnergy
        val dPos = (expectedPos - system.state.head.globalCenter).norm

        maxDe = Math.max(maxDe, dE)
        maxDpos = Math.max(maxDpos, dPos)

        if (step == stepsCount - 1) {
          println(s"${solver}: dE = ${maxDe}, dPos = ${maxDpos}")
        }
        // assert(dE <= 1.2e-10)
        // assert(dPos <= 2.4e-8)
      }
    }
  }

  test("calculate free rotation body precession for different integrators") {
    assume(false, "skip test")

    val stepsCount = 1000
    val dt = 0.01

    val solvers = Seq(
      Pga3dPhysicsSolverEuler,
      Pga3dPhysicsSolverHeun,
      Pga3dPhysicsSolverMidPoint,
      Pga3dPhysicsSolverRK4,

      Pga3dPhysicsSolverGaussLegendre(iterations = 0),
      Pga3dPhysicsSolverGaussLegendre(iterations = 1),
      Pga3dPhysicsSolverGaussLegendre(iterations = 2),
      Pga3dPhysicsSolverGaussLegendre(iterations = 3),
      Pga3dPhysicsSolverGaussLegendre(iterations = 4),
      Pga3dPhysicsSolverGaussLegendre(iterations = 5),
      Pga3dPhysicsSolverGaussLegendre(iterations = 6),
    )

    for (solver <- solvers) {
      val system = Pga3dPhysicsSystemForTest(Array(Pga3dPhysicsSystemForTest.simpleBody(Pga3dMotor.id)), solver)

      val errors = for (_ <- 0 until stepsCount)
        yield {
          system.doStep(dt, _ => ())
          system.getError()
        }
      val maxError = errors.reduce(_ max _)
      println(s"solver = ${solver}: error = ${maxError}")
    }
  }