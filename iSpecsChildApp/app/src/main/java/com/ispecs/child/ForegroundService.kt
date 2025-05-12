package com.ispecs.child

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ispecs.child.ble.MyBleManager
import com.ispecs.child.helper.FileHelper
import com.ispecs.child.utils.BluetoothUtils
import java.util.*

class ForegroundService : Service() {

    private var deviceName: String = ""
    private var deviceAddress: String = ""
    private val NOTIFICATION_CHANNEL_ID = "example.overlayTest"
    private val NOTIF_ID = 2

    private var mHandler: Handler? = null
    private val mInterval = 30000

    private lateinit var bleManager: MyBleManager

    private var screenTime = 0 // Default screen time in minutes
    private var glassesConnected = false
    private var startTime: Long = 0
    private var glassesStatus = 0 // 0 => off, 1 => on
    private var batteryPercentage = 0
    private lateinit var lastConnectedBluetoothDevice: BluetoothDevice

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        glassesConnected = true
        mHandler = Handler(Looper.getMainLooper())

        val preferences = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE)
        deviceAddress = preferences.getString(App.CONNECTED_MAC_ADDRESS_PREF, "") ?: ""
        screenTime = preferences.getInt("screenTime", 0)

        startMyOwnForeground()

        if (glassesConnected) {
            connectBLE(deviceAddress)
        }
    }


    private fun startMyOwnForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Background Service"
            val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH)
            chan.setShowBadge(true)

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)

            val notification = getNotification("iSpecs Child App is Running in Background")
            startForeground(NOTIF_ID, notification)
        } else {
            startForeground(1, Notification())
        }

        startTime = System.currentTimeMillis()
        mHandler?.post(screenTimeRunnable)
    }

    private fun getNotification(content: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, LoginActivity::class.java),
            PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle("iSpecs Child App")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationManager.IMPORTANCE_MAX)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setNumber(batteryPercentage)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = getNotification(content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notification)
    }

    private fun connectBLE(macAddress: String) {
        val bluetoothAdapter = BluetoothUtils.getBluetoothAdapter(this)
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(macAddress)

        if (bluetoothDevice == null) {
            Log.e("BLE", "Device not found with MAC: $macAddress")
            return
        }

        lastConnectedBluetoothDevice = bluetoothDevice

        bleManager = MyBleManager(this)

        bleManager.connect(bluetoothDevice)
            .timeout(30000)
            .retry(3, 1000)
            .done {
                updateNotification("Connected")
                setupOnConnected()
            }
            .fail { _, _ ->
                updateNotification("Disconnected")
                bleManager.scheduleReconnect(bluetoothDevice)
            }
            .enqueue()
    }



    private fun setupOnConnected() {
        updateNotification("Connected")

        App.window?.close()

        bleManager.enableNotifications()
        //bleManager.enableIndications()

        setupBleListeners()

        uploadDeviceConnectionStatus(applicationContext, true)

    }

    private fun setupBleListeners() {
        // ✅ Listen for connection state changes
        bleManager.setConnectionStateListener { state ->
            when (state) {
                MyBleManager.ConnectionState.CONNECTING -> updateNotification("Connecting...")
                MyBleManager.ConnectionState.CONNECTED -> {
                    updateNotification("Connected!")
                    blurScreen(1) // clear screen on connected
                }
                MyBleManager.ConnectionState.READY -> updateNotification("Ready to communicate")

                MyBleManager.ConnectionState.DISCONNECTED -> {
                    updateNotification("Disconnected")
                    blurScreen(0) // blur screen on disconnect
                    bleManager.scheduleReconnect(lastConnectedBluetoothDevice)
                    App.window.setAlertMessage("iSpecs device disconnected, check if iSpecs device is powered on (Blinking blue light) and is in close proximity of device")
                }
                MyBleManager.ConnectionState.LINK_LOSS -> updateNotification("Connection lost!")
                MyBleManager.ConnectionState.ERROR -> updateNotification("BLE Error!")
            }
        }

        // ✅ Listen for data when available
        bleManager.setOnDataReceivedListener { data ->
            Log.d("BLE", "Received Hex Data: $data")
            parseHexData(data)
        }

    }

    private fun parseHexData(value: ByteArray) {
        for (i in value.indices) {
            val hexString = String.format("%02X", value[i]) // Convert Byte to HEX
            Log.d("ByteArrayPrinter", hexString)

            when (i) {

                11 -> {
                    val battery_percentage = Integer.parseInt(hexString, 16)
                    //updateBatteryUI(battery_percentage)
                    if(battery_percentage < 20){
                        updateNotification("Battery low, please charge the iSpecs device")
                    }
                }
                12 -> {
                    val glasses_status = Integer.parseInt(hexString, 16)
                    updateNotification("Glasses Status "+glasses_status.toString())
                    if(glasses_status == 1) {
                        App.window.setAlertMessage("")
                    } else {
                        App.window.setAlertMessage("To use the mobile device please wear the glasses.")
                    }
                    blurScreen(glasses_status)
                }
            }
        }

        // ✅ Upload to Firebase
        App.uploadDataToFirebase(value)

    }

    private fun blurScreen(state: Int) {
        if (state == 1) {
            App.window?.close()
            App.IS_BLUR = false
        }
         else {
            App.window?.open()
            App.IS_BLUR = true
         }
    }

    private fun writeDateTimeToCharacteristic() {
        val calendar = Calendar.getInstance()
        val data = byteArrayOf(
            36, 84,
            calendar.get(Calendar.DAY_OF_MONTH).toByte(),
            (calendar.get(Calendar.MONTH) + 1).toByte(),
            (calendar.get(Calendar.YEAR) % 100).toByte(),
            calendar.get(Calendar.HOUR_OF_DAY).toByte(),
            calendar.get(Calendar.MINUTE).toByte(),
            calendar.get(Calendar.SECOND).toByte(),
            0
        )

        bleManager.writeCharacteristic(App.WRITE_SERVICE_UUID, App.WRITE_CHAR_UUID, data)
    }

    companion object {
        fun uploadDeviceConnectionStatus(context: Context, isConnected: Boolean) {
            val sharedPreferences = context.getSharedPreferences(App.PREF_NAME, Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("userId", null) ?: return

            val databaseReference: DatabaseReference =
                FirebaseDatabase.getInstance().getReference("Users").child(userId).child("is_child_app_running")

            databaseReference.setValue(isConnected)
                .addOnSuccessListener { Log.d("Firebase", "Status updated successfully") }
                .addOnFailureListener { e -> Log.e("Firebase", "Error updating status", e) }
        }
    }

    private fun logData(fileName: String, data: String) {
        val fileExists = FileHelper.doesFileExist(applicationContext, fileName)

        if (fileExists) {
            FileHelper.appendToFile(applicationContext, fileName, "$data\n")
        } else {
            FileHelper.saveTextFile(applicationContext, fileName, "$data\n")
        }
    }

    private fun checkScreenTime() {
        if (screenTime > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            // Convert screenTime from minutes to milliseconds
            if (elapsed > screenTime * 60000) {
                App.window?.setAlertMessage("Screen time limit exceeded. Please take a break.")
                // Blur the screen if the elapsed time exceeds screenTime minutes
                blurScreen(0)
            }
        }
    }

    private val screenTimeRunnable = object : Runnable {
        override fun run() {
            checkScreenTime()
            mHandler?.postDelayed(this, mInterval.toLong())
        }
    }

    override fun onDestroy() {

        mHandler?.removeCallbacks(screenTimeRunnable)
        // Update connection status etc.
        uploadDeviceConnectionStatus(applicationContext, false)

        // Remove listeners and stop operations
        bleManager.removeConnectionStateListener()
        bleManager.stopBleOperations()

        // **Important: Clean up BLE manager to unregister broadcast receivers**
        bleManager.close()

        App.window?.close()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()

        super.onDestroy()
    }
}
