package it.termostato.app.data

/**
 * Log di una chiamata API — simile al tab "Network" dei DevTools del browser.
 */
data class NetworkLog(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String = "GET",
    val url: String,
    val path: String = url.substringAfterLast("/vc_service/"),
    val status: Int = 0,          // 0 = in corso, >0 = HTTP status, -1 = errore
    val duration: Long = 0,       // ms
    val requestBody: String = "",
    val responseBody: String = "",
    val error: String? = null,
    val state: CallState = CallState.PENDING
) {
    enum class CallState { PENDING, SUCCESS, ERROR, TIMEOUT }

    val isFinished: Boolean get() = state != CallState.PENDING

    val statusLabel: String get() = when (state) {
        CallState.PENDING -> "⏳ …"
        CallState.SUCCESS -> "✅ $status"
        CallState.ERROR -> "❌ ${error ?: "errore"}"
        CallState.TIMEOUT -> "⏱ timeout"
    }

    val durationLabel: String get() = when {
        !isFinished -> "—"
        duration < 1000 -> "${duration}ms"
        else -> "${"%.1f".format(duration / 1000.0)}s"
    }

    val sizeLabel: String get() {
        val bytes = responseBody.length
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / 1024.0 / 1024.0)} MB"
        }
    }
}
