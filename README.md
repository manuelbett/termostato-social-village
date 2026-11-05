# Termostato Social Village

App Android nativa per il controllo del termostato dell'abitazione — UI moderna in Kotlin + Jetpack Compose.

[![GitHub](https://img.shields.io/badge/GitHub-Repository-black)](https://github.com/) [![Play Store](https://img.shields.io/badge/Play%20Store-Gratuito-green)](https://play.google.com/)

## Download

Disponibile gratuitamente su Google Play Store.

## Funzionalità

| Funzione | Descrizione |
|---|---|
| **Login** | ID Appartamento + PIN, Remember me |
| **Temperatura** | Visualizzazione setpoint + temperatura ambiente + umidità |
| **Regolazione** | Pulsanti + / − con conferma/annulla |
| **Stato** | Spento / Manuale / Automatico |
| **Zone** | Lista zone, regolazione per-zona, rinomina |
| **Consumi** | Calorie/Frigorie (kWh), acqua fredda/calda (impulsi) |
| **Storico** | Grafici mensili contatori con corrente e progressivo |
| **Profili** | BASE / MEDIUM / ADVANCED (UI adattiva) |

## Stack tecnico

- Kotlin 2.0 + Jetpack Compose (Material 3)
- Navigation Compose, ViewModel, StateFlow
- OkHttp per chiamate REST
- SharedPreferences per persistenza sessione

## API Backend

Le API REST originali sono ancora attive su:
`https://www.radiaxweb.rehom.it/EmptyApp13Web/rest/vc_service`

Tutte le chiamate sono **GET** con risposta JSON `{..., error: <int>}`.

## Build

```powershell
$env:JAVA_HOME = "C:\MTech\software\Java\Amzn_Corretto_jdk21.0.10_7"
$env:ANDROID_SDK_ROOT = "C:\MTech\software\android-sdk"
.\gradlew.bat :app:assembleDebug
```

## Requisiti

- JDK 21 (Amazon Corretto)
- Android SDK 34
- Gradle 8.9 (wrapper incluso)
