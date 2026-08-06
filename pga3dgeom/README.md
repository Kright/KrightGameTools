# PGA3D Geometry Module

## Features

### Geometric Primitives
- **Axis-Aligned Bounding Box (AABB)**: Fast collision detection and spatial partitioning
- **Triangles**: 3D triangle representation with various geometric operations
- **Edges**: Line segments in 3D space with intersection testing
- **Spheres, Capsules and Cylinders**: Simple bounding volumes with collision queries
- **Rays**: Precomputed rays for efficient intersection tests against many AABBs (e.g. BVH traversal)

### Geometric Algorithms
- **Digital Differential Analyzer (DDA)**: Efficient ray traversal through a grid
- **Nearest Point Calculations**: Find the nearest point on geometric primitives
- **Intersection Testing**: Detect intersections between various geometric primitives
- **Contact Queries**: `deepestContact` returns the contact point, unit normal and penetration depth

The result of a query that may find nothing (`intersection`, `deepestContact`) is `T | Null` with
`null` for "no intersection" - no `Option` allocation on the hot path; the boolean `intersects`
methods are the cheap yes/no companions.

## Key Classes

### `Pga3dAABB`
Represents an Axis-Aligned Bounding Box in 3D space.
```scala
// Create an AABB from points
val aabb = Pga3dAABB(min, max)

// Create an AABB from a triangle
val triangleAabb = Pga3dAABB(triangle)

// Check if an AABB contains a point
val contains = aabb.contains(point)

// Check for intersection with another AABB
val intersects = aabb.intersects(otherAabb)

// Expand an AABB
val expanded = aabb.expand(amount)
```

### `Pga3dTriangle`
Represents a triangle in 3D space.
```scala
// Create a triangle from three points
val triangle = Pga3dTriangle(a, b, c)

// Get the plane of the triangle
val plane = triangle.plane

// Calculate the area of the triangle
val area = triangle.area

// Find the nearest point on the triangle to a given point
val nearest = triangle.getNearestPoint(point)

// Check for intersection with an edge
val intersects = triangle.intersects(edge, eps)

// The intersection point itself (null when there is none)
val point: Pga3dPoint | Null = triangle.intersection(edge, eps)
```

### `Pga3dEdge`
Represents an edge (line segment) in 3D space.
```scala
// Create an edge from two points
val edge = Pga3dEdge(a, b)

// Get the center of the edge
val center = edge.center

// Find the nearest point on the edge to a given point
val nearest = edge.getNearestPoint(point)
```

### `Pga3dSphere`
Represents a sphere.
```scala
val sphere = Pga3dSphere(center, r)

// Boolean overlap queries
val overlaps = sphere.intersects(otherSphere)
val touchesTriangle = sphere.intersects(triangle)
val touchesCapsule = sphere.intersects(capsule)

// Contact with a triangle: nearest triangle point, unit normal towards the center,
// penetration depth r - distance; null when there is no contact
val contact: Pga3dContact | Null = sphere.deepestContact(triangle)

// Bounding box of the sphere
val aabb = sphere.toAABB
```

### `Pga3dCapsule`
All points within `r` of the segment `[a, b]`, stored by the two hemisphere centers;
`a == b` degenerates to a sphere. `Pga3dContact(point, normal, depth)` is 7 flat doubles.
```scala
val capsule = Pga3dCapsule(a, b, r)
// or engine-style, from the center and the half axis
val fromCenter = Pga3dCapsule.fromCenter(center, halfAxis, r)

// Boolean overlap queries
val hitsSphere = capsule.intersects(sphere)
val hitsCapsule = capsule.intersects(otherCapsule)
val hitsTriangle = capsule.intersects(triangle)

// Contact with a triangle; when the axis pierces the triangle, the normal is the plane
// normal towards the larger part of the axis and pushing by depth fully separates
val contact: Pga3dContact | Null = capsule.deepestContact(triangle)

// The axis segment, the bounding box, widening
val axis = capsule.edge
val aabb = capsule.toAABB
val wider = capsule.expand(dr)
```

### `Pga3dRay`
A ray with a precomputed reciprocal of the direction for efficient intersection tests
against many AABBs, for example while traversing a BVH tree.
```scala
// direction is used as is: a point on the ray is origin + direction * t
val ray = Pga3dRay(origin, direction)

// or with a normalized direction, so t is the euclidean distance
val normalizedRay = Pga3dRay.normalized(origin, direction)

// Check for intersection with an AABB
val hits = ray.intersects(aabb)

// t of the entry point, 0.0 if origin is inside, Double.PositiveInfinity on miss
val t = ray.intersectionT(aabb)
```

### `Pga3dDigitalDifferentialAnalyzer`
Implements a Digital Differential Analyzer algorithm for 3D ray traversal through a grid.
```scala
// Create a DDA from an origin and direction
val dda = new Pga3dDigitalDifferentialAnalyzer(origin, direction)

// Step through the grid
dda.doStep()

// Access the current cell coordinates
val (x, y, z) = (dda.x, dda.y, dda.z)
```