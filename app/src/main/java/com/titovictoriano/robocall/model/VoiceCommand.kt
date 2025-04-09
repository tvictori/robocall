package com.titovictoriano.robocall.model

sealed class VoiceCommand {
    data class Call(val contactName: String) : VoiceCommand()
    data class SendText(val contactName: String, val message: String) : VoiceCommand()
    object EnableBluetooth : VoiceCommand()
    object DisableBluetooth : VoiceCommand()
    object ScanBluetoothDevices : VoiceCommand()
    data class ConnectBluetoothDevice(val deviceName: String) : VoiceCommand()
    object Unknown : VoiceCommand()
}

sealed class VoiceState {
    object Idle : VoiceState()
    object Preparing : VoiceState()
    object Listening : VoiceState()
    data class Processing(val command: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
    data class Success(val message: String) : VoiceState()
} 