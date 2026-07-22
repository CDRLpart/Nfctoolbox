package com.nfctools.app.viewmodel

import android.app.Application
import android.nfc.Tag
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nfctools.app.nfc.NFCManager
import com.nfctools.app.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class NFCViewModel(application: Application) : AndroidViewModel(application) {

    private val nfcManager = NFCManager(application)

    val currentTag = mutableStateOf<TagInfo?>(null)
    val lastRawTag = mutableStateOf<Tag?>(null)
    val isScanning = mutableStateOf(false)
    val nfcEnabled = mutableStateOf(nfcManager.isNfcEnabled())
    val nfcSupported = mutableStateOf(nfcManager.isNfcSupported())
    val tagHistory = mutableStateListOf<TagHistoryEntry>()
    val copiedTagData = mutableStateOf<TagCopyData?>(null)
    val rawDump = mutableStateOf<String>("")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val successMessage = mutableStateOf<String?>(null)
    val pendingWrite = mutableStateOf<PendingWrite?>(null)

    private val _events = MutableSharedFlow<NFCEvent>()
    val events: SharedFlow<NFCEvent> = _events

    sealed class NFCEvent {
        data class TagScanned(val tagInfo: TagInfo) : NFCEvent()
        data class WriteComplete(val success: Boolean, val message: String) : NFCEvent()
        data class CopyComplete(val success: Boolean, val message: String) : NFCEvent()
        data class FormatComplete(val success: Boolean, val message: String) : NFCEvent()
        data class ProtectComplete(val success: Boolean, val message: String) : NFCEvent()
        data class DumpComplete(val success: Boolean, val dump: String) : NFCEvent()
    }

    fun onTagDiscovered(tag: Tag) {
        lastRawTag.value = tag
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Check for pending write first
                pendingWrite.value?.let { pending ->
                    executeWrite(tag, pending.payload)
                    pendingWrite.value = null
                    return@launch
                }

                val tagInfo = nfcManager.readTagInfo(tag)
                currentTag.value = tagInfo
                val entry = TagHistoryEntry(
                    id = tagInfo.id, hexId = tagInfo.hexId, type = tagInfo.type,
                    timestamp = tagInfo.timestamp, action = "Read"
                )
                tagHistory.add(0, entry)
                if (tagHistory.size > 100) tagHistory.removeAt(tagHistory.lastIndex)
                _events.emit(NFCEvent.TagScanned(tagInfo))
            } catch (e: Exception) {
                errorMessage.value = "Error reading tag: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun setPendingWrite(payload: WritePayload) {
        pendingWrite.value = PendingWrite(payload)
        successMessage.value = "Pending write set. Scan a tag to write."
    }

    private fun executeWrite(tag: Tag, payload: WritePayload) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = nfcManager.writeTag(tag, payload)
                result.fold(
                    onSuccess = {
                        successMessage.value = "Tag written successfully!"
                        _events.emit(NFCEvent.WriteComplete(true, "Write successful"))
                        onTagDiscovered(tag)
                    },
                    onFailure = { error ->
                        errorMessage.value = "Write failed: ${error.message}"
                        _events.emit(NFCEvent.WriteComplete(false, error.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                errorMessage.value = "Write error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun writeTag(tag: Tag, payload: WritePayload) {
        executeWrite(tag, payload)
    }

    fun copyTag(tag: Tag) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = nfcManager.copyTagData(tag)
                result.fold(
                    onSuccess = { copyData ->
                        copiedTagData.value = copyData
                        successMessage.value = "Tag copied to memory!"
                        _events.emit(NFCEvent.CopyComplete(true, "Copy successful"))
                    },
                    onFailure = { error ->
                        errorMessage.value = "Copy failed: ${error.message}"
                        _events.emit(NFCEvent.CopyComplete(false, error.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                errorMessage.value = "Copy error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun pasteTag(tag: Tag) {
        val copyData = copiedTagData.value ?: return
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = nfcManager.writeCopyData(tag, copyData)
                result.fold(
                    onSuccess = {
                        successMessage.value = "Tag pasted successfully!"
                        _events.emit(NFCEvent.WriteComplete(true, "Paste successful"))
                        onTagDiscovered(tag)
                    },
                    onFailure = { error ->
                        errorMessage.value = "Paste failed: ${error.message}"
                        _events.emit(NFCEvent.WriteComplete(false, error.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                errorMessage.value = "Paste error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun formatTag(tag: Tag, readOnly: Boolean = false) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = if (readOnly) nfcManager.formatReadOnly(tag) else nfcManager.formatTag(tag)
                result.fold(
                    onSuccess = {
                        successMessage.value = if (readOnly) "Tag formatted as read-only!" else "Tag formatted successfully!"
                        _events.emit(NFCEvent.FormatComplete(true, "Format successful"))
                        onTagDiscovered(tag)
                    },
                    onFailure = { error ->
                        errorMessage.value = "Format failed: ${error.message}"
                        _events.emit(NFCEvent.FormatComplete(false, error.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                errorMessage.value = "Format error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun protectTag(tag: Tag, config: ProtectionConfig) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = nfcManager.protectTag(tag, config)
                result.fold(
                    onSuccess = {
                        successMessage.value = "Tag protected successfully!"
                        _events.emit(NFCEvent.ProtectComplete(true, "Protection applied"))
                        onTagDiscovered(tag)
                    },
                    onFailure = { error ->
                        errorMessage.value = "Protection failed: ${error.message}"
                        _events.emit(NFCEvent.ProtectComplete(false, error.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                errorMessage.value = "Protection error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun dumpTag(tag: Tag) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = nfcManager.getRawDump(tag)
                result.fold(
                    onSuccess = { dump ->
                        rawDump.value = dump
                        _events.emit(NFCEvent.DumpComplete(true, dump))
                    },
                    onFailure = { error ->
                        errorMessage.value = "Dump failed: ${error.message}"
                        _events.emit(NFCEvent.DumpComplete(false, ""))
                    }
                )
            } catch (e: Exception) {
                errorMessage.value = "Dump error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearMessages() { errorMessage.value = null; successMessage.value = null }
    fun clearCurrentTag() { currentTag.value = null }
    fun clearCopiedData() { copiedTagData.value = null }
    fun clearPendingWrite() { pendingWrite.value = null }
    fun checkNfcStatus() { nfcEnabled.value = nfcManager.isNfcEnabled(); nfcSupported.value = nfcManager.isNfcSupported() }
    fun openNfcSettings() { nfcManager.openNfcSettings() }
    fun bytesToHex(bytes: ByteArray): String = nfcManager.bytesToHex(bytes)
}
