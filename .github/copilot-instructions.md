# Workspace: TermostatoNew

Progetto Android **Kotlin + Jetpack Compose** — **Termostato Social Village**, app moderna per il controllo del termostato.

## Stack
- Kotlin 2.0.0, Compose BOM 2024.06.00, AGP 8.5.2, Gradle 8.9 (wrapper)
- minSdk 26, compileSdk 34, targetSdk 34
- Package: `it.termostato.app`
- Single-activity + Compose Navigation, ViewModel + Repository

## Build
- JDK 21 richiesto: `C:\MTech\software\Java\Amzn_Corretto_jdk21.0.10_7`
- Android SDK: `C:\MTech\software\android-sdk`
- Build: `.\gradlew.bat :app:assembleDebug` (da PowerShell con JAVA_HOME e ANDROID_SDK_ROOT impostati)
- Task VS Code: **Build Debug APK** (Ctrl+Shift+B)

## API backend (legacy, ancora attive)
- Base: `https://www.radiaxweb.rehom.it/EmptyApp13Web/rest/vc_service`
- Tutte le chiamate sono **GET** con JSON di risposta `{... , error: <int>}`
- Errori: 0=OK, 1=generico, 2=credenziali, 3=account non attivo, 5=id appartamento invalido, 6=no connessione, 7=trasmissione in corso, 8=parametri errati, 9=zone id invalido
- Endpoint: login, logout, statusAndTemperatures, setStatusAndSetpoint, consumption, metersHistory, zones, zoneTemperatures, setZoneName, setZoneTemperature, getnotifications, resetpasswordlink, newregistration
- Profile: BASE | MEDIUM | ADVANCED (determina UI base vs avanzata)

## Struttura progetto
- `app/src/main/java/it/termostato/app/` — codice sorgente
  - `MainActivity.kt` — entry point + navigazione
  - `MainViewModel.kt` — stato UI + logica business
  - `data/ApiClient.kt` — client REST (OkHttp)
  - `data/Models.kt` — data class API
  - `data/SessionManager.kt` — persistenza token/credenziali
  - `ui/LoginScreen.kt` — schermata login
  - `ui/HomeScreen.kt` — temperatura + stato + regolazione
  - `ui/ZonesScreen.kt` — lista zone + dettaglio
  - `ui/ConsumptionScreen.kt` — consumi + storico contatori
  - `ui/theme/` — Material 3 theme
