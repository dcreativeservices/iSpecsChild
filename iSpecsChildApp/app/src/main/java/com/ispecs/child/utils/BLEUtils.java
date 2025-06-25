package com.ispecs.child.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class BLEUtils {
    private static final String PREF_NAME = "your_pref_file_name";
    private static final String KEY_MAC_ADDRESS = "CONNECTED_MAC_ADDRESS_PREF";

    // Save MAC
    public static void saveMacAddress(Context context, String mac) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_MAC_ADDRESS, mac)
                .apply();
    }

    public static String loadMacAddress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_MAC_ADDRESS, "");
    }

}
