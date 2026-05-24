# GA Module

Experimental support for geometric algebra (GA) and plane-based geometric algebra (PGA).

## Features

- Support for any dimensions
- MultiVector operations: addition, multiplication (geometric, outer, inner, sandwich, etc)
- Specialized support for PGA (Plane-based Geometric Algebra)
- Differential solvers (Runge-Kutta, Heun's method)

## Examples

See `ga/src/test/scala/com/github/kright/ga` for usage examples:
- `PGA3OneBody.scala`: rigid body rotation simulation using PGA
- `DifferentialSolvers.scala`: generic implementation of numerical integration methods
