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
import androidx.core.content.ContextCompat
import com.titovictoriano.robocall.model.VoiceCommand

class CommandExecutor(private val context: Context) {
    
    fun executeCommand(command: VoiceCommand): Boolean {
        return when (command) {
            is VoiceCommand.Call -> initiateCall(command.contactName)
            is VoiceCommand.SendText -> sendTextMessage(command.contactName, command.message)
            VoiceCommand.Unknown -> false
        }
    }

    private fun initiateCall(contactName: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val phoneNumber = findContactPhoneNumber(contactName) ?: return false

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
            return false
        }

        val phoneNumber = findContactPhoneNumber(contactName) ?: return false

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    private fun findContactPhoneNumber(contactName: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return null
    }
} 