# NotifySync — Android App

Kotlin + Jetpack Compose app that captures notifications from selected apps and uploads them to
the NotifySync API.

## What's inside

- **Jetpack Compose** UI — Login/Register, Sync Status, App Selection screens
- **NotificationListenerService** — captures notifications from user-selected packages
- **Room** — offline queue so nothing is lost without a connection
- **WorkManager** — uploads the queue with exponential-backoff retry + heartbeat
- **Retrofit + Moshi** — talks to the API; **DataStore** — stores the JWT, device id, prefs

## Prerequisites

- Android Studio (Koala / 2024.1+), or the command-line SDK + JDK 17
- An emulator or a physical device (notification capture must be tested on a real screen)

## Configure the API endpoint

The base URL is a `buildConfigField` defaulted for the Android emulator (`10.0.2.2` = host
machine). Override per build:

```bash
./gradlew assembleDebug -PapiBaseUrl="http://192.168.1.50:5080/"   # LAN IP of your API
# or for production:
./gradlew assembleRelease -PapiBaseUrl="https://api.yourdomain.com/"
```

> `10.0.2.2:5080` reaches a server running on your computer **from the Android emulator**.
> On a physical phone, use your computer's LAN IP and make sure they're on the same network.

## Build the APK

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

Or open the `NotifySyncApp/` folder in Android Studio and hit **Run ▶**.

## Use it on a device

1. Install the APK (`adb install app-debug.apk`, or open the file on the phone).
2. Sign in / register — the app auto-registers the device with the API.
3. Tap **Choose** and pick the apps to monitor.
4. Tap **Grant Notification Access** → enable NotifySync in the system list.
5. Leave **Sync enabled** on. New notifications from the selected apps now appear on the web
   dashboard in real time.

## Release build (signed)

```bash
# 1. create a keystore (once)
keytool -genkey -v -keystore notifysync.jks -keyalg RSA -keysize 2048 -validity 10000 -alias notifysync

# 2. add signingConfigs to app/build.gradle.kts, then:
./gradlew assembleRelease       # signed APK
./gradlew bundleRelease         # .aab for Google Play
```
