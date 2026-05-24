package me.kright.gametools.pga3d.codegen.cpp.ops

import me.kright.gametools.pga3d.codegen.common.FileContent
import me.kright.gametools.pga3d.codegen.cpp.ops.TranslatorWithQuaternionGenerator.{quaternionWithTranslator, translatorWithQuaternion}
import me.kright.gametools.pga3d.codegen.cpp.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp, StructBodyPart}

class TranslatorWithQuaternionGenerator extends CppCodeGenerator {
  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(
      Seq(
        s"#include \"${CppSubclasses.motor.name}.h\"",
        s"#include \"${CppSubclasses.quaternion.name}.h\"",
        s"#include \"${CppSubclasses.translator.name}.h\"",
      ),
      getClass.getName)


    val impl = CppCodeBuilder()

    code.namespace(codeGen.namespace) {
      for (translatorFirst <- Seq(true, false)) {
        val structName = if (translatorFirst) translatorWithQuaternion else quaternionWithTranslator
        val otherName = if (translatorFirst) quaternionWithTranslator else translatorWithQuaternion

        code("")
        code.struct(structName) {
          val fieldClasses =
            if (translatorFirst) Seq(CppSubclasses.translator, CppSubclasses.quaternion)
            else Seq(CppSubclasses.quaternion, CppSubclasses.translator)

          for (field <- fieldClasses) {
            code(s"${field.name} ${field.name.toLowerCase}{};")
          }

          code("")
          code(s"static size_t constexpr componentsCount = ${CppSubclasses.quaternion.name}::componentsCount + ${CppSubclasses.translator.name}::componentsCount;")

          code("")
          code(s"[[nodiscard]] constexpr std::array<double, componentsCount> toArray() const noexcept {")
          code.block {
            code("// a compiler will optimize this")
            if (translatorFirst) {
              code(s"return { translator.toArray()[0], translator.toArray()[1], translator.toArray()[2], quaternion.toArray()[0], quaternion.toArray()[1], quaternion.toArray()[2], quaternion.toArray()[3] };")
            } else {
              code(s"return { quaternion.toArray()[0], quaternion.toArray()[1], quaternion.toArray()[2], quaternion.toArray()[3], translator.toArray()[0], translator.toArray()[1], translator.toArray()[2] };")
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
                    |.quaternion = Quaternion::from(values.last<Quaternion::componentsCount>())""".stripMargin
                )
              } else {
                code(
                  """.quaternion = Quaternion::from(values.first<Quaternion::componentsCount>()),
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
          code(s"[[nodiscard]] constexpr ${if (translatorFirst) quaternionWithTranslator else translatorWithQuaternion} reversed() const noexcept;")
          impl(s"[[nodiscard]] constexpr ${otherName} ${structName}::reversed() const noexcept { return { ${
            fieldClasses.reverse.map(f => s".${f.name.toLowerCase()} = ${f.name.toLowerCase()}.reversed()").mkString(", ")
          } }; }")

          code("")
          code(s"[[nodiscard]] constexpr $otherName to${otherName}() const noexcept;")
          impl(s"[[nodiscard]] constexpr ${otherName} ${structName}::to${otherName}() const noexcept { return ${
            if (translatorFirst) s"{ .quaternion = quaternion, .translator = quaternion.reversed().sandwich(translator).toTranslator() }"
            else s"{ .translator = quaternion.sandwich(translator).toTranslator(), .quaternion = quaternion }"
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

    Seq(FileContent(codeGen.directory.resolve(translatorWithQuaternion + ".h"), code.toString))
  }

  override def generateStructBody(cls: CppSubclass): Seq[StructBodyPart] = {
    if (cls != CppSubclasses.motor) return Seq()

    val code = CppCodeBuilder()


    structBodyPart(code.toString)
  }
}

object TranslatorWithQuaternionGenerator:
  val translatorWithQuaternion = s"${CppSubclasses.translator.name}With${CppSubclasses.quaternion.name}"
  val quaternionWithTranslator = s"${CppSubclasses.quaternion.name}With${CppSubclasses.translator.name}"
