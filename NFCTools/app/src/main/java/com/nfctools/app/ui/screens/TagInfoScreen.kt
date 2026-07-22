package com.nfctools.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nfctools.app.viewmodel.NFCViewModel
import com.nfctools.app.ui.theme.NfcBlue
import com.nfctools.app.ui.theme.NfcCyan
import com.nfctools.app.ui.theme.NfcGreen
import com.nfctools.app.ui.theme.NfcOrange
import com.nfctools.app.ui.theme.NfcPurple
import com.nfctools.app.ui.theme.NfcYellow

@Composable
fun TagInfoScreen(viewModel: NFCViewModel) {
    val tag = viewModel.currentTag.value
    val scrollState = rememberScrollState()

    if (tag == null) {
        EmptyState(
            icon = Icons.Default.Nfc,
            title = "No tag scanned yet",
            message = "Scan a tag first to see detailed information"
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Column {
                    Text(tag.type, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("ID: ${tag.hexId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        // Basic Info
        InfoCard(title = "Basic Information", icon = Icons.Default.Info) {
            InfoRow("Tag ID (Hex)", tag.hexId)
            InfoRow("Memory Size", "${tag.size} bytes")
            InfoRow("Writable", if (tag.writable) "Yes ✓" else "No ✗")
            InfoRow("Can Lock", if (tag.canMakeReadOnly) "Yes ✓" else "No ✗")
        }

        // Technologies
        InfoCard(title = "Technologies", icon = Icons.Default.DeveloperBoard) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tag.technologies.forEach { tech ->
                    val color = when {
                        "MifareClassic" in tech -> NfcGreen
                        "MifareUltralight" in tech -> NfcBlue
                        "IsoDep" in tech -> NfcPurple
                        "NfcV" in tech -> NfcYellow
                        "NfcF" in tech -> NfcCyan
                        "Ndef" in tech -> NfcOrange
                        else -> MaterialTheme.colorScheme.primary
                    }
                    AssistChip(
                        onClick = { },
                        label = { Text(tech) },
                        leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = color)
                    )
                }
            }
        }

        // NDEF Message
        tag.ndefMessage?.let { ndef ->
            InfoCard(title = "NDEF Message", icon = Icons.Default.Message) {
                InfoRow("NDEF Type", ndef.type)
                InfoRow("Message Size", "${ndef.size} bytes")
                InfoRow("Records", "${ndef.records.size}")

                ndef.records.forEachIndexed { index, record ->
                    Spacer(Modifier.height(8.dp))
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Record ${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            InfoRow("TNF", record.tnf)
                            InfoRow("Type", record.type)
                            record.languageCode?.let { InfoRow("Language", it) }
                            record.uri?.let { InfoRow("URI", it) }
                            Text("Payload: ${record.payload.take(120)}${if (record.payload.length > 120) "..." else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Tech Details
        tag.techDetails.nfcA?.let {
            TechDetailCard("NFC-A (ISO 14443-A)", Icons.Default.Wifi, NfcGreen) {
                InfoRow("ATQA", it.atqa)
                InfoRow("SAK", it.sak)
                InfoRow("Max Transceive", "${it.maxTransceiveLength} bytes")
                InfoRow("Timeout", "${it.timeout} ms")
            }
        }

        tag.techDetails.mifareClassic?.let {
            TechDetailCard("MIFARE Classic", Icons.Default.SdStorage, NfcBlue) {
                InfoRow("Type", it.type)
                InfoRow("Sectors", "${it.sectorCount}")
                InfoRow("Blocks", "${it.blockCount}")
                InfoRow("Block Size", "${it.blockSize} bytes")
                InfoRow("Total Memory", "${it.totalMemory} bytes")
                Text("${it.sectors.count { s -> s.isAuthenticated }}/${it.sectorCount} sectors authenticated", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        tag.techDetails.mifareUltralight?.let {
            TechDetailCard("MIFARE Ultralight", Icons.Default.SdStorage, NfcCyan) {
                InfoRow("Type", it.type)
                InfoRow("Pages", "${it.pageCount}")
                InfoRow("Total Memory", "${it.size} bytes")
            }
        }

        tag.techDetails.isoDep?.let {
            TechDetailCard("ISO-DEP", Icons.Default.Security, NfcPurple) {
                InfoRow("Historical Bytes", it.historicalBytes)
                InfoRow("Extended APDU", if (it.extendedLengthApduSupported) "Yes" else "No")
                InfoRow("Max Transceive", "${it.maxTransceiveLength} bytes")
            }
        }
    }
}

@Composable
fun InfoCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
fun TechDetailCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: androidx.compose.ui.graphics.Color, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.15f)) {
                    Box(modifier = Modifier.padding(6.dp)) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = accentColor)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(modifier = Modifier.padding(24.dp)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

// InfoRow is defined in ScanScreen.kt (same package: com.nfctools.app.ui.screens)
