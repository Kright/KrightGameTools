# Kright Game Tools

## Introduction

KrightGameTools consists of several independent modules in Scala and C++ for 3d applications: math and physics
simulation. Most Scala modules support both JVM and Scala.js.

It contains some basic classes like vectors, quaternion, matrices, and rigid body physics built on top of them.
All of this is implemented from scratch and doesn't depend on other libraries.
So I hope the library could be used with in graalvm or from Kotlin or Java.

All the code is under MIT license. Contributions are welcome, feel free to send a pull request.

## Table of Contents

- [Getting started](#Getting-started)
    - [Sbt](#sbt)
    - [Gradle](#Gradle)
- [Library modules](#Library-modules)
    - [Simple modules](#simple-modules)
    - [Advanced modules with geometric algebra](#advanced-modules-with-geometric-algebra)
    - [C++](#c-code)
- [Tests](#tests)
- [How to change this library and try changes locally in other project](#how-to-change-this-library-and-try-changes-locally-in-other-project)

## Examples:

1. [examples/precession](examples/precession/README.md): Scala + sbt, simulate body precession
2. [examples/nbody](examples/nbody/README.md): Gradle + Kotlin + Java on JVM and GraalVM, simulation of N-bodies in
   space.
3. [examples/indigodemo](examples/indigodemo/README.md): Indigo game engine with scala js.
4. [cpp](cpp/main.cpp): C++ code for 3d physics simulation.

### Getting started

You may add the whole library or specific modules.

### sbt

```scala
libraryDependencies += "me.kright" %% "gametools-pga3d" % "0.9.0"
```

For Scala.js use `%%%` instead of `%%`:

```scala
libraryDependencies += "me.kright" %%% "gametools-pga3d" % "0.9.0"
```

Or for separate modules

```scala
libraryDependencies ++= Seq(
  "me.kright" %% "gametools-pga3d" % "0.9.0",
  "me.kright" %% "gametools-pga3dphysics" % "0.9.0",
  "me.kright" %% "gametools-mathutil" % "0.9.0",
)
```

### Gradle:

```groovy
dependencies {
  implementation 'me.kright:gametools-pga3d_3:0.9.0'
  implementation 'me.kright:gametools-mathutil_3:0.9.0'
  implementation 'me.kright:gametools-matrix_3:0.9.0'
  implementation 'me.kright:gametools-pga3dphysics_3:0.9.0'
}
```

## Library modules

Initially, it was a repo for simple math with matrices and vectors. I implemented physics for 3d on top of that math.
During my development, I figured out that plane-based geometric algebra is a fantastic way to describe physics
equations. So there is a code with geometric algebra, which includes math and physics.
too.

### Modules

I'm inspired by https://bivector.net/PGADYN.html

I rewrote physics equations in PGA, it looks like PGA is a better way of describing physics.

* [**symbolic**](symbolic/README.md): simple implementation for AST like `(1.0 + ("y" * "x"))`
* [**ga**](ga/README.md): experimental support for geometric algebra (GA) and plane-based geometric algebra (PGA).
  See [https://bivector.net](https://bivector.net) for more details. Suitable for any dimensions
* [**vector**](vector/README.md): Vector2d, Vector3d, Vector4d
* [**pga3d**](pga3d/README.md): efficient library for 3d PGA with generated code and some common cases—Pga3dPlane,
  Pga3dPoint,
  Pga3dQuaternion, Pga3dBivector, etc.
  There is a huge number of similar methods (for each pair of classes for each type of multiplication). Because of
  generated methods for each case it's possible to know at compile time that, for example, dot product of two bivectors
  is a scalar or geometric product of two planes is a motor.
* [**pga3dCodeGen**](pga3dCodeGen/README.md): hand-made code generator for pga3d module. It does operations in symbolic
  form, and searches the
  most narrow subclass of multivector for the result. Generates both Scala and C++ code.
* [**pga3dPhysics**](pga3dphysics/README.md): some helper classes for implementing physics engine - body inertia,
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
multiplication associativity, zero and id elements, morphisms between quaternions and corresponding matrices or euler
angles.
For physics, it's ok to check that total energy and impulse are constant in body systems without friction.

# How to change this library and try changes locally in other project

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
