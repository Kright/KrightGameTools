# Matrix module

Implementation of matrices and basic linear algebra.

## Features

- Matrix operations (addition, multiplication, etc.)
- Transformation matrices for 3D graphics and physics
- Integration with `arrayview` for efficient data handling

## Usage example

```scala
import me.kright.gametools.matrix.Matrix4d

val m = Matrix4d.identity
val translation = Matrix4d.translation(1.0, 2.0, 3.0)
val combined = m * translation
```
