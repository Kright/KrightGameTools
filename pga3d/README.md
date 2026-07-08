## Plane-based geometric algebra for 3d

Contains generated code for special cases: planes, rotors, points, etc.
For example, the point class contains only three fields and a rotor has only four. So rotation of a point by a rotor is
efficient as usual code in math libraries.

The ideas shared by the pga2d/pga3d modules — the products (geometric, dot, meet, join, sandwich),
bulk and weight, duality, exp/log, the narrowest-result-type rule and immutability — are described
once in [pga-concepts.md](../pga-concepts.md).

Full operation/result-type reference: [operations.md](operations.md).

### Plane:

* [**Pga3dPlane**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dPlane.scala): plane, 4 fields.
* [**Pga3dPlaneIdeal**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dPlaneIdeal.scala): plane passing through the center of coordinates, 3 fields. Dual to Pga3dVector

```scala
val plane = Pga3dPlane(a, b, c, d)  // ax + by + cz + d = 0
val idealPlane = Pga3dPlaneIdeal(a, b, c)  // ax + by + cz = 0
val point = Pga3dPoint(x, y, z)

// Projecting a point onto a plane
val projectedPoint = point.projectOntoPlane(plane)

// Mirroring by a plane
val mirroredPoint = plane.sandwich(point)
```

### Point:

* [**Pga3dPoint**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dPoint.scala): Point in space. Stored as dual representation with human-friendly fields x, y, z and fixed w=1.
* [**Pga3dPointCenter**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dPointCenter.scala): singleton object, center of coordinates.
* [**Pga3dVector**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dVector.scala): difference between points. Consist of x, y, z and fixed w=0. Pga3dTranslator moved points, but not
  vectors.
* [**Pga3dProjectivePoint**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dProjectivePoint.scala): general case with four homogeneous coordinates (x, y, z, w). Could be mapped to point via (x/w, y/w, z/w) when w != 0.

```scala
// Creating a point
val point1 = Pga3dPoint(x, y, z)
val point2 = Pga3dPoint(x2, y2, z2)

// difference between points is a vector
val vector = point1 - point2

// sum of vector and point is a point
val point3 = vector * 2 + point1
```

### Line

In addition, it represents velocity and force.

* [**Pga3dBivector**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dBivector.scala): 6 fields (xy, xz, yz, xw, yw, zw)
* [**Pga3dBivectorBulk**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dBivectorBulk.scala) - bivector with only 3 fields (xy, xz, yz).
* [**Pga3dBivectorWeight**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dBivectorWeight.scala) - bivector with only 3 fields (xw, yw, zw).

Sometimes it's easier to work with `wedge` and `antiWedge` as for `meet` and `join` operations.

```scala
// Creating a bivector
val bivector = Pga3dBivector(xy, xz, yz, xw, yw, zw)

// Getting bulk and weight components of a bivector
val bulkComponent = bivector.bulk
val weightComponent = bivector.weight

// creating line as intersection of two planes
val line0 = plane1 meet plane2
val line0_ = plane1 ^ plane2

// Creating a line from two points
val line1 = point1 join point2
val line1_ = point1 v point2

// Creating a line from a point and a vector
val line2 = point join vector
val line2_ = point v vector

// Splitting a bivector into a line and shift
val (line, shift) = bivector.split()
```

### Movement

* [**Pga3dRotor**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dRotor.scala): represents rotation, 4 fields (scalar, xy, xz, yz). It is the exponent of Pga3dBivectorBulk.
  `Pga3dQuaternion` remains available as a backward-compatible alias for `Pga3dRotor`.
* [**Pga3dTranslator**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dTranslator.scala): represents linear movement, 3 fields (wx, wy, wz). It is the exponent of Pga3dBivectorWeight
* [**Pga3dMotor**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dMotor.scala): combination of rotation and linear movement. Has 8 fields (scalar, all bivector fields and pseudoscalar),
  exponent of Pga3dBivector

To move everything with these classes, you need to call `motor.sandwich(obj)`

```scala
// Creating a rotor for rotation between two vectors
val from = Pga3dVector(1, 2, 3)
val to = Pga3dVector(3, 4, 5)
val rotor = Pga3dRotor.rotation(from, to)

// Making a rotor as a geometric product of two planes 
// The rotation axis is a meet line of planes, the rotation angle is double angle between planes)
val rotor2 = plane1 geometric plane2

// Applying rotation to anything using the sandwich product (could rotate point, line, plane, rotor, etc.)
val rotatedPoint = rotor.sandwich(point)

// Inverse of rotation
val inverseRotation = rotor.reverse

// Apply inverse of rotation in rotor
val restoredPoint = rotor.reverseSandwich(point)
val restoredPoint2 = rotor.reverse.sandwich(point)

// Creating a translator to add a vector
val vector = Pga3dVector(1, 2, 3)
val translator = Pga3dTranslator.addVector(vector)

// Applying translation to a point
val translatedPoint = translator.sandwich(point)
// This is equivalent to:
val translatedPoint = point + vector

// Creating a motor by combining a translator and a rotor
val motor = translator.geometric(rotor)

// Applying a motor to a point (combined rotation and translation)
val transformedPoint = motor.sandwich(point)

// Computing the logarithm of a motor 
val bivector = motor.log()

// Computing the exponential of a bivector (results back in a motor)
val motor2 = bivector.exp()

// Interpolating between two rotors/motors: slerp() follows the exact geodesic (constant
// angular velocity), nlerp() is a cheaper renormalized-lerp approximation of it
val halfway = rotor.slerp(rotor2, 0.5)
val approxHalfway = rotor.nlerp(rotor2, 0.5)
```

### Other classes:

* [**Pga3dMultivector**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dMultivector.scala): class with all 16 fields for a general case
* [**Pga3dPseudoScalar**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dPseudoScalar.scala): class with one field. Library has no scalar class and uses just Double instead.
* [**Pga3dMatrix**](shared/src/main/scala/me/kright/gametools/pga3d/Pga3dMatrix.scala): object with some utility code

```scala
// Converting specialized classes to multivector
val multivectorFromPoint = point.toMultivector
val multivectorFromRotor = rotor.toMultivector

// Creating a pseudoscalar
val pseudoscalar = Pga3dPseudoScalar(value)
```