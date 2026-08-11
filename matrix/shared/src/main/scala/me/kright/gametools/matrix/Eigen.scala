package me.kright.gametools.matrix

import me.kright.arrayview.{ArrayView1dFlat, ArrayView2dFlat}

/**
 * The result of a symmetric eigendecomposition:
 * input == vectors * diagonalMatrix(diagonal) * vectors.transposed,
 * where `diagonal` holds the eigenvalues as a 1d view and the k-th column of `vectors` is the
 * unit eigenvector of the k-th eigenvalue. Produced by [[SymmetricMatrixDiagonalization.eigen]].
 */
final case class Eigen(diagonal: ArrayView1dFlat[Double], vectors: ArrayView2dFlat[Double])
