package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.ops.TranslatorWithRotorGenerator.{rotorWithTranslator, translatorWithRotor}
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}
import scala.collection.immutable.ArraySeq

class TranslatorWithRotorGenerator extends CppCodeGenerator {
  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(
      ArraySeq(
        s"#include \"${CppSubclasses.motor.name}.h\"",
        s"#include \"${CppSubclasses.rotor.name}.h\"",
        s"#include \"${CppSubclasses.translator.name}.h\"",
      ),
      code.generatorName(this))


    val impl = CppCodeBuilder()

    code.namespace(codeGen.namespace) {
      for (translatorFirst <- ArraySeq(true, false)) {
        val structName = if (translatorFirst) translatorWithRotor else rotorWithTranslator
        val otherName = if (translatorFirst) rotorWithTranslator else translatorWithRotor

        code("")
        code.struct(structName) {
          val fieldClasses =
            if (translatorFirst) ArraySeq(CppSubclasses.translator, CppSubclasses.rotor)
            else ArraySeq(CppSubclasses.rotor, CppSubclasses.translator)

          for (field <- fieldClasses) {
            code(s"${field.name} ${field.name.toLowerCase}{};")
          }

          code("")
          code(s"static size_t constexpr componentsCount = ${CppSubclasses.rotor.name}::componentsCount + ${CppSubclasses.translator.name}::componentsCount;")

          code("")
          code(s"[[nodiscard]] constexpr std::array<double, componentsCount> toArray() const noexcept {")
          code.block {
            code("// a compiler will optimize this")
            if (translatorFirst) {
              code(s"return { translator.toArray()[0], translator.toArray()[1], translator.toArray()[2], rotor.toArray()[0], rotor.toArray()[1], rotor.toArray()[2], rotor.toArray()[3] };")
            } else {
              code(s"return { rotor.toArray()[0], rotor.toArray()[1], rotor.toArray()[2], rotor.toArray()[3], translator.toArray()[0], translator.toArray()[1], translator.toArray()[2] };")
            }
          }
          code("}")

          code("")
          code(s"[[nodiscard]] static constexpr ${structName} from(const std::span<double, componentsCount>& values) noexcept {")
          code.block {
            code("return {")
            code.block {
              if (translatorFirst) {
                code(
                  """.translator = Translator::from(values.first<Translator::componentsCount>()),
                    |.rotor = Rotor::from(values.last<Rotor::componentsCount>())""".stripMargin
                )
              } else {
                code(
                  """.rotor = Rotor::from(values.first<Rotor::componentsCount>()),
                    |.translator = Translator::from(values.last<Translator::componentsCount>())""".stripMargin
                )
              }
            }
            code("};")
          }
          code("}")

          code("")
          code(s"[[nodiscard]] constexpr ${CppSubclasses.motor.name} to${CppSubclasses.motor.name}() const noexcept { return ${fieldClasses.head.name.toLowerCase}.geometric(${fieldClasses.last.name.toLowerCase}); }")

          code("")
          code(s"[[nodiscard]] constexpr ${if (translatorFirst) rotorWithTranslator else translatorWithRotor} reversed() const noexcept;")
          impl(s"[[nodiscard]] constexpr ${otherName} ${structName}::reversed() const noexcept { return { ${
            fieldClasses.reverse.map(f => s".${f.name.toLowerCase()} = ${f.name.toLowerCase()}.reversed()").mkString(", ")
          } }; }")

          code("")
          code(s"[[nodiscard]] constexpr $otherName to${otherName}() const noexcept;")
          impl(s"[[nodiscard]] constexpr ${otherName} ${structName}::to${otherName}() const noexcept { return ${
            if (translatorFirst) s"{ .rotor = rotor, .translator = rotor.reversed().sandwich(translator).toTranslator() }"
            else s"{ .translator = rotor.sandwich(translator).toTranslator(), .rotor = rotor }"
          }; };")


          code("")
          code(s"[[nodiscard]] static constexpr $structName id() noexcept { ${
            fieldClasses.map(f => s".${f.name.toLowerCase()} = ${s"${f.name}::id()"}")
              .mkString("return { ", ", ", " };")
          } }")
        }
      }

      code("")
      code(impl.toString)
    }

    ArraySeq(FileContent(codeGen.directory.resolve(translatorWithRotor + ".h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls != CppSubclasses.motor) return ArraySeq()

    val code = CppCodeBuilder()


    structBodyPart(code.toString)
  }
}

object TranslatorWithRotorGenerator:
  val translatorWithRotor = s"${CppSubclasses.translator.name}With${CppSubclasses.rotor.name}"
  val rotorWithTranslator = s"${CppSubclasses.rotor.name}With${CppSubclasses.translator.name}"
