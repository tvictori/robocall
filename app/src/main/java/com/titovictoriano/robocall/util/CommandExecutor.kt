package com.titovictoriano.robocall.util

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.titovictoriano.robocall.model.VoiceCommand

class CommandExecutor(private val context: Context) {
    private val bluetoothController = BluetoothController(context)
    private val TAG = "CommandExecutor"
    
    fun executeCommand(command: VoiceCommand): Boolean {
        return try {
            when (command) {
                is VoiceCommand.Call -> initiateCall(command.contactName)
                is VoiceCommand.SendText -> sendTextMessage(command.contactName, command.message)
                is VoiceCommand.EnableBluetooth -> bluetoothController.enableBluetooth()
                is VoiceCommand.DisableBluetooth -> bluetoothController.disableBluetooth()
                is VoiceCommand.ScanBluetoothDevices -> bluetoothController.startDiscovery()
                is VoiceCommand.ConnectBluetoothDevice -> connectToDevice(command.deviceName)
                VoiceCommand.Unknown -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: ${e.message}", e)
            false
        }
    }

    private fun initiateCall(contactName: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Call permission not granted")
            return false
        }

        val phoneNumber = findContactPhoneNumber(contactName)
        if (phoneNumber == null) {
            Log.w(TAG, "Could not find phone number for contact: $contactName")
            return false
        }

        // Use ACTION_CALL to directly initiate the call
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    private fun sendTextMessage(contactName: String, message: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SMS permission not granted")
            return false
        }

        val phoneNumber = findContactPhoneNumber(contactName)
        if (phoneNumber == null) {
            Log.w(TAG, "Could not find phone number for contact: $contactName")
            return false
        }

        try {
            // Use the default SMS manager instance for better compatibility
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }

            smsManager?.sendTextMessage(
                phoneNumber,  // destination
                null,        // service center (null for default)
                message,     // message content
                null,        // sent intent
                null         // delivery intent
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}", e)
            return false
        }
    }

    private fun connectToDevice(deviceName: String): Boolean {
        val devices = bluetoothController.getPairedDevices()
        val device = devices.find { it.name?.equals(deviceName, ignoreCase = true) == true }
        return device != null
    }

    private fun findContactPhoneNumber(contactName: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Contacts permission not granted")
            return null
        }

        val contentResolver: ContentResolver = context.contentResolver
        var cursor: Cursor? = null

        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactName%")

            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)

            if (cursor?.moveToFirst() == true) {
                return cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
            } else {
                Log.w(TAG, "No contact found with name: $contactName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding contact: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        return null
    }
} 