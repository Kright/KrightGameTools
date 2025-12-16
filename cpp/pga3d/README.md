Header ["pga3d.h"](pga3d.h) includes everything.

File ["types.h"](types.h) includes all definitions without complex implementations.

Each operation is placed in its own header, so if you don't need it, you can exclude it from compilation. 
Unfortunately, C++ does not have extension methods, so geometric operations have to be declared inside struct bodies.

Generally, there is a list of types and a list of math operations for them.
Types and operations are based on projective geometric algebra, but I choose useful subtypes and gave them names like "Plane" or "Vector".
All code is in namespace `pga3d`.

## Types

Each type is a constexpr struct with fields of type `double`.

* [**Multivector**](Multivector.h): general type, consists of 16 numbers, usually not needed. All other types are more narrow subtypes when some fields constrained to be 0 or 1.

**Geometric objects:**

* [**Plane**](Plane.h): 3D plane, consists of 4 numbers.
* [**PlaneIdeal**](PlaneIdeal.h): 3D plane containing center of coordinates, consists of 3 numbers.
* [**Bivector**](Bivector.h): 3D bivector, could represent line, consists of 6 numbers.
* [**BivectorWeight**](BivectorWeight.h): narrow case of 3D bivector, consists of 3 numbers only.
* [**BivectorBulk**](BivectorBulk.h): narrow case, consists of other 3 numbers of bivector.
* [**ProjectivePoint**](ProjectivePoint.h): projective point (x, y, z, w) corresponding to 3d point (x/w, y/w, z/w).
* [**Point**](Point.h): 3D point, consists of 3 numbers, the same as ProjectivePoint but w = 1. Represents Point in 3D space.
* [**Vector**](Vector.h): 3D vector, consists of 3 numbers, the same as ProjectivePoint but w = 0. Represents the difference between two points.
* [**PointCenter**](PointCenter.h): Point in the center of coordinates, w = 1 and x = y = z = 0.

**Transformations:**

* [**Quaternion**](Quaternion.h): describing rotation.
* [**Translator**](Translator.h): describing translation.
* [**ProjectiveTranslator**](ProjectiveTranslator.h): non-normalized Translator.
* [**Motor**](Motor.h): represents both rotation and translation.

**Other types:**

* [**PseudoScalar**](PseudoScalar.h): represents something like an `i` - dual to scalar.
* [**TranslatorWithQuaternion, QuaternionWithTranslator**](TranslatorWithQuaternion.h): decomposition of Motor to translation and rotation.

## Operations

All headers with name starting with "ops"

Geometric algebra operations are applicable to all types.
If operation definition does not exist, it means that the result is zero.

TranslatorWithQuaternion and QuaternionWithTranslator are the only exceptions and miss some operations.

* `T::from(array)`, `T::from(span)` and `t.toArray()` for storing/retrieving values. 
* `t.toSomeName()`: safe conversion to another type.
* `t.toSomeTypeUnsafe()`: throws out some fields. Useful in cases when a type system returns a wider type, but you are sure that it could be narrower.

[**Arithmetic**](opsArithmetic.h)

Addition and subtraction are working as usual. Multiplication and division are defined with scalars only.
`a.madd(b, multiplier)` - based on std::fma, equivalent to `a + b * multiplier`.

**Geometric algebra operations**

* `t.dual()`: dual in terms of geometric algebra. `t.dual().dual()` returns the same values, but sometimes with wider types. 
* `t.weight(), t.bulk()`: zeroes one or another half of multivector fields.
* `t.reversed()`: useful for transformations, inverts them. In geometric algebra it is a fast, trivial operation. `t.reversed().reversed()` returns the original value.
* `t.antiReversed()`: same, but in dual space.
* `t.geometric(), t.antiGeometric()`: geometric product and the same in dual space.
* `t.dot(), t.antiDot()`: inner product and the same in dual space.
* `t.wedge()`: exterior product. Another name is `meet`. For example, meet of two planes is a line, meet of three planes is a dot.
* `t.antiWedge()`: exterior product in dual space. Another name is `join`. For example, join of two points is a line, join of three points is a plane.
* `t.sandwich()`: sandwich product. Useful for transformations, `motor.sandwich(element)` does this. Equivalent to `motor.geometric(element).geometric(motor.reversed())`, fused into one operation.
* `t.cross()`: cross product (anti-commutator) `a.cross(b)` equals to `0.5 * (a.geometric(b) - b.geometric(a))`

**Special operations for transformations**

Representing transformations as square matrices
```
std::array<double, 16> sandwichAsMatrixForProjectivePoint()
std::array<double, 9> sandwichAsMatrixForVector()
std::array<double, 36> sandwichAsMatrixForBivector()
std::array<double, 16> sandwichAsMatrixForPlane()
```
