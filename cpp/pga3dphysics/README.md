Classes for implementing rigid body physics in 3D on top of pga3d code.

[pga3dphysics.h] includes everything.

All code is in namespace `pga3d`.

**Geometric primitives**

* [Edge](Edge.h)
* [Triangle](Triangle.h)
* [Cylinder](Cylinder.h)
* [Sphere](Sphere.h)
* [AABB](AABB.h)

**Physics converters**

* [Energy](Energy.h): concept for anything with energy (physics body, spring, array of bodies)
* [Forque](Forque.h): helper class for creating forces and torques and for extracting them from bivectors
* [Velocity](Velocity.h): same helpers for velocities

**Inertia**

* [InertiaLocalSphere](InertiaLocalSphere.h): inertia tensor of a sphere with center mass in the center of coordinates.
* [InertiaLocal](InertiaLocal.h): local inertia of a rigid body with three main axes, mass in the center of coordinates.
* [InertiaMovedLocal](InertiaMovedLocal.h): based on InertiaLocal, but rotated and translated.
* [InertiaSummable](InertiaSummable.h): summable representation of inertia tensor. Several inertias can be added together.

**Physics bodies** 

* [BodyState](BodyState.h): Transform and velocity of body
* [PhysicsBody](PhysicsBody.h): Rigid body with inertia tensor and state (velocity and transform)
* [BodyPoint](BodyPoint.h): Point in body coordinates
* [BodyLine](BodyLine.h): Line in body coordinates

**Physics constraints** 

* [PhysicsBodyConnection](PhysicsBodyConnection.h): Common concepts for connections between bodies.
* [PhysicsBodyConnections](PhysicsBodyConnections.h): Collection of connections between bodies (for example, springs)
* [Gravity](Gravity.h): Adds Earth's gravity for a system of bodies.
* [Friction](Friction.h): friction
  * VelocityFriction: friction based on velocity of point
  * Position1dFriction: friction based on position change for a one-dimensional case (for example, used in Spring for damping)
  * Position3dFriction: friction based on position change for a three-dimensional case (for example, used in friction of a body point to another body)
  * Point3dFriction: contains body point, other body and velocity and 3d position frictions, calculates friction.
* [Spring](Spring.h): Spring with friction.
* [SpringToLine](SpringToLine.h): Spring with constant force and damping, which is attached to a line (chooses the nearest point on a line).
* [Torsion](Torsion.h): Torsional spring.

**Other** 

* [LinearOperator](LinearOperator.h): non-optimized code for making matrix representations of linear operators.
* [PhysicsSolverRK4](PhysicsSolverRK4.h): Precise solver for physics bodies of fourth-order.