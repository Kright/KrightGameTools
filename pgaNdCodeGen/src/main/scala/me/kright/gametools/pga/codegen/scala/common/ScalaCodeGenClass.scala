package me.kright.gametools.pga.codegen.scala.common

import me.kright.gametools.pga.codegen.common.{FileContent, GeneratedFileSystem}

import java.nio.file.{Files, Path}

trait ScalaCodeGenClass:
  def name: String

  def isObject: Boolean

  /** the class-name prefix stripped by typeNameWithoutPrefix, e.g. "Pga3d" or "Pga2d" */
  def typeNamePrefix: String

  /** FQCN embedded in the default generateClassDoc() comment */
  def generatedComment: String

  /** package of the generated Scala file */
  def targetPackage: String

  def typeName: String =
    if (isObject) s"${name}.type"
    else name

  def typeNameWithoutPrefix: String =
    if (typeName.startsWith(typeNamePrefix)) typeName.drop(typeNamePrefix.length).capitalize
    else typeName.capitalize

  def generateImports(): String = ""

  def generateClassDoc(): String =
    s"/** this code is generated, see $generatedComment */"

  def generateCode(): String

  def writeToFile(packageDir: Path, fs: GeneratedFileSystem): Unit =
    require(Files.exists(packageDir))

    val clsPath = packageDir.resolve(s"${name}.scala")

    val code =
      s"""package $targetPackage
         |
         |${generateImports()}
         |${generateClassDoc()}
         |${generateCode()}""".stripMargin

    fs.write(FileContent(clsPath, code))
