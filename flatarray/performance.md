# FlatArray performance

Why `flatarray` exists, in numbers: storing many small all-`Double` structs in one `Array[Double]`
(`FlatArray[T]`) instead of a `scala.Array[T]` of boxed immutable case-class objects gives **zero
allocation, zero GC, and roughly constant ns/element**, while the boxed array is several times slower
and — for scattered access under ZGC — up to ~30x slower.

## Setup

- **Machine:** AMD Ryzen 9 5950X (a single thread sees the full 32 MB L3).
- **JVM:** OpenJDK 25.0.3, measured under both G1 (default) and generational ZGC (`-XX:+UseZGC`),
  `-Xmx4g -Xms1g`.
- **Element:** `Pga3dPoint` = 3 `Double`s. In `FlatArray` that is **24 B/element** packed; as a boxed
  object it is ~40 B (16 B header + 24 B payload), **plus** the reference in the array (4 B compressed
  under G1, 8 B under ZGC, which has no compressed oops).
- **Workload:** apply a unit `Pga3dRotor` sandwich to each point (`rotor.sandwich(p).toPointUnsafe`),
  ~a few dozen flops/element.
- **Three storage variants:**
  1. `FlatArray[Pga3dPoint]` — `mapInPlace`, mutates the doubles in place.
  2. `Array[Pga3dPoint]` **sequential** — objects allocated in iteration order (heap order matches
     index order).
  3. `Array[Pga3dPoint]` **shuffled** — same objects, references permuted (fixed seed), so iteration
     hits random heap addresses.

## Methodology and its caveats

This is a hand-rolled harness (`System.nanoTime` around a batch of passes, thorough warmup to trigger
C2, several timed rounds reported as min/median/max, variant order reshuffled per round), **not JMH** —
run-to-run spread is a few percent, so treat differences under ~10% as noise. A `@volatile` checksum of
the results is accumulated so nothing is dead-code-eliminated. Two independent JVM runs are shown as
`run1 / run2` where it matters. (A reproducible JMH version lives in the `benchmark` project — see
[How to re-run](#how-to-re-run).)

**The master-recopy trap.** For the boxed write-back variants, `arr(i) = f(arr(i))` allocates a *new*
`Pga3dPoint` per element (immutable case class; storing it into the array makes it escape — this
allocation is inherent and part of the honest comparison, not something to optimize away). But that
means after one in-place pass the array holds freshly TLAB-allocated objects **in iteration order** —
the shuffled variant would silently become sequential from pass 2 on. To keep the layout honest, an
immutable master array (sequential or shuffled) is kept, and its references are `System.arraycopy`-ed
back into the working array before **each** pass (outside the timed region). Reads then always hit the
master's layout; writes allocate throwaway objects that are discarded on the next re-copy. `FlatArray`
needs no such reset — it mutates doubles in place and allocates nothing.

## Results

Numbers are **ns/element** (median), lower is better. `B/elem` is measured allocation
(`ThreadMXBean.getThreadAllocatedBytes`).

### Write-back, scaling — G1

| N    | FlatArray | Array seq   | Array shuf  | B/elem (flat / boxed) | GCs (boxed) |
|------|-----------|-------------|-------------|-----------------------|-------------|
| 0.5M | **1.20 / 1.24** | 5.85 / 5.99 | 5.94 / 6.11 | 0.0 / 40.0 | ~2 |
| 2M   | **1.46 / 1.51** | 6.62 / 6.65 | 6.56 / 6.59 | 0.0 / 40.0 | ~3 |
| 8M   | **1.68 / 1.64** | 6.98 / 8.41 | 6.88 / 7.44 | 0.0 / 40.0 | 5–9 |

`FlatArray` triggers **0 GCs** at every size. The boxed variants are 4–6x slower and their GC load
grows with N.

### Write-back, scaling — generational ZGC

| N    | FlatArray | Array seq | Array shuf | B/elem (boxed) | Alloc stalls |
|------|-----------|-----------|------------|----------------|--------------|
| 0.5M | **1.39** | 4.6 (noisy) | 30.2 | 40.0 | 0 |
| 2M   | **1.61** | 4.74 | 42.0 | 40.0 | 0 |
| 8M   | **1.66** | 6.63 | 50.2 | 40.0 | 0 |

ZGC delivers its low-pause promise (below) and never stalls an allocating thread, but it has **no
compressed oops and a load barrier on every reference read**. `FlatArray` reads raw doubles — no
references, no barrier — so it is unchanged. **Shuffled** boxed reads are catastrophic: every scattered
`arr(i)` load pays the colored-pointer barrier on top of a cache miss → ~30–50 ns/element, up to ~30x
`FlatArray`.

### Read-only locality (rotate + accumulate, no writes, no allocation), N = 0.5M

| GC   | FlatArray | Array seq | Array shuf |
|------|-----------|-----------|------------|
| G1   | **1.59**  | 1.74–1.86 | 10.2–10.5  |
| ZGC  | **1.60**  | 1.87      | 9.2        |

With allocation removed entirely, scattered access is still ~6–7x slower than contiguous — pure read
locality, independent of GC.

### GC pause statistics (over a full multi-size run)

| Collector | STW pauses | min / median / max | total STW | concurrent work |
|-----------|-----------|--------------------|-----------|-----------------|
| G1        | 78 | 0.85 / 9.9 / 147 ms | ~1.4 s | — |
| ZGC       | 179 phases | 3 / 11 / 28 **µs** | ~0 ms | Minor 35 cyc / 5.9 s + Major 13 cyc / 6.8 s (concurrent) |

G1's pauses are milliseconds and driven entirely by the boxed variants' churn. ZGC's pauses are
microseconds; its GC cost is instead concurrent background wall-time (does not stop the app).
`FlatArray` contributes nothing to either.

*Caveat:* a copying/relocating GC can move objects and partially heal (or reshuffle) the boxed layout
over a long run, so absolute boxed numbers drift a little between runs; the qualitative gap does not.

## Secondary experiment: does manual loop unrolling help?

A scratch `mapInPlaceUnrolled(unroll: 2 | 4 | 8)` (read N elements into locals, apply f to all, write
all back, plain-loop tail) was compared against the plain inline `mapInPlace` (ns/element, median):

| variant  | rotor (compute-bound) | add-vector (memory-bound) |
|----------|-----------------------|----------------------------|
| baseline | **1.24 / 1.22** | **0.70 / 0.67** |
| unroll 2 | 1.31 / 1.29 | 0.70 / 0.67 |
| unroll 4 | 1.49 / 1.48 | 0.69 / 0.67 |
| unroll 8 | 1.74 / 1.70 | 1.00 / 0.97 |

**Conclusion: manual unrolling does not help — it hurts.** The compute-bound case gets monotonically
slower (batching 4–8 `Pga3dPoint` temporaries plus rotor intermediates blows up register pressure and
spills); the memory-bound case is flat for 2/4 (bandwidth-limited) and worse at 8. The plain inline
loop already collapses to scalar-replaced raw array reads/writes, and C2 + the CPU's out-of-order
engine already extract the available ILP. Keep the simple loop.

## Conclusions

1. **Layout > allocation-order > collector choice.** The biggest lever is contiguous vs scattered reads
   (~6–7x, even with GC removed); the second is flat vs boxed allocation/GC pressure. `FlatArray` wins
   both by construction.
2. **`FlatArray` is ~constant and free:** ~1.2–1.7 ns/element from 0.5M to 8M, **0 bytes allocated, 0
   GCs**, on both collectors.
3. **On the ZGC deployment target the gap widens.** ZGC's load barrier turns any non-sequential boxed
   access into a cliff (up to ~30x); flat storage sidesteps it entirely.
4. **The inline hot path is already optimal** — manual unrolling only adds register pressure.

## How to re-run

The `benchmark` project (JMH, JVM-only, not published) reproduces the write-back comparison:

```
sbt "benchmark/Jmh/run -wi 5 -i 5 -f1 -p size=500000 .*"
```

Add `-p size=2000000` (or a comma-separated list) to sweep sizes, and
`-jvmArgsAppend -XX:+UseZGC` / `-jvmArgsAppend '-Xlog:gc'` to compare collectors or inspect GC. A quick
wiring smoke test is `-wi 1 -i 1 -f1`. The tables above come from the ad-hoc `System.nanoTime` harness
described in [Methodology](#methodology-and-its-caveats), not from JMH, so exact numbers will differ
slightly; the relationships hold.
