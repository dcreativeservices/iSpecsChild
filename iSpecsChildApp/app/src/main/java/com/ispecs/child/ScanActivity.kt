package com.ispecs.child

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ispecs.child.ble.BLEScanner
import com.ispecs.child.ble.MyBleManager
import com.ispecs.child.models.Device
import com.ispecs.child.utils.BLEUtils
import com.ispecs.child.utils.BluetoothUtils
import com.ispecs.child.utils.DialogUtils
import com.ispecs.child.utils.LocationUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScanActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceListAdapter
    private lateinit var scanHeaderLayout: RelativeLayout
    private lateinit var connectedDeviceLayout: RelativeLayout
    private lateinit var exitAppBtn: Button
    private val deviceList = mutableListOf<Device>()
    private lateinit var bleScanner: BLEScanner
    private lateinit var progressDialog: ProgressDialog
    private lateinit var messageDialog: Dialog
    private lateinit var sharedPreferences: SharedPreferences
    private var mac: String =""
    private var selectedDeviceMacAddress: String = ""
    private var selectedDeviceName: String = ""
    private var clearConnectedDevice = false
    private lateinit var btnRefresh: ImageButton
    private lateinit var bleManager: MyBleManager

    private var lastDeviceMac: String? = ""
    private var isDeviceConnected = false

    private lateinit var selectedDevMacAdd: String

    private val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
    private var bluetoothDevice: BluetoothDevice? = null

    private val handler = Handler()
    var count = 0
    private var firstBit: String = ""
    private var secondBit: String = ""
    private var thirdBit: String = ""
    private var batteryPercentage = 0

    private val REQUEST_BLUETOOTH_PERMISSION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        App.window.close()

        supportActionBar?.hide()

        bleManager = MyBleManager(this)

        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        messageDialog = Dialog(this)

        sharedPreferences = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE)
        selectedDeviceMacAddress = sharedPreferences.getString(App.CONNECTED_MAC_ADDRESS_PREF, "").orEmpty()
        selectedDeviceName = sharedPreferences.getString(App.CONNECTED_DEVICE_NAME_PREF, "").orEmpty()

        scanHeaderLayout = findViewById(R.id.scan_header_layout)
        connectedDeviceLayout = findViewById(R.id.connected_device_layout)
        exitAppBtn = findViewById(R.id.exit_app_btn)

        recyclerView = findViewById(R.id.scanned_devices_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DeviceListAdapter()
        recyclerView.adapter = adapter


        findViewById<Button>(R.id.exit_app_btn).setOnClickListener { exitApp() }

        findViewById<LinearLayout>(R.id.disconnect_layout).setOnClickListener{
            val sp = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE).edit()
            sp.putString(App.CONNECTED_MAC_ADDRESS_PREF, "")
            sp.putString(App.CONNECTED_DEVICE_NAME_PREF, "")
            sp.commit()

            // clear selected device local variables

            // clear selected device local variables
            selectedDeviceName = ""
            selectedDeviceMacAddress = ""
            BLEUtils.saveMacAddress(this, selectedDeviceMacAddress)
            bleManager.disconnectDevice()

        }

            scanning()
        btnRefresh = findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener { deviceList.clear()
            btnRefresh.animate()
                .rotationBy(360f)
                .setDuration(1000)  // duration in milliseconds
                .start()
                scanning()
        }
        checkPermissionsSequentially()

    }
    private fun scanning(){
        bleScanner = BLEScanner(this) { device, rssi ->
            handleDeviceFound(device, rssi, isBLE = true)
        }
    }
    private fun startScan() {
        if (!BluetoothUtils.isBluetoothEnabled()) {
            DialogUtils.showDialog(this, "Note", "Please turn on Bluetooth to scan nearby BLE devices")
            return
        }

        if (!LocationUtils.isLocationEnabled(this)) {
            DialogUtils.showDialog(this, "Note", "Please turn on Location to scan nearby BLE devices")
            return
        }

        connectedDeviceLayout.visibility = View.INVISIBLE
        scanHeaderLayout.visibility = View.VISIBLE


        checkLocationPermission()
    }

    private fun handleDeviceFound(device: BluetoothDevice, rssi: Int?,isBLE: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // ✅ Android 12+ (API 31 and above) needs BLUETOOTH_CONNECT permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return
            }
        } else {
            // ✅ Android 11 and below do not need BLUETOOTH_CONNECT permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH),
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return
            }
        }

        scanHeaderLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.VISIBLE
        btnRefresh.visibility=View.VISIBLE
        lastDeviceMac = BLEUtils.loadMacAddress(this)

        if (device.address == lastDeviceMac) {
                // Stop scanning first
            bleScanner.stopScan()
            selectedDeviceMacAddress = device.address
            selectedDeviceName = device.name ?: "Unknown"
            connectBLE()
        }


        // ✅ Proceed if permission is granted
        if (!deviceList.any { it.address == device.address } && device.name != null) {
            deviceList.add(Device(device.name, device.address, rssi.toString()))
            adapter.notifyDataSetChanged()
        }
    }
    fun isBLEDevice(context: Context, device: BluetoothDevice): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.type == BluetoothDevice.DEVICE_TYPE_LE
            } else {
                false
            }
        } else {
            device.type == BluetoothDevice.DEVICE_TYPE_LE
        }
    }

    fun connectBLE() {
        val bluetoothAdapter = BluetoothUtils.getBluetoothAdapter(this)
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(selectedDeviceMacAddress)

        if (bluetoothDevice == null) {
            Log.e("BLE", "Device not found with MAC: $selectedDeviceMacAddress")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return
            }
        }

        bleScanner.stopScan()
        //bluetoothScanner.stopScan()
        progressDialog.setMessage("Connecting...")
        progressDialog.show()
        if (bluetoothDevice.type == BluetoothDevice.DEVICE_TYPE_LE || isBLEDevice(this, bluetoothDevice)){
            bleManager.connect(bluetoothDevice)
                .timeout(30000)
                .retry(3, 1000)
                .done {
                    setupOnConnected()
                    selectedDevMacAdd=bluetoothDevice.address
                    BLEUtils.saveMacAddress(this, selectedDeviceMacAddress)
                }
                .fail { _, _ ->
                    progressDialog.dismiss()
                    Log.e("BLE", "Failed to connect")
                    //
                    val sp = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE).edit()
                    sp.putString(App.CONNECTED_MAC_ADDRESS_PREF, "")
                    sp.putString(App.CONNECTED_DEVICE_NAME_PREF, "")
                    sp.commit()
                    selectedDeviceMacAddress = ""
                    selectedDeviceName = ""
                    startScan()
                }
                .enqueue()
        }
    }
    private fun clearScanUI() {
        scanHeaderLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
       // connectBtn.visibility = View.GONE
      //  scanBtn.visibility = View.GONE
        btnRefresh.visibility = View.GONE // now this will work
    }

    private fun setupOnConnected() {
        /*recyclerView.visibility = View.INVISIBLE
        scanHeaderLayout.visibility = View.INVISIBLE
        btnRefresh.visibility=View.INVISIBLE*/
        isDeviceConnected=true
        clearScanUI()
        progressDialog.dismiss()
        connectedDeviceLayout.visibility = View.VISIBLE
        exitAppBtn.visibility = View.VISIBLE
        mac=maskMacAddress(selectedDeviceMacAddress)
        findViewById<TextView>(R.id.connected_to_txt).text = selectedDeviceName+"\n"+mac
       // findViewById<TextView>(R.id.mac).text = mac+""
        sharedPreferences.edit()
            .putString(App.CONNECTED_MAC_ADDRESS_PREF, selectedDeviceMacAddress)
            .putString(App.CONNECTED_DEVICE_NAME_PREF, selectedDeviceName)
            .putString(App.CONNECTED_AT_PREF, getCurrentTime())
            .apply()

        findViewById<TextView>(R.id.connected_at_txt).text = getCurrentTime()


        // ✅ Enable Notifications (Will subscribe automatically)
        bleManager.enableNotifications()

        // ✅ Listen for data when available
        bleManager.setOnDataReceivedListener { data ->
            Log.d("BLE", "Received Hex Data: $data")
            parseHexData(data)
        }

        bleManager.setConnectionStateListener { state ->
            when (state) {
                MyBleManager.ConnectionState.CONNECTING -> {

                }
                MyBleManager.ConnectionState.CONNECTED -> {
                    bleScanner.stopScan()
                }
                MyBleManager.ConnectionState.READY -> {


                }

                MyBleManager.ConnectionState.DISCONNECTED -> {
                    startScan()
                }
                MyBleManager.ConnectionState.LINK_LOSS -> {


                }
                MyBleManager.ConnectionState.ERROR -> {

                }
            }
        }

        //writeDateTimeToCharacteristic()
    }

    private var bluetoothSocket: BluetoothSocket? = null



    private fun parseHexData(value: ByteArray) {
            for (i in value.indices) {
                val hexString = String.format("%02X", value[i]) // Convert Byte to HEX
                Log.d("ByteArrayPrinter", hexString)

                when (i) {
                    1 -> firstBit = hexString
                    2 -> secondBit = hexString
                    3 -> thirdBit = hexString
                    11 -> {
                        val battery_percentage = Integer.parseInt(hexString, 16)
                        updateBatteryUI(battery_percentage)
                    }
                    12 -> {
                        val glasses_status = Integer.parseInt(hexString, 16)
                        updateGlassesStatusUI(glasses_status)
                    }
                }
            }

            // ✅ Upload to Firebase
            App.uploadDataToFirebase(value)

    }

    private fun updateBatteryUI(batteryLevel: Int) {
        val batteryDrawable = when {
            batteryLevel > 90 -> R.drawable.ic_battery_full
            batteryLevel > 70 -> R.drawable.ic_battery_6_bar
            batteryLevel > 60 -> R.drawable.ic_battery_5_bar
            batteryLevel > 50 -> R.drawable.ic_battery_4_bar
            batteryLevel > 40 -> R.drawable.ic_battery_3_bar
            batteryLevel > 30 -> R.drawable.ic_battery_2_bar
            batteryLevel > 10 -> R.drawable.ic_battery_1_bar
            else -> R.drawable.ic_battery_1_bar
        }

        runOnUiThread {
            findViewById<ImageView>(R.id.battery_icon).setImageDrawable(getDrawable(batteryDrawable))
            findViewById<TextView>(R.id.battery_level).text = "$batteryLevel%"
        }
    }

    private fun updateGlassesStatusUI(status: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.glasses_status_txt).text = if (status == 1) "Wearing" else "Not Wearing"
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

    private fun exitApp() {

        bleManager.removeConnectionStateListener()
        bleManager.stopBleOperations()

        bleScanner.stopScan()

        finish()
        startMyForegroundService()
    }

    fun startMyForegroundService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent) // ✅ Required for Android 8+
        } else {
            startService(serviceIntent) // ✅ Works for older versions
        }
    }

    override fun onDestroy() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        if (messageDialog.isShowing) {
            messageDialog.dismiss()
        }
        bleScanner.stopScan()

        super.onDestroy()
    }


    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            checkBluetoothPermission()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10)
        }
    }

    private fun checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
                bleScanner.startScan()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 20)
            }
        } else {
            bleScanner.startScan()
        }
    }

    private fun checkPermissionsSequentially() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 100)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        }
        requestNextPermission()
    }

    private fun requestNextPermission() {
        if (permissions.isNotEmpty()) {
            val permission = permissions.removeAt(0)
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 10)
            } else {
                requestNextPermission()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission()
            } else {
                checkBluetoothEnabled()
            }
        }
    }

    private fun checkBluetoothEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 40)
                return
            }
        }
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 200)
        } else {
            if (selectedDeviceMacAddress.isNotEmpty()) {
                connectBLE()
            } else {
                bleScanner.startScan()            }
        }
    }


    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 30)
        } else {
            checkBluetoothEnabled()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestNextPermission()

        } else {
            Toast.makeText(this, "Permission denied. Some features may not work.", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            checkPermissionsSequentially()
        } else if (requestCode == 200) {
            checkBluetoothEnabled()
        }
    }

    inner class DeviceListAdapter : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val address: TextView = view.findViewById(R.id.device_address)
            val name: TextView = view.findViewById(R.id.device_name)
            val rssi: TextView = view.findViewById(R.id.rssi)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.scanned_device_view, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = deviceList[position]
            holder.address.text = device.address
            holder.name.text = device.name
            holder.rssi.text = device.rssi
            holder.itemView.setOnClickListener {
                selectedDeviceMacAddress = device.address
                selectedDeviceName = device.name + "_" + selectedDeviceMacAddress[selectedDeviceMacAddress.length-2] + selectedDeviceMacAddress[selectedDeviceMacAddress.length-1]
                if (device.address == selectedDeviceMacAddress) {
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.selected_color))
                } else {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT)  // or default background
                }
                connectBLE()
            }
        }

        override fun getItemCount() = deviceList.size
    }
    fun maskMacAddress(mac: String): String {
        val segments = mac.split(':')
        val last = segments.last()
        return "**:**:**:$last"
    }
}
