package com.ispecs.child;

import static android.app.PendingIntent.getActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().hide();

        database = FirebaseDatabase.getInstance();

        final EditText editTextParentId = findViewById(R.id.editTextparentID);
        final EditText editTextPasscode = findViewById(R.id.editTextPasscode);
        Button buttonLogin = findViewById(R.id.login_btn);

        SharedPreferences sharedPreferences = getSharedPreferences(App.PREF_NAME, Context.MODE_PRIVATE);
        String savedParentID = sharedPreferences.getString("parentID" , "");

        editTextParentId.setText(savedParentID);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String parentId = editTextParentId.getText().toString().trim();
                String passcode = editTextPasscode.getText().toString().trim();
                if (!parentId.isEmpty() && !passcode.isEmpty()) {
                    findUserIdByParentIdAndPasscode(parentId, passcode, new Callback() {
                        @Override
                        public void onCallback(String userId, int screenTime, int blur_intensity , int blur_delay, int fade_in , boolean mute) {
                            if (userId != null) {
                                saveLoginInfo(userId , parentId, passcode, screenTime , blur_intensity , blur_delay, fade_in, mute);
                                if(isMyServiceRunning(ForegroundService.class)) {
                                    stopService(new Intent(LoginActivity.this, ForegroundService.class));
                                }
                                if(App.window != null){
                                    App.window.updateBlurSettings();
                                }
                                Intent intent = new Intent(LoginActivity.this, ScanActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "No matching user found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(LoginActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();

                }
            }
        });

        TextView versionTextView = findViewById(R.id.version_txt);
        versionTextView.setText("Version: " + getVersionName(this));
    }

    public String getVersionName(Context context) {
        String versionName = "";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    private void findUserIdByParentIdAndPasscode(String parentId, final String passcode, final Callback callback) {
        DatabaseReference usersRef = database.getReference("Users");
        Query query = usersRef.orderByChild("parent_id").equalTo(parentId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userPasscode = userSnapshot.child("child_passcode").getValue(String.class);
                    if (passcode.equals(userPasscode)) {
                        int screenTime = userSnapshot.hasChild("screen_time") ? userSnapshot.child("screen_time").getValue(Integer.class) : 0;
                        int blur_intensity = userSnapshot.hasChild("blur_intensity") ? userSnapshot.child("blur_intensity").getValue(Integer.class) : 0;
                        int blur_delay = userSnapshot.hasChild("blur_delay") ? userSnapshot.child("blur_delay").getValue(Integer.class) : 0;
                        int fade_in = userSnapshot.hasChild("fade_in") ? userSnapshot.child("fade_in").getValue(Integer.class) : 0;
                        boolean mute = userSnapshot.hasChild("mute") ? userSnapshot.child("mute").getValue(Boolean.class) : true;
                        callback.onCallback(userSnapshot.getKey(), screenTime, blur_intensity, blur_delay, fade_in, mute );
                        return;
                    }
                }
                callback.onCallback(null, 2 ,2 , 2, 2, true); // No matching user found
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                callback.onCallback(null, 2, 2, 2, 2, true);
            }
        });
    }

    private void saveLoginInfo(String userId, String parentID, String passcode, int screenTime , int blur_intensity, int blur_delay, int fade_in , boolean mute) {
        SharedPreferences sharedPreferences = getSharedPreferences(App.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("parentID" , parentID);
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", userId);
        editor.putInt("screenTime", screenTime);
        editor.putInt(App.BLUR_INTENSITY_PREF, blur_intensity);
        editor.putInt("blur_delay", blur_delay);
        editor.putInt("fade_in", fade_in);
        editor.putBoolean("mute", mute);
        editor.apply();
    }

    private interface Callback {
        void onCallback(String userId, int screenTime, int blur_intensity, int blur_delay, int fade_in , boolean mute);
    }

    @Override
    protected void onResume() {
        super.onResume();
       App.IS_APP_VISIBLE = true;
        if (App.IS_BLUR) {
            App.window.close();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        App.IS_APP_VISIBLE = false;
        if (App.IS_BLUR) {
            App.window.open();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}