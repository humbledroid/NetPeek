package io.netpeek.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.netpeek.sdk.NetworkCall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPeekDetailScreen(
    call: NetworkCall,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Request", "Response")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> OverviewTab(call)
                    1 -> RequestTab(call)
                    2 -> ResponseTab(call)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(call: NetworkCall) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow("URL", call.url)
        DetailRow("Method", call.method)
        DetailRow("Status", call.responseCode?.toString() ?: "\u2014")
        DetailRow("Duration", call.durationMs?.let { "${it}ms" } ?: "\u2014")
        DetailRow("Timestamp", call.timestamp.toString())
        DetailRow("Error", if (call.isError) "Yes" else "No")
    }
}

@Composable
private fun RequestTab(call: NetworkCall) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Headers")
        CodeBlock(call.requestHeaders.prettyPrintJson())
        val requestBody = call.requestBody
        if (!requestBody.isNullOrBlank()) {
            SectionHeader("Body")
            CodeBlock(requestBody.prettyPrintJson())
        }
    }
}

@Composable
private fun ResponseTab(call: NetworkCall) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Headers")
        CodeBlock(call.responseHeaders?.prettyPrintJson() ?: "\u2014")
        val responseBody = call.responseBody
        if (!responseBody.isNullOrBlank()) {
            SectionHeader("Body")
            CodeBlock(responseBody.prettyPrintJson())
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(90.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    HorizontalDivider()
}

@Composable
private fun CodeBlock(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
    }
}

private fun String.prettyPrintJson(): String {
    return try {
        // Simple indentation for JSON-like strings
        if (startsWith("{") || startsWith("[")) {
            val sb = StringBuilder()
            var indent = 0
            var inString = false
            for (char in this) {
                when {
                    char == '"' -> { inString = !inString; sb.append(char) }
                    inString -> sb.append(char)
                    char == '{' || char == '[' -> {
                        sb.append(char)
                        sb.append('\n')
                        indent++
                        repeat(indent * 2) { sb.append(' ') }
                    }
                    char == '}' || char == ']' -> {
                        sb.append('\n')
                        indent--
                        repeat(indent * 2) { sb.append(' ') }
                        sb.append(char)
                    }
                    char == ',' -> {
                        sb.append(char)
                        sb.append('\n')
                        repeat(indent * 2) { sb.append(' ') }
                    }
                    char == ':' -> sb.append(": ")
                    else -> sb.append(char)
                }
            }
            sb.toString()
        } else this
    } catch (e: Exception) {
        this
    }
}
