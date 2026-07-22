package com.nfctools.app.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.nfctools.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

class NFCManager(private val context: Context) {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    companion object {
        const val TAG = "NFCManager"

        val DEFAULT_KEY_A = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val DEFAULT_KEY_B = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

        val MIFARE_CLASSIC_1K_KEYS = listOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()),
            byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
            byteArrayOf(0x4D.toByte(), 0x3A.toByte(), 0x99.toByte(), 0xC3.toByte(), 0x51.toByte(), 0xDD.toByte()),
            byteArrayOf(0x1A.toByte(), 0x98.toByte(), 0x2C.toByte(), 0x7E.toByte(), 0x45.toByte(), 0x9A.toByte()),
            byteArrayOf(0xA0.toByte(), 0xB0.toByte(), 0xC0.toByte(), 0xD0.toByte(), 0xE0.toByte(), 0xF0.toByte()),
            byteArrayOf(0xA1.toByte(), 0xB1.toByte(), 0xC1.toByte(), 0xD1.toByte(), 0xE1.toByte(), 0xF1.toByte()),
            byteArrayOf(0x71.toByte(), 0x4C.toByte(), 0x5C.toByte(), 0x88.toByte(), 0x6E.toByte(), 0x97.toByte()),
            byteArrayOf(0x58.toByte(), 0x7E.toByte(), 0xE5.toByte(), 0xF9.toByte(), 0x35.toByte(), 0x0F.toByte()),
            byteArrayOf(0xA0.toByte(), 0x47.toByte(), 0x05.toByte(), 0xDF.toByte(), 0xB5.toByte(), 0xA2.toByte()),
            byteArrayOf(0x53.toByte(), 0x3C.toByte(), 0xB6.toByte(), 0xC7.toByte(), 0x23.toByte(), 0x3F.toByte()),
            byteArrayOf(0x8F.toByte(), 0xD0.toByte(), 0xA4.toByte(), 0xF2.toByte(), 0x56.toByte(), 0xE9.toByte())
        )

        val URI_PREFIX_MAP = mapOf(
            0x00 to "", 0x01 to "http://www.", 0x02 to "https://www.",
            0x03 to "http://", 0x04 to "https://", 0x05 to "tel:",
            0x06 to "mailto:", 0x07 to "ftp://anonymous:anonymous@",
            0x08 to "ftp://ftp.", 0x09 to "ftps://", 0x0A to "sftp://",
            0x0B to "smb://", 0x0C to "nfs://", 0x0D to "ftp://",
            0x0E to "dav://", 0x0F to "news:", 0x10 to "telnet://",
            0x11 to "imap:", 0x12 to "rtsp://", 0x13 to "urn:",
            0x14 to "pop:", 0x15 to "sip:", 0x16 to "sips:",
            0x17 to "tftp:", 0x18 to "btspp://", 0x19 to "btl2cap://",
            0x1A to "btgoep://", 0x1B to "tcpobex://", 0x1C to "irdaobex://",
            0x1D to "file://", 0x1E to "urn:epc:id:", 0x1F to "urn:epc:tag:",
            0x20 to "urn:epc:pat:", 0x21 to "urn:epc:raw:", 0x22 to "urn:epc:",
            0x23 to "urn:nfc:"
        )
    }

    init { nfcAdapter = NfcAdapter.getDefaultAdapter(context) }

    fun isNfcSupported(): Boolean = nfcAdapter != null
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true
    fun openNfcSettings() {
        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun enableForegroundDispatch(activity: Activity) {
        if (nfcAdapter == null) return
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        val filters = arrayOf(
            android.content.IntentFilter("android.nfc.action.NDEF_DISCOVERED"),
            android.content.IntentFilter("android.nfc.action.TECH_DISCOVERED"),
            android.content.IntentFilter("android.nfc.action.TAG_DISCOVERED")
        )
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    suspend fun readTagInfo(tag: Tag): TagInfo = withContext(Dispatchers.IO) {
        val id = tag.id?.let { bytesToHex(it) } ?: "Unknown"
        val techList = tag.techList.map { it.substringAfterLast('.') }
        val ndef = Ndef.get(tag)
        val ndefMessage = ndef?.let { readNdefMessage(it) }
        val techDetails = readTechDetails(tag)
        val type = determineTagType(tag, techList)
        val size = calculateTagSize(tag, techList)
        val writable = isTagWritable(tag)
        val canMakeReadOnly = ndef?.canMakeReadOnly() ?: false

        TagInfo(
            id = id, hexId = id, technologies = techList, type = type,
            size = size, writable = writable, canMakeReadOnly = canMakeReadOnly,
            ndefMessage = ndefMessage, techDetails = techDetails
        )
    }

    private fun readNdefMessage(ndef: Ndef): NdefMessageInfo? {
        return try {
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()
            message?.let { msg ->
                NdefMessageInfo(
                    records = msg.records.map { parseNdefRecord(it) },
                    type = ndef.type ?: "Unknown",
                    size = msg.toByteArray().size
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading NDEF", e)
            null
        }
    }

    private fun parseNdefRecord(record: NdefRecord): NdefRecordInfo {
        val tnf = when (record.tnf) {
            NdefRecord.TNF_EMPTY -> "EMPTY"
            NdefRecord.TNF_WELL_KNOWN -> "WELL_KNOWN"
            NdefRecord.TNF_MIME_MEDIA -> "MEDIA"
            NdefRecord.TNF_ABSOLUTE_URI -> "ABSOLUTE_URI"
            NdefRecord.TNF_EXTERNAL_TYPE -> "EXTERNAL_TYPE"
            NdefRecord.TNF_UNKNOWN -> "UNKNOWN"
            NdefRecord.TNF_UNCHANGED -> "UNCHANGED"
            else -> "UNKNOWN"
        }
        val type = String(record.type, Charset.forName("UTF-8"))
        val payload = record.payload
        var text: String? = null
        var uri: String? = null
        var mimeType: String? = null
        var languageCode: String? = null
        var isText = false
        var isUri = false

        when {
            record.tnf == NdefRecord.TNF_WELL_KNOWN && type == "T" -> {
                isText = true
                if (payload.isNotEmpty()) {
                    val statusByte = payload[0].toInt()
                    val utf16 = statusByte and 0x80 != 0
                    val languageCodeLength = statusByte and 0x3F
                    languageCode = String(payload, 1, languageCodeLength, Charset.forName("US-ASCII"))
                    val textEncoding = if (utf16) Charset.forName("UTF-16") else Charset.forName("UTF-8")
                    text = String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, textEncoding)
                }
            }
            record.tnf == NdefRecord.TNF_WELL_KNOWN && type == "U" -> {
                isUri = true
                if (payload.isNotEmpty()) {
                    val uriPrefix = URI_PREFIX_MAP[payload[0].toInt()] ?: ""
                    uri = uriPrefix + String(payload, 1, payload.size - 1, Charset.forName("UTF-8"))
                }
            }
            record.tnf == NdefRecord.TNF_MIME_MEDIA -> {
                mimeType = type
                text = bytesToHex(payload)
            }
            record.tnf == NdefRecord.TNF_ABSOLUTE_URI -> {
                isUri = true
                uri = String(record.type, Charset.forName("UTF-8"))
            }
            else -> { text = bytesToHex(payload) }
        }

        return NdefRecordInfo(
            tnf = tnf, type = type,
            payload = text ?: uri ?: bytesToHex(payload),
            mimeType = mimeType, uri = uri, languageCode = languageCode,
            isText = isText, isUri = isUri
        )
    }

    private fun readTechDetails(tag: Tag): TechDetails {
        return TechDetails(
            nfcA = NfcA.get(tag)?.let {
                NfcAInfo(atqa = bytesToHex(it.atqa), sak = String.format("0x%02X", it.sak),
                    maxTransceiveLength = it.maxTransceiveLength, timeout = it.timeout)
            },
            nfcB = NfcB.get(tag)?.let {
                NfcBInfo(applicationData = bytesToHex(it.applicationData),
                    protocolInfo = bytesToHex(it.protocolInfo), maxTransceiveLength = it.maxTransceiveLength)
            },
            nfcF = NfcF.get(tag)?.let {
                NfcFInfo(manufacturer = bytesToHex(it.manufacturer), systemCode = bytesToHex(it.systemCode),
                    maxTransceiveLength = it.maxTransceiveLength, timeout = it.timeout)
            },
            nfcV = NfcV.get(tag)?.let {
                NfcVInfo(dsfId = String.format("0x%02X", it.dsfId),
                    responseFlags = String.format("0x%02X", it.responseFlags), maxTransceiveLength = it.maxTransceiveLength)
            },
            isoDep = IsoDep.get(tag)?.let {
                IsoDepInfo(historicalBytes = bytesToHex(it.historicalBytes),
                    extendedLengthApduSupported = it.isExtendedLengthApduSupported,
                    maxTransceiveLength = it.maxTransceiveLength, timeout = it.timeout)
            },
            mifareClassic = readMifareClassicDetails(tag),
            mifareUltralight = readMifareUltralightDetails(tag),
            ndef = Ndef.get(tag)?.let {
                NdefTechInfo(type = it.type ?: "Unknown", maxSize = it.maxSize,
                    isWritable = it.isWritable, canMakeReadOnly = it.canMakeReadOnly())
            },
            ndefFormatable = NdefFormatable.get(tag) != null
        )
    }

    private fun readMifareClassicDetails(tag: Tag): MifareClassicInfo? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            mifare.connect()
            val type = when (mifare.type) {
                MifareClassic.TYPE_CLASSIC -> "Classic"
                MifareClassic.TYPE_PLUS -> "Plus"
                MifareClassic.TYPE_PRO -> "Pro"
                else -> "Unknown"
            }
            val sectors = (0 until mifare.sectorCount).map { sectorIndex ->
                val blocks = mutableListOf<BlockInfo>()
                val firstBlock = mifare.sectorToBlock(sectorIndex)
                val blockCountInSector = mifare.getBlockCountInSector(sectorIndex)
                var authenticated = false
                var keyA: String? = null
                var keyB: String? = null
                var accessBits = ""

                for (key in MIFARE_CLASSIC_1K_KEYS) {
                    try { if (mifare.authenticateSectorWithKeyA(sectorIndex, key)) { authenticated = true; keyA = bytesToHex(key); break } } catch (_: Exception) {}
                }
                if (!authenticated) {
                    for (key in MIFARE_CLASSIC_1K_KEYS) {
                        try { if (mifare.authenticateSectorWithKeyB(sectorIndex, key)) { authenticated = true; keyB = bytesToHex(key); break } } catch (_: Exception) {}
                    }
                }

                for (blockOffset in 0 until blockCountInSector) {
                    val blockIndex = firstBlock + blockOffset
                    val isTrailer = blockOffset == blockCountInSector - 1
                    val data = if (authenticated) {
                        try {
                            val blockData = mifare.readBlock(blockIndex)
                            if (isTrailer) {
                                keyA = bytesToHex(blockData.copyOfRange(0, 6))
                                accessBits = bytesToHex(blockData.copyOfRange(6, 10))
                                keyB = bytesToHex(blockData.copyOfRange(10, 16))
                            }
                            bytesToHex(blockData)
                        } catch (e: Exception) { "READ_ERROR" }
                    } else { "AUTH_REQUIRED" }
                    blocks.add(BlockInfo(index = blockIndex, data = data, isTrailer = isTrailer))
                }
                SectorInfo(index = sectorIndex, blocks = blocks, keyA = keyA, keyB = keyB,
                    accessBits = accessBits, isAuthenticated = authenticated)
            }
            mifare.close()
            MifareClassicInfo(type = type, size = mifare.size, sectorCount = mifare.sectorCount,
                blockCount = mifare.blockCount, blockSize = MifareClassic.BLOCK_SIZE,
                totalMemory = mifare.size, sectors = sectors)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Mifare Classic", e)
            try { mifare.close() } catch (_: Exception) {}
            null
        }
    }

    private fun readMifareUltralightDetails(tag: Tag): MifareUltralightInfo? {
        val ultralight = MifareUltralight.get(tag) ?: return null
        return try {
            ultralight.connect()
            val type = when (ultralight.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                else -> "Unknown"
            }
            val pageCount = if (type == "Ultralight C") 48 else 16
            val size = pageCount * 4
            val pages = (0 until pageCount).map { pageIndex ->
                val data = try { bytesToHex(ultralight.readPages(pageIndex)) } catch (e: Exception) { "READ_ERROR" }
                PageInfo(index = pageIndex, data = data)
            }
            ultralight.close()
            MifareUltralightInfo(type = type, size = size, pageCount = pageCount, pages = pages, timeout = ultralight.timeout)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Mifare Ultralight", e)
            try { ultralight.close() } catch (_: Exception) {}
            null
        }
    }

    private fun determineTagType(tag: Tag, techList: List<String>): String {
        return when {
            "MifareClassic" in techList -> "MIFARE Classic"
            "MifareUltralight" in techList -> "MIFARE Ultralight"
            "IsoDep" in techList -> "ISO-DEP (DESFire/ISO 14443-4)"
            "NfcV" in techList -> "NFC-V (ISO 15693)"
            "NfcF" in techList -> "NFC-F (FeliCa)"
            "NfcB" in techList -> "NFC-B (ISO 14443-B)"
            "NfcA" in techList -> "NFC-A (ISO 14443-A)"
            "Ndef" in techList -> "NDEF Formatted"
            else -> "Unknown"
        }
    }

    private fun calculateTagSize(tag: Tag, techList: List<String>): Int {
        return when {
            "MifareClassic" in techList -> MifareClassic.get(tag)?.size ?: 0
            "MifareUltralight" in techList -> {
                val ul = MifareUltralight.get(tag)
                if (ul?.type == MifareUltralight.TYPE_ULTRALIGHT_C) 192 else 64
            }
            "Ndef" in techList -> Ndef.get(tag)?.maxSize ?: 0
            else -> 0
        }
    }

    private fun isTagWritable(tag: Tag): Boolean {
        val ndef = Ndef.get(tag)
        return ndef?.isWritable ?: (NdefFormatable.get(tag) != null)
    }

    // ==================== WRITE ====================
    suspend fun writeTag(tag: Tag, payload: WritePayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ndef = Ndef.get(tag) ?: NdefFormatable.get(tag)
                ?: return@withContext Result.failure(Exception("Tag does not support NDEF"))
            val message = createNdefMessage(payload)
            when (ndef) {
                is Ndef -> {
                    ndef.connect()
                    if (ndef.isWritable) {
                        ndef.writeNdefMessage(message)
                        ndef.close()
                        Result.success(Unit)
                    } else {
                        ndef.close()
                        Result.failure(Exception("Tag is not writable"))
                    }
                }
                is NdefFormatable -> {
                    ndef.connect()
                    ndef.format(message)
                    ndef.close()
                    Result.success(Unit)
                }
                else -> Result.failure(Exception("Unsupported tag type"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Write error", e)
            Result.failure(e)
        }
    }

    private fun createNdefMessage(payload: WritePayload): NdefMessage {
        val record = when (payload.type) {
            WriteType.TEXT -> createTextRecord(payload.data, payload.language)
            WriteType.URI -> createUriRecord(payload.data)
            WriteType.CONTACT -> createContactRecord(payload.data)
            WriteType.WIFI -> createWifiRecord(payload.data)
            WriteType.RAW_NDEF -> createRawNdefRecord(payload.data)
            WriteType.RAW_DATA -> createMimeRecord(payload.data, payload.mimeType ?: "application/octet-stream")
            WriteType.SMART_POSTER -> createSmartPosterRecord(payload.data)
            WriteType.MIME -> createMimeRecord(payload.data, payload.mimeType ?: "application/octet-stream")
        }
        return NdefMessage(record)
    }

    private fun createTextRecord(text: String, language: String): NdefRecord {
        val langBytes = language.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = langBytes.size.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, "T".toByteArray(), ByteArray(0), payload)
    }

    private fun createUriRecord(uri: String): NdefRecord = NdefRecord.createUri(uri)

    private fun createContactRecord(vcardData: String): NdefRecord {
        return NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/vcard".toByteArray(), ByteArray(0),
            vcardData.toByteArray(Charset.forName("UTF-8")))
    }

    private fun createWifiRecord(wifiConfig: String): NdefRecord {
        return NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, "application/vnd.wfa.wsc".toByteArray(),
            ByteArray(0), wifiConfig.toByteArray(Charset.forName("UTF-8")))
    }

    private fun createRawNdefRecord(hexData: String): NdefRecord {
        return NdefRecord(NdefRecord.TNF_UNKNOWN, ByteArray(0), ByteArray(0), hexToBytes(hexData))
    }

    private fun createMimeRecord(data: String, mimeType: String): NdefRecord {
        return NdefRecord.createMime(mimeType, data.toByteArray(Charset.forName("UTF-8")))
    }

    private fun createSmartPosterRecord(data: String): NdefRecord {
        val uriRecord = createUriRecord(data)
        val titleRecord = createTextRecord(data, "en")
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, "Sp".toByteArray(), ByteArray(0),
            uriRecord.payload + titleRecord.payload)
    }

    // ==================== FORMAT ====================
    suspend fun formatTag(tag: Tag): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formatable = NdefFormatable.get(tag)
                ?: return@withContext Result.failure(Exception("Tag is not NDEF formatable"))
            formatable.connect()
            formatable.format(NdefMessage(NdefRecord.createTextRecord("en", "")))
            formatable.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Format error", e)
            Result.failure(e)
        }
    }

    suspend fun formatReadOnly(tag: Tag): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formatable = NdefFormatable.get(tag)
                ?: return@withContext Result.failure(Exception("Tag is not NDEF formatable"))
            formatable.connect()
            formatable.formatReadOnly(NdefMessage(NdefRecord.createTextRecord("en", "")))
            formatable.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Format readonly error", e)
            Result.failure(e)
        }
    }

    // ==================== COPY ====================
    suspend fun copyTagData(tag: Tag): Result<TagCopyData> = withContext(Dispatchers.IO) {
        try {
            val techList = tag.techList.map { it.substringAfterLast('.') }
            val rawData = mutableListOf<ByteArray>()
            var ndefMessage: ByteArray? = null

            Ndef.get(tag)?.let { ndef ->
                try {
                    ndef.connect()
                    ndef.ndefMessage?.let { msg -> ndefMessage = msg.toByteArray() }
                    ndef.close()
                } catch (e: Exception) { try { ndef.close() } catch (_: Exception) {} }
            }

            if ("MifareClassic" in techList) {
                MifareClassic.get(tag)?.let { mifare ->
                    try {
                        mifare.connect()
                        for (sector in 0 until mifare.sectorCount) {
                            var authenticated = false
                            for (key in MIFARE_CLASSIC_1K_KEYS) {
                                try { if (mifare.authenticateSectorWithKeyA(sector, key)) { authenticated = true; break } } catch (_: Exception) {}
                            }
                            if (authenticated) {
                                val firstBlock = mifare.sectorToBlock(sector)
                                val blockCount = mifare.getBlockCountInSector(sector)
                                for (block in 0 until blockCount) {
                                    try { rawData.add(mifare.readBlock(firstBlock + block)) } catch (e: Exception) { rawData.add(ByteArray(16) { 0x00.toByte() }) }
                                }
                            }
                        }
                        mifare.close()
                    } catch (e: Exception) { try { mifare.close() } catch (_: Exception) {} }
                }
            }

            if ("MifareUltralight" in techList) {
                MifareUltralight.get(tag)?.let { ultralight ->
                    try {
                        ultralight.connect()
                        val pageCount = if (ultralight.type == MifareUltralight.TYPE_ULTRALIGHT_C) 48 else 16
                        for (page in 0 until pageCount step 4) {
                            try { rawData.add(ultralight.readPages(page)) } catch (e: Exception) { rawData.add(ByteArray(16) { 0x00.toByte() }) }
                        }
                        ultralight.close()
                    } catch (e: Exception) { try { ultralight.close() } catch (_: Exception) {} }
                }
            }

            Result.success(TagCopyData(sourceId = bytesToHex(tag.id), techType = techList.firstOrNull() ?: "Unknown",
                rawData = rawData, ndefMessage = ndefMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Copy error", e)
            Result.failure(e)
        }
    }

    suspend fun writeCopyData(tag: Tag, copyData: TagCopyData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            copyData.ndefMessage?.let { bytes ->
                val ndef = Ndef.get(tag)
                if (ndef != null && ndef.isWritable) {
                    ndef.connect()
                    ndef.writeNdefMessage(NdefMessage(bytes))
                    ndef.close()
                    return@withContext Result.success(Unit)
                }
            }

            if ("MifareClassic" in tag.techList.map { it.substringAfterLast('.') } && copyData.techType == "MifareClassic") {
                MifareClassic.get(tag)?.let { mifare ->
                    try {
                        mifare.connect()
                        var blockIndex = 0
                        for (sector in 0 until mifare.sectorCount) {
                            var authenticated = false
                            for (key in MIFARE_CLASSIC_1K_KEYS) {
                                try { if (mifare.authenticateSectorWithKeyA(sector, key)) { authenticated = true; break } } catch (_: Exception) {}
                            }
                            if (authenticated) {
                                val blockCount = mifare.getBlockCountInSector(sector)
                                for (block in 0 until blockCount) {
                                    if (blockIndex < copyData.rawData.size) {
                                        try { mifare.writeBlock(blockIndex, copyData.rawData[blockIndex]) } catch (e: Exception) { Log.w(TAG, "Failed to write block $blockIndex", e) }
                                    }
                                    blockIndex++
                                }
                            }
                        }
                        mifare.close()
                        return@withContext Result.success(Unit)
                    } catch (e: Exception) { try { mifare.close() } catch (_: Exception) {} }
                }
            }
            Result.failure(Exception("Could not write copy data to tag"))
        } catch (e: Exception) {
            Log.e(TAG, "Write copy error", e)
            Result.failure(e)
        }
    }

    // ==================== PROTECTION ====================
    suspend fun protectTag(tag: Tag, config: ProtectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when {
                "MifareUltralight" in tag.techList.map { it.substringAfterLast('.') } -> protectMifareUltralight(tag, config)
                "MifareClassic" in tag.techList.map { it.substringAfterLast('.') } -> protectMifareClassic(tag, config)
                "Ndef" in tag.techList.map { it.substringAfterLast('.') } -> protectNdefTag(tag, config)
                else -> Result.failure(Exception("Protection not supported for this tag type"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Protection error", e)
            Result.failure(e)
        }
    }

    private fun protectMifareUltralight(tag: Tag, config: ProtectionConfig): Result<Unit> {
        val ultralight = MifareUltralight.get(tag) ?: return Result.failure(Exception("Not a MIFARE Ultralight tag"))
        return try {
            ultralight.connect()
            if (config.type == ProtectionType.PASSWORD && config.password != null) {
                val pwd = config.password.toByteArray(Charset.forName("UTF-8")).copyOf(4)
                val pack = byteArrayOf(0x00.toByte(), 0x00.toByte())
                val type = ultralight.type
                val (pwdPage, packPage, auth0Page) = when (type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> Triple(0, 0, 0)
                    else -> Triple(133, 134, 19)
                }
                if (pwdPage > 0) {
                    ultralight.writePage(pwdPage, pwd.copyOf(4))
                    ultralight.writePage(packPage, pack.copyOf(4))
                    ultralight.writePage(auth0Page, byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), config.fromPage.toByte()))
                }
            } else if (config.type == ProtectionType.PERMANENT_LOCK) {
                ultralight.writePage(2, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
            }
            ultralight.close()
            Result.success(Unit)
        } catch (e: Exception) {
            try { ultralight.close() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    private fun protectMifareClassic(tag: Tag, config: ProtectionConfig): Result<Unit> {
        val mifare = MifareClassic.get(tag) ?: return Result.failure(Exception("Not a MIFARE Classic tag"))
        return try {
            mifare.connect()
            for (sector in 0 until mifare.sectorCount) {
                val trailerBlock = mifare.sectorToBlock(sector) + mifare.getBlockCountInSector(sector) - 1
                var authenticated = false
                for (key in MIFARE_CLASSIC_1K_KEYS) {
                    try { if (mifare.authenticateSectorWithKeyA(sector, key)) { authenticated = true; break } } catch (_: Exception) {}
                }
                if (authenticated) {
                    val accessBits = byteArrayOf(0xFF.toByte(), 0x07.toByte(), 0x80.toByte())
                    val trailerData = DEFAULT_KEY_A + accessBits + DEFAULT_KEY_B
                    mifare.writeBlock(trailerBlock, trailerData)
                }
            }
            mifare.close()
            Result.success(Unit)
        } catch (e: Exception) {
            try { mifare.close() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    private fun protectNdefTag(tag: Tag, config: ProtectionConfig): Result<Unit> {
        val ndef = Ndef.get(tag) ?: return Result.failure(Exception("Not an NDEF tag"))
        return try {
            if (config.type == ProtectionType.PERMANENT_LOCK && ndef.canMakeReadOnly()) {
                ndef.connect()
                ndef.makeReadOnly()
                ndef.close()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Permanent lock not supported or password protection not available for this NDEF tag"))
            }
        } catch (e: Exception) {
            try { ndef.close() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    // ==================== RAW DUMP ====================
    suspend fun getRawDump(tag: Tag): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            val techList = tag.techList.map { it.substringAfterLast('.') }
            sb.appendLine("=== NFC TAG RAW DUMP ===")
            sb.appendLine("Tag ID: ${bytesToHex(tag.id)}")
            sb.appendLine("Technologies: ${techList.joinToString(", ")}")
            sb.appendLine("Timestamp: ${java.util.Date()}")
            sb.appendLine()

            if ("MifareClassic" in techList) {
                sb.appendLine("--- MIFARE CLASSIC DUMP ---")
                MifareClassic.get(tag)?.let { mifare ->
                    mifare.connect()
                    for (sector in 0 until mifare.sectorCount) {
                        sb.appendLine("Sector $sector:")
                        var authenticated = false
                        for (key in MIFARE_CLASSIC_1K_KEYS) {
                            try { if (mifare.authenticateSectorWithKeyA(sector, key)) { authenticated = true; break } } catch (_: Exception) {}
                        }
                        if (!authenticated) {
                            for (key in MIFARE_CLASSIC_1K_KEYS) {
                                try { if (mifare.authenticateSectorWithKeyB(sector, key)) { authenticated = true; break } } catch (_: Exception) {}
                            }
                        }
                        val firstBlock = mifare.sectorToBlock(sector)
                        val blockCount = mifare.getBlockCountInSector(sector)
                        for (block in 0 until blockCount) {
                            val blockIndex = firstBlock + block
                            val data = if (authenticated) {
                                try { bytesToHex(mifare.readBlock(blockIndex)) } catch (e: Exception) { "READ_ERROR" }
                            } else { "AUTH_REQUIRED" }
                            val marker = if (block == blockCount - 1) " [TRAILER]" else ""
                            sb.appendLine("  Block $blockIndex: $data$marker")
                        }
                        sb.appendLine()
                    }
                    mifare.close()
                }
            }

            if ("MifareUltralight" in techList) {
                sb.appendLine("--- MIFARE ULTRALIGHT DUMP ---")
                MifareUltralight.get(tag)?.let { ultralight ->
                    ultralight.connect()
                    val pageCount = if (ultralight.type == MifareUltralight.TYPE_ULTRALIGHT_C) 48 else 16
                    for (page in 0 until pageCount) {
                        val data = try { bytesToHex(ultralight.readPages(page).copyOf(4)) } catch (e: Exception) { "READ_ERROR" }
                        sb.appendLine("  Page $page: $data")
                    }
                    ultralight.close()
                }
                sb.appendLine()
            }

            if ("Ndef" in techList) {
                sb.appendLine("--- NDEF MESSAGE DUMP ---")
                Ndef.get(tag)?.let { ndef ->
                    ndef.connect()
                    ndef.ndefMessage?.let { message ->
                        sb.appendLine("NDEF Type: ${ndef.type}")
                        sb.appendLine("Max Size: ${ndef.maxSize} bytes")
                        sb.appendLine("Writable: ${ndef.isWritable}")
                        sb.appendLine()
                        message.records.forEachIndexed { index, record ->
                            sb.appendLine("Record $index:")
                            sb.appendLine("  TNF: ${record.tnf}")
                            sb.appendLine("  Type: ${String(record.type, Charset.forName("UTF-8"))}")
                            sb.appendLine("  ID: ${bytesToHex(record.id)}")
                            sb.appendLine("  Payload (${record.payload.size} bytes):")
                            sb.appendLine("    Hex: ${bytesToHex(record.payload)}")
                            sb.appendLine("    UTF-8: ${String(record.payload, Charset.forName("UTF-8")).take(200)}")
                            sb.appendLine()
                        }
                    }
                    ndef.close()
                }
            }

            if ("IsoDep" in techList) {
                sb.appendLine("--- ISO-DEP INFO ---")
                IsoDep.get(tag)?.let { isoDep ->
                    sb.appendLine("  Historical Bytes: ${bytesToHex(isoDep.historicalBytes)}")
                    sb.appendLine("  Extended APDU: ${isoDep.isExtendedLengthApduSupported}")
                    sb.appendLine("  Max Transceive: ${isoDep.maxTransceiveLength}")
                    sb.appendLine("  Timeout: ${isoDep.timeout}")
                }
                sb.appendLine()
            }

            if ("NfcA" in techList) {
                sb.appendLine("--- NFC-A INFO ---")
                NfcA.get(tag)?.let { nfcA ->
                    sb.appendLine("  ATQA: ${bytesToHex(nfcA.atqa)}")
                    sb.appendLine("  SAK: 0x${String.format("%02X", nfcA.sak)}")
                    sb.appendLine("  Max Transceive: ${nfcA.maxTransceiveLength}")
                    sb.appendLine("  Timeout: ${nfcA.timeout}")
                }
                sb.appendLine()
            }
            Result.success(sb.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Dump error", e)
            Result.failure(e)
        }
    }

    // ==================== UTILS ====================
    fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null) return "null"
        return bytes.joinToString("") { String.format("%02X", it) }
    }

    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace(":", "")
        return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
