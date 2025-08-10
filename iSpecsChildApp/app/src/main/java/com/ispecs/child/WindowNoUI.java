package com.ispecs.child;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.WINDOW_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import jp.wasabeef.blurry.Blurry;

public class WindowNoUI {

    // declaring required variables
    private Context context;
    private View mView;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;
    private LayoutInflater layoutInflater;

    private Boolean mute;

    public WindowNoUI(Context context){
        this.context=context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // set the layout parameters of the window
            mParams = new WindowManager.LayoutParams(
                    // Shrink the window to wrap the content rather
                    // than filling the screen
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    // Display it on top of other application windows
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    // Don't let it grab the input focus
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    // Make the underlying application window visible
                    // through any transparent parts
                    PixelFormat.TRANSLUCENT);
        }
        // getting a LayoutInflater
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // inflating the view with the custom layout we created
        mView = layoutInflater.inflate(R.layout.popup_window_no_ui, null);


        // Define the position of the
        // window within the screen
        mParams.gravity = Gravity.CENTER;
        mWindowManager = (WindowManager)context.getSystemService(WINDOW_SERVICE);


        FloatingActionButton fab_app_icon = mView.findViewById(R.id.fab_app_icon);

        fab_app_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(context.getApplicationContext() , LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        });

       updateBlurSettings();
    }

    private boolean isBlurring = false;

    public void updateBlurSettings() {
        if (isBlurring) return; // Prevent duplicate blur calls
        isBlurring = true;

        SharedPreferences sp = context.getSharedPreferences(App.PREF_NAME, Context.MODE_PRIVATE);
        int blur_percentage = sp.getInt(App.BLUR_INTENSITY_PREF, 10);
        int fade_in_delay = sp.getInt("fade_in", 5);
        mute = sp.getBoolean("mute", true);

        FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);
        overlay_layout.setBackgroundColor(Color.parseColor("#" + getBlur(blur_percentage) + "ffffff"));

        // Step 1: Manually capture the view into a bitmap
        overlay_layout.setDrawingCacheEnabled(true);
        overlay_layout.buildDrawingCache();
        Bitmap cachedBitmap = overlay_layout.getDrawingCache();

        if (cachedBitmap != null) {
            // Step 2: Create a safe copy
            Bitmap safeBitmap = Bitmap.createBitmap(cachedBitmap);
            overlay_layout.setDrawingCacheEnabled(false); // Reset drawing cache
            ImageView blur_background = mView.findViewById(R.id.blur_background);
            // Step 3: Apply the blur safely
            Blurry.with(context)
                    .radius(10)
                    .sampling(8)
                    .color(Color.parseColor("#" + getBlur(blur_percentage) + "ffffff"))
                    .async()
                    .animate(fade_in_delay * 100)
                    .from(safeBitmap)
                    .into(blur_background);
        }

        // Step 4: Unlock after delay
        overlay_layout.postDelayed(() -> {
            isBlurring = false;
        }, 1500); // Prevent blur flooding for 1.5s
    }


    /*public void updateBlurSettings() {

        SharedPreferences sp = context.getSharedPreferences(App.PREF_NAME , MODE_PRIVATE);
        int blur_percentage = sp.getInt(App.BLUR_INTENSITY_PREF, 10);
        int fade_in_delay = sp.getInt("fade_in", 5);
        mute = sp.getBoolean("mute", true);

        FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);

        overlay_layout.setBackgroundColor(Color.parseColor("#"+getBlur(blur_percentage)+"ffffff"));

        Blurry.with(context)
                .radius(10)
                .sampling(8)
                .color(Color.parseColor("#"+getBlur(blur_percentage)+"ffffff"))
                .async()
                .animate(fade_in_delay* 100)
                .onto(overlay_layout);
    }*/

    public void setAlertMessage(String message){
        if(mView != null){
            TextView alertMessageView = mView.findViewById(R.id.alert_message);
            alertMessageView.setText(message);
        }
    }

    public void open() {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if(mute) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                } else {
                    am.setStreamMute(AudioManager.STREAM_MUSIC, true);
                }
            }

            // check if the view is already
            // inflated or present in the window
            if(mView.getWindowToken()==null) {
                if(mView.getParent()==null) {
                    mWindowManager.addView(mView, mParams);
                    FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);
                    overlay_layout.setVisibility(View.VISIBLE);

                    LinearLayout overlay_message_layout = mView.findViewById(R.id.overlay_message_layout);
                    overlay_message_layout.setVisibility(View.VISIBLE);

                    FloatingActionButton fab_app_icon = mView.findViewById(R.id.fab_app_icon);
                    fab_app_icon.setVisibility(View.VISIBLE);
                    // Add fade-in animation
                    AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                    fadeIn.setDuration(2000); // Duration of the animation in milliseconds
                    overlay_layout.startAnimation(fadeIn);
                    overlay_message_layout.startAnimation(fadeIn);
                    fab_app_icon.startAnimation(fadeIn);
                }

            }
            else  {
                FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);
                overlay_layout.setVisibility(View.VISIBLE);

                LinearLayout overlay_message_layout = mView.findViewById(R.id.overlay_message_layout);
                overlay_message_layout.setVisibility(View.VISIBLE);

                FloatingActionButton fab_app_icon = mView.findViewById(R.id.fab_app_icon);
                fab_app_icon.setVisibility(View.VISIBLE);
                // Add fade-in animation
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(2000); // Duration of the animation in milliseconds
                overlay_layout.startAnimation(fadeIn);
                overlay_message_layout.startAnimation(fadeIn);
                fab_app_icon.startAnimation(fadeIn);


            }

        } catch (Exception e) {
            Log.d("Error1",e.toString());
        }

    }

    public void close() {
        updateBlurSettings();
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            } else {
                am.setStreamMute(AudioManager.STREAM_MUSIC, false);
            }

            // ✅ Remove view from WindowManager so touch returns
            if (mView.getWindowToken() != null) {
                mWindowManager.removeView(mView);
            }

        } catch (Exception e) {
            Log.e("WindowNoUI", "Error closing overlay", e);
        }
    }


    /*public void close() {
        updateBlurSettings();

        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            } else {
                am.setStreamMute(AudioManager.STREAM_MUSIC, false);
            }

            FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);
            overlay_layout.setVisibility(View.GONE);

            LinearLayout overlay_message_layout = mView.findViewById(R.id.overlay_message_layout);
            overlay_message_layout.setVisibility(View.GONE);

            FloatingActionButton fab_app_icon = mView.findViewById(R.id.fab_app_icon);
            fab_app_icon.setVisibility(View.GONE);

        } catch (Exception e) {
            Log.d("Error2",e.toString());
        }
    }*/

    public void stop() {
        try {
            // remove the view from the window
            ((WindowManager)context.getSystemService(WINDOW_SERVICE)).removeView(mView);
            // invalidate the view
            mView.invalidate();

            ((ViewGroup)mView.getParent()).removeAllViews();

        } catch (Exception e) {

        }
    }



    private String getBlur(int perc) {
        switch (perc) {

            case 100:
                return "FF";
            case 95:
                return "F2";
            case 90:
                return "E6";
            case 85:
                return "D9";
            case 80:
                return "CC";
            case 75:
                return "BF";
            case 70:
                return "B3";
            case 65:
                return "A6";
            case 60:
                return "99";
            case 55:
                return "8C";
            case 50:
                return "80";
            case 45:
                return "73";
            case 40:
                return "66";
            case 35:
                return "59";
            case 30:
                return "4D";
            case 25:
                return "40";
            case 20:
                return "33";
            case 15:
                return "26";
            case 10:
                return "1A";
            case 5:
                return "0D";
            case 0:
                return "00";
        }
        return "80";


    }
}
