package com.titovictoriano.robocall.util

import com.titovictoriano.robocall.model.VoiceCommand

object CommandParser {
    private val CALL_KEYWORDS = listOf("call", "phone", "dial")
    private val TEXT_KEYWORDS = listOf("text", "message", "send message to", "send text to")

    fun parseCommand(text: String): VoiceCommand {
        val lowerText = text.lowercase()
        
        return when {
            // Call commands
            CALL_KEYWORDS.any { lowerText.startsWith("$it ") } -> {
                extractCallCommand(lowerText)
            }
            
            // Text message commands
            TEXT_KEYWORDS.any { lowerText.startsWith("$it ") } -> {
                extractTextCommand(lowerText)
            }
            
            // Bluetooth commands
            lowerText.matches(Regex("(turn on|enable) bluetooth.*")) -> 
                VoiceCommand.EnableBluetooth
            
            lowerText.matches(Regex("(turn off|disable) bluetooth.*")) -> 
                VoiceCommand.DisableBluetooth
            
            lowerText.matches(Regex("(scan|search|look) for (bluetooth )?devices.*")) -> 
                VoiceCommand.ScanBluetoothDevices
            
            lowerText.startsWith("connect to ") -> {
                val deviceName = text.substring(11).trim()
                VoiceCommand.ConnectBluetoothDevice(deviceName)
            }
            
            else -> VoiceCommand.Unknown
        }
    }

    private fun extractCallCommand(input: String): VoiceCommand {
        val contactName = CALL_KEYWORDS
            .firstOrNull { keyword -> input.startsWith(keyword) }
            ?.let { keyword ->
                input.substringAfter(keyword).trim()
            }
        
        return if (!contactName.isNullOrBlank()) {
            VoiceCommand.Call(contactName)
        } else {
            VoiceCommand.Unknown
        }
    }

    private fun extractTextCommand(input: String): VoiceCommand {
        val keyword = TEXT_KEYWORDS
            .firstOrNull { keyword -> input.startsWith(keyword) }
            ?: return VoiceCommand.Unknown

        val remaining = input.substringAfter(keyword).trim()
        
        // Try to extract "[command] [contact] saying [message]" pattern
        if (remaining.contains(" saying ")) {
            val contactName = remaining.substringBefore(" saying ").trim()
            val message = remaining.substringAfter(" saying ").trim()
            
            if (contactName.isNotBlank() && message.isNotBlank()) {
                return VoiceCommand.SendText(contactName, message)
            }
        }
        
        // Try to extract "[command] [contact] [message]" pattern
        val words = remaining.split(" ")
        if (words.size >= 2) {
            val contactName = words[0]
            val message = words.subList(1, words.size).joinToString(" ")
            if (contactName.isNotBlank() && message.isNotBlank()) {
                return VoiceCommand.SendText(contactName, message)
            }
        }

        return VoiceCommand.Unknown
    }
} 