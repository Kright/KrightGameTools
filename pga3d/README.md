## Plane-based geometric algebra for 3d

Contains generated code for special cases: planes, quaternions, points, etc.
They behave exactly like multivectors, but where a lot of fields are zeros and computations are much simpler.
In addition, this provides more type-safety, so a sum of two Pga3dVector is a Pga3dVector.

For example, the point class contains only three fields and quaternion has only four. So rotation of point by quaternion is
efficient as usual code in math libraries.

All classes are immutable, modern JVM is quite good with JIT and escape analysis.
Geometric algebra and classes are complex enough, thinking about mutability will make coding and debugging much harder.  

### Plane:

* [**Pga3dPlane**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dPlane.scala): plane, 4 fields.
* [**Pga3dPlaneIdeal**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dPlaneIdeal.scala): plane passing through the center of coordinates, 3 fields. Dual to Pga3dVector

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

* [**Pga3dPoint**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dPoint.scala): Point in space. Stored as dual representation with human-friendly fields x, y, z and fixed w=1.
* [**Pga3dPointCenter**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dPointCenter.scala): singleton object, center of coordinates.
* [**Pga3dVector**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dVector.scala): difference between points. Consist of x, y, z and fixed w=0. Pga3dTranslator moved points, but not
  vectors.
* [**Pga3dProjectivePoint**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dProjectivePoint.scala): general case with four homogeneous coordindates (x, y, z, w). Could be mapped to point via (x/w, y/w, z/w) when w != 0.

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

* [**Pga3dBivector**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dBivector.scala): 6 fields (xy, xz, yz, xw, yw, zw)
* [**Pga3dBivectorBulk**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dBivectorBulk.scala) - bivector with only 3 fields (xy, xz, yz).
* [**Pga3dBivectorWeight**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dBivectorWeight.scala) - bivector with only 3 fields (xw, yw, zw).

Sometimes it's easier to work with `wedge` and `antiWedge` as for `meet` and `join` operations.

Joining points produce lines and planes (expanding the degrees of freedom).
Meeting planes produce lines and points (contracting the degrees of freedom).

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

* [**Pga3dQuaternion**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dQuaternion.scala): represents rotation, 4 fields (scalar, xy, xz, yz). It is the exponent of Pga3dBivectorBulk
* [**Pga3dTranslator**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dTranslator.scala): represents linear movement, 3 fields (wx, wy, wz). It is the exponent of Pga3dBivectorWeight
* [**Pga3dMotor**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dMotor.scala): combination of rotation and linear movement. Has 8 fields (scalar, all bivector fields and pseudoscalar),
  exponent of Pga3dBivector

To move everything with these classes, you need to call `motor.sandwich(obj)`

```scala
// Creating a quaternion for rotation between two vectors
val from = Pga3dVector(1, 2, 3)
val to = Pga3dVector(3, 4, 5)
val quaternion = Pga3dQuaternion.rotation(from, to)

// Making quaternion as a geometric product of two planes 
// The rotation axis is a meet line of planes, the rotation angle is double angle between planes)
val quaternion2 = plane1 geometric plane2

// Applying rotation to anything using the sandwich product (could rotate point, line, plane, quatenrion, etc.)
val rotatedPoint = quaternion.sandwich(point)

// Inverse of rotation
val inverseRotation = quaterion.reverse

// Apply inverse of rotation in quaternion
val restoredPoint = quaternion.reverseSandwich(point)
val restoredPoint2 = quaternion.reverse.sandwich(point)

// Creating a translator to add a vector
val vector = Pga3dVector(1, 2, 3)
val translator = Pga3dTranslator.addVector(vector)

// Applying translation to a point
val translatedPoint = translator.sandwich(point)
// This is equivalent to:
val translatedPoint = point + vector

// Creating a motor by combining a translator and a quaternion
val motor = translator.geometric(quaternion)

// Applying a motor to a point (combined rotation and translation)
val transformedPoint = motor.sandwich(point)

// Computing the logarithm of a motor 
val bivector = motor.log()

// Computing the exponential of a bivector (results back in a motor)
val motor2 = bivector.exp()

```

### Other classes:

* [**Pga3dMultivector**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dMultivector.scala): class with all 16 fields for a general case
* [**Pga3dPseudoScalar**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dPseudoScalar.scala): class with one field. Library has no scalar class and uses just Double instead.
* [**Pga3dMatrix**](shared/src/main/scala/me.kright.gametools.pga3d/Pga3dMatrix.scala): object with some utility code

```scala
// Converting specialized classes to multivector
val multivectorFromPoint = point.toMultivector
val multivectorFromQuaternion = quaternion.toMultivector

// Creating a pseudoscalar
val pseudoscalar = Pga3dPseudoScalar(value)
```