package com.ispecs.child.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Looper
import android.util.Log
import com.ispecs.child.App
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.*

class MyBleManager(context: Context) : BleManager(context) {

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var indicateCharacteristic: BluetoothGattCharacteristic? = null

    private var connectionStateListener: ((ConnectionState) -> Unit)? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null // ✅ Callback for received data
    private val handler = android.os.Handler(Looper.getMainLooper()) // ✅ Fix: Initialize Handler

    /** ✅ **Method to Start Auto-Connecting to a Device** */
    private fun connectToDevice(device: BluetoothDevice) {
        connect(device)
            .retry(3, 1000) // ✅ Retry connection up to 3 times
            .useAutoConnect(false) // ✅ Enable auto-reconnect feature
            .timeout(30000) // ✅ Timeout after 30 seconds
            .done {
                Log.d("MyBleManager", "✅ Connected to device: ${device.address}")
                connectionStateListener?.invoke(ConnectionState.CONNECTED)
            }
            .fail { _, _ ->
                Log.e("MyBleManager", "❌ Failed to connect to device: ${device.address}")
                connectionStateListener?.invoke(ConnectionState.DISCONNECTED)
                scheduleReconnect(device) // ✅ If connection fails, schedule a reconnect
            }
            .enqueue()
    }

    /** ✅ **Automatically Schedule a Reconnection if Device Disconnects** */
    fun scheduleReconnect(reconnectDevice: BluetoothDevice) {
        reconnectDevice?.let { device ->
            Log.d("MyBleManager", "🔄 Scheduling reconnect attempt for ${device.address}...")
            handler.postDelayed({
                Log.d("MyBleManager", "🚀 Trying to reconnect to ${device.address}...")
                connectToDevice(device)
            }, 5000) // ✅ Retry connection every 5 seconds
        }
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return object : BleManagerGattCallback() {
            override fun initialize() {
                enableNotifications()
                //enableIndications()
            }

            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                val service = gatt.getService(UUID.fromString(App.DATA_SERVICE_UUID))
                if (service != null) {
                    //writeCharacteristic = service.getCharacteristic(UUID.fromString(App.WRITE_CHAR_UUID))
                    notifyCharacteristic = service.getCharacteristic(UUID.fromString(App.DATA_CHAR_UUID))
                    //indicateCharacteristic = service.getCharacteristic(UUID.fromString(App.LOGS_CHAR_UUID))
                }
                //return writeCharacteristic != null && notifyCharacteristic != null && indicateCharacteristic != null

                return notifyCharacteristic != null
            }

            override fun onServicesInvalidated() {
                writeCharacteristic = null
                notifyCharacteristic = null
                indicateCharacteristic = null
            }


            override fun onDeviceDisconnected() {
                Log.d("MyBleManager", "❌ Device Disconnected!")
                connectionStateListener?.invoke(ConnectionState.DISCONNECTED)
            }


            override fun onCharacteristicNotified(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val data = characteristic.value // ✅ Get the notification data
                if (data != null) {
                    val hexString = data.joinToString(" ") { String.format("%02X", it) } // Convert to hex
                    Log.d("MyBleManager", "🔥 Notification Received: $hexString")

                    onDataReceived?.invoke(data) // ✅ Pass data to callback
                } else {
                    Log.e("MyBleManager", "❌ Received null notification data")
                }
            }


        }
    }

    /** ✅ **Public function to enable notifications and listen for incoming data** */
    fun enableNotifications() {
        notifyCharacteristic?.let {
            super.enableNotifications(it)
                .with { _, data ->
                    val receivedData = data.value
                    if (receivedData != null) {
                        Log.d("MyBleManager", "Notification received: ${receivedData.contentToString()}")
                        onDataReceived?.invoke(receivedData) // ✅ Trigger callback with received data
                    }
                }
                .fail { _, _ -> Log.e("MyBleManager", "Failed to enable notifications") }
                .enqueue()
        } ?: Log.e("MyBleManager", "notifyCharacteristic is NULL")
    }

    /** ✅ **Public function to enable indications** */
    fun enableIndications() {
        indicateCharacteristic?.let {
            super.enableIndications(it)
                .with { _, data -> Log.d("MyBleManager", "Indication received: ${data.value?.contentToString()}") }
                .enqueue()
        }
    }

    /** ✅ **Public function to Set Connection State Listener** */
    fun setConnectionStateListener(listener: (ConnectionState) -> Unit) {
        connectionStateListener = listener
    }

    /** ✅ **Public function to set a callback for received data** */
    fun setOnDataReceivedListener(listener: (ByteArray) -> Unit) {
        onDataReceived = listener
    }

    /** ✅ **Public function to write data to a characteristic** */
    fun writeCharacteristic(serviceUUID: String, charUUID: String, data: ByteArray) {
        val characteristic = writeCharacteristic ?: return
        writeCharacteristic(characteristic, Data(data))
            .done { Log.d("MyBleManager", "Data written successfully") }
            .fail { _, _ -> Log.e("MyBleManager", "Failed to write data") }
            .enqueue()
    }

    /** ✅ **Public function to disconnect the device** */
    fun disconnectDevice() {
        disconnect().enqueue()
    }

    /** ✅ **Public function to Remove Connection State Listener** */
    fun removeConnectionStateListener() {
        Log.d("MyBleManager", "🔌 Removing connection state listener")
        connectionStateListener = null
    }

    /** ✅ **Public function to Disconnect & Stop Reconnection Attempts** */
    fun stopBleOperations() {
        Log.d("MyBleManager", "🔌 Stopping BLE operations and removing auto-reconnect")
        handler.removeCallbacksAndMessages(null) // ✅ Remove pending reconnect tasks
        disconnect().enqueue() // ✅ Disconnect the device properly
    }

    /** ✅ **Enum for Connection States** */
    enum class ConnectionState {
        CONNECTING, CONNECTED, READY, DISCONNECTED, LINK_LOSS, ERROR
    }
}

