# Relun for Android

Native Kotlin + Jetpack Compose app for Relun, built from the "Relun Prototype" design.
It talks to `relun-backend` (REST + Socket.IO).

## Run it

1. Open this folder in Android Studio (or use `gradlew.bat`). JDK 17+; Android Studio's bundled JBR works.
2. Start the backend (`relun-backend`, `npm run dev`, port 9000).
3. Run the `app` configuration. Debug builds talk to `http://10.0.2.2:9000/`, which is your PC as seen from the emulator.
   For a physical phone, set your PC's LAN address in `local.properties`:
   ```
   relun.apiBaseUrl=http://192.168.1.10:9000/
   relun.releaseApiBaseUrl=https://api.relun.app/
   ```
   Cleartext HTTP is only allowed in debug builds.

If the emulator shows a black screen, cold-boot it with software rendering:
`emulator -avd Pixel_9_Pro -gpu swiftshader_indirect`.

## Push notifications (FCM)

1. Create a Firebase project, then add an Android app with package `com.relun.app`.
2. Put `google-services.json` in `app/`. The Google Services plugin is applied only when this file exists.
3. On the backend, set `FIREBASE_SERVICE_ACCOUNT_PATH` to a service-account JSON from the same Firebase project.

Without these, the app runs normally and simply doesn't register for push.

## Coin purchases (Google Play Billing)

1. In Play Console, create one-time products: `coins_100`, `coins_500`, `coins_1000`, `coins_5000`.
2. On the backend, set `GOOGLE_PLAY_SERVICE_ACCOUNT_PATH` and `ANDROID_PACKAGE_NAME`.
   Outside `NODE_ENV=production`, purchases are accepted unverified so you can test with license testers.

A purchase is consumed only after the backend has verified and credited it. Purchases left unfinished after a crash are completed on the next launch.

## Layout

```
data/        DTOs, Retrofit API, token refresh, Socket.IO client, repositories
billing/     Play Billing
push/        FCM service + token registration
location/    Fused location + geocoding
di/          AppContainer (manual DI)
ui/theme     Colours, Outfit/Pacifico type, segment accents (Relationship rose / Fun orange)
ui/components Shared buttons, inputs, sheets, dialogs, toasts, photos
ui/navigation Auth / onboarding / main switch, MainViewModel (sheets, match, coins)
ui/screens   auth, onboarding, discover, profile, match, messages, chat, dates, me, settings, sheets
```
