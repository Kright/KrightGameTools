Vectors treated as columns. For quaternions and matrices, multiplication order is math-like, for example:

```scala
(matrixA * matrixB) * vec === matrixA * (matrixB * vec)
(quaternionA * quaternionB) * vec === quaternionA * (quaternionB * vec)
```

Support conversions between rotation matrix, quaternions, and Euler angles.
