# KDouble Field  Extension


KDoubleType allows to use custom fields with double types (HR,Power,etc)

<a href="https://www.buymeacoffee.com/enderthor" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy Me A Coffee" height="41" width="174"></a>

## Requirements
- Karoo (tested on last Karoo ) with version 1.527 or later
- Tested with Karoo 3 and metric configuration

## Installation

You can sideload the app using the following steps for Karoo 2

1. Download the APK from the releases .
2. Prepare your Karoo for sideloading by following the [step-by-step guide](https://www.dcrainmaker.com/2021/02/how-to-sideload-android-apps-on-your-hammerhead-karoo-1-karoo-2.html) by DC Rainmaker.
3. Install the app using the command `adb install app-release.apk`.


If you've Karoo 3 and v > 1.527 you can sideload the app using the following steps:

1. Link with apk (releases link) from your mobile ( https://github.com/lockevod/Karoo-KDoubleType/releases/latest/download/kdouble.apk )
2. Share with Hammerhead companion app
3. Install the app using the Hammerhead companion app.

**It's mandatory to reset the Karoo after the installation (shutdown and start again).**

## Instructions

- Add custom fields to your profiles (HR, Power, etc).
- You can add horizontal or vertical fields.
- You can add rolling fields and select de rolling period (ie. change every 5-10-20-30 seconds between three fields...). You can chose one/two/three rolling mesures, if you chose only one.. then you only have one messure in this field.
- You can configure the fields in the configuration tab and select if you want colored zones or not.
- You can use Smart Climb Field. This field is for map page (1x2 so-so space) and will show 4-5 measures (it changes "smart")
- **W' Balance Field**: You can add W' Balance (W Prime Balance) field to track your anaerobic capacity in real-time. This field shows how much of your anaerobic energy reserve (W') remains available based on your power output above Critical Power (CP).
  
## Configuration
- You can configure, for double custom fields, left/up and right/top sides in custom configuration tab.
- Horizontal fields rounds to integer numbers ( ie for speed 12.6 you will see 13) except for IF field.
- If you chose vertical fields you can see 5 digits (including the decimal point for speed and slope).
- Coloured zones are based in your Karoo zones. Slope zones are based in Hammerhead climber zones or you can chose use the Zwift colors.
- You can align  to the left, center or right, but you can select to use Karoo default alignment (your alignment in the Karoo profile).
- Rolling fields shows only one field (for small and medium size fields) and change every 5-10-20-30 seconds between three different measures. You can select only two if you want (or only want but I don't know if this is very useful ;) )
- You can use Headwind Field, this is from Timklge app, and it's mandatory to install and configure the app. KDouble extension only takes the values from Timklge app and shows in the field (rolling/vertical/horizontal).
In this case, you have to select Timklge headwind field also in this profile.
- Rolling fields have Extra Time option. If you check this option first field has x3 time.
- **W' Balance Configuration**: Configure your W' Balance parameters in the W'BAL tab:
  - **Critical Power (CP)**: You can use your Karoo FTP as CP or set a custom value. CP represents the highest power output you can sustain indefinitely.
  - **W' (W Prime)**: Your anaerobic capacity in joules (typically 15,000-25,000J for trained cyclists). This represents the finite amount of work you can do above CP.
  - **Visual Zones**: Enable color-coded zones to visually represent your W' Balance status (green = high reserve, yellow = moderate, red = low/depleted).
  - **Temporal Constants**: The app uses optimized default values (τ+ = 546s for recovery, τ- = 316s for depletion) based on scientific research.

### Calculating Your W' and Critical Power Values

To get accurate W' Balance readings, you need to determine your Critical Power and W' values:

**Online Calculators:**
- [High North Critical Power Calculator](https://www.highnorth.co.uk/articles/critical-power-calculator) - Upload your power files to calculate CP and W'
- [Intervals.icu](https://intervals.icu/) - Provides direct CP and W' calculations from your training data
- https://powerlab.icu/ 

**Manual Testing:**
- Perform 3-minute and 12-minute all-out efforts on separate days
- Use the power values in the calculators above
- Alternatively, use a 20-minute FTP test result as an approximation for CP

Smart Climb Field. This field is for map page (1x2 so-so space) and will show 4-5 measures (it changes "smart")
  - Four fields (you can select all available measures in the app) and horizontal/vertical/zones (it's a custom field = 2 x Double fields)
  - One climber field (center position). You've two select two measures, one is active when you start a climb and the other is active (or not, you can select) when you finish the climb. You can select all available measures in the app but I use with the distance to top
- Bell Field. You can add a Bell Fiel to emulate Bell sound (sound has to be enabled). Karoo hasn't a speaker (has a buzzer) and it isn't possible to emulate a bell completely, but you can use this field to emulate a bell sound. You can select the sound in the configuration tab.

## Know Bugs
- The max number of every field horizontal is 3 digits, but it's better if you don't mix two types with 3 digits always.
- Not intensive tested with all the fields, please report any issue. For example, number adaptation for the fields is not tested with all the fields.
- If you use several fields your Karoo can be freeze especially if you have a Karoo 2. I've made several performance improvements but be careful to use 5 fields + rolling + headwind (in the same rolling or custom fields).. it's a lot of data to process.
- Extensions (Hammerhead API) have a problem with rendering (sometimes and it's random)... if you have some field that dissapears when you use custom fields, you can try to change ride profile, kill ride app or reboot karoo. Sorry, but I cannot solve this at this moment (we're talking with Hammerhead).
- Karoo 2 has low resources. Kdouble limit number of fields you can use in Karoo. Please be careful (also with Karoo 3) if you use several custom field, several extensions (apps) and you have a lot of these fields in your profile...

## Credits

- Made possible by the generous usage terms of timklge (apache 2.0). He has a great development and I use part of his code to create this extension.
  https://github.com/timklge?tab=repositories
- Thanks to valterc for the great ki2 app. Colors file is from his app.
- Thanks to vinapp for the vinapp app and for share code with me. 
- Thanks to Hammerhead for the great Karoo device.
- Thanks to DC Rainmaker for the great guide to sideload apps.
- Thanks to Boxicons for the great icons.
- Thanks to iconduck.com for the great icons.
- Credits and copyright. Please respect license and specific parts licencsers (icons, etc). If you use this app you're agree.
- KDouble doesn't save or share any information for it's use, but it use firebase crashlytics service only for crashes in app (and firebase use this crash information). I only use this information to prevent new crashes in the app. Please if you isn't agree with Firebase use (this conditions are in firebase web and can change, please read it), please you cannot use app. If you use it you are agree with all conditions and copyrights.
## Links

[karoo-ext source](https://github.com/hammerheadnav/karoo-ext)