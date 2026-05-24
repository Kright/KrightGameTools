# Symbolic Module

Simple implementation for AST (Abstract Syntax Tree) of mathematical expressions like `(1.0 + ("y" * "x"))`.

## Features

- Representation of expressions as `Symbolic[F, S]` where `F` is function type and `S` is symbol type.
- `SymbolicStr`: a predefined type for expressions with `String` functions and `Double | String` symbols.
- Support for basic simplifications and transformations.
- Pretty printing and ordering of expressions.
- Integration with `Numeric` trait for easy expression building using standard operators.

## Usage example

```scala
import me.kright.gametools.symbolic.SymbolicStr
import me.kright.gametools.symbolic.SymbolicStr.{given, *}

val x = SymbolicStr("x")
val y = SymbolicStr("y")
val expr = x * y + 1.0
println(expr) // ((x * y) + 1.0)
```
