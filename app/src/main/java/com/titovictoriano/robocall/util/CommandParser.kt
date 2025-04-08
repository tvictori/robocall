package com.titovictoriano.robocall.util

import com.titovictoriano.robocall.model.VoiceCommand

object CommandParser {
    private val CALL_KEYWORDS = listOf("call", "phone", "dial")
    private val TEXT_KEYWORDS = listOf("text", "message", "send message to", "send text to")

    fun parseCommand(input: String): VoiceCommand {
        val lowercaseInput = input.lowercase()
        
        return when {
            CALL_KEYWORDS.any { keyword -> lowercaseInput.startsWith(keyword) } -> {
                extractCallCommand(lowercaseInput)
            }
            TEXT_KEYWORDS.any { keyword -> lowercaseInput.startsWith(keyword) } -> {
                extractTextCommand(lowercaseInput)
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
        
        // Try to extract "text [contact] saying [message]" pattern
        if (remaining.contains("saying")) {
            val contactName = remaining.substringBefore("saying").trim()
            val message = remaining.substringAfter("saying").trim()
            
            if (contactName.isNotBlank() && message.isNotBlank()) {
                return VoiceCommand.SendText(contactName, message)
            }
        }
        
        // Try to extract "text [contact] [message]" pattern
        val words = remaining.split(" ")
        if (words.size >= 2) {
            val contactName = words[0]
            val message = words.subList(1, words.size).joinToString(" ")
            return VoiceCommand.SendText(contactName, message)
        }

        return VoiceCommand.Unknown
    }
} 