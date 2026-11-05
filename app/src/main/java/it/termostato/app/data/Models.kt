package it.termostato.app.data

/**
 * Modelli dati per le API del Termostato Virtuale (RadiaxWeb VC Service).
 * Tutte le risposte contengono un campo `error` (Int).
 * 0=OK, 1=generico, 2=credenziali, 3=account non attivo, 5=id appartamento invalido,
 * 6=no connessione, 7=trasmissione in corso, 8=parametri errati, 9=zone id invalido
 */

data class ApiResponse(
    val error: Int = 0
) {
    val isOk: Boolean get() = error == 0
}

data class LoginResponse(
    val error: Int = 0,
    val token: String = "",
    val profile: String = "BASE",   // BASE | MEDIUM | ADVANCED
    val season: String = ""         // estate/inverno
) {
    val isOk: Boolean get() = error == 0 && token.isNotBlank()
}

data class StatusResponse(
    val error: Int = 0,
    val status: Int = 0,             // 0=spento, 1=manuale, 2=automatico
    val setPointTemperature: Double = 0.0,
    val envTemperature: Double = 0.0,
    val humidity: Double = -1.0      // -1 = non disponibile
)

data class SetStatusRequest(
    val status: Int?,                // null = non cambiare
    val setPoint: Double?,           // delta o valore assoluto
    val currentSetPoint: Double?
)

data class ConsumptionResponse(
    val error: Int = 0,
    val consumption: Double = 0.0,        // kWh (calories o frigories a seconda della stagione)
    val coldWaterConsumption: Double = 0.0, // impulsi
    val hotWaterConsumption: Double = 0.0   // impulsi
)

data class MetersHistoryResponse(
    val error: Int = 0,
    // Calorie (riscaldamento) o Frigories (raffrescamento)
    val calories: List<Double> = emptyList(),
    val caloriesMonthNames: List<String> = emptyList(),
    val caloriesCurrent: Double = 0.0,
    val caloriesProgressive: Double = 0.0,
    val frigories: List<Double> = emptyList(),
    val frigoriesMonthNames: List<String> = emptyList(),
    val frigoriesCurrent: Double = 0.0,
    val frigoriesProgressive: Double = 0.0,
    // Acqua
    val coldwater: List<Double> = emptyList(),
    val coldwaterMonthNames: List<String> = emptyList(),
    val coldwaterCurrent: Double = 0.0,
    val coldwaterProgressive: Double = 0.0,
    val hotwater: List<Double> = emptyList(),
    val hotwaterMonthNames: List<String> = emptyList(),
    val hotwaterCurrent: Double = 0.0,
    val hotwaterProgressive: Double = 0.0
)

data class ZonesResponse(
    val error: Int = 0,
    val zones: List<Zone> = emptyList()
)

data class Zone(
    val id: String = "",
    val name: String = ""
)

data class ZoneTemperaturesResponse(
    val error: Int = 0,
    val name: String = "",
    val currentEnvTempSetPoint: Double = 0.0,
    val setPointTempAdjustment: Double = 0.0,
    val envTemperature: Double = 0.0,
    val humidity: Double = -1.0
)
