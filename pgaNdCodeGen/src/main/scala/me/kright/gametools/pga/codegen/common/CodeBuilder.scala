package me.kright.gametools.pga.codegen.common

trait CodeBuilder:
  protected var level: Int = 0
  protected val code = StringBuilder()
  val padding: String

  inline def block(addCode: => Unit): Unit = {
    level += 1
    addCode
    level -= 1
  }

  def apply(lines: String): Unit = {
    val prefix = padding.repeat(level)
    for (line <- lines.split("\n")) {
      code.append(prefix).append(line).append("\n")
    }
  }

  /** a javadoc-style doc comment: the first and the last lines hold only the markers */
  def comment(lines: String): Unit = {
    apply("/**")
    for (line <- lines.split("\n")) {
      apply(if (line.isBlank) " *" else s" * $line")
    }
    apply(" */")
  }

  override def toString: String =
    code.toString().split("\n").map(s => if (s.isBlank) "" else s).mkString("\n")
