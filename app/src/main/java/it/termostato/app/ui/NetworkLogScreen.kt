package it.termostato.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.termostato.app.data.NetworkLog
import it.termostato.app.data.NetworkLogger
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkLogScreen(
    onBack: () -> Unit
) {
    val logs by NetworkLogger.logs.collectAsState()
    var selectedLog by remember { mutableStateOf<NetworkLog?>(null) }
    var filter by remember { mutableStateOf("all") } // all | errors | pending

    val filtered = when (filter) {
        "errors" -> logs.filter { it.state == NetworkLog.CallState.ERROR || it.state == NetworkLog.CallState.TIMEOUT }
        "pending" -> logs.filter { it.state == NetworkLog.CallState.PENDING }
        else -> logs
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text("Rete", fontWeight = FontWeight.Bold)
                    Text(
                        "${logs.size} richieste",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            },
            actions = {
                IconButton(onClick = { NetworkLogger.clear() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Cancella log")
                }
            }
        )

        // Filtri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == "all",
                onClick = { filter = "all" },
                label = { Text("Tutte (${logs.size})") }
            )
            FilterChip(
                selected = filter == "errors",
                onClick = { filter = "errors" },
                label = {
                    Text("Errori (${logs.count { it.state == NetworkLog.CallState.ERROR || it.state == NetworkLog.CallState.TIMEOUT }})")
                }
            )
            FilterChip(
                selected = filter == "pending",
                onClick = { filter = "pending" },
                label = { Text("In corso (${logs.count { it.state == NetworkLog.CallState.PENDING }})") }
            )
        }

        // Stato vuoto
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Nessuna chiamata registrata",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Le chiamate API appariranno qui automaticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Lista richieste
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.id }) { log ->
                    NetworkLogItem(
                        log = log,
                        isExpanded = selectedLog?.id == log.id,
                        onClick = {
                            selectedLog = if (selectedLog?.id == log.id) null else log
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkLogItem(
    log: NetworkLog,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (log.state) {
        NetworkLog.CallState.PENDING -> Color(0xFF9E9E9E)
        NetworkLog.CallState.SUCCESS -> Color(0xFF4CAF50)
        NetworkLog.CallState.ERROR -> Color(0xFFF44336)
        NetworkLog.CallState.TIMEOUT -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp)
        ) {
            // Riga principale
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Indicatore stato
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                )
                Spacer(Modifier.width(8.dp))

                // Metodo
                Text(
                    text = log.method,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))

                // Path
                Text(
                    text = log.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Durata
                Text(
                    text = log.durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Sottotitolo
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
                Spacer(Modifier.width(8.dp))
                if (log.isFinished && log.responseBody.isNotBlank()) {
                    Text(
                        text = log.sizeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dettagli espansi
            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(Modifier.height(8.dp))

                // URL completo
                DetailRow(label = "URL", value = log.url)

                // Timestamp
                DetailRow(
                    label = "Ora",
                    value = SimpleDateFormat("HH:mm:ss.SSS", Locale.ITALY).format(Date(log.timestamp))
                )

                // Error message
                if (log.error != null) {
                    DetailRow(label = "Errore", value = log.error, isError = true)
                }

                // Response body
                if (log.responseBody.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Risposta",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = prettyJson(log.responseBody),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF9CDCFE),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isError: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Formatta JSON compatto in modo leggibile. */
private fun prettyJson(raw: String): String {
    if (!raw.startsWith("{") && !raw.startsWith("[")) return raw
    val sb = StringBuilder()
    var indent = 0
    var inString = false
    var i = 0
    while (i < raw.length) {
        val c = raw[i]
        when {
            c == '"' && (i == 0 || raw[i - 1] != '\\') -> {
                inString = !inString
                sb.append(c)
            }
            inString -> sb.append(c)
            c == '{' || c == '[' -> {
                sb.append(c).append('\n')
                indent++
                sb.append("  ".repeat(indent))
            }
            c == '}' || c == ']' -> {
                sb.append('\n')
                indent--
                sb.append("  ".repeat(indent)).append(c)
            }
            c == ',' -> {
                sb.append(c).append('\n').append("  ".repeat(indent))
            }
            c == ':' -> sb.append(c).append(' ')
            c == ' ' && sb.lastOrNull() == ' ' -> {} // skip spazi multipli
            else -> sb.append(c)
        }
        i++
    }
    return sb.toString()
}
