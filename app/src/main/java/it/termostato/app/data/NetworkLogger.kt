package it.termostato.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Logger centralizzato per tutte le chiamate API.
 * Registra URL, tempi, risposte ed errori — come il tab Network dei DevTools.
 */
object NetworkLogger {

    private const val MAX_ENTRIES = 200

    private val _logs = MutableStateFlow<List<NetworkLog>>(emptyList())
    val logs: StateFlow<List<NetworkLog>> = _logs.asStateFlow()

    /** Registra l'inizio di una chiamata. Restituisce l'ID per completarla. */
    fun start(url: String, method: String = "GET", requestBody: String = ""): Long {
        val entry = NetworkLog(
            url = url,
            method = method,
            requestBody = requestBody,
            state = NetworkLog.CallState.PENDING
        )
        val current = _logs.value.toMutableList()
        current.add(0, entry) // più recente in cima
        _logs.value = current.take(MAX_ENTRIES)
        return entry.id
    }

    /** Registra il completamento con successo — sostituisce la voce pending. */
    fun complete(id: Long, status: Int, responseBody: String, duration: Long) {
        updateEntry(id) { entry ->
            entry.copy(
                status = status,
                responseBody = responseBody,
                duration = duration,
                state = if (status in 200..299) NetworkLog.CallState.SUCCESS else NetworkLog.CallState.ERROR,
                error = if (status !in 200..299) "HTTP $status" else null
            )
        }
    }

    /** Registra un errore di connessione/timeout. */
    fun error(id: Long, error: String, duration: Long, isTimeout: Boolean = false) {
        updateEntry(id) { entry ->
            entry.copy(
                status = -1,
                error = error,
                duration = duration,
                state = if (isTimeout) NetworkLog.CallState.TIMEOUT else NetworkLog.CallState.ERROR,
                responseBody = ""
            )
        }
    }

    /** Registra una risposta API (body JSON + codice errore API). */
    fun completeRaw(id: Long, responseBody: String, duration: Long, apiError: Int) {
        updateEntry(id) { entry ->
            entry.copy(
                status = if (apiError == 6) -1 else 200,
                responseBody = responseBody,
                duration = duration,
                state = when {
                    apiError == 0 -> NetworkLog.CallState.SUCCESS
                    apiError == 6 -> NetworkLog.CallState.ERROR
                    else -> NetworkLog.CallState.SUCCESS // HTTP 200 con errore API (es. error:1)
                },
                error = if (apiError != 0) "API error $apiError" else null
            )
        }
    }

    /** Aggiorna in-place la voce con l'ID dato. */
    private fun updateEntry(id: Long, transform: (NetworkLog) -> NetworkLog) {
        val current = _logs.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = transform(current[index])
            _logs.value = current
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
