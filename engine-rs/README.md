# aerofast-engine (Rust)

The AeroFast Pro calculation engine, ported from the Kotlin engine in
`app/src/main/java/com/aistudio/aerofast/trmst/data`. This is phase 1: the engine as a
standalone Rust library, proven to give the same answers as the Kotlin engine. The Android
app still uses the Kotlin engine; connecting the apps to this library (Android first,
iOS later) is phase 2.

## What is ported

| Rust module | Kotlin source |
|---|---|
| `catalog`, `gen_enums`, `gen_materials` | FastenerData.kt, ExtendedMaterials.kt, ResearchedMaterials.kt (tables generated) |
| `thread` | IsoThreadClass.kt, ThreadRollingBlank.kt |
| `tensile` | TensileRange.kt, MillCert.kt, JointGrip.kt |
| `steel` | SteelComposition.kt |
| `poka_yoke` | PokaYokeEngine.kt |
| `dimensions` | DimensionTables.kt |
| `bolt_forging` | BoltForging.kt |
| `part_geometry` | PartGeometry.kt |
| `heading` | HeadingProgression.kt |
| `soft_anneal` | SoftAnnealEngine.kt (incl. AnnealCycleEngine) |
| `heat_treat_cycle` | HeatTreatCycle.kt |
| `calculation` | CalculationEngine.kt |
| `tooling` | ToolingDesign.kt, ExtrusionDie.kt |
| `raw_material` | RawMaterialPlanner.kt |
| `production_report` | ProductionReport.kt |
| `quality` | QualityEngine.kt, QualityDocs.kt |
| `heading_animation` | HeadingAnimation.kt |
| `selection_filter` | SelectionFilter.kt |
| `drawing_scan` | DrawingScan.kt, ExtractedFastenerSpec.kt, ShopDefaults.kt |

Not ported, because they are not engine code: the AI services and JSON parsing
(GeminiAiService, AiProviders, MediaAi), QuestionParser, OfflineKnowledge, and the Android
storage classes (AppStateStore, AndroidPrefs, CustomMaterialStore).

## How sameness is proven

1. `desktop/src/test/.../golden/GoldenExport.kt` runs the **Kotlin** engine on 900 parts
   (every fastener type, material, property class, coating, thread class and size, plus
   drawing, tensile-range, mill-cert, wire and header-choice cases) and writes every input
   and result to `golden/cases.json.gz`. It also exports the catalogue data
   (`golden/enums.json`, `materials.json`, `objects.json`).
2. `codegen/gen.py` generates the Rust enum and material tables from that export, so the
   data cannot drift by hand-copying.
3. `tests/parity.rs` re-runs all 900 cases in Rust and compares every field: about
   544 000 values, numbers to 1e-9 relative and text exactly, including the plain-text
   production report, tooling sheet, PSW and ECN.
4. `tests/kotlin_tests_*.rs` are the Kotlin engine unit tests ported one for one
   (255 tests). These check the published-standard values (ISO 965, ISO 898-1, ASME B1.1,
   VDI 2230, AMS 2759, …), not just agreement with Kotlin.

```
cargo test --release
```

To refresh the golden data after a Kotlin engine change:

```
GOLDEN_DIR=$PWD/engine-rs/golden ./gradlew :desktop:test --tests '*GoldenExport*' --configure-on-demand
cd engine-rs && python3 -I codegen/gen.py && cargo test --release
```

## Notes

- Text formatting follows the JVM exactly (`%.Nf` half-up rounding, `Double.toString`,
  grouping separators), see `src/jfmt.rs`.
- `#![forbid(unsafe_code)]`. The release build uses LTO and strips symbols, which makes
  the compiled engine much harder to read back than the Kotlin bytecode in an APK.
- `quality::normal_inv` returns `None` outside 0 < p < 1 where Kotlin throws.
