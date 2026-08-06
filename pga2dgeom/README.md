# PGA2D Geometry Module

The 2d sibling of [pga3dgeom](../pga3dgeom/README.md) with the same style and terminology.
Compared to the 3d module there is no cylinder, the sphere becomes `Pga2dCircle`,
and triangles gain 2d-only features: an interior (exact `contains` without eps) and a signed area.

## Features

### Geometric Primitives
- **Axis-Aligned Bounding Box (AABB)**: Fast collision detection and spatial partitioning
- **Triangles**: 2D triangle representation with various geometric operations
- **Edges**: Line segments in 2D space with intersection testing
- **Circles and Capsules**: The 2d siblings of the 3d sphere and capsule (a capsule is a stadium shape)
- **Rays**: Precomputed rays for efficient intersection tests against many AABBs (e.g. BVH traversal)

### Geometric Algorithms
- **Digital Differential Analyzer (DDA)**: Efficient ray traversal through a grid
- **Nearest Point Calculations**: Find the nearest point on geometric primitives
- **Intersection Testing**: Detect intersections between various geometric primitives

The result of a query that may find nothing (`intersection`) is `T | Null` with `null` for
"no intersection" - no `Option` allocation on the hot path; the boolean `intersects` methods
are the cheap yes/no companions. (`deepestContact` is 3d-only for now: the convention for a
contact with the center inside the volume, common in 2d, deserves a design decision.)

## Key Classes

### `Pga2dAABB`
Represents an Axis-Aligned Bounding Box in 2D space.
```scala
// Create an AABB from points
val aabb = Pga2dAABB(min, max)

// Create an AABB from a triangle
val triangleAabb = Pga2dAABB(triangle)

// Check if an AABB contains a point
val contains = aabb.contains(point)

// Check for intersection with another AABB
val intersects = aabb.intersects(otherAabb)

// Expand an AABB
val expanded = aabb.expand(amount)

// Area and perimeter (instead of volume and surfaceArea in 3d)
val area = aabb.area
val perimeter = aabb.perimeter
```

### `Pga2dTriangle`
Represents a triangle in 2D space.
```scala
// Create a triangle from three points
val triangle = Pga2dTriangle(a, b, c)

// Calculate the area of the triangle
val area = triangle.area

// Signed area is positive for counter-clockwise order of vertices
val signedArea = triangle.signedArea

// Unlike in 3d, a 2d triangle has an interior, so contains is exact
val inside = triangle.contains(point)

// Find the nearest point on the triangle to a given point
val nearest = triangle.getNearestPoint(point)

// Check for intersection with an edge
val intersects = triangle.intersects(edge, eps)

// The intersection point itself (null when there is none)
val point: Pga2dPoint | Null = triangle.intersection(edge, eps)
```

### `Pga2dEdge`
Represents an edge (line segment) in 2D space.
```scala
// Create an edge from two points
val edge = Pga2dEdge(a, b)

// Get the center of the edge
val center = edge.center

// Find the nearest point on the edge to a given point
val nearest = edge.getNearestPoint(point)
```

### `Pga2dCircle`
Represents a circle, the 2d sibling of `Pga3dSphere`.
```scala
val circle = Pga2dCircle(center, r)

// Boolean overlap queries
val overlaps = circle.intersects(otherCircle)
val touchesTriangle = circle.intersects(triangle)
val touchesCapsule = circle.intersects(capsule)

// Bounding box of the circle
val aabb = circle.toAABB
```

### `Pga2dCapsule`
All points within `r` of the segment `[a, b]` (a stadium shape), stored by the two cap centers;
`a == b` degenerates to a circle. Mirrors `Pga3dCapsule`.
```scala
val capsule = Pga2dCapsule(a, b, r)
// or engine-style, from the center and the half axis
val fromCenter = Pga2dCapsule.fromCenter(center, halfAxis, r)

// Boolean overlap queries
val hitsCircle = capsule.intersects(circle)
val hitsCapsule = capsule.intersects(otherCapsule)
val hitsTriangle = capsule.intersects(triangle)

// The axis segment, the bounding box, widening
val axis = capsule.edge
val aabb = capsule.toAABB
val wider = capsule.expand(dr)
```

### `Pga2dRay`
A ray with a precomputed reciprocal of the direction for efficient intersection tests
against many AABBs, for example while traversing a BVH tree.
```scala
// direction is used as is: a point on the ray is origin + direction * t
val ray = Pga2dRay(origin, direction)

// or with a normalized direction, so t is the euclidean distance
val normalizedRay = Pga2dRay.normalized(origin, direction)

// Check for intersection with an AABB
val hits = ray.intersects(aabb)

// t of the entry point, 0.0 if origin is inside, Double.PositiveInfinity on miss
val t = ray.intersectionT(aabb)
```

### `Pga2dDigitalDifferentialAnalyzer`
Implements a Digital Differential Analyzer algorithm for 2D ray traversal through a grid.
```scala
// Create a DDA from an origin and direction
val dda = new Pga2dDigitalDifferentialAnalyzer(origin, direction)

// Step through the grid
dda.doStep()

// Access the current cell coordinates
val (x, y) = (dda.x, dda.y)
```
