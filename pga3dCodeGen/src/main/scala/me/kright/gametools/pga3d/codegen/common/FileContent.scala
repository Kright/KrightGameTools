package me.kright.gametools.pga3d.codegen.common

import java.nio.file.{Files, Path}

case class FileContent(path: Path, content: String):
  def write(): Boolean =
    FileWriter.writeToFile(path, content, createDirs = true)

  def writeWithLogging(): Boolean =
    val writeNew = write()
    println(s"file ${if (writeNew) "generated" else "is up-to-date"} = $path, lines = ${content.lines().count()}, symbols = ${content.size}")
    writeNew


object FileContent:
  def load(path: Path): FileContent =
    FileContent(path, Files.readString(path))