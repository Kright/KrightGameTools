package me.kright.gametools.pga3d.codegen.common

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

  override def toString: String =
    code.toString().split("\n").map(s => if (s.isBlank) "" else s).mkString("\n")
