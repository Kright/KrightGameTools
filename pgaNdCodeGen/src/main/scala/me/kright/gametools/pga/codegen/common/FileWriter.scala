package me.kright.gametools.pga.codegen.common

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

object FileWriter:
  def writeToFile(path: Path, content: String, createDirs: Boolean): Boolean =
    // if file exists and content is identical, skip writing and report
    if (Files.exists(path)) {
      val existing = Files.readString(path, StandardCharsets.UTF_8)
      if (existing == content) {
        return false
      }
    }

    if (createDirs) {
      Files.createDirectories(path.getParent)
    }

    Files.write(path, content.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING,
    )

    true

