# KDouble Field  Extension

> [!WARNING]  
> This app is currently in beta stage and its main features might not work at all.

KDoubleType allows to use custom fields with double types (HR,Power,etc)

## Requirements
- Karoo (tested on last Karoo ) with version 1.524.2003 or later
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
- You can add two horizontal fields and two vertical fields.
- You can configure the fields in the configuration tab and select if you want colored zones or not.

## Configuration
- You can configure left and right sides in configuration tab.
- Horizontal fields rounds to integer numbers ( ie for speed 12.6 you will see 13)
- If you chose vertical fields you can see 5 digits (including the decimal point for speed and slope).
- Coloured zones are based in your Karoo zones. Slope zones are based in Hammerhead climber zones.
- You can align text/image to the left or center in horizontal fields.

## Know Bugs
- The max number of every field horizontal is 3 digits, but it's better if you don't mix two types with 3 digits always.
- There isn't possible to configure alignment of the fields (currently, will be in future versions).
- Not intensive tested with all the fields, please report any issue. For example, number adaptation for the fields is not tested with all the fields.

## Credits

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
