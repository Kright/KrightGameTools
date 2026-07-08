package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.FileContent
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppCodeGenerator, CppSubclass, CppSubclasses, Pga3dCodeGenCpp}

class ToStreamOpGenerator extends CppCodeGenerator {

  override def generateFiles(codeGen: Pga3dCodeGenCpp): Seq[FileContent] = {
    val code = CppCodeBuilder()

    code.myHeader(Seq("#include <ostream>"), code.generatorName(this))

    code.namespace(codeGen.namespace) {
      for (cls <- CppSubclasses.all if cls.shouldBeGenerated) {
        code(s"inline std::ostream &operator<<(std::ostream &os, const ${cls.name} &v) {")
        code.block {
          if (cls.variableFields.nonEmpty) {
            code(s"return os << \"${cls.name}{\" << ${cls.variableFields.map(f => s"\".${f.name} = \" << v.${f.name}").mkString(" << \", \"")} << \"}\";")
          } else {
            code(s"return os << \"${cls.name}{}\";")
          }
        }
        code.apply("}")
      }

      import TranslatorWithQuaternionGenerator.quaternionWithTranslator as qt
      import TranslatorWithQuaternionGenerator.translatorWithQuaternion as tq
      val q = CppSubclasses.quaternion.name.toLowerCase
      val t = CppSubclasses.translator.name.toLowerCase

      code(
        s"""inline std::ostream &operator<<(std::ostream &os, const ${qt} &v) {
           |   return os << "${qt}{" << v.${q} << ", " << v.${t} << "}";
           |}
           |inline std::ostream &operator<<(std::ostream &os, const ${tq} &v) {
           |    return os << "${tq}{" << v.${t} << ", " << v.${q} << "}";
           |}
           |""".stripMargin)
      code(s"""""")
    }

    Seq(FileContent(codeGen.directory.resolve("opsStr.h"), code.toString))
  }
}
