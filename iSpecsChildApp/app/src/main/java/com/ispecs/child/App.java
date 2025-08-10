package com.ispecs.child;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ispecs.child.utils.BLEUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class App extends Application {

    public static final String PREF_NAME = "prefs";
    public static final String CONNECTED_MAC_ADDRESS_PREF = "mac_address";
    public static final String CONNECTED_DEVICE_NAME_PREF = "device_name";
    public static final String BLUR_INTENSITY_PREF = "blur_intensity";
    public static final String CONNECTED_AT_PREF = "connected_at";
    public static final String PASSCODE_PREF = "passcode";
    public static final String DATA_SERVICE_UUID = "017b0001-37e3-457d-3763-2a9b2d24cbb2";
    public static final String DATA_CHAR_UUID = "017b0002-37e3-457d-3763-2a9b2d24cbb2";
    public static final String LOGS_SERVICE_UUID = "007b0001-37e3-457d-3763-2a9b2d24cbb2";
    public static final String LOGS_CHAR_UUID = "007b0002-37e3-457d-3763-2a9b2d24cbb2";
    public static final String WRITE_SERVICE_UUID = "027b0001-37e3-457d-3763-2a9b2d24cbb2";
    public static final String WRITE_CHAR_UUID = "027b0002-37e3-457d-3763-2a9b2d24cbb2";
    public static boolean IS_APP_VISIBLE = false;
    public static boolean IS_BLUR = false;

    public static Context appContext;

    public static WindowNoUI window = null;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        window = new WindowNoUI(this);

    }

    public static void uploadDataToFirebase(byte[] value) {

        // check for error values
        if (value.length != 13) {
            return;
        }
        // Create the data to be uploaded
        Map<String, Object> data = new HashMap<>();

        // Get the current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        // Get the current time
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());


        for (int i = 0; i < value.length; i++) {
            String decodedString = String.format("%02X", value[i]);
            Log.d("ByteArrayPrinter", decodedString);

            int intValue = Integer.parseInt(decodedString, 16);
            if (i == value.length - 1) {
                int[] bits = new int[8];
                for (int j = 7; j >= 0; j--) {
                    bits[7 - j] = (intValue >> j) & 1;
                }
                for (int j = 3; j < bits.length; j++) {
                    switch (j) {
                        case 3:
                            data.put("History_Status", bits[j]);
                            break;
                        case 4:
                            data.put("ACL_Fault_Status", bits[j]);
                            break;
                        case 5:
                            data.put("PRXY2_Fault_Status", bits[j]);
                            break;
                        case 6:
                            data.put("PRXY1_Fault_Status", bits[j]);
                            break;
                        case 7:
                            data.put("Spece_Status", bits[j]);
                            break;
                    }
                }
            }
                switch (i) {
                    case 0:
                        data.put("sensor1", intValue);
                        break;
                    case 1:
                        data.put("sensor2", intValue);
                        break;
                    case 2:
                        data.put("acl_x", intValue);
                        break;
                    case 3:
                        data.put("acl_y", intValue);
                        break;
                    case 4:
                        data.put("acl_z", intValue);
                        break;
                    case 5:
                        data.put("day", intValue);
                        break;
                    case 6:
                        data.put("month", intValue);
                        break;
                    case 7:
                        data.put("year", intValue);
                        break;
                    case 8:
                        data.put("hour", intValue);
                        break;
                    case 9:
                        data.put("minute", intValue);
                        break;
                    case 10:
                        data.put("second", intValue);
                        break;
                    case 11:
                        data.put("battery", intValue);
                        break;
                    case 12:
                        data.put("status", intValue);
                        break;
                }

            }

            data.put("uploaded_at", currentTime);

            SharedPreferences sharedPreferences = appContext.getSharedPreferences(App.PREF_NAME, Context.MODE_PRIVATE);
            String parentId = sharedPreferences.getString("parentID", null);
            String mac = BLEUtils.loadMacAddress(appContext);

            if (parentId == null) {
                // Handle the case where parent ID is not found
                return;
            }

            // Get the Firebase database reference
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("logs").child(parentId).child(mac).child(currentDate);

            // Push the data to the specified node
            databaseReference.push().setValue(data)
                    .addOnSuccessListener(aVoid -> {
                        // Data uploaded successfully
                        Log.d("Overlay App", "Uploaded Successfully");
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error
                        Log.d("Overlay App", "Error uploading " + e.getMessage());
                    });
        }

}
