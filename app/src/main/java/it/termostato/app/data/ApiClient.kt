package it.termostato.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Client REST per il Termostato Virtuale (RadiaxWeb VC Service).
 * Tutte le chiamate sono GET con risposta JSON `{... , error: <int>}`.
 * Usa WebViewApiClient (rete Chromium) per superare le restrizioni MIUI.
 */
object ApiClient {

    private const val TAG = "TermostatoAPI"
    private const val BASE_URL = "https://www.radiaxweb.rehom.it/EmptyApp13Web/rest/vc_service"

    /** Inizializza la WebView (chiamare una volta dall'Application/Activity) */
    fun init(context: Context) {
        WebViewApiClient.init(context)
    }

    // Credenziali salvate per auto-relogin
    private var savedApartmentId: String = ""
    private var savedPin: String = ""

    fun saveCredentials(apartmentId: String, pin: String) {
        savedApartmentId = apartmentId
        savedPin = pin
    }

    /** Prova a rinnovare il token automaticamente. Restituisce true se ok. */
    private suspend fun tryRelogin(): Boolean {
        if (savedApartmentId.isBlank() || savedPin.isBlank()) return false
        Log.d(TAG, "Auto-relogin con credenziali salvate…")
        val resp = login(savedApartmentId, savedPin, rememberMe = true)
        return resp.isOk
    }

    private suspend fun get(path: String): JSONObject {
        val urlString = "$BASE_URL/$path"
        val logId = NetworkLogger.start(urlString)
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "API GET $path")

        var result = doGet(path)

        // Auto-relogin se token scaduto (error:2)
        if (result.optInt("error", 0) == 2 && tryRelogin()) {
            Log.d(TAG, "Token rinnovato, riprovo $path")
            // Sostituisci il vecchio token nel path con quello nuovo
            val token = currentToken
            val newPath = if (token.isNotBlank()) {
                path.replace(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), token)
                    .replace(Regex("token=[^&]*"), "token=$token")
            } else path
            NetworkLogger.completeRaw(logId, result.toString(), System.currentTimeMillis() - startTime, 2)
            // Nuova chiamata con token aggiornato
            val retryId = NetworkLogger.start("$BASE_URL/$newPath")
            val retryStart = System.currentTimeMillis()
            result = doGet(newPath)
            val elapsed = System.currentTimeMillis() - retryStart
            NetworkLogger.completeRaw(retryId, result.toString(), elapsed, result.optInt("error", 0))
            return result
        }

        val elapsed = System.currentTimeMillis() - startTime
        NetworkLogger.completeRaw(logId, result.toString(), elapsed, result.optInt("error", 0))
        return result
    }

    private var currentToken: String = ""

    private suspend fun doGet(path: String): JSONObject {
        return try {
            WebViewApiClient.get(path)
        } catch (e: Exception) {
            Log.w(TAG, "WebView fallback to HttpURLConnection: ${e.message}")
            getHttp(path)
        }
    }

    /** Fallback: HttpURLConnection diretto */
    private suspend fun getHttp(path: String): JSONObject = withContext(Dispatchers.IO) {
        val urlString = "$BASE_URL/$path"
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 30_000
            conn.readTimeout = 45_000
            conn.setRequestProperty("Connection", "close")
            conn.useCaches = false
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "{}"
            }
            conn.disconnect()
            Log.d(TAG, "HTTP RESP [$code] $body")
            JSONObject(body)
        } catch (e: Exception) {
            Log.e(TAG, "HTTP FAIL $urlString: ${e.message}")
            JSONObject().put("error", 6)
        }
    }

    private fun str(json: JSONObject, key: String): String {
        val v = json.optString(key, "")
        return if (v == "null" || v == "NULL") "" else v
    }

    // ── Auth ──────────────────────────────────────────────────────────

    suspend fun login(apartmentId: String, pin: String, rememberMe: Boolean, gctoken: String = ""): LoginResponse {
        // Usa doGet() diretto per evitare ricorsione con auto-relogin
        val json = doGet("login/$apartmentId?pwd=$pin&rememberme=${if (rememberMe) "true" else "false"}&gctoken=$gctoken")
        val resp = LoginResponse(
            error = json.optInt("error", 0),
            token = str(json, "token"),
            profile = str(json, "profile").ifBlank { "BASE" },
            season = str(json, "season")
        )
        if (resp.isOk) {
            currentToken = resp.token
            if (rememberMe) {
                savedApartmentId = apartmentId
                savedPin = pin
            }
        }
        Log.d(TAG, "login: error=${resp.error} token=${resp.token.take(8)}… profile=${resp.profile}")
        return resp
    }

    suspend fun logout(token: String): ApiResponse {
        val json = get("logout/?token=$token")
        return ApiResponse(json.optInt("error", 0))
    }

    // ── Stato e temperature ──────────────────────────────────────────

    suspend fun statusAndTemperatures(token: String): StatusResponse {
        val json = get("statusAndTemperatures/$token")
        val resp = StatusResponse(
            error = json.optInt("error", 0),
            status = json.optInt("status", 0),
            setPointTemperature = json.optDouble("setPointTemperature", 0.0),
            envTemperature = json.optDouble("envTemperature", 0.0),
            humidity = json.optDouble("humidity", -1.0)
        )
        Log.d(TAG, "status: error=${resp.error} status=${resp.status} sp=${resp.setPointTemperature} env=${resp.envTemperature} hum=${resp.humidity}")
        return resp
    }

    suspend fun setStatusAndSetpoint(
        token: String,
        status: Int? = null,
        setPoint: Double? = null,
        currentSetPoint: Double? = null
    ): ApiResponse {
        val params = mutableListOf<String>()
        status?.let { params.add("status=$it") }
        setPoint?.let { params.add("setPoint=$it") }
        currentSetPoint?.let { params.add("currentSetPoint=$it") }
        val query = params.joinToString("&")
        val json = get("setStatusAndSetpoint/$token?$query")
        Log.d(TAG, "setStatusAndSetpoint: query=$query → error=${json.optInt("error", 0)}")
        return ApiResponse(json.optInt("error", 0))
    }

    // ── Consumi ──────────────────────────────────────────────────────

    suspend fun consumption(token: String): ConsumptionResponse {
        val json = get("consumption/$token")
        return ConsumptionResponse(
            error = json.optInt("error", 0),
            consumption = json.optDouble("consumption", 0.0),
            coldWaterConsumption = json.optDouble("coldWaterConsumption", 0.0),
            hotWaterConsumption = json.optDouble("hotWaterConsumption", 0.0)
        )
    }

    suspend fun metersHistory(token: String): MetersHistoryResponse {
        val json = get("metersHistory/$token")
        fun toList(key: String): List<Double> {
            val arr = json.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.optDouble(it, 0.0) }
        }
        fun toStrList(key: String): List<String> {
            val arr = json.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.optString(it, "") }
        }
        return MetersHistoryResponse(
            error = json.optInt("error", 0),
            calories = toList("calories"),
            caloriesMonthNames = toStrList("caloriesMonthNames"),
            caloriesCurrent = json.optDouble("caloriesCurrent", 0.0),
            caloriesProgressive = json.optDouble("caloriesProgressive", 0.0),
            frigories = toList("frigories"),
            frigoriesMonthNames = toStrList("frigoriesMonthNames"),
            frigoriesCurrent = json.optDouble("frigoriesCurrent", 0.0),
            frigoriesProgressive = json.optDouble("frigoriesProgressive", 0.0),
            coldwater = toList("coldwater"),
            coldwaterMonthNames = toStrList("coldwaterMonthNames"),
            coldwaterCurrent = json.optDouble("coldwaterCurrent", 0.0),
            coldwaterProgressive = json.optDouble("coldwaterProgressive", 0.0),
            hotwater = toList("hotwater"),
            hotwaterMonthNames = toStrList("hotwaterMonthNames"),
            hotwaterCurrent = json.optDouble("hotwaterCurrent", 0.0),
            hotwaterProgressive = json.optDouble("hotwaterProgressive", 0.0)
        )
    }

    // ── Zone ─────────────────────────────────────────────────────────

    suspend fun zones(token: String): ZonesResponse {
        val json = get("zones/$token")
        val zonesArr = json.optJSONArray("zones")
        val zones = mutableListOf<Zone>()
        if (zonesArr != null) {
            for (i in 0 until zonesArr.length()) {
                val z = zonesArr.optJSONObject(i) ?: continue
                zones.add(Zone(id = str(z, "id"), name = str(z, "name")))
            }
        }
        Log.d(TAG, "zones: error=${json.optInt("error", 0)} count=${zones.size}")
        return ZonesResponse(error = json.optInt("error", 0), zones = zones)
    }

    suspend fun zoneTemperatures(token: String, zoneId: String): ZoneTemperaturesResponse {
        val json = get("zoneTemperatures/$token?zoneId=$zoneId")
        val resp = ZoneTemperaturesResponse(
            error = json.optInt("error", 0),
            name = str(json, "name"),
            currentEnvTempSetPoint = json.optDouble("currentEnvTempSetPoint", 0.0),
            setPointTempAdjustment = json.optDouble("setPointTempAdjustment", 0.0),
            envTemperature = json.optDouble("envTemperature", 0.0),
            humidity = json.optDouble("humidity", -1.0)
        )
        Log.d(TAG, "zoneTemperatures($zoneId): error=${resp.error} name=${resp.name} env=${resp.envTemperature}")
        return resp
    }

    suspend fun setZoneName(token: String, zoneId: String, name: String): ApiResponse {
        val json = get("setZoneName/$token?zoneId=$zoneId&name=$name")
        return ApiResponse(json.optInt("error", 0))
    }

    suspend fun setZoneTemperature(token: String, zoneId: String, setPoint: Double): ApiResponse {
        val json = get("setZoneTemperature/$token?zoneId=$zoneId&setPoint=$setPoint")
        return ApiResponse(json.optInt("error", 0))
    }

    // ── Altro ────────────────────────────────────────────────────────

    suspend fun getNotifications(): ApiResponse {
        val json = get("getnotifications")
        return ApiResponse(json.optInt("error", 0))
    }

    suspend fun resetPasswordLink(user: String, email: String): ApiResponse {
        val json = get("resetpasswordlink/?user=$user&email=$email")
        return ApiResponse(json.optInt("error", 0))
    }

    suspend fun newRegistration(
        nameSurname: String, email: String, phone: String,
        address: String, city: String, stair: String,
        floor: String, apartmentNumber: String
    ): ApiResponse {
        val json = get(
            "newregistration/?namesurname=$nameSurname&email=$email&phone=$phone" +
            "&address=$address&city=$city&stair=$stair&floor=$floor" +
            "&apartmentnumber=$apartmentNumber"
        )
        return ApiResponse(json.optInt("error", 0))
    }
}
