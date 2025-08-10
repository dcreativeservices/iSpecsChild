package com.ispecs.child.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import no.nordicsemi.android.support.v18.scanner.*

class BLEScanner(
    private val context: Context,
    private val onDeviceFound: (BluetoothDevice, Int) -> Unit
) {
    private var scanner = BluetoothLeScannerCompat.getScanner()
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setLegacy(false)
        .setReportDelay(0)
        .setUseHardwareBatchingIfSupported(false)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d("BLEScanner", "Device found: ${result.device.address} - RSSI: ${result.rssi}")
            onDeviceFound(result.device, result.rssi)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                Log.d("BLEScanner", "Batch Device found: ${result.device.address} - RSSI: ${result.rssi}")
                onDeviceFound(result.device, result.rssi)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLEScanner", "❌ Scan failed with error: $errorCode")
            if (errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                Log.e("BLEScanner", "⚠️ Scanner status=6 detected, resetting scanner...")
                safeRestartScan()
            }
        }
    }

    fun startScan() {
        if (hasPermissions()) {
            stopScan() // ✅ Always stop before starting new scan
            scanner.startScan(null, scanSettings, scanCallback)
            Log.d("BLEScanner", "✅ Scanning started")
        } else {
            Log.e("BLEScanner", "❌ Bluetooth permissions not granted")
        }
    }

    fun stopScan() {
        try {
            scanner.stopScan(scanCallback)
            Log.d("BLEScanner", "✅ Scanning stopped")
        } catch (e: Exception) {
            Log.e("BLEScanner", "⚠️ Error stopping scan: ${e.message}")
        }
    }

    /** ✅ Reset the scanner instance */
    fun resetScanner() {
        scanner = BluetoothLeScannerCompat.getScanner()
        Log.d("BLEScanner", "🔄 Scanner reset")
    }

    /** ✅ Restart scan safely after releasing scanner */
    fun safeRestartScan() {
        stopScan()
        resetScanner()

        Handler(Looper.getMainLooper()).postDelayed({
            startScan()
        }, 1000)
    }

    /** 🔧 Optional: toggle BT to reset system BLE stack (use only when needed) */
    /*fun toggleBluetooth() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && adapter.isEnabled) {
            adapter.disable()
            Handler(Looper.getMainLooper()).postDelayed({
                adapter.enable()
            }, 2000)
        }
    }*/

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}
