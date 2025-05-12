package com.ispecs.child;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
    private Button viewLogsBtn;

    private EditText blurDelayET, batteryAlertET, blurIntensityET, loginPasscodeET;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        assert getSupportActionBar() != null;   //null check
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setTitle("Settings");

        viewLogsBtn = findViewById(R.id.view_logs_btn);

        blurDelayET = findViewById(R.id.blur_delay_et);
        batteryAlertET = findViewById(R.id.battery_alert_et);
        blurIntensityET = findViewById(R.id.blur_intensity_et);
        loginPasscodeET = findViewById(R.id.passcode_et);

       // ((TextView)findViewById(R.id.version_txt)).setText(BuildConfig.VERSION_NAME);

        SharedPreferences sp = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE);

        blurIntensityET.setText(String.valueOf(sp.getInt(App.BLUR_INTENSITY_PREF, 50)));

        loginPasscodeET.setText(sp.getString(App.PASSCODE_PREF, "1234"));


        viewLogsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(SettingsActivity.this, LogsActivity.class));

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.save_menu_item) {

            SharedPreferences.Editor sp = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE).edit();

            sp.putInt(App.BLUR_INTENSITY_PREF, Integer.parseInt(blurIntensityET.getText().toString()));
            sp.putString(App.PASSCODE_PREF, loginPasscodeET.getText().toString());

            sp.commit();

            Toast.makeText(SettingsActivity.this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }
}