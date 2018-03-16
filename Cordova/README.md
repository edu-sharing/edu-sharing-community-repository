# edu-sharing mobile app 2.0

This project is a cordova wrapper for then edu-sharing FRONTEND angular app.

Minimum Requirements:
* iOS >= 8 & iPhone 5S
* Android >= 4.4

## Setup

Check that you did all setup for the edu-sharing angular app in the FRONTEND folder and the `ng build` is running with no problems in that project.  

To build the mobile app make sure basic framework is installed:

* CORDOVA version should be >=8.0.0

`npm install -g cordova`

On first time building run a:

`mkdir www` and a

`cordova prepare`

Now all platform and plugin data will get loaded and added into your project.

Before final building check if you have all requirements for Android and iOS installed by using:

`cordova requirements`

Basic requirements should be:
* Java 1.8.x
* Android SDK Version : Android 8.0 (API 26)
* Gradle  (https://gradle.org/install/)

_Note that if you build on Windows - you should probably remove the iOS platform from the configuration with `ionic cordova platform remove ios` to avoid error messages during build. Please remember to not commit a configuration without iOS later on._

## Building for Android & iOS

Just run the node package script:

`npm start`

It will make a fresh production build of the angular edu-sharing app and copy it into the www directory of this cordova project. Then Android and iOS projects gets build.

NOTE: This will compile the edu-sharing app code with the --prod flag, what includes the AOT option.

If this runs without errors, you are ready to run all the rest of the possible cordova commands. For example to run the app on a connected android development phone just run `cordova run android` or open the `./platforms/ios/edu-sharing.xcodeproj` file in XCode to run it for iOS.

NOTE: If you want to build the app for development and testing, instead of `npm start` you can use `npm restart` to skip AOT and make a quicker build.