package com.github.kright.math

import com.github.kright.math.VectorMathGenerators.*
import com.github.kright.matrix.{Matrix, Matrix2d, Matrix3d, Matrix4d}
import org.scalacheck.Gen

object MathGenerators:
  val gaussianQuaternions: Gen[Quaternion] =
    for (w <- gaussian;
         x <- gaussian;
         y <- gaussian;
         z <- gaussian)
    yield Quaternion(w, x, y, z)

  val normalizedQuaternions: Gen[Quaternion] =
    Gen.oneOf(
      Gen.const(Quaternion.id),
      Gen.const(-Quaternion.id),
      gaussianQuaternions.map(_.normalized())
    )

  val eulerAngles: Gen[EulerAngles] =
    for (yaw <- double1;
         pitch <- double1;
         roll <- double1)
    yield new EulerAngles(yaw * Math.PI, pitch * 0.5 * Math.PI, roll * Math.PI)

  val matrices3: Gen[Matrix] =
    for (vx <- vectors3InCube;
         vy <- vectors3InCube;
         vz <- vectors3InCube)
    yield Matrix3d(Array(
      vx.x, vx.y, vx.z,
      vy.x, vy.y, vy.z,
      vz.x, vz.y, vz.z
    ))

  val matrices2: Gen[Matrix] =
    for (m00 <- double1; m01 <- double1;
         m10 <- double1; m11 <- double1)
    yield Matrix2d(Array(m00, m01, m10, m11))

  val matrices4: Gen[Matrix] =
    for (vx <- vectors4InCube;
         vy <- vectors4InCube;
         vz <- vectors4InCube;
         vw <- vectors4InCube)
    yield Matrix4d(Array(
      vx.x, vx.y, vx.z, vx.w,
      vy.x, vy.y, vy.z, vy.w,
      vz.x, vz.y, vz.z, vz.w,
      vw.x, vw.y, vw.z, vw.w
    ))
