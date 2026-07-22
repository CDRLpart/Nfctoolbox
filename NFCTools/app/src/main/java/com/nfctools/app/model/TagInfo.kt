package com.nfctools.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TagInfo(
    val id: String,
    val hexId: String,
    val technologies: List<String>,
    val type: String,
    val size: Int,
    val writable: Boolean,
    val canMakeReadOnly: Boolean,
    val ndefMessage: NdefMessageInfo?,
    val techDetails: TechDetails,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class NdefMessageInfo(
    val records: List<NdefRecordInfo>,
    val type: String,
    val size: Int
) : Parcelable

@Parcelize
data class NdefRecordInfo(
    val tnf: String,
    val type: String,
    val payload: String,
    val mimeType: String?,
    val uri: String?,
    val languageCode: String?,
    val isText: Boolean,
    val isUri: Boolean
) : Parcelable

@Parcelize
data class TechDetails(
    val nfcA: NfcAInfo?,
    val nfcB: NfcBInfo?,
    val nfcF: NfcFInfo?,
    val nfcV: NfcVInfo?,
    val isoDep: IsoDepInfo?,
    val mifareClassic: MifareClassicInfo?,
    val mifareUltralight: MifareUltralightInfo?,
    val ndef: NdefTechInfo?,
    val ndefFormatable: Boolean
) : Parcelable

@Parcelize
data class NfcAInfo(
    val atqa: String,
    val sak: String,
    val maxTransceiveLength: Int,
    val timeout: Int
) : Parcelable

@Parcelize
data class NfcBInfo(
    val applicationData: String,
    val protocolInfo: String,
    val maxTransceiveLength: Int
) : Parcelable

@Parcelize
data class NfcFInfo(
    val manufacturer: String,
    val systemCode: String,
    val maxTransceiveLength: Int,
    val timeout: Int
) : Parcelable

@Parcelize
data class NfcVInfo(
    val dsfId: String,
    val responseFlags: String,
    val maxTransceiveLength: Int
) : Parcelable

@Parcelize
data class IsoDepInfo(
    val historicalBytes: String,
    val extendedLengthApduSupported: Boolean,
    val maxTransceiveLength: Int,
    val timeout: Int
) : Parcelable

@Parcelize
data class MifareClassicInfo(
    val type: String,
    val size: Int,
    val sectorCount: Int,
    val blockCount: Int,
    val blockSize: Int,
    val totalMemory: Int,
    val sectors: List<SectorInfo>
) : Parcelable

@Parcelize
data class SectorInfo(
    val index: Int,
    val blocks: List<BlockInfo>,
    val keyA: String?,
    val keyB: String?,
    val accessBits: String,
    val isAuthenticated: Boolean
) : Parcelable

@Parcelize
data class BlockInfo(
    val index: Int,
    val data: String,
    val isTrailer: Boolean
) : Parcelable

@Parcelize
data class MifareUltralightInfo(
    val type: String,
    val size: Int,
    val pageCount: Int,
    val pages: List<PageInfo>,
    val timeout: Int
) : Parcelable

@Parcelize
data class PageInfo(
    val index: Int,
    val data: String
) : Parcelable

@Parcelize
data class NdefTechInfo(
    val type: String,
    val maxSize: Int,
    val isWritable: Boolean,
    val canMakeReadOnly: Boolean
) : Parcelable

@Parcelize
data class WritePayload(
    val type: WriteType,
    val data: String,
    val mimeType: String? = null,
    val language: String = "en"
) : Parcelable

enum class WriteType {
    TEXT, URI, CONTACT, WIFI, RAW_NDEF, RAW_DATA, SMART_POSTER, MIME
}

@Parcelize
data class TagCopyData(
    val sourceId: String,
    val techType: String,
    val rawData: List<ByteArray>,
    val ndefMessage: ByteArray?,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class ProtectionConfig(
    val type: ProtectionType,
    val password: String?,
    val fromPage: Int,
    val permanent: Boolean
) : Parcelable

enum class ProtectionType {
    NONE, PASSWORD, PERMANENT_LOCK, AUTHENTICATED
}

@Parcelize
data class TagHistoryEntry(
    val id: String,
    val hexId: String,
    val type: String,
    val timestamp: Long,
    val action: String
) : Parcelable

@Parcelize
data class PendingWrite(
    val payload: WritePayload,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
