ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.3"

lazy val root = (project in file("."))
  .settings(
    name := "precession",
    libraryDependencies += "com.badlogicgames.gdx" % "gdx" % "1.13.5",
    libraryDependencies += "com.badlogicgames.gdx" % "gdx-backend-lwjgl3" % "1.13.5",
    libraryDependencies += "com.badlogicgames.gdx" % "gdx-platform" % "1.13.5" classifier "natives-desktop",
    libraryDependencies += "me.kright" %% "gametools-pga3d" % "0.9.0",
    libraryDependencies += "me.kright" %% "gametools-pga3dphysics" % "0.9.0",
    libraryDependencies += "me.kright" %% "gametools-mathutil" % "0.9.0",
  )
