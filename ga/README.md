# GA Module

Experimental support for geometric algebra (GA) and plane-based geometric algebra (PGA).

## Features

- Support for any dimensions
- MultiVector operations: addition, multiplication (geometric, outer, inner, sandwich, etc)
- Specialized support for PGA (Plane-based Geometric Algebra)
- Differential solvers (Runge-Kutta, Heun's method)

## Examples

See usage examples in tests:
- [PGA3OneBody.scala](src/test/scala/me/kright/gametools/ga/PGA3OneBody.scala): rigid body rotation simulation using PGA
- [DifferentialSolvers.scala](src/test/scala/me/kright/gametools/ga/DifferentialSolvers.scala): generic implementation of numerical integration methods
