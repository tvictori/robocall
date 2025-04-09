package com.titovictoriano.robocall.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothController(private val context: Context) {
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    init {
        updateBluetoothState()
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    private fun updateBluetoothState() {
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
    }

    fun enableBluetooth(): Boolean {
        if (!hasBluetoothPermission()) {
            return false
        }

        return if (bluetoothAdapter?.isEnabled == false) {
            try {
                // For Android 12 and above, we need to show system dialog
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(enableBtIntent)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.enable()
                    updateBluetoothState()
                    true
                }
            } catch (e: SecurityException) {
                false
            }
        } else true
    }

    fun disableBluetooth(): Boolean {
        if (!hasBluetoothPermission()) {
            return false
        }

        return if (bluetoothAdapter?.isEnabled == true) {
            try {
                // For Android 12 and above, we should show a system dialog
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val disableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    disableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(disableBtIntent)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.disable()
                    updateBluetoothState()
                    true
                }
            } catch (e: SecurityException) {
                false
            }
        } else true
    }

    fun startDiscovery(): Boolean {
        if (!hasBluetoothPermission()) {
            return false
        }

        return try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter?.startDiscovery() ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    fun stopDiscovery() {
        if (!hasBluetoothPermission()) {
            return
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            // Handle security exception
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            return emptyList()
        }

        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }
} 