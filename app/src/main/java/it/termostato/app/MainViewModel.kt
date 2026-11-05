package it.termostato.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.termostato.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val sessionExpired: Boolean = false,
    val profile: String = "BASE",
    val season: String = "",
    // Home
    val status: Int = 0,                  // 0=off, 1=manuale, 2=auto
    val setPointTemperature: Double = 0.0,
    val envTemperature: Double = 0.0,
    val humidity: Double = -1.0,
    val editSetPoint: Double? = null,     // temperatura in modifica
    // Zone
    val zones: List<Zone> = emptyList(),
    val selectedZone: Zone? = null,
    val zoneTemps: ZoneTemperaturesResponse? = null,
    val zoneEditAdjustment: Double? = null,
    // Consumi
    val consumption: ConsumptionResponse? = null,
    // Storico
    val metersHistory: MetersHistoryResponse? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionManager(application)

    private val _ui = MutableStateFlow(UiState(isLoggedIn = session.isLoggedIn))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        // Il refresh viene triggerato dalla HomeScreen LaunchedEffect
    }

    private fun setBusy(b: Boolean) { _ui.value = _ui.value.copy(isLoading = b) }
    private fun setError(msg: String?) { _ui.value = _ui.value.copy(error = msg) }

    fun clearError() = setError(null)

    // ── Auth ──────────────────────────────────────────────────────────

    fun login(apartmentId: String, pin: String, rememberMe: Boolean) {
        viewModelScope.launch {
            setBusy(true)
            try {
                // Salva credenziali per auto-relogin
                ApiClient.saveCredentials(apartmentId, pin)
                val resp = ApiClient.login(apartmentId, pin, rememberMe)
                if (resp.isOk) {
                    session.saveLogin(resp.token, resp.profile, resp.season, apartmentId, pin, rememberMe)
                    _ui.value = _ui.value.copy(
                        isLoggedIn = true,
                        profile = resp.profile,
                        season = resp.season,
                        error = null
                    )
                    refreshStatus()
                } else {
                    setError(errorDescription(resp.error))
                }
            } catch (e: Exception) {
                setError("Errore di connessione: ${e.message}")
            }
            setBusy(false)
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { ApiClient.logout(session.token) } catch (_: Exception) {}
            session.clearSession()
            _ui.value = UiState(isLoggedIn = false)
        }
    }

    // ── Stato ─────────────────────────────────────────────────────────

    fun refreshStatus() {
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.statusAndTemperatures(session.token)
                if (resp.error == 0) {
                    _ui.value = _ui.value.copy(
                        status = resp.status,
                        setPointTemperature = resp.setPointTemperature,
                        envTemperature = resp.envTemperature,
                        humidity = resp.humidity,
                        editSetPoint = null,
                        error = null
                    )
                } else if (resp.error == 2) {
                    session.clearSession()
                    _ui.value = UiState(isLoggedIn = false, sessionExpired = true)
                } else {
                    setError(errorDescription(resp.error))
                }
            } catch (e: Exception) {
                setError("Errore di connessione: ${e.message}")
            }
            setBusy(false)
        }
    }

    fun setStatus(newStatus: Int) {
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.setStatusAndSetpoint(
                    session.token,
                    status = newStatus,
                    currentSetPoint = _ui.value.setPointTemperature
                )
                if (resp.isOk) {
                    _ui.value = _ui.value.copy(status = newStatus, error = null)
                    refreshStatus()
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
            setBusy(false)
        }
    }

    fun adjustSetPoint(delta: Double) {
        val current = _ui.value.editSetPoint ?: _ui.value.setPointTemperature
        _ui.value = _ui.value.copy(editSetPoint = current + delta)
    }

    fun confirmSetPoint() {
        val target = _ui.value.editSetPoint ?: return
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.setStatusAndSetpoint(
                    session.token,
                    setPoint = target,
                    currentSetPoint = _ui.value.setPointTemperature
                )
                if (resp.isOk) {
                    _ui.value = _ui.value.copy(setPointTemperature = target, editSetPoint = null, error = null)
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
            setBusy(false)
        }
    }

    fun cancelSetPoint() {
        _ui.value = _ui.value.copy(editSetPoint = null)
    }

    // ── Zone ──────────────────────────────────────────────────────────

    fun loadZones() {
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.zones(session.token)
                if (resp.error == 0) {
                    _ui.value = _ui.value.copy(zones = resp.zones, error = null)
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
            setBusy(false)
        }
    }

    fun selectZone(zone: Zone) {
        _ui.value = _ui.value.copy(selectedZone = zone, zoneEditAdjustment = null)
        loadZoneTemperatures(zone.id)
    }

    fun clearSelectedZone() {
        _ui.value = _ui.value.copy(selectedZone = null, zoneTemps = null, zoneEditAdjustment = null)
    }

    fun loadZoneTemperatures(zoneId: String) {
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.zoneTemperatures(session.token, zoneId)
                if (resp.error == 0) {
                    _ui.value = _ui.value.copy(zoneTemps = resp, error = null)
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
            setBusy(false)
        }
    }

    fun adjustZone(delta: Double) {
        val current = _ui.value.zoneEditAdjustment
            ?: _ui.value.zoneTemps?.setPointTempAdjustment ?: 0.0
        _ui.value = _ui.value.copy(zoneEditAdjustment = current + delta)
    }

    fun confirmZoneAdjustment() {
        val zone = _ui.value.selectedZone ?: return
        val target = _ui.value.zoneEditAdjustment ?: return
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.setZoneTemperature(session.token, zone.id, target)
                if (resp.isOk) {
                    _ui.value = _ui.value.copy(zoneEditAdjustment = null, error = null)
                    loadZoneTemperatures(zone.id)
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
            setBusy(false)
        }
    }

    fun cancelZoneAdjustment() {
        _ui.value = _ui.value.copy(zoneEditAdjustment = null)
    }

    fun renameZone(zoneId: String, name: String) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.setZoneName(session.token, zoneId, name)
                if (resp.isOk) {
                    loadZones()
                    _ui.value = _ui.value.copy(selectedZone = _ui.value.selectedZone?.copy(name = name))
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
        }
    }

    // ── Consumi ───────────────────────────────────────────────────────

    fun loadConsumption() {
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.consumption(session.token)
                if (resp.error == 0) {
                    _ui.value = _ui.value.copy(consumption = resp, error = null)
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
            setBusy(false)
        }
    }

    // ── Storico ───────────────────────────────────────────────────────

    fun loadMetersHistory() {
        viewModelScope.launch {
            setBusy(true)
            try {
                val resp = ApiClient.metersHistory(session.token)
                if (resp.error == 0) {
                    _ui.value = _ui.value.copy(metersHistory = resp, error = null)
                } else setError(errorDescription(resp.error))
            } catch (e: Exception) {
                setError("Errore: ${e.message}")
            }
            setBusy(false)
        }
    }

    // ── Util ──────────────────────────────────────────────────────────

    private fun errorDescription(code: Int): String = when (code) {
        1 -> "Termostato non raggiungibile — riprova tra qualche istante"
        2 -> "Sessione scaduta — effettua nuovamente il login"
        3 -> "Account non attivo"
        4 -> "Sistema operativo non valido"
        5 -> "ID appartamento non valido"
        6 -> "Errore di connessione — controlla la rete"
        7 -> "Trasmissione in corso — riprova tra qualche secondo"
        8 -> "Parametri errati"
        9 -> "Zona non valida"
        else -> "Errore sconosciuto ($code)"
    }
}
