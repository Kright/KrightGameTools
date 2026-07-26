# GA Module

Experimental support for geometric algebra (GA) and plane-based geometric algebra (PGA) in any
dimension and signature. A multivector is represented as a map from basis blade to coefficient:
flexible and correct, but slower than the specialized generated classes of pga2d/pga3d.

**This module is the source of truth for the code generators**: pgaNdCodeGen evaluates every
operation of pga2d/pga3d/C++ symbolically with `MultiVector[Sym]` from this module, so the
correctness of the generated libraries reduces to the correctness of `ga` (which is what the
property-based tests here check: associativity, morphisms, energy conservation in the physics
examples). The module is not published; it is a compile-time dependency of the generator and a
test dependency elsewhere.

## Key classes

- `BasisBlade` / `BasisBladeWithSign` - a basis blade as a bitmask of generators, with orientation
- `Signature` - the metric: how many generators square to +1, -1 and 0 (`Signature.pga2`, `Signature.pga3`)
- `GA` - an algebra instance: blades, products and signs for a signature
- `MultiVector[T]` - a generic multivector over any numeric `T`; used with `T = Double` for computation
  and `T = Sym` (from the `symbolic` module) for deriving formulas in the code generators
- `GARepresentationConfig` / `GARepresentation` - naming of generators and blades (e.g. `wxyz`, scalar `s`,
  pseudoscalar `i`) used for field names in the generated classes
- `PGA2` / `PGA3` - the plane-based algebras consumed by pgaNdCodeGen
- operations: geometric, dot, wedge, antiWedge, sandwich, dual, reverse, grade selection - see
  `GAOperations` and the `MultiVector` methods

## Features

- Support for any dimensions and signatures
- MultiVector operations: addition, multiplication (geometric, outer, inner, sandwich, etc)
- Specialized support for PGA (Plane-based Geometric Algebra)
- Differential solvers (Runge-Kutta, Heun's method)

## Examples

See usage examples in tests:
- [PGA3OneBody.scala](src/test/scala/me/kright/gametools/ga/PGA3OneBody.scala): rigid body rotation simulation using PGA
- [DifferentialSolvers.scala](src/test/scala/me/kright/gametools/ga/DifferentialSolvers.scala): generic implementation of numerical integration methods
