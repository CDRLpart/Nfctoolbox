package com.nfctools.app.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class NFCHostApduService : HostApduService() {

    companion object {
        private const val TAG = "NFCHostApduService"
        private val SUCCESS_SW = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val FAILURE_SW = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SELECT_AID_CLA = 0x00
        private val SELECT_AID_INS = 0xA4.toByte()
        private val SELECT_AID_P1 = 0x04.toByte()
        private val SELECT_AID_P2 = 0x00.toByte()

        private val SAMPLE_NDEF = byteArrayOf(
            0xD1.toByte(), 0x01.toByte(), 0x19.toByte(),
            0x54.toByte(), 0x02.toByte(), 0x65.toByte(), 0x6E.toByte(),
            0x48.toByte(), 0x65.toByte(), 0x6C.toByte(), 0x6C.toByte(),
            0x6F.toByte(), 0x20.toByte(), 0x66.toByte(), 0x72.toByte(),
            0x6F.toByte(), 0x6D.toByte(), 0x20.toByte(), 0x4E.toByte(),
            0x46.toByte(), 0x43.toByte(), 0x20.toByte(), 0x54.toByte(),
            0x6F.toByte(), 0x6F.toByte(), 0x6C.toByte(), 0x73.toByte(),
            0x21.toByte()
        )
    }

    private var selectedAid: ByteArray? = null
    private var ndefMessage: ByteArray = SAMPLE_NDEF

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "HCE Deactivated: $reason")
        selectedAid = null
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) return FAILURE_SW
        return try {
            when {
                commandApdu[0] == SELECT_AID_CLA.toByte() && commandApdu[1] == SELECT_AID_INS &&
                commandApdu[2] == SELECT_AID_P1 && commandApdu[3] == SELECT_AID_P2 -> {
                    val aidLength = commandApdu[4].toInt()
                    if (commandApdu.size >= 5 + aidLength) {
                        selectedAid = commandApdu.copyOfRange(5, 5 + aidLength)
                        Log.d(TAG, "AID Selected: ${selectedAid?.let { bytesToHex(it) }}")
                        SUCCESS_SW
                    } else FAILURE_SW
                }
                commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xB0.toByte() -> handleReadBinary(commandApdu)
                commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xD6.toByte() -> handleUpdateBinary(commandApdu)
                else -> { Log.d(TAG, "Unknown APDU: ${bytesToHex(commandApdu)}"); FAILURE_SW }
            }
        } catch (e: Exception) { Log.e(TAG, "APDU processing error", e); FAILURE_SW }
    }

    private fun handleReadBinary(commandApdu: ByteArray): ByteArray {
        val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
        val length = commandApdu[4].toInt()
        val response = if (offset < ndefMessage.size) {
            val end = minOf(offset + length, ndefMessage.size)
            ndefMessage.copyOfRange(offset, end)
        } else byteArrayOf()
        return response + SUCCESS_SW
    }

    private fun handleUpdateBinary(commandApdu: ByteArray): ByteArray {
        val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
        val length = commandApdu[4].toInt()
        if (commandApdu.size >= 5 + length) {
            val data = commandApdu.copyOfRange(5, 5 + length)
            val newNdef = ndefMessage.copyOf(maxOf(ndefMessage.size, offset + length))
            System.arraycopy(data, 0, newNdef, offset, length)
            ndefMessage = newNdef
            Log.d(TAG, "NDEF updated at offset $offset, length $length")
            SUCCESS_SW
        } else FAILURE_SW
    }

    private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { String.format("%02X", it) }
}
