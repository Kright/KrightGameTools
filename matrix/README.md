# Matrix module

Linear algebra over [arrayview](https://github.com/kright/arrayView) views of doubles.

There is no matrix class and no alias: any `ArrayView2d[Double]` is a matrix, and all the
operations are extension methods on that interface. A lazily transposed view, a slice or a
broadcast of a bigger array works like any other matrix - `a * b.transposed` multiplies
without materializing the transposed copy. Operations that produce a new matrix return the
concrete `ArrayView2dFlat[Double]`.

## Features

- arithmetic (`+`, `-`, `*`, scalar `*` and `/`, the in-place `+=`, `-=`, `*=`) with an
  fma-based multiplication;
- `determinant` / `inverted` with hardcoded formulas for the sizes 1..4 (`Matrix2d`,
  `Matrix3d`, `Matrix4d` expose them directly, plus the projection-matrix factories in
  `Matrix4d`);
- `SymmetricMatrixDiagonalization.eigen(m): Eigen(diagonal, vectors)` - the Jacobi
  eigendecomposition of a symmetric matrix; the eigenvalues come back as a 1d view
  (`diagonalMatrix(values)` builds the square matrix back when needed);
- `CholeskyDecomposition` for symmetric positive-definite matrices;
- `equalsWithEps` (a `CanEqualWithEps` instance for any view of doubles), `show` for
  human-readable printing, the `matrixFromValues` / `identityMatrix` factories.

## Usage example

```scala
import me.kright.arrayview.ArrayView2dFlat
import me.kright.gametools.matrix.*

val m = matrixFromValues(2, 2)(1.0, 2.0, 3.0, 4.0)
val gram = m.transposed * m           // the transposed view multiplies without a copy
val Eigen(eigenvalues, vectors) = SymmetricMatrixDiagonalization.eigen(gram)
val restored = vectors * diagonalMatrix(eigenvalues) * vectors.transposed
```
