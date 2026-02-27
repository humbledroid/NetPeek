package io.netpeek.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen(viewModel: SampleViewModel, onOpenInspector: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NetPeek Sample", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "A KMP app using the NetPeek SDK",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInspector) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Open Inspector",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                "API Endpoints",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ApiButton(
                    label = "GET  /get",
                    color = Color(0xFF4CAF50),
                    isLoading = state.activeRequest == ActiveRequest.GET,
                    enabled = state.activeRequest == null,
                    modifier = Modifier.weight(1f)
                ) { viewModel.fireGet() }

                ApiButton(
                    label = "POST  /post",
                    color = Color(0xFF2196F3),
                    isLoading = state.activeRequest == ActiveRequest.POST,
                    enabled = state.activeRequest == null,
                    modifier = Modifier.weight(1f)
                ) { viewModel.firePost() }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ApiButton(
                    label = "404  /status/404",
                    color = Color(0xFFF44336),
                    isLoading = state.activeRequest == ActiveRequest.ERROR_404,
                    enabled = state.activeRequest == null,
                    modifier = Modifier.weight(1f)
                ) { viewModel.fire404() }

                ApiButton(
                    label = "⏱  Timeout",
                    color = Color(0xFFFF9800),
                    isLoading = state.activeRequest == ActiveRequest.TIMEOUT,
                    enabled = state.activeRequest == null,
                    modifier = Modifier.weight(1f)
                ) { viewModel.fireTimeout() }
            }

            // Response log
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.activeRequest != null) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                    Text(
                        text = state.log,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "💡  Tap the bug icon above to open the NetPeek inspector.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ApiButton(
    label: String,
    color: Color,
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(label, fontFamily = FontFamily.Monospace)
        }
    }
}
