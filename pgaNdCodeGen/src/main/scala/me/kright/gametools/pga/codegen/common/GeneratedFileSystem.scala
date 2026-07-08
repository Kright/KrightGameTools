package me.kright.gametools.pga.codegen.common

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.mutable

/** status of a generated file compared to what is currently on disk */
enum FileStatus derives CanEqual:
  case Created
  case Modified
  case UpToDate

object FileStatus:
  def of(path: Path, content: String): FileStatus =
    if (!Files.exists(path)) {
      FileStatus.Created
    } else if (Files.readString(path, StandardCharsets.UTF_8) == content) {
      FileStatus.UpToDate
    } else {
      FileStatus.Modified
    }

/**
 * abstraction over the generator's file output: implementations either write files (real mode)
 * or record what would change without writing (dry-run check mode)
 */
trait GeneratedFileSystem:
  /** process one generated file */
  def write(file: FileContent): Unit

/**
 * writes generated files to disk, skipping files whose on-disk content is already identical,
 * logging one line per file (preserves the previous FileContent.writeWithLogging behavior)
 */
class RealFileSystem extends GeneratedFileSystem:
  override def write(file: FileContent): Unit =
    val writeNew = FileWriter.writeToFile(file.path, file.content, createDirs = true)
    println(s"file ${if (writeNew) "generated" else "is up-to-date"} = ${file.path}, lines = ${file.content.lines().count()}, symbols = ${file.content.size}")

/** writes nothing; records how each generated file compares to disk so callers can report drift */
class CheckFileSystem extends GeneratedFileSystem:
  private val records = mutable.ArrayBuffer.empty[(FileContent, FileStatus)]

  override def write(file: FileContent): Unit =
    records += (file -> FileStatus.of(file.path, file.content))

  /** every processed file with its status, in generation order */
  def snapshot: Seq[(FileContent, FileStatus)] = records.toSeq

  /** files that would change (new or modified) */
  def changed: Seq[(FileContent, FileStatus)] =
    snapshot.filter((_, status) => status != FileStatus.UpToDate)

  def hasChanges: Boolean = changed.nonEmpty

  def summary: String =
    if (changed.isEmpty) {
      s"check mode: all ${snapshot.size} generated file(s) are up-to-date"
    } else {
      val created = changed.count((_, s) => s == FileStatus.Created)
      val modified = changed.count((_, s) => s == FileStatus.Modified)
      val sb = StringBuilder()
      sb ++= s"check mode: ${changed.size} of ${snapshot.size} generated file(s) would change ($created new, $modified modified)\n"
      for ((file, status) <- changed) {
        val tag = status match {
          case FileStatus.Created => "new"
          case FileStatus.Modified => "modified"
          case FileStatus.UpToDate => "up-to-date"
        }
        sb ++= s"  [$tag] ${file.path}\n"
      }
      sb.toString.stripTrailing()
    }

  def printSummary(): Unit = println(summary)
