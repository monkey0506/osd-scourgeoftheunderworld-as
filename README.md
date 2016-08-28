# OCEANSPIRIT DENNIS: SCOURGE OF THE UNDERWORLD

This [Android Studio](https://developer.android.com/studio/index.html) project is designed for
building standalone APKs of [Adventure Game Studio](http://www.adventuregamestudio.co.uk/) games,
for release on the Google Play Store.

## Prerequisites

You will need the following to build this project:

* Android Studio (requires the
  [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html))
* Android SDK and NDK (can be obtained via the SDK manager once Android Studio is installed)
* git submodules (run `git submodule init && git submodule update` after cloning this repo)
* AGS engine native libraries (see instructions to rebuild or download below).
* A **compiled** AGS game (**NOTE:** Game must be **< 2 GB total**).
* (optional) [jobbifier](https://github.com/monkey0506/jobbifier), replacement for `jobb` tool

### Downloading AGS Engine Native Libraries (recommended)

It is much easier to download the pre-built native libraries than it is to rebuild them yourself, so
unless you have made any changes to the AGS engine or the built-in plugin sources there's no need to
build them. You can grab the latest libraries from the
[AGS build server](http://teamcity.bigbluecup.org/viewType.html?buildTypeId=AdventureGameStudio_EngineAndroid)
(requires login/registration, but you may login as "guest" with no password). Select the engine
branch you want to use (releases are stable builds, master is latest dev build) and click on the
*Artifacts* link. This will bring you to a page where you can download `engine-android.zip`, which
contains the APK of your selected branch of AGS.

Once you have obtained `engine-android.zip`, you can extract `AGS-debug.apk`. Open the APK file with
your choice of archive tool (7-Zip, WinRAR, etc.). Extract the contents of the `lib` folder
(armeabi, armeabi-v7a, mips, and x86 folders) to `PROJECT_DIR/libs` (where `PROJECT_DIR` is the
directory where you cloned this project).

### Rebuilding AGS Engine Native Libraries

To rebuild the AGS engine native libraries you will need a copy of the
[AGS source code](https://github.com/adventuregamestudio/ags). Simply download that and update your
`local.static.properties` file with the location of your working copy. You'll also need to run the
`AGS_SOURCE/Android/buildlibs/buildall.sh` script first. Then, the project's gradle scripts will
invoke the NDK to rebuild the engine libraries.

## Setting up the project for your game

With the prerequisites installed, you will need to change the following items to set up the project
for building your game.

* Update package name:
  * Open the project in Android Studio, then in the project tree navigate to
    app/java/com.monkeymoto.osd.scourge.
  * Right-click on this folder and select "Refactor -> Move...". When prompted, select "Move
    package 'com.monkeymoto.osd.scourge' to another package" (the default). You may receive a
    warning that multiple directories will be moved, select Yes. Type the *parent* name of your
    package, not the *final* package name (e.g., `com.bigbluecup` *not* `com.bigbluecup.game`),
    select "Refactor" and then click "Do Refactor".
  * Right-click on the new project folder in the project tree (e.g., `com.bigbluecup.scourge`)
    and select "Refactor -> Rename". Type the package name for your game (e.g., `game`), then
    select "Refactor" and click "Do Refactor".
  * Finally, delete the `com.monkeymoto.osd` folder.

* Update `project.properties`. This file contains gradle settings related to your project. The
  application ID, version code, and version name need to be set to match your project settings
  (application ID is your package name).

* Update `project.xml`. This file contains resources for your project. The values there are
  described in that file.

* Update `local.static.properties`. This file contains local data that should **NOT** be added
  to version control (.gitignore will ignore your changes to this file). You need to add your
  keystore path, alias, and passwords, and optionally the path to your copy of the AGS source
  (if you are rebuilding the engine native libraries). See the
  [Java docs on keytool](http://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)
  or use the
  [Android Studio signing wizard](https://developer.android.com/studio/publish/app-signing.html)
  to generate a keystore.

* Update `private.xml`. This file contains definitions for your RSA public key and an
  integer-array with bytes for your salt which is used by the ExpansionDownloaderService. These
  values are necessary if distributing your app via the Google Play Store. The RSA public key
  is provided in the Google Play Developer Console, and the salt bytes may be any number of
  values in the range [-128, 127]. You may need to upload an APK without the RSA public key
  first before the key is provided. That APK should not be public unless the OBB file is
  embedded.

* Update graphics resources (`app/src/main/res`). You can use the
  [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/) to easily generate
  graphics for your app, or use your preferred method. Everything in the `drawable` and various
  `mipmap-*` folders should be replaced with your resources.

* Build your expansion file (see instructions below).

## APK Expansion File

Your game's data files need to be packaged into an
[OBB file](https://developer.android.com/google/play/expansion-files.html). The `jobb` tool included
with the Android SDK has been identified with bugs that prevent using game folders of *< 4 MB* or
*> 511 MB*. The `jobbifier` tool has fixes for these bugs and includes a GUI application. If you
prefer the command line, the jobb source included with the jobbifier tool may still be run from the
command line.

Whether using the jobb tool or jobbifier, the input directory is your AGS game's Compiled folder. If
using AGS 3.4.0, you will need to copy the data files from "Compiled" into a separate directory
first (newer versions of AGS will place these files into a separate "Compiled/data" folder, and
eventually include an Android builder). Specifically you need the ".ags" or ".exe" file and any
resource and VOX files. Note that (in my tests) not all versions of Android properly support
mounting embedded OBB files, so it is currently not recommended to add a password when creating the
OBB file.

Your expansion OBB file *may* be embedded into the APK by placing it in the "app/src/main/assets"
folder, but it should be understood that this requires *copying* the OBB file onto external storage
before it can be used (duplicating the size of your game files!). The preferred method therefore is
to use the Downloader Library, though this only applies to apps published via Google Play. You may
explore other methods of bundling the game files into the APK, but these two methods are currently
supported by the code. You can also distribute the OBB file yourself by other means, but you must
make sure that the file is installed to the `OBB_FILE_EXTERNAL_PATH` (see
`app/src/main/java/MainActivity.java`) on the user's device.

## Building the APK

To build your APK for release, simply select "Build -> Generated Signed APK..." in Android Studio
and follow the wizard, or run `gradlew.bat assembleRelease` from the project directory. This APK
will be signed and aligned for release on Google Play.
