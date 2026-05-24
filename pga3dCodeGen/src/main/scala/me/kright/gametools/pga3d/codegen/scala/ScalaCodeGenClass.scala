package me.kright.gametools.pga3d.codegen.scala

import me.kright.gametools.pga3d.codegen.common.{FileContent, FileWriter}

import java.nio.file.{Files, Path}

trait ScalaCodeGenClass:
  def name: String

  def isObject: Boolean

  def typeName: String =
    if (isObject) s"${name}.type"
    else name

  def typeNameWithoutPrefix: String =
    if (typeName.startsWith("Pga3d")) typeName.drop(5).capitalize
    else typeName.capitalize

  def generateImports(): String = ""

  def generateCode(): String

  def writeToFile(packageDir: Path): Unit =
    require(Files.exists(packageDir))

    val clsPath = packageDir.resolve(s"${name}.scala")

    val code =
      s"""package me.kright.gametools.pga3d
         |
         |${generateImports()}
         |/** this code is generated, see me.kright.gametools.pga3d.codegen.CodeGenClass */
         |${generateCode()}""".stripMargin

    FileContent(clsPath, code).writeWithLogging()
