package me.kright.gametools.pga.codegen.common

import java.nio.file.{Files, Path}

case class FileContent(path: Path, content: String)

object FileContent:
  def load(path: Path): FileContent =
    FileContent(path, Files.readString(path))
