package com.nfctools.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nfctools.app.model.ProtectionConfig
import com.nfctools.app.model.ProtectionType
import com.nfctools.app.viewmodel.NFCViewModel
import com.nfctools.app.ui.theme.NfcBlue
import com.nfctools.app.ui.theme.NfcGreen
import com.nfctools.app.ui.theme.NfcOrange
import com.nfctools.app.ui.theme.NfcPurple
import com.nfctools.app.ui.theme.NfcRed

@Composable
fun ToolsScreen(viewModel: NFCViewModel) {
    val tag = viewModel.currentTag.value
    val lastRawTag = viewModel.lastRawTag.value
    val copiedData by viewModel.copiedTagData
    val rawDumpText by viewModel.rawDump
    val scrollState = rememberScrollState()

    var showFormatDialog by remember { mutableStateOf(false) }
    var showProtectDialog by remember { mutableStateOf(false) }
    var showDumpSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NFC Tools", style = MaterialTheme.typography.headlineMedium)

        if (tag == null) {
            EmptyState(icon = Icons.Default.Build, title = "No tag scanned", message = "Scan a tag first to use these tools")
            return@Column
        }

        ToolCard(title = "Copy Tag", description = "Copy all tag data to memory for cloning", icon = Icons.Default.ContentCopy, action = "Copy", accentColor = NfcBlue, onAction = { lastRawTag?.let { viewModel.copyTag(it) } })

        if (copiedData != null) {
            ToolCard(title = "Paste/Clone Tag", description = "Write copied data to a new tag", icon = Icons.Default.ContentPaste, action = "Paste", accentColor = NfcGreen, isHighlighted = true, onAction = { lastRawTag?.let { viewModel.pasteTag(it) } })
        }

        ToolCard(title = "Raw Hex Dump", description = if (rawDumpText.isEmpty()) "Complete memory dump in hexadecimal" else "View saved dump", icon = Icons.Default.Terminal, action = if (rawDumpText.isEmpty()) "Dump" else "View", accentColor = NfcPurple, onAction = { if (rawDumpText.isEmpty()) { lastRawTag?.let { viewModel.dumpTag(it) } } else { showDumpSheet = true } })

        ToolCard(title = "Format Tag", description = "Erase and reformat to factory NDEF", icon = Icons.Default.DeleteForever, action = "Format", accentColor = NfcRed, isDanger = true, onAction = { showFormatDialog = true })

        ToolCard(title = "Write Protection", description = "Password protect or permanently lock", icon = Icons.Default.Lock, action = "Protect", accentColor = NfcOrange, onAction = { showProtectDialog = true })
    }

    if (showFormatDialog) {
        FormatDialog(onDismiss = { showFormatDialog = false }, onFormat = { readOnly -> lastRawTag?.let { viewModel.formatTag(it, readOnly) }; showFormatDialog = false })
    }

    if (showProtectDialog) {
        ProtectDialog(onDismiss = { showProtectDialog = false }, onProtect = { config -> lastRawTag?.let { viewModel.protectTag(it, config) }; showProtectDialog = false })
    }

    if (showDumpSheet) {
        ModalBottomSheet(onDismissRequest = { showDumpSheet = false }) {
            Column(modifier = Modifier.fillMaxHeight(0.85f).padding(16.dp)) {
                Text("Raw Hex Dump", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                OutlinedCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Text(text = rawDumpText, modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDumpSheet = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Close") }
            }
        }
    }
}

@Composable
fun ToolCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: String, accentColor: androidx.compose.ui.graphics.Color, onAction: () -> Unit, isHighlighted: Boolean = false, isDanger: Boolean = false) {
    val cardColor = when {
        isDanger -> MaterialTheme.colorScheme.errorContainer
        isHighlighted -> accentColor.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = cardColor)) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(12.dp), color = accentColor.copy(alpha = 0.15f)) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = if (isDanger) MaterialTheme.colorScheme.error else accentColor)
                    }
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onAction, colors = if (isDanger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(10.dp)) { Text(action) }
        }
    }
}

@Composable
fun FormatDialog(onDismiss: () -> Unit, onFormat: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Format Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This will erase all data on the tag!", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Standard format: Reformat to empty NDEF (recoverable)", style = MaterialTheme.typography.bodySmall)
                Text("Read-only format: Permanent factory format (irreversible!)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onFormat(false) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(10.dp)) { Text("Standard Format") }
                OutlinedButton(onClick = { onFormat(true) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(10.dp)) { Text("Read-Only Format (PERMANENT)") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectDialog(onDismiss: () -> Unit, onProtect: (ProtectionConfig) -> Unit) {
    var protectionType by remember { mutableStateOf(ProtectionType.PASSWORD) }
    var password by remember { mutableStateOf("") }
    var fromPage by remember { mutableStateOf("4") }
    var permanent by remember { mutableStateOf(false) }
    val protectionTypes = listOf(ProtectionType.PASSWORD, ProtectionType.PERMANENT_LOCK, ProtectionType.AUTHENTICATED)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Tag Protection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow {
                    protectionTypes.forEachIndexed { index, type ->
                        SegmentedButton(selected = protectionType == type, onClick = { protectionType = type }, shape = SegmentedButtonDefaults.itemShape(index = index, count = protectionTypes.size)) {
                            Text(when (type) { ProtectionType.PASSWORD -> "Password"; ProtectionType.PERMANENT_LOCK -> "Lock"; ProtectionType.AUTHENTICATED -> "Auth"; else -> "None" })
                        }
                    }
                }
                if (protectionType == ProtectionType.PASSWORD) {
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (4+ chars)") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = fromPage, onValueChange = { fromPage = it }, label = { Text("Protect From Page/Sector") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                if (protectionType == ProtectionType.PERMANENT_LOCK) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = permanent, onCheckedChange = { permanent = it })
                        Text("I understand this is PERMANENT and IRREVERSIBLE", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onProtect(ProtectionConfig(type = protectionType, password = if (protectionType == ProtectionType.PASSWORD) password else null, fromPage = fromPage.toIntOrNull() ?: 4, permanent = permanent)) },
                enabled = when (protectionType) { ProtectionType.PASSWORD -> password.length >= 4; ProtectionType.PERMANENT_LOCK -> permanent; else -> true },
                shape = RoundedCornerShape(10.dp)
            ) { Text("Apply Protection") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
