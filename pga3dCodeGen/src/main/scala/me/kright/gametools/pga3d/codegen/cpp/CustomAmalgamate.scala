package me.kright.gametools.pga3d.codegen.cpp

import me.kright.gametools.pga3d.codegen.common.FileContent

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*


private class CustomAmalgamate(val files: Map[String, FileContent]) {
  val result = new StringBuilder()
  val includedFiles = new mutable.HashSet[String]()

  val ignoredLines = Set[String](
    "// Copyright (c) 2025 Igor Slobodskov",
    "// SPDX-License-Identifier: MIT",
  )

  def getStdIncludes(code: Seq[String]): Seq[String] = {
    code
      .flatMap(_.strip().split("\n"))
      .filter(_.startsWith("#include <"))
      .distinct
      .sorted
  }

  def fuse(text: String): String = {
    val includes = getStdIncludes(Seq(text) ++ files.values.map(_.content))

    {
      val code = CppCodeBuilder()
      code.myHeader(includes, getClass.getName)
      result.append(code).append("\n")
    }

    fuse(text, None, 0)

    {
      val missedFiles = files.keySet -- includedFiles
      require(missedFiles.isEmpty,
        s"""
           |missedFiles = ${missedFiles.toSeq.sorted.mkString(", ")}
           |existingFiles = ${files.keys.toSeq.sorted.mkString(", ")}""".stripMargin)
    }

    removeRepeatingEmptyLines(result.toString())
  }

  private def removeRepeatingEmptyLines(text: String): String = {
    val lines = text.strip().split("\n")

    lines.zip(Seq("") ++ lines)
      .filterNot((cur, prev) => cur.isBlank && prev.isBlank)
      .map((cur, prev) => cur)
      .mkString("\n")
  }

  private def fuse(text: String, fileName: Option[String], level: Int): Unit = {
    println(s"${"  " * level}${fileName.getOrElse("root")}")

    val lines = text.strip().split("\n")
    val lastIncludeLine: Int = lines.zipWithIndex.filter(_._1.startsWith("#include")).map(_._2).maxOption.getOrElse(-1)

    var printedName = false

    for ((line, i) <- lines.zipWithIndex) {
      if (line.startsWith("#")) {
        if (line.startsWith("#include")) {
          if (line.startsWith("#include <")) {
            // skip, all such includes gathered at the file beginning
          } else {
            require(line.startsWith("#include \""))
            val includePath = line.strip().stripPrefix("#include \"").stripSuffix("\"")
            val fileName = Path.of(includePath).getFileName.toString
            if (!includedFiles.contains(fileName)) {
              includedFiles.add(fileName)
              fuse(files(fileName).content, Option(fileName), level + 1)
            }
          }
        } else if (line.startsWith("#pragma once")) {
          require(line == "#pragma once") // skip
        } else {
          require(false, "unknown directive: " + line)
        }
      } else {
        if (i >= lastIncludeLine + 1 && !printedName) {
          fileName.foreach { name =>
            result.append(s"\n// ${name}\n\n")
          }
          printedName = true
        }

        if (!ignoredLines.contains(line) &&
          !(i > 0 && line.isBlank && ignoredLines.contains(lines(i - 1)))) {
          result.append(line).append("\n")
        }
      }
    }
  }
}

object CustomAmalgamate:
  private val pga3d = Path.of("cpp/pga3d")
  private val pga3dphysics = Path.of("cpp/pga3dphysics")

  def fuse(dirs: Seq[Path], includes: String, output: Path): Unit = {
    val amalgamate = new CustomAmalgamate(getFiles(dirs))
    val code = amalgamate.fuse(includes)
    FileContent(output, code).writeWithLogging()
  }

  def fuseMath(): Unit = {
    fuse(
      Seq(pga3d),
      """
        |#include "pga3d.h"
        |""".stripMargin,
      Path.of("cpp/fused/pga3d.h")
    )
  }

  def fusePhysicsWithMath(): Unit = {
    fuse(
      Seq(pga3d, pga3dphysics),
      """
        |#include "pga3d.h"
        |#include "pga3dphysics.h"
        |""".stripMargin,
      Path.of("cpp/fused/pga3dphysics.h")
    )
  }

  private def getFiles(dirs: Seq[Path]): Map[String, FileContent] = {
    val files = dirs.flatMap(dir => Files.list(dir).iterator().asScala).filterNot(_.getFileName.toString.endsWith(".md"))

    require(files.map(_.getFileName.toString).toSet.size == files.size)

    val loadedFiles = files.map(FileContent.load)

    {
      val invalidFiles = loadedFiles.filterNot(_.content.startsWith("// Copyright (c) 2025 Igor Slobodskov\n// SPDX-License-Identifier: MIT"))
      invalidFiles.foreach(f => println("WARNING: " + f.path.getFileName + " has no license!"))
    }

    loadedFiles.map(f => f.path.getFileName.toString -> f).toMap
  }
