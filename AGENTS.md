# AGENTS.md — KDouble (karoo-dual)

## Project Overview

KDouble is a **Karoo cycling computer extension** (`com.enderthor.kCustomField`) built on the Hammerhead `karoo-ext` SDK. It registers custom data fields (double, rolling, smart climb, bell) that appear on the Karoo ride screen. The app has two surfaces:
- **Extension service** (`KarooCustomFieldExtension`): runs on the Karoo, renders fields as `RemoteViews` via Glance.
- **Config activity** (`MainActivity` → `TabLayout`): Compose UI for the user to configure each field slot.

## Architecture

```
KarooCustomFieldExtension (KarooExtension)
  └─ karooSystem: KarooSystemService        ← connects to Karoo sensor bus
  └─ types: List<DataTypeImpl>
       ├─ CustomDoubleType (×6)             ← horizontal/vertical dual metrics
       ├─ CustomRollingType (×3)            ← cycles between up to 3 metrics
       ├─ CustomClimbType (×1)              ← smart climb composite field
       └─ BellActionDataType (×1)           ← tap-to-beep action field
```

Every `DataTypeImpl` implements `startView(context, config, emitter)`:
1. Opens a `CoroutineScope(Dispatchers.IO + Job())`.
2. Combines `context.streamXxxSettings()` + `karooSystem.streamDataFlow(typeId)` into a config state.
3. Calls `GlanceRemoteViews.compose()` to produce `RemoteViews`, then `emitter.updateView(remoteViews)`.
4. Registers cleanup via `emitter.setCancellable { scope.cancel() }` — **preview mode skips cancellation** (see comments in `CustomDoubleTypeBase`).

## Data Flow

```
DataStore ("settings")           KarooSystemService
   streamXxxSettings()              streamDataFlow(typeId)   streamUserProfile()
          │                                │                         │
          └────────────── combine() ───────┘─────────────────────────┘
                                   │
                             GlobalConfigState
                                   │
                        GlanceRemoteViews.compose()
                                   │
                          emitter.updateView(remoteViews)
```

`KarooSystemService.streamDataFlow()` wraps `addConsumer(OnStreamState.StartStreaming)` as a `callbackFlow` — see `Extensions.kt`.

## Settings / Persistence

All config lives in `DataStore<Preferences>` named `"settings"` (defined as `Context.dataStore`):

| Key | Type | Default source |
|-----|------|---------------|
| `generalsettings` | `GeneralSettings` | `defaultGeneralSettings` |
| `doublefieldsettings` | `List<DoubleFieldSettings>` (6 entries, indices 0–5) | `defaultDoubleFieldSettings` |
| `onefieldsettings` | `List<OneFieldSettings>` (3 entries) | `defaultOneFieldSettings` |
| `climbfieldsettings` | `List<ClimbFieldSettings>` | `defaultClimbFieldSettings` |
| `wprimebalancesettings` | `WPrimeBalanceSettings` | `defaultWPrimeBalanceSettings` |

**Always use `jsonWithUnknownKeys`** (defined in `Extensions.kt`) for deserialization to survive schema evolution across versions.

Defaults are pre-encoded JSON strings in `Configdata.kt` (e.g. `val defaultGeneralSettings = Json.encodeToString(GeneralSettings())`).

## Key Patterns

- **Sticky stream state**: `StickyStreamState.process()` in `DataTypeFunctions.kt` caches the last valid `StreamState.Streaming` for 7 seconds to survive brief sensor dropout.
- **Cancellation guard**: `@Volatile private var isCancelled` + global `ViewState.setCancelled()` — check before every `emitter.updateView()` call.
- **Hardware throttle**: `karooSystem.hardwareType == HardwareType.K2` → use `RefreshTime.MID` (800 ms) instead of `RefreshTime.HALF` (200 ms). Always coerce refresh to `≥ 100L`.
- **Retry pattern**: `.retryWhen { cause, attempt -> delay(…); true }` — max 4 retries with `RETRY_SHORT`/`RETRY_LONG` delays from `Configdata.kt`.
- **Synthetic streams**: `WPRIME_BALANCE`, `VO2MAX`, `FTPG` are computed locally inside `getFieldFlow()`, not streamed from Karoo sensors. State for W′ is held in `WPrimeBalanceState` singleton.
- **External headwind**: Guarded by `generalSettings.isheadwindenabled`; reads from extension id `karoo-headwind`. If disabled, replaced by `flowOf(StreamHeadWindData(0.0, 0.0))`.
- **Extension-field pickers**: the `Dropdown*Field` composables in `TabFunctions.kt` build options from `KarooAction.entries`, `.sortedBy { it.name }` (alphabetical), then filter by `generalSettings` toggles — `isheadwindenabled` (HEADWIND), `iskpowerenabled` (`::kpower::`), `iskghostenabled` (`::kghost::`). KPower/KGhost default ON and filtering is **non-destructive** (the currently-selected action is re-added so it stays visible; no reset). HEADWIND is the exception: it resets to `SPEED` when disabled (a headwind field is broken without the extension).
- **L/R dual fields** (`powerField = true`): rendered as a single `left/right` pair. `multipleStreamValues()` in `DataTypeFunctions.kt` reads the pair via `MultiFields` (in `Configdata.kt`): two field keys from one `DataPoint`, or `onlyfirst = true` to derive `right = 100 − left` from a single value (native pedal balance). The view shows `--/--` when the stream is not `Streaming`. (KPower no longer publishes balance/dynamics streams — the Karoo shows those natively for a paired meter — so the only KPower fields consumed here are single-value power streams.)

## Adding a New Metric

1. Add an entry to `KarooAction` enum in `Configdata.kt` — provide `action` (DataType ID), `label`, `icon`, `colorday/night`, `zone`, `convert`, and optionally `powerField = true` for left/right dual values. For a `powerField` metric also add a `MultiFields` entry (the two field keys, or `onlyfirst = true` to derive `right = 100 − left`). KPower/KGhost fields (`::kpower::` / `::kghost::` action IDs) are auto-grouped and gated by the `iskpowerenabled` / `iskghostenabled` toggles — no extra wiring needed.
2. Register in `app/src/main/res/xml/extension_info.xml` with a new `typeId`.
3. Add instance to `KarooCustomFieldExtension.types` list.
4. If the metric is computed (not a native Karoo stream), add a branch in `KarooSystemService.getFieldFlow()` in `DataTypeFunctions.kt`.

## Build & Deploy

```bash
# Build release APK
./gradlew assembleRelease
# Output: app/release/app-release.apk

# Sideload to Karoo 2
adb install app/release/app-release.apk

# Karoo 3: share APK link via companion app (no ADB needed)
```

**Mandatory**: reboot the Karoo after install/update.

## Key Files

| File | Purpose |
|------|---------|
| `datatype/Configdata.kt` | All enums, data classes, default settings, timing constants |
| `datatype/DataTypeFunctions.kt` | `getFieldFlow()`, zone coloring, value conversion, W′ model |
| `datatype/CustomDoubleTypeBase.kt` | Base `startView()` for dual-metric fields |
| `datatype/CustomRollingTypeBase.kt` | Base `startView()` for cyclic rolling fields |
| `datatype/CustomDoubleTypeView.kt` | Glance composables for field rendering |
| `extensions/Extensions.kt` | DataStore stream helpers, `streamDataFlow`, `streamUserProfile` |
| `extensions/Zones.kt` | Zone color lookup (`getZone`), slope/W′ zone definitions |
| `extensions/KarooCustomFieldExtension.kt` | Extension entry point, `types` registry |

## Logging

Timber is used throughout. Debug builds log everything; release builds log `WARN+` only. Log prefixes:
- `"DOUBLE ..."` — dual-metric field events
- `"ROLLING ..."` — rolling field events  
- `"CLIMB ..."` — climb field events

