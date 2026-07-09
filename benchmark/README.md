# Benchmark subproject

JMH microbenchmarks for the library. Currently it holds the **FlatArray-vs-boxed-`Array` rotor
benchmark** (`FlatArrayRotorBenchmark`): applying a unit `Pga3dRotor` sandwich to every point stored in
a `FlatArray[Pga3dPoint]` versus a `scala.Array[Pga3dPoint]` of boxed objects, both sequential and with
references shuffled. It is JVM-only and not published (`publish / skip := true`).

## Running

Full run (all benchmarks, default warmups/iterations/forks):

```
sbt "benchmark/Jmh/run"
```

Quicker / filtered runs pass standard JMH options after `run`:

```
# fewer warmups (-wi) and measured iterations (-i), single fork (-f1)
sbt "benchmark/Jmh/run -wi 3 -i 3 -f1 .*"

# only the write-back benchmarks, one problem size
sbt "benchmark/Jmh/run -p size=500000 .*Write"

# sweep several sizes
sbt "benchmark/Jmh/run -p size=500000,2000000 .*"

# smallest wiring smoke test
sbt "benchmark/Jmh/run -wi 1 -i 1 -f1 -p size=500000 .*"
```

The trailing argument is a regex over benchmark method names (`.*` = all).

## Garbage collector

The default JVM collector is used unless you override it. To compare collectors or inspect GC, append
JVM args:

```
sbt "benchmark/Jmh/run -jvmArgsAppend -XX:+UseZGC .*"
sbt "benchmark/Jmh/run -jvmArgsAppend -Xlog:gc .*"
```

## Recorded results

For the measured numbers, the run conditions (machine, JDK, collectors), the methodology and its
caveats, and the conclusions, see [../flatarray/performance.md](../flatarray/performance.md).
