# KDouble Field  Extension

KDoubleType allows to use custom fields with double types (HR,Power,etc)

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
- You can add rolling fields and select de rolling period (ie. change every 5-10-20-30 seconds between three fields...)
- You can configure the fields in the configuration tab and select if you want colored zones or not.

## Configuration
- You can configure, for double custom fields, left/up and right/top sides in custom configuration tab.
- Horizontal fields rounds to integer numbers ( ie for speed 12.6 you will see 13) except for IF field.
- If you chose vertical fields you can see 5 digits (including the decimal point for speed and slope).
- Coloured zones are based in your Karoo zones. Slope zones are based in Hammerhead climber zones or you can chose use the Zwift colors.
- You can align  to the left, center or right, but you can select to use Karoo default alignment (your alignment in the Karoo profile).
- Rolling fields shows only one field (for small and medium size fields) and change every 5-10-20-30 seconds between three different measures. You can select only two if you want (or only want but I don't know if this is very useful ;) )
- You can use Headwind Field, this is from Timklge app, and it's mandatory to install and configure the app. KDouble extension only takes the values from Timklge app and shows in the field (rolling/vertical/horizontal).
In this case, you have to select Timklge headwind field also in this profile.

## Know Bugs
- The max number of every field horizontal is 3 digits, but it's better if you don't mix two types with 3 digits always.
- Not intensive tested with all the fields, please report any issue. For example, number adaptation for the fields is not tested with all the fields.
- If you use several fields your Karoo can be freeze especially if you have a Karoo 2. I've made several performance improvements but be careful to use 5 fields + rolling + headwind (in the same rolling or custom fields).. it's a lot of data to process.
- New Headwind releases doesn't work with KDouble extension because the new version doesn't stream wind data (v1.2.5 and later). You can use the last version of Headwind app (v1.2.4) to use the headwind field in KDouble extension.
- Hammerhead API has some bugs with the custom fields, they're working in a fix, but I've implemented some local fixes to avoid the Karoo freeze (but it can be happen).

- Made possible by the generous usage terms of timklge (apache 2.0). He has a great development and I use part of his code to create this extension.
  https://github.com/timklge?tab=repositories
- Thanks to valterc for the great ki2 app. Colors file is from his app.
- Thanks to vinapp for the vinapp app and for share code with me. 
- Thanks to Hammerhead for the great Karoo device.
- Thanks to DC Rainmaker for the great guide to sideload apps.
- Thanks to Boxicons for the great icons.
- Thanks to iconduck.com for the great icons.

## Links

[karoo-ext source](https://github.com/hammerheadnav/karoo-ext)
