## module for code generation for pga2d and pga3d

Single generator module for both PGA algebras. It computes every operation in symbolic form (using the `ga` and
`symbolic` modules with the `PGA2` / `PGA3` algebras) and searches the most narrow subclass of multivector for the
result, then emits the specialized Scala (and, for 3d, C++) classes.

Source layout under `src/main/scala/me/kright/gametools/pga/codegen/`:

* `common` - dimension- and language-agnostic helpers (code builders, file writing, multivector subclass model)
* `scala` - the Scala generators, as a sub-package:
  * `scala/common` - the Scala-generation framework shared by 2d and 3d: the framework wrappers
    (`MultivectorUnaryOp`, `CodeGenResult`, `OperationsReference`), the single concrete `ScalaMultivectorSubClass`
    class, the `ScalaPgaAlgebra` interface each dimension implements, and `scala/common/ops` - the op generators
    whose logic is identical across dimensions (naming/field-count differences only), written once against
    `ScalaPgaAlgebra`
  * `scala/pga3d` - the `Pga3dScalaAlgebra` object (class list, op list, naming for 3d), the op generators whose
    math genuinely differs per dimension or that are 3d-only, plus matrix conversions
  * `scala/pga2d` - the `Pga2dScalaAlgebra` object and its per-dimension op generators
* `cpp3d` - 3d C++ generation (result placed into ../cpp/pga3d)

Run it from the repository root. Generate everything (2d Scala, 3d Scala, then C++):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.runCodeGen"
```

Generate only the 3d Scala code (into ../pga3d):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.scala.pga3d.runScalaCodeGen"
```

Generate only the 2d Scala code (into ../pga2d):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.scala.pga2d.runScalaCodeGen"
```

Generate only the C++ code (into ../cpp/pga3d):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.cpp3d.runCppCodeGen"
```

### Check mode (dry run)

Generators can also run against a dry-run filesystem that writes nothing, instead comparing each generated file
to what is currently on disk and reporting how many / which files would change. This is useful for CI to detect
that checked-in generated code has drifted from the generator (someone hand-edited a generated file, or changed
the generator without re-running it):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.runCodeGenCheck"
```

It prints a summary (count and paths of files that would change, or "all ... up-to-date" if none would) and exits
with a NONZERO status if any file would change (zero when the tree is clean).

File output is abstracted behind the `GeneratedFileSystem` trait in `common` (implementations `RealFileSystem` and
`CheckFileSystem`); the three run functions `runScala3dCodeGen` / `runScala2dCodeGen` / `runCppCodeGen` take it as
a parameter, and the param-less `@main`s default to `RealFileSystem`.

Caveat: in check mode the C++ amalgamation (`CustomAmalgamate`) reads the current on-disk `cpp/pga3d` files rather
than freshly-generated content, so its result is accurate on a clean tree (the intended CI use) but is not a full
dry-run of amalgamation when the per-file C++ output has itself drifted.

### 2d vs 3d

The 2d algebra has basis (x, y, w) and 8 basis blades: scalar; x, y, w; xy, wx, wy; xyw.

* a grade-1 element is called a line instead of a plane (Pga2dLine, Pga2dLineIdeal), because in 2d a hyperplane is a line
* the analog of Pga3dRotor is Pga2dRotor with just 2 fields (cos and sin of the half-angle)
* there is no bivector family (Pga3dBivector, Pga3dBivectorBulk, Pga3dBivectorWeight): in 2d the grade-2 elements
  are the point family (Pga2dProjectivePoint, Pga2dPoint, Pga2dVector). exp and log are defined on them directly
* 2d motors have no pseudoscalar part, so motor renormalization is a uniform scale
  and exp of a grade-2 element has no additional correction term
* 3d-specific operations are not generated for 2d: bivector split, quaternion/motor axes, projection of quaternion
  to rotation in plane
* C++ generation exists only for 3d
* matrix conversions (ScalaMatrixCodeGen) exist only for 3d; the 2d analog is future work

### Notes on the merged generators

* This module was formed by merging the former `pga3dCodeGen` and `pga2dCodeGen` modules.
* The two former per-dimension `ScalaMultivectorSubClass` class bodies were byte-identical: every difference
  between the algebras lived in their companion objects (class lists, op lists, naming, doc strings). Because
  there was nothing different in the class body to subclass, it collapses into a **single concrete class** in
  `scala/common`, injected with a `ScalaPgaAlgebra` instance (`Pga3dScalaAlgebra` / `Pga2dScalaAlgebra`) - not a
  base class with per-dimension subclasses. All per-dimension variation lives in the two algebra objects.
* Given that interface, the op generators split into two groups:
  * Shared once, in `scala/common/ops`, against `ScalaPgaAlgebra`: the 15 ops whose 2d/3d difference is naming or
    field count only (`DefToString`, `DefNorm`, `DefMotorToRotorAndTranslator`, `DefDivideByScalar`,
    `DefMultiplyToScalar`, `DefVariablesComponentsCount`, `DefZeroObjectMethods`, `DefConstAndDualFields`,
    `DefDistanceToPoint`, `DefObjectMethodsForMotor`, `DefObjectMethodsForTranslator`,
    `DefMinMaxForPointOrVector`, `DefPlusMinusMadd`, `DefConvertTo`, `DefMethodsIfAnyPoint`), plus the framework
    wrappers (`MultivectorUnaryOp`, `CodeGenResult`, `OperationsReference`) and the base class itself.
  * Kept per-dimension, in `scala/pga3d/ops` and `scala/pga2d/ops`: ops whose math genuinely differs -
    `DefExpForBivector`, `DefInterpolation`, `DefLogForMotor`, `DefObjectMethodsForRotor`, `DefProjection`,
    `DefRenormalizedForMotor` (both dimensions, different formulas) - plus the 3d-only ops (`DefBivectorSplit`,
    `DefMotorAndRotorAxices`, `DefRotorProjectToRotationInPlane`) and matrix generation (`ScalaMatrixCodeGen`).
* Generated-file header comments embed the real, current generator FQCNs (e.g.
  "Generated by pgaNdCodeGen (me.kright.gametools.pga.codegen.scala.pga3d.runScalaCodeGen)").
