package io.netpeek.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.netpeek.sdk.NetworkCall
import io.netpeek.sdk.NetworkCallRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPeekListScreen(
    repository: NetworkCallRepository,
    onCallSelected: (NetworkCall) -> Unit
) {
    val calls by repository.getAllCalls().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val filtered = if (searchQuery.isBlank()) calls
    else calls.filter { it.url.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NetPeek")
                        Spacer(Modifier.width(8.dp))
                        if (calls.isNotEmpty()) {
                            Badge { Text(calls.size.toString()) }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { repository.clearAll() } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search URL\u2026") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No requests captured yet.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { call ->
                        NetworkCallRow(call = call, onClick = { onCallSelected(call) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkCallRow(call: NetworkCall, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MethodBadge(method = call.method)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatTimestamp(call.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            StatusCodeBadge(code = call.responseCode, isError = call.isError)
            if (call.durationMs != null) {
                Text(
                    text = "${call.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun MethodBadge(method: String) {
    val color = when (method.uppercase()) {
        "GET" -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        "PUT" -> Color(0xFFFF9800)
        "DELETE" -> Color(0xFFF44336)
        "PATCH" -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = method.uppercase(),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatusCodeBadge(code: Int?, isError: Boolean) {
    val color = when {
        code == null -> Color(0xFF9E9E9E)
        isError || code >= 400 -> Color(0xFFF44336)
        code >= 300 -> Color(0xFFFF9800)
        code >= 200 -> Color(0xFF4CAF50)
        else -> Color(0xFF9E9E9E)
    }
    Text(
        text = code?.toString() ?: "\u2014",
        color = color,
        style = MaterialTheme.typography.labelMedium
    )
}

private fun formatTimestamp(epochMs: Long): String {
    // Simple formatting without java.util.Date for KMP compatibility
    val seconds = epochMs / 1000
    val minutes = seconds / 60 % 60
    val hours = seconds / 3600 % 24
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
}
