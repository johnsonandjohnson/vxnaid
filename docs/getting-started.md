# Getting Started

## Requirements
The Android tablet application will run on Android devices:
* Android 6.0 and above (minimum API level 23)
* Screen size 7" and above

While you can install the app on smaller devices and even phones, the layout is optimized for tablet devices and as such some data entry fields may appear too small for comfortable use.

To utilize the iris scan functionality:
* USB Iris scanner device, the APK is preconfigured for use with a Iritec IriShield MK-2120U
* USB On-the-Go (OTG) connector

A list of all iris scanners supported by the Neurotechnology SDK can be found [here](https://www.neurotechnology.com/verieye-supported-eye-iris-scanners.html). Utilizing these will require source code changes and re-building the APK file, as the USB vendor-id and product-id are used as an intent filter for the application.

## Installing the APK
The APK is not available from the Google Play Store, and will need to be manually installed on the tablet devices (side-loaded).

1. Prepare an SD-card suitable for your table device with the `.apk` file or download it directly onto the device using a web browser
2. Enable the installation of apps from unknown sources in the settings. The location of this setting differs depending on your device manufacturer:
   * Samsung: Settings > Biometrics and Security > Install Unknown Apps. Tap on 'My Files' (or the applicable browser from which you will install the apk) and ensure 'Allow from this source' is enabled.
   * Google: Settings > Apps & Notifications > Advances > Special App Access > Install Unknown Apps. Tap on 'Files' (or the applicable browser from which you will install the apk) and ensure 'Allow from this source' is enabled.
3. Locate the apk file and tap it to install. If you could not follow the instructions in step 2, you will get a prompt here to take you to the settings menu where you can allow installation from unknown sources.

[filename](config/backend-url.md ':include')

[filename](config/iris-license.md ':include')

