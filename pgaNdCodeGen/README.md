## module for code generation for pga2d and pga3d

Single generator module for both PGA algebras. It computes every operation in symbolic form (using the `ga` and
`symbolic` modules with the `PGA2` / `PGA3` algebras) and searches the most narrow subclass of multivector for the
result, then emits the specialized Scala (and, for 3d, C++) classes.

Source layout under `src/main/scala/me/kright/gametools/pga/codegen/`:

* `common` - dimension- and language-agnostic core: the multivector subclass model, code builders and file
  writing, plus the single sources of truth shared by all backends:
  * `PgaSubclassFields` - derives the field structure of every generated class from a PGA instance of any
    dimension (the algebra objects and the C++ class list only add names and descriptions)
  * `NormSymbolics` / `AxesSymbolics` - the symbolic derivations of the norm families and the rotor axes
  * `SharedFormulas` + `FormulaTemplate` - the hand-written numerical methods (rotation, log, exp, split,
    renormalized, projectToRotationInPlane) as language-neutral templates plus symbolic result builders,
    rendered into Scala and C++ by `FormulaTemplate`
* `scalagen` - the Scala generators, as a sub-package (named to avoid shadowing the root `scala` package):
  * `scalagen/common` - the Scala-generation framework shared by 2d and 3d: the framework wrappers
    (`MultivectorUnaryOp`, `CodeGenResult`, `OperationsReference`), the single concrete `ScalaMultivectorSubClass`
    class, the `ScalaPgaAlgebra` interface each dimension implements, and `scalagen/common/ops` - the op generators
    whose logic is identical across dimensions (naming/field-count differences only), written once against
    `ScalaPgaAlgebra`
  * `scalagen/pga3d` - the `Pga3dScalaAlgebra` object (class list, op list, naming for 3d), the op generators whose
    math genuinely differs per dimension or that are 3d-only, plus matrix conversions
  * `scalagen/pga2d` - the `Pga2dScalaAlgebra` object and its per-dimension op generators
* `cpp3d` - 3d C++ generation (result placed into ../cpp/pga3d)

Run it from the repository root. Generate everything (2d Scala, 3d Scala, then C++):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.runCodeGen"
```

Generate only the 3d Scala code (into ../pga3d):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.scalagen.pga3d.runScalaCodeGen"
```

Generate only the 2d Scala code (into ../pga2d):

```bash
sbt "pgaNdCodeGen/runMain me.kright.gametools.pga.codegen.scalagen.pga2d.runScalaCodeGen"
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

CI runs this check on every push/PR (see `.github/workflows/test.yml`), so a change to the generator must be
committed together with the regenerated output (and vice versa - generated files must not be edited by hand).

Note: this module is deliberately excluded from the root sbt aggregate - it is a development-time code
generator, not part of the published gametools library, so `sbt test`/`sbt compile` at the root do not touch
it. The dedicated CI step above is what keeps it compiling.

File output is abstracted behind the `GeneratedFileSystem` trait in `common` (implementations `RealFileSystem` and
`CheckFileSystem`); the three run functions `runScala3dCodeGen` / `runScala2dCodeGen` / `runCppCodeGen` take it as
a parameter, and the param-less `@main`s default to `RealFileSystem`.

Caveat: in check mode the C++ amalgamation (`CustomAmalgamate`) reads the current on-disk `cpp/pga3d` files rather
than freshly-generated content, so its result is accurate on a clean tree (the intended CI use) but is not a full
dry-run of amalgamation when the per-file C++ output has itself drifted.

### 2d vs 3d

The 2d algebra has basis (x, y, w) and 8 basis blades: scalar; x, y, w; xy, wx, wy; xyw.

* a grade-1 element is called a line instead of a plane (Pga2dLine, Pga2dLineCentral), because in 2d a hyperplane is a line
* the analog of Pga3dRotor is Pga2dRotor with just 2 fields (cos and sin of the half-angle)
* there is no bivector family (Pga3dBivector, Pga3dBivectorBulk, Pga3dBivectorWeight): in 2d the grade-2 elements
  are the point family (Pga2dProjectivePoint, Pga2dPoint, Pga2dVector). exp and log are defined on them directly
* 2d motors have no pseudoscalar part, so motor renormalization is a uniform scale
  and exp of a grade-2 element has no additional correction term
* 3d-specific operations are not generated for 2d: bivector split, projection of a rotor to rotation in plane
* C++ generation exists only for 3d
* matrix conversions (Pga3dMatrix / Pga2dMatrix) are generated for both dimensions by the shared ScalaMatrixCodeGen

### Architecture: single sources of truth

Every piece of math exists exactly once; the per-language generators only render it:

1. **Class structure** comes from `common/PgaSubclassFields`: field lists are derived from the PGA blades
   (even grades for the motor, the dual grade-(n-1) fields for points, and so on). `Pga3dScalaAlgebra`,
   `Pga2dScalaAlgebra` and `cpp3d/CppSubclasses` instantiate it and attach names, descriptions and op lists.
2. **Generated operations** (products, conversions, norms, axes...) are computed symbolically with the `ga`
   module and rendered per class pair; the norm and axis derivations live in `common/NormSymbolics` and
   `common/AxesSymbolics`.
3. **Hand-written numerical methods** live in `common/SharedFormulas` as templates in a small line dialect
   (`@name = expr` locals, `@x = IF c THEN a ELSE b` conditionals, bare `sqrt`/`sin`/`asin` math calls,
   `{Rotor}`-style class placeholders, `{this}` and `::` for language-specific spellings) plus symbolic
   builders for the result constructors. `common/FormulaTemplate.renderScala/renderCpp` turn one template
   into both backends, so a numerical fix cannot land in one language and be forgotten in the other.
   Comment lines are treated as prose and never rewritten.
4. **C++ generation is two-phase**: each `cpp3d` op generator implements `generateStructBody` (the
   declarations placed inside the struct in `<Class>.h`) and `generateFiles` (the definitions emitted into
   an `ops*.h` header); `CustomAmalgamate` then fuses everything into `cpp/fused/*.h` following the
   include directives.

### How to add a new operation

1. Decide where the math lives:
   * identical for 2d and 3d up to naming - write one `Def*` op in `scalagen/common/ops` against the
     `ScalaPgaAlgebra` interface;
   * genuinely dimension-specific - write it in `scalagen/pga2d/ops` and/or `scalagen/pga3d/ops`;
   * a hand-written numerical formula that must also exist in C++ - add a template and a symbolic result
     builder to `common/SharedFormulas`, and render it from both the Scala op and the C++ generator.
2. Register the op in the `unaryOperations` / `binaryOperations` list of each algebra object
   (`Pga3dScalaAlgebra`, `Pga2dScalaAlgebra`); for C++ add or extend a generator in `cpp3d/ops` and list it
   in `Pga3dCodeGenCpp`.
3. Regenerate (`runCodeGen`), review the diff of the generated files, and commit the generator change
   together with the regenerated output - CI runs `runCodeGenCheck` and fails on any drift.
4. Add tests in the pga2d/pga3d test suites (property-based where possible; `PrecisionTest` for anything
   with branches, series or cancellation-prone arithmetic).

### Code conventions

* Generated files are never edited by hand; the header comment of every file names the generator to change.
* `Seq(...)` literals are created as `ArraySeq(...)` (array-backed, cache-friendly); plain `Seq` stays as
  the interface type. Where iteration order drives emission order, use `ListSet`/`ArraySeq`, never `HashSet`.
