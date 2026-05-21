package com.github.kright.math

import com.github.kright.math.VectorMathGenerators.*
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

  val matrices3: Gen[Matrix3d] =
    for (vx <- vectors3InCube;
         vy <- vectors3InCube;
         vz <- vectors3InCube)
    yield Matrix3d().setRows(vx, vy, vz)

  val matrices2: Gen[Matrix2d] =
    for (m00 <- double1; m01 <- double1;
         m10 <- double1; m11 <- double1)
    yield new Matrix2d(m00, m01, m10, m11)

  val matrices4: Gen[Matrix4d] =
    for (vx <- vectors4InCube;
         vy <- vectors4InCube;
         vz <- vectors4InCube;
         vw <- vectors4InCube)
    yield Matrix4d().setRows(vx, vy, vz, vw)
