package me.kright.gametools.benchmark

import me.kright.gametools.physics1d.*
import me.kright.gametools.pga3d.*
import me.kright.gametools.pga3d.physics.*
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

/**
 * cost of one solver step over an array of free rotating bodies, with an empty force callback:
 * this is the pure per-step solver overhead (stage bookkeeping, exp/log, renormalization),
 * without the user force evaluation. Solvers calling the force callback more often pay
 * proportionally more when the forces are not free - see the "force evaluations per step"
 * column in pga3dphysics/Solvers.md: euler 1, verlet 1, heun/midpoint 2, rk4/rkmk4 4,
 * rkf45 6, gaussLegendre(3) 9.
 *
 * The bodies rotate without translating, so the state stays bounded over any number of
 * benchmark invocations.
 *
 * Measured (8 bodies per step, ns per step / per body; the bodies use Pga3dInertiaMovedLocal,
 * which stores its pose as a Pga3dTransform; the universal solvers run over an
 * IntegrableUniformArray of the bodies with a reused OdeSolverCache):
 *   euler            638 /  80
 *   midPoint        1244 / 155
 *   heun            1261 / 158
 *   rk4             2500 / 312
 *   rk4InsideWorld  2585 / 323
 *   rkf45           4104 / 513
 *   verlet          4669 / 584   (one derivative pass, but the displacement solve dominates the
 *                                 step - per fixed-point iteration: bivector exp + motor product +
 *                                 reverseSandwich + inertia.invert)
 *   gaussLegendre3  5826 / 728
 *   rkmk4           6516 / 815
 *
 * Run with: sbt "benchmark/Jmh/run -f1 .*PhysicsSolverBenchmark.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class PhysicsSolverBenchmark:
  private val bodiesCount = 8
  private val dt = 0.001

  private def makeBodies(): Array[Pga3dPhysicsBody] =
    Array.tabulate(bodiesCount) { i =>
      val motor = Pga3dTranslator.addVector(Pga3dVector(i, 2.0 * i, -i)).toMotor
      Pga3dPhysicsBody(
        Pga3dInertia.moved(motor.reverse, Pga3dInertiaLocal(1.0, 3.0, 2.0, 1.0)),
        motor,
        motor.reverseSandwich(Pga3dBivector(xy = 1.0, yz = 1.0, xz = -1.0)),
      )
    }

  var bodies: IntegrableUniformArray[Pga3dPhysicsBody] = uninitialized
  var world: Integrable = uninitialized
  var cache: OdeSolverCache = uninitialized
  var verletBodies: Array[Pga3dPhysicsBody] = uninitialized

  var rkmk4Solver: Pga3dOdeSolverRKMK4[Pga3dPhysicsBody] = uninitialized
  var rkf45Solver: OdeSolverRKF45 = uninitialized
  var gaussLegendreSolver: OdeSolverGaussLegendre = uninitialized

  // the stateless Verlet works on caller-owned arrays instead of bodies
  var verletInertias: Array[Pga3dInertia] = uninitialized
  var verletPrev: Array[Pga3dMotor] = uninitialized
  var verletCurrent: Array[Pga3dMotor] = uninitialized
  var verletNext: Array[Pga3dMotor] = uninitialized
  var verletLocalBs: Array[Pga3dBivector] = uninitialized
  var verletForques: Array[Pga3dBivector] = uninitialized

  @Setup
  def setup(): Unit =
    bodies = IntegrableUniformArray(makeBodies(), Pga3dBodyState.componentsCount)
    world = Integrable(IntegrableUniformArray(makeBodies(), Pga3dBodyState.componentsCount), BenchmarkOscillatorA(1.0, 0.0))
    cache = OdeSolverCache()
    verletBodies = makeBodies()

    rkmk4Solver = Pga3dOdeSolverRKMK4(bodies.elements)
    rkf45Solver = OdeSolverRKF45()
    gaussLegendreSolver = OdeSolverGaussLegendre(iterations = 3)

    verletInertias = verletBodies.map(_.inertia)
    verletCurrent = verletBodies.map(_.motor)
    verletLocalBs = verletBodies.map(_.localB)
    verletForques = Array.fill(bodiesCount)(Pga3dBivector.zero)
    verletPrev = Pga3dVerletSolver.makePrevMotors(
      verletInertias, verletCurrent, verletLocalBs, verletForques, dt)
    verletNext = new Array[Pga3dMotor](bodiesCount)

  @Benchmark
  def euler(): Unit =
    OdeSolverEuler.step(bodies, dt, _ => (), cache)

  @Benchmark
  def heun(): Unit =
    OdeSolverHeun.step(bodies, dt, _ => (), cache)

  @Benchmark
  def midPoint(): Unit =
    OdeSolverMidPoint.step(bodies, dt, _ => (), cache)

  @Benchmark
  def rk4(): Unit =
    OdeSolverRK4.step(bodies, dt, _ => (), cache)

  @Benchmark
  def rk4InsideWorld(): Unit =
    OdeSolverRK4.step(world, dt, _ => (), cache)

  @Benchmark
  def rkmk4(): Unit =
    rkmk4Solver.step(bodies, dt, _ => (), cache)

  @Benchmark
  def rkf45(): Unit =
    rkf45Solver.step(bodies, dt, _ => (), cache)

  @Benchmark
  def verlet(): Unit =
    Pga3dVerletSolver.step(
      verletInertias, verletPrev, verletCurrent, verletForques, dt, dt, verletNext, verletLocalBs)
    val tmp = verletPrev
    verletPrev = verletCurrent
    verletCurrent = verletNext
    verletNext = tmp

  @Benchmark
  def gaussLegendre3(): Unit =
    gaussLegendreSolver.step(bodies, dt, _ => (), cache)
