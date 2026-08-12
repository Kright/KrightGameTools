package me.kright.gametools.benchmark

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
 * which stores its pose as a Pga3dTransform - that alone made every solver 1.2-1.3x faster
 * than the motor-based inertia; the pair-grouped sums of the generated code shaved a further
 * 0-5% off; the solvers write the motor back as body.transform = Pga3dTransform(motor), so the
 * write-back normalizes once instead of renormalized + another renormalization in the motor
 * setter - that took ~10% off rkf45, the rest moved within run-to-run noise):
 *   euler          711 /  89
 *   midPoint      1393 / 174
 *   heun          1397 / 175
 *   rk4           2850 / 356
 *   verlet        4717 / 590   (one derivative pass, but the displacement solve dominates the
 *                               step - per fixed-point iteration: bivector exp + motor product +
 *                               reverseSandwich + inertia.invert)
 *   rkmk4         5249 / 656
 *   rkf45         6308 / 789
 *   gaussLegendre3 7284 / 911
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

  var eulerBodies: Array[Pga3dPhysicsBody] = uninitialized
  var heunBodies: Array[Pga3dPhysicsBody] = uninitialized
  var midPointBodies: Array[Pga3dPhysicsBody] = uninitialized
  var rk4Bodies: Array[Pga3dPhysicsBody] = uninitialized
  var rkmk4Bodies: Array[Pga3dPhysicsBody] = uninitialized
  var rkf45Bodies: Array[Pga3dPhysicsBody] = uninitialized
  var verletBodies: Array[Pga3dPhysicsBody] = uninitialized
  var gaussLegendreBodies: Array[Pga3dPhysicsBody] = uninitialized

  var rkf45Solver: Pga3dPhysicsSolverRKF45 = uninitialized
  var gaussLegendreSolver: Pga3dPhysicsSolverGaussLegendre = uninitialized

  // the stateless Verlet works on caller-owned arrays instead of bodies
  var verletInertias: Array[Pga3dInertia] = uninitialized
  var verletPrev: Array[Pga3dMotor] = uninitialized
  var verletCurrent: Array[Pga3dMotor] = uninitialized
  var verletNext: Array[Pga3dMotor] = uninitialized
  var verletLocalBs: Array[Pga3dBivector] = uninitialized
  var verletForques: Array[Pga3dBivector] = uninitialized

  @Setup
  def setup(): Unit =
    eulerBodies = makeBodies()
    heunBodies = makeBodies()
    midPointBodies = makeBodies()
    rk4Bodies = makeBodies()
    rkmk4Bodies = makeBodies()
    rkf45Bodies = makeBodies()
    verletBodies = makeBodies()
    gaussLegendreBodies = makeBodies()

    rkf45Solver = Pga3dPhysicsSolverRKF45()
    gaussLegendreSolver = Pga3dPhysicsSolverGaussLegendre(iterations = 3)

    verletInertias = verletBodies.map(_.inertia)
    verletCurrent = verletBodies.map(_.motor)
    verletLocalBs = verletBodies.map(_.localB)
    verletForques = Array.fill(bodiesCount)(Pga3dBivector.zero)
    verletPrev = Pga3dPhysicsSolverVerlet.makePrevMotors(
      verletInertias, verletCurrent, verletLocalBs, verletForques, dt)
    verletNext = new Array[Pga3dMotor](bodiesCount)

  @Benchmark
  def euler(): Unit =
    Pga3dPhysicsSolverEuler.step(eulerBodies, dt, _ => ())

  @Benchmark
  def heun(): Unit =
    Pga3dPhysicsSolverHeun.step(heunBodies, dt, _ => ())

  @Benchmark
  def midPoint(): Unit =
    Pga3dPhysicsSolverMidPoint.step(midPointBodies, dt, _ => ())

  @Benchmark
  def rk4(): Unit =
    Pga3dPhysicsSolverRK4.step(rk4Bodies, dt, _ => ())

  @Benchmark
  def rkmk4(): Unit =
    Pga3dPhysicsSolverRKMK4.step(rkmk4Bodies, dt, _ => ())

  @Benchmark
  def rkf45(): Unit =
    rkf45Solver.step(rkf45Bodies, dt, _ => ())

  @Benchmark
  def verlet(): Unit =
    Pga3dPhysicsSolverVerlet.step(
      verletInertias, verletPrev, verletCurrent, verletForques, dt, dt, verletNext, verletLocalBs)
    val tmp = verletPrev
    verletPrev = verletCurrent
    verletCurrent = verletNext
    verletNext = tmp

  @Benchmark
  def gaussLegendre3(): Unit =
    gaussLegendreSolver.step(gaussLegendreBodies, dt, _ => ())
