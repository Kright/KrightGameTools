# Kright Game Tools

## Introduction

KrightGameTools consists of several independent modules in Scala and C++ for 2d and 3d applications: math and physics
simulation. Most Scala modules support both JVM and Scala.js.

It contains some basic classes like vectors, quaternion, matrices, and rigid body physics built on top of them.
All of this is implemented from scratch and doesn't depend on other libraries.
So I hope the library could be used within GraalVM or from Kotlin or Java.

All the code is under MIT license. Contributions are welcome, feel free to send a pull request.

## Table of Contents

- [Examples](#examples)
- [Getting started](#getting-started)
    - [Sbt](#sbt)
    - [Gradle](#gradle)
- [Library modules](#library-modules)
    - [Modules](#modules)
    - [C++](#c++-code)
- [Tests](#tests)
- [How to change this library and try changes locally in another project](#how-to-change-this-library-and-try-changes-locally-in-another-project)

## Examples

1. [examples/precession](examples/precession/README.md): Scala + sbt, simulate body precession
2. [examples/nbody](examples/nbody/README.md): Gradle + Kotlin + Java on JVM and GraalVM, simulation of N-bodies in
   space.
3. [examples/indigodemo](examples/indigodemo/README.md): Indigo game engine with scala js.
4. [cpp](cpp/main.cpp): C++ code for 3d physics simulation.

## Getting started

### sbt

```scala
libraryDependencies ++= Seq(
  "me.kright" %% "gametools-mathutil" % "0.9.0",
  "me.kright" %% "gametools-vector" % "0.9.0",
  "me.kright" %% "gametools-matrix" % "0.9.0",
  "me.kright" %% "gametools-pga2d" % "0.9.0",
  "me.kright" %% "gametools-pga3d" % "0.9.0",
  "me.kright" %% "gametools-pga3dgeom" % "0.9.0",
  "me.kright" %% "gametools-pga3dphysics" % "0.9.0"
)
```

For Scala.js use `%%%` instead of `%%`:

```scala
libraryDependencies += "me.kright" %%% "gametools-pga3d" % "0.9.0"
```

### Gradle

Note: suffix `_3` is for Scala 3.

```groovy
dependencies {
  implementation 'me.kright:gametools-mathutil_3:0.9.0'
  implementation 'me.kright:gametools-vector_3:0.9.0'
  implementation 'me.kright:gametools-matrix_3:0.9.0'
  implementation 'me.kright:gametools-pga2d_3:0.9.0'
  implementation 'me.kright:gametools-pga3d_3:0.9.0'
  implementation 'me.kright:gametools-pga3dgeom_3:0.9.0'
  implementation 'me.kright:gametools-pga3dphysics_3:0.9.0'
}
```

## Library modules

Initially, it was a repo for simple math with matrices and vectors. I implemented physics for 3d on top of that math.
During my development, I figured out that plane-based geometric algebra is a fantastic way to describe physics
equations. So there is a code with geometric algebra, which includes math and physics too.

### Modules

I'm inspired by https://bivector.net/PGADYN.html

I rewrote physics equations in PGA, it looks like PGA is a better way of describing physics.

The concepts shared by the pga2d and pga3d modules (the products, bulk and weight, duality, exp/log,
the narrowest-result-type rule) are described in [pga-concepts.md](pga-concepts.md).

* [**mathutil**](mathutil/README.md): small utilities shared by the other modules - fast math operations, fast ranges
  for loop iterations, precision-related helpers, `FlatDoubleSerializer` typeclass for reading and writing objects as
  flat arrays of doubles.
* [**symbolic**](symbolic/README.md): simple implementation for AST like `(1.0 + ("y" * "x"))` with simplification
  rules. Used by the code generators to derive formulas symbolically.
* [**ga**](ga/README.md): experimental support for geometric algebra (GA) and plane-based geometric algebra (PGA).
  See [https://bivector.net](https://bivector.net) for more details. Suitable for any dimensions and signatures,
  represents a multivector as a map from basis blade to coefficient. Flexible, but slower than the generated
  pga2d/pga3d classes; used as the source of truth for the code generators.
* [**vector**](vector/README.md): Vector2d, Vector3d, Vector4d - simple immutable vector classes with the usual
  operations.
* [**matrix**](matrix/README.md): implementation of matrices and basic linear algebra - matrix operations,
  transformation matrices for 3d graphics and physics.
* [**pga2d**](pga2d/README.md): efficient library for 2d PGA with generated code and some common cases—Pga2dLine,
  Pga2dPoint, Pga2dRotor, Pga2dMotor, etc. The 2d counterpart of pga3d with the same style and terminology, except
  that a grade-1 element is called a line instead of a plane, and the analog of a quaternion is Pga2dRotor with just
  2 fields (cos and sin of the half-angle).
* [**pga3d**](pga3d/README.md): efficient library for 3d PGA with generated code and some common cases—Pga3dPlane,
  Pga3dPoint,
  Pga3dRotor, Pga3dBivector, etc.
  There is a huge number of similar methods (for each pair of classes for each type of multiplication). Because of
  generated methods for each case it's possible to know at compile time that, for example, dot product of two bivectors
  is a scalar or geometric product of two planes is a motor.
* [**pgaNdCodeGen**](pgaNdCodeGen/README.md): hand-made code generator for the pga2d and pga3d modules. It does
  operations in symbolic form, and searches the most narrow subclass of multivector for the result. Generates Scala
  code for both, plus C++ code for 3d.
* [**pga3dphysics**](pga3dphysics/README.md): some helper classes for implementing physics engine - body inertia,
  physics solvers, etc. Under active development now.
* [**pga3dgeom**](pga3dgeom/README.md): classes for geometry - edges, triangles, axis-aligned bounding boxes

### C++ code

* [**cpp**](cpp/README.md): same code as in pga3d and pga3dphysics in Scala, but for C++.

The current implementation is experimental and may be changed in the future.
I use C++ 20 because of Unreal Engine 5 requirements.

## Tests

```bash
sbt test
```

I use scalaCheck and property-based approach. It goes well with checking math properties such as addition or
multiplication associativity, zero and id elements, morphisms between quaternions and corresponding matrices.
For physics, it's ok to check that total energy and impulse are constant in body systems without friction.

## How to change this library and try changes locally in another project

Change lib code, publish to local ivy repo:

```bash
sbt publishLocal
```

Or to the local maven:

```bash
sbt publishM2
```

In my case "~/.ivy2/local/me.kright/gametools-pga3d_3/0.9.1-SNAPSHOT"
and "~/.m2/repository/me/kright/gametools-pga3d_3/0.9.1-SNAPSHOT"

After that, add the local library to another project. In my case, it was for sbt:

```scala
libraryDependencies += "me.kright" %% "gametools-pga3d" % "0.9.1-SNAPSHOT"
```

And for Gradle:

```groovy
implementation "me.kright:gametools-pga3d_3:0.9.1-SNAPSHOT"
```

Maybe you will need to remove cached lib, it will be placed in path like "~/.cache/coursier/v1/https/repo1.maven.org/maven2/me/kright/".
