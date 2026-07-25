package me.kright.gametools.pga.codegen.common

import scala.collection.immutable.ArraySeq
import scala.util.matching.Regex

/**
 * Renders the language-neutral bodies of hand-written numerical methods (see SharedFormulas)
 * into Scala or C++, so the two generated backends share one source of the math.
 *
 * Line dialect (leading spaces express nesting relative to the method body):
 *  - "@name = expr"                        - a local double
 *  - "@name: {Cls} = expr"                 - a local of a generated class
 *  - "@name = IF cond THEN expr ELSE expr" - a conditional local double
 *  - "return expr"                         - a return statement
 *  - comments and `if (...) {` / `}` control lines pass through
 *
 * Expression dialect: bare math functions (sqrt, sin, cos, asin, atan2, abs) and
 * diffOfProducts get language prefixes; {Cls} placeholders become language class names;
 * zero-argument accessors from the `properties` list get () appended in C++;
 * (-this) is translated to the C++ spelling.
 */
object FormulaTemplate:
  private val mathFunctions = ArraySeq("sqrt", "sin", "cos", "asin", "atan2", "abs")
  private val properties = ArraySeq("normSquare", "norm", "bulkNorm", "normalizedByNorm", "log", "exp")
  // zero-arg methods called on `this` without a dot; must not collide with template locals
  private val bareProperties = ArraySeq("bulkNormSquare", "bulkNorm")
  private val classPlaceholder: Regex = """\{(\w+)\}""".r

  private case class TargetLang(mathPrefix: String,
                                diffOfProducts: String,
                                className: String => String,
                                declDouble: String => String,
                                declTyped: (String, String) => String,
                                conditional: (String, String, String, String) => String,
                                statementSuffix: String,
                                propertySuffix: String,
                                minusThis: String,
                                thisRef: String,
                                indent: String => String)

  private def scalaLang(classPrefix: String) = TargetLang(
    mathPrefix = "Math.",
    diffOfProducts = "diffOfProducts",
    className = name => classPrefix + name,
    declDouble = name => s"val $name = ",
    declTyped = (name, cls) => s"val $name = ",
    conditional = (name, cond, whenTrue, whenFalse) =>
      s"val $name = if ($cond) {\n  $whenTrue\n} else {\n  $whenFalse\n}",
    statementSuffix = "",
    propertySuffix = "",
    minusThis = "(-this)",
    thisRef = "this",
    indent = identity,
  )

  private val cppLang = TargetLang(
    mathPrefix = "std::",
    diffOfProducts = "detail::diffOfProducts",
    className = identity,
    declDouble = name => s"const double $name = ",
    declTyped = (name, cls) => s"const $cls $name = ",
    conditional = (name, cond, whenTrue, whenFalse) =>
      s"const double $name = ($cond)\n    ? ($whenTrue)\n    : ($whenFalse);",
    statementSuffix = ";",
    propertySuffix = "()",
    minusThis = "(-(*this))",
    thisRef = "(*this)",
    indent = spaces => spaces + spaces, // the templates nest by 2 spaces, C++ by 4
  )

  def renderScala(template: String, classPrefix: String): String = render(template, scalaLang(classPrefix))

  def renderCpp(template: String): String = render(template, cppLang)

  private def render(template: String, lang: TargetLang): String =
    template.split("\n", -1).toSeq.map(renderLine(_, lang)).mkString("\n")

  private def renderLine(rawLine: String, lang: TargetLang): String =
    val indentLength = rawLine.takeWhile(_ == ' ').length
    val line = rawLine.drop(indentLength)
    if (line.isEmpty) return ""
    val indent = lang.indent(rawLine.take(indentLength))

    def sub(text: String): String = substitute(text, lang)

    val rendered =
      if (line.startsWith("@")) {
        val content = line.drop(1)
        val eqIndex = content.indexOf('=')
        require(eqIndex > 0, s"malformed declaration line: $rawLine")
        val head = content.take(eqIndex).trim
        val rhs = content.drop(eqIndex + 1).trim

        val (name, typedCls) = head.split(':').map(_.trim.nn) match
          case Array(n) => (n, None)
          case Array(n, cls) => (n, Some(cls))
          case _ => throw new IllegalArgumentException(s"malformed declaration head: $head")

        if (rhs.startsWith("IF ")) {
          val thenIndex = rhs.indexOf(" THEN ")
          val elseIndex = rhs.indexOf(" ELSE ")
          require(thenIndex > 0 && elseIndex > thenIndex, s"malformed conditional: $rawLine")
          val cond = rhs.slice(3, thenIndex)
          val whenTrue = rhs.slice(thenIndex + 6, elseIndex)
          val whenFalse = rhs.drop(elseIndex + 6)
          lang.conditional(name, sub(cond), sub(whenTrue), sub(whenFalse))
        } else typedCls match
          case None => lang.declDouble(name) + sub(rhs) + lang.statementSuffix
          case Some(cls) => lang.declTyped(name, sub(cls)) + sub(rhs) + lang.statementSuffix
      }
      // comments are prose: only class-name placeholders are substituted, so formulas
      // like "x/sin(x)" are not rewritten into language syntax
      else if (line.startsWith("//")) classPlaceholder.replaceAllIn(line, m => Regex.quoteReplacement(lang.className(m.group(1).nn)))
      else if (line == "}" || line.endsWith("{")) sub(line)
      else sub(line) + lang.statementSuffix

    // multi-line renders (block conditionals) inherit the line's indentation
    indent + rendered.replace("\n", "\n" + indent)

  private def substitute(text: String, lang: TargetLang): String =
    var t = text
    t = t.replace("{this}", lang.thisRef)
    t = t.replace("diffOfProducts(", lang.diffOfProducts + "(")
    for (f <- mathFunctions) t = t.replaceAll(s"\\b$f\\(", lang.mathPrefix + f + "(").nn
    t = classPlaceholder.replaceAllIn(t, m => Regex.quoteReplacement(lang.className(m.group(1).nn)))
    for (p <- properties) t = t.replaceAll(s"\\.$p\\b(?!\\()", s".$p${lang.propertySuffix}").nn
    for (p <- bareProperties) t = t.replaceAll(s"(?<![.\\w])$p\\b(?!\\()", s"$p${lang.propertySuffix}").nn
    t = t.replace("(-this)", lang.minusThis)
    t
