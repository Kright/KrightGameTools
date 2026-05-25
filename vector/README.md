The vector module provides simple implementations for 2D, 3D, and 4D vectors.
All of them are immutable.

## Classes

* [VectorNd](shared/src/main/scala/me/kright/gametools/vector/VectorNd.scala): common interface
* [Vector2d](shared/src/main/scala/me/kright/gametools/vector/Vector2d.scala)
* [Vector3d](shared/src/main/scala/me/kright/gametools/vector/Vector3d.scala)
* [Vector4d](shared/src/main/scala/me/kright/gametools/vector/Vector4d.scala)

## Usage example

```scala
import me.kright.gametools.vector.Vector3d

val v1 = Vector3d(1.0, 2.0, 3.0)
val v2 = Vector3d(4.0, 5.0, 6.0)

val sum = v1 + v2
val dot = v1 dot v2
val cross = v1 cross v2
val length = v1.length
val normalized = v1.normalized
```
