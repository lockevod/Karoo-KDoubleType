# KDouble Field Extension

KDoubleType allows you to use custom fields with double types (HR, Power, etc.)

## Requirements
- Karoo (tested on latest firmware) with version 1.527 or later
- Tested with Karoo 2 and Karoo 3 in both metric and imperial configurations

## Installation

### Karoo 2 (ADB sideload)
1. Download the APK from the [releases page](https://github.com/lockevod/Karoo-KDoubleType/releases/latest/download/kdouble.apk).
2. Prepare your Karoo for sideloading by following the [step-by-step guide](https://www.dcrainmaker.com/2021/02/how-to-sideload-android-apps-on-your-hammerhead-karoo-1-karoo-2.html) by DC Rainmaker.
3. Install the app using the command `adb install app-release.apk`.

### Karoo 3 (v1.527+)
1. Copy the APK link: `https://github.com/lockevod/Karoo-KDoubleType/releases/latest/download/kdouble.apk`
2. Share it with the Hammerhead companion app.
3. Install the app using the Hammerhead companion app.

**It's mandatory to reboot the Karoo after installation (shutdown and start again).**

## Field Types

| Field | Slots | Description |
|-------|-------|-------------|
| **KDouble** (×3) | 1 slot | Two metrics side by side — horizontal or vertical split |
| **KRolling** (×3) | 1 slot | Cycles automatically between up to 3 metrics |
| **KSextuple** (×3) | 1 slot (large) | Six metrics in a 2×3 grid — ideal for a single full-width slot |
| **Smart Climb** (×1) | 1 slot | Adaptive climb field: 4 standard metrics + 1 smart center that switches between start-of-climb / on-climb metrics |
| **Bell** (×1) | action | Tap-to-buzz action field |

## Instructions

- Add custom fields to your profiles (HR, Power, Speed, etc.).
- **Double fields**: show two metrics horizontally (left/right) or vertically (top/bottom).
- **Rolling fields**: show one metric at a time and cycle between up to three — configurable interval (5 / 10 / 20 / 30 s). If you pick only one metric it behaves as a static field.
- **Sextuple fields**: show six metrics simultaneously in a compact 2×3 grid. Best used in a full-width or large slot. Up to 3 independent sextuple fields available. ⚠️ Avoid using more than one sextuple field simultaneously in the same profile on low-resource devices.
- **Smart Climb field**: designed for the Karoo map page (1×2 large space). Displays 4 configurable metrics plus a smart center panel that shows a *start-of-climb* metric by default and automatically switches to an *on-climb* metric (e.g. distance-to-top) when a climb is detected. The field respects the reduced slot size inside the Karoo climber screen.
- **Bell field**: emulates a bell sound using the Karoo buzzer. Sound must be enabled on the device.
- **W′ Balance field**: tracks your anaerobic energy reserve in real-time based on power output above Critical Power (CP).

## Configuration

- For **Double** fields, configure left/top and right/bottom metric in the custom configuration tab.
- Horizontal fields round to integers (e.g. speed 12.6 → 13), except the IF field.
- Vertical fields show up to 5 characters (including decimal point for speed and slope).
- Colour zones are based on your Karoo zones. Slope zones use Hammerhead climber zones, or you can switch to Zwift colours.
- Alignment can be set to left, centre, or right, or you can follow the default Karoo profile alignment.
- **Rolling fields** — *Extra Time* option: when enabled the first metric gets 3× the display time of the others.
- **Headwind field**: reads data from the [timklge Headwind extension](https://github.com/timklge?tab=repositories). You must install and configure that app separately, and also add its headwind field to the same profile.
- **Show KPower / Show KGhost fields** (General tab): toggles whether the KPower (`KPW…`) and KGhost (`Ghost…`) metrics appear in the field pickers. Both default ON; turn one OFF to declutter the pickers if you don't use that extension. Existing configured fields are left untouched.
- **Sextuple fields** have 6 independently configurable metric slots. Each of the 3 sextuple field instances (One / Two / Three) stores its own configuration.

### W′ Balance Configuration

Configure your W′ Balance parameters in the **W′BAL** tab:

- **Critical Power (CP)**: use your Karoo FTP as CP or set a custom value. CP represents the highest power output you can sustain indefinitely.
- **W′ (W Prime)**: your anaerobic capacity in joules (typically 15,000–25,000 J for trained cyclists).
- **Visual Zones**: enable colour-coded zones (green = high reserve, yellow = moderate, red = low/depleted).
- **Temporal constants**: optimised defaults (τ+ = 546 s recovery, τ− = 316 s depletion) based on scientific research.

### Calculating Your W′ and Critical Power

**Online calculators:**
- [High North Critical Power Calculator](https://www.highnorth.co.uk/articles/critical-power-calculator)
- [Intervals.icu](https://intervals.icu/)
- [PowerLab.icu](https://powerlab.icu/)

**Manual testing:**
- Perform 3-minute and 12-minute all-out efforts on separate days and use the calculators above.
- A 20-minute FTP test result can be used as an approximation for CP.

## Fields from other extensions (KSafe, KGhost & KPower)

If you also have **KSafe**, **KGhost** or **KPower** installed, their live data shows up as extra
metrics you can pick in any KDouble field (Double, Rolling, Sextuple, Smart Climb). Nothing
to configure here — just select them like any other metric. If the source extension isn't
installed (or its data isn't available yet) the metric simply stays blank (`---`).

All metrics in the pickers are listed in **alphabetical order**, so the extension fields group
together by their prefix (`KPW…` for KPower, `Ghost…` for KGhost). To keep the picker tidy if
you don't use these extensions, the **General** settings tab has **Show KPower Fields** and
**Show KGhost Fields** toggles (both ON by default). Turning one OFF just hides those entries
from the pickers — it does **not** change or remove fields you have already configured.

### KSafe — fueling (carbs, hydration, calories)

Requires the [KSafe](https://github.com/lockevod/Karoo-KSafe) extension with fueling enabled. These
update **only while you are recording a ride**:

| Metric | Shows |
|--------|-------|
| **Carb g/h** | Current carbohydrate burn rate (g/h) |
| **Carb Avg g/h** | Session-average carb burn rate (g/h) |
| **Carbs Burned** | Total carbohydrates burned so far (g) |
| **Carb Deficit** | Carb balance — **positive = you need to eat**, negative = surplus |
| **Hydration** | Fluid deficit — **positive = you need to drink**. Shown in millilitres, switching to **litres** from 1 L (e.g. `3.5L`) |
| **Kcal Rate** | Energy expenditure (kcal/h) |
| **Kcal Total** | Total calories burned so far (kcal) |

### KGhost — gap to your ghost

Requires the [KGhost](https://github.com/lockevod/KGhost) extension with a ghost/target set.

| Metric | Shows |
|--------|-------|
| **Ghost Gap Time** | Time gap to the ghost, formatted **M:SS** (e.g. `1:30`) |
| **Ghost Gap Dist** | Distance gap to the ghost — **metres** under 1 km (`850m`), **km** above (`1.2k`) |

The gap is **signed: positive = you are ahead of the ghost, negative = behind** (e.g. `-1:30`,
`-850m`). The `M:SS` / `m`·`k` formatting also makes it obvious at a glance whether a field is
the time or the distance gap.

When your position is briefly uncertain (a GPS dropout, or while you are off-route) KGhost
reports the gap as an **estimate**. Because the field can't recolour just the number, an
estimated value is prefixed with a tilde — e.g. `~-1:30` means *about 1:30 behind, estimated*.

### KPower — power

Requires the [KPower](https://github.com/lockevod/Karoo-Power_Extension) extension. The
**estimated** fields work without any meter (KPower's physical model). The **real meter** fields
only update while a power meter is enabled in KPower **and** you are recording a ride. Power
fields are coloured by your Karoo power zones.

All KPower metrics are prefixed **`KPW`**. Fields marked **`Est`** come from KPower's estimated
model (no meter needed); everything else (`KPW Pwr`, `KPW NP`, …) comes from the **real ANT+
meter**.

| Metric | Shows |
|--------|-------|
| **KPW Est Pwr** | Estimated power, instant (W) |
| **KPW Est Pwr 3s** | Estimated power, 3 s smoothed (W) |
| **KPW Est NP** | Estimated normalized power (W) |
| **KPW Pwr** | Real meter power, instant (W) |
| **KPW Pwr 3s** | Real meter power, 3 s smoothed (W) |
| **KPW NP** | Real meter normalized power (W) |

> **Torque, balance, torque effectiveness and pedal smoothness are no longer listed.** KPower no
> longer publishes streams for these — the Karoo shows them natively for a paired meter (torque,
> average/max torque, L/R pedal balance, torque effectiveness, pedal smoothness), and KPower still
> writes them to the FIT. Use the Karoo's own native fields for those metrics.

## Known Bugs / Limitations

- The maximum number of digits in a horizontal field is 3 — avoid combining two metrics that both need 3 digits.
- Not all field combinations have been tested. Please report any display or rounding issues.
- Using many fields simultaneously (especially Rolling + Headwind on a Karoo 2) can cause performance issues. Use with care.
- The Hammerhead extension API occasionally causes fields to disappear randomly. If a field goes blank: switch your ride profile, force-stop the ride app, or reboot the Karoo. This is a known SDK issue being discussed with Hammerhead.
- Karoo 2 has limited resources. Be careful when combining multiple custom fields, rolling fields, and other extensions in the same profile.

## Credits

- Made possible by the generous usage terms of [timklge](https://github.com/timklge?tab=repositories) (Apache 2.0). Parts of his code are used in this extension.
- Thanks to [valterc](https://github.com/valterc) for the great ki2 app — colour definitions are based on his work.
- Thanks to [kleinkm](https://github.com/kleinkm) for the inspiration and contributions around the Sextuple field.
- Thanks to vinapp for sharing code and collaboration.
- Thanks to Hammerhead for the Karoo platform and SDK.
- Thanks to DC Rainmaker for the sideloading guide.
- Thanks to Boxicons and iconduck.com for icons.
- KDouble does **not** save or share any personal data.

## Links

- [Latest APK](https://github.com/lockevod/Karoo-KDoubleType/releases/latest/download/kdouble.apk)
- [karoo-ext SDK source](https://github.com/hammerheadnav/karoo-ext)
