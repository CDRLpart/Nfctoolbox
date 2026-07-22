package com.nfctools.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nfctools.app.viewmodel.NFCViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: NFCViewModel) {
    val history = viewModel.tagHistory
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Scan History", style = MaterialTheme.typography.headlineMedium)
            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.tagHistory.clear() }) { Text("Clear All") }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (history.isEmpty()) {
            EmptyState(icon = Icons.Default.History, title = "No history yet", message = "Scanned tags will appear here")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { entry ->
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Column {
                                    Text(entry.type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("ID: ${entry.hexId.take(12)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                            AssistChip(onClick = { }, label = { Text(entry.action) }, leadingIcon = { Icon(Icons.Default.RadioButtonChecked, contentDescription = null, modifier = Modifier.size(14.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer))
                        }
                    }
                }
            }
        }
    }
}
