package com.nfctools.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nfctools.app.model.WritePayload
import com.nfctools.app.model.WriteType
import com.nfctools.app.viewmodel.NFCViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(viewModel: NFCViewModel) {
    var selectedType by remember { mutableStateOf(WriteType.TEXT) }
    var textContent by remember { mutableStateOf("") }
    var uriContent by remember { mutableStateOf("https://") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var rawHex by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("text/plain") }
    var showPendingDialog by remember { mutableStateOf(false) }
    val pendingWrite by viewModel.pendingWrite

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Write to NFC Tag", style = MaterialTheme.typography.headlineMedium)

        pendingWrite?.let {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Pending, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Write Pending", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Scan a tag to write this data", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { viewModel.clearPendingWrite() }) { Text("Cancel") }
                }
            }
        }

        val writeTypes = WriteType.values()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            writeTypes.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = writeTypes.size)
                ) { Text(type.name.replace("_", " ")) }
            }
        }

        when (selectedType) {
            WriteType.TEXT -> OutlinedTextField(value = textContent, onValueChange = { textContent = it }, label = { Text("Text Content") }, placeholder = { Text("Enter text to write...") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
            WriteType.URI -> OutlinedTextField(value = uriContent, onValueChange = { uriContent = it }, label = { Text("URI / URL") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())
            WriteType.CONTACT -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = contactName, onValueChange = { contactName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            }
            WriteType.WIFI -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it }, label = { Text("WiFi Network Name (SSID)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wifiPassword, onValueChange = { wifiPassword = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            }
            WriteType.RAW_NDEF, WriteType.RAW_DATA -> OutlinedTextField(value = rawHex, onValueChange = { rawHex = it }, label = { Text("Raw Hex Data") }, placeholder = { Text("E.g., D1010F5402656E48656C6C6F") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            WriteType.MIME -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = mimeType, onValueChange = { mimeType = it }, label = { Text("MIME Type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = textContent, onValueChange = { textContent = it }, label = { Text("Content") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
            WriteType.SMART_POSTER -> OutlinedTextField(value = uriContent, onValueChange = { uriContent = it }, label = { Text("Smart Poster URI") }, modifier = Modifier.fillMaxWidth())
        }

        val isContentValid = when (selectedType) {
            WriteType.TEXT -> textContent.isNotBlank()
            WriteType.URI -> uriContent.isNotBlank()
            WriteType.CONTACT -> contactName.isNotBlank()
            WriteType.WIFI -> wifiSsid.isNotBlank()
            WriteType.RAW_NDEF, WriteType.RAW_DATA -> rawHex.isNotBlank()
            WriteType.MIME -> textContent.isNotBlank() && mimeType.isNotBlank()
            WriteType.SMART_POSTER -> uriContent.isNotBlank()
        }

        Button(
            onClick = {
                val payload = when (selectedType) {
                    WriteType.TEXT -> WritePayload(WriteType.TEXT, textContent)
                    WriteType.URI -> WritePayload(WriteType.URI, uriContent)
                    WriteType.CONTACT -> WritePayload(WriteType.CONTACT, "BEGIN:VCARD
VERSION:3.0
FN:$contactName
TEL:$contactPhone
EMAIL:$contactEmail
END:VCARD")
                    WriteType.WIFI -> WritePayload(WriteType.WIFI, "SSID:$wifiSsid;PWD:$wifiPassword")
                    WriteType.RAW_NDEF -> WritePayload(WriteType.RAW_NDEF, rawHex)
                    WriteType.RAW_DATA -> WritePayload(WriteType.RAW_DATA, rawHex, mimeType)
                    WriteType.MIME -> WritePayload(WriteType.MIME, textContent, mimeType)
                    WriteType.SMART_POSTER -> WritePayload(WriteType.SMART_POSTER, uriContent)
                }
                viewModel.setPendingWrite(payload)
                showPendingDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isContentValid && pendingWrite == null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Prepare to Write")
        }

        ElevatedCard(shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Instructions", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("1. Enter the content you want to write", style = MaterialTheme.typography.bodyMedium)
                Text("2. Tap 'Prepare to Write'", style = MaterialTheme.typography.bodyMedium)
                Text("3. Hold an NFC tag against your device", style = MaterialTheme.typography.bodyMedium)
                Text("4. Wait for the success confirmation", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { showPendingDialog = false },
            icon = { Icon(Icons.Default.Nfc, contentDescription = null) },
            title = { Text("Write Ready") },
            text = { Text("Hold an NFC tag against the back of your device to write the data.") },
            confirmButton = { TextButton(onClick = { showPendingDialog = false }) { Text("Got it") } }
        )
    }
}
