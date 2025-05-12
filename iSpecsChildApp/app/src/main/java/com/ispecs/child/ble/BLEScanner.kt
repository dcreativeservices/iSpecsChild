package com.ispecs.child.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import no.nordicsemi.android.support.v18.scanner.*

class BLEScanner(
    private val context: Context,
    private val onDeviceFound: (BluetoothDevice, Int) -> Unit
) {

    private val scanner = BluetoothLeScannerCompat.getScanner()
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
            Log.e("BLEScanner", "Scan failed with error: $errorCode")
        }
    }

    fun startScan() {
        if (hasPermissions()) {
            scanner.startScan(null, scanSettings, scanCallback)
            Log.d("BLEScanner", "Scanning started")
        } else {
            Log.e("BLEScanner", "Bluetooth permissions not granted")
        }
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
        Log.d("BLEScanner", "Scanning stopped")
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

