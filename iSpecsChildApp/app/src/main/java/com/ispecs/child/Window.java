package com.ispecs.child;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.FrameLayout;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.WINDOW_SERVICE;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import jp.wasabeef.blurry.Blurry;

public class Window {

    // declaring required variables
    private Context context;
    private View mView;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;
    private LayoutInflater layoutInflater;

    public Window(Context context){
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
        mView = layoutInflater.inflate(R.layout.popup_window, null);
        // set onClickListener on the remove button, which removes
        // the view from the window
        mView.findViewById(R.id.window_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if( ((Button)view).getTag().equals("clear")){
                    close();
                }
                else {
                    open();
                }
            }
        });

        mView.findViewById(R.id.stop_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop();
            }
        });

        // Define the position of the
        // window within the screen
        mParams.gravity = Gravity.CENTER;
        mWindowManager = (WindowManager)context.getSystemService(WINDOW_SERVICE);

        SharedPreferences sp = context.getSharedPreferences("prefs" , MODE_PRIVATE);
        int blur_percentage = sp.getInt("blur_percentage", 50);

        FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);

        overlay_layout.setBackgroundColor(Color.parseColor("#"+getBlur(blur_percentage)+"ffffff"));

        Blurry.with(context)
                .radius(10)
                .sampling(8)
                .color(Color.parseColor("#"+getBlur(blur_percentage)+"ffffff"))
                .async()
                .animate(500)
                .onto(overlay_layout);

    }

    public void open() {



        try {

            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            } else {
                am.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }

            // check if the view is already
            // inflated or present in the window
            if(mView.getWindowToken()==null) {
                if(mView.getParent()==null) {
                    mWindowManager.addView(mView, mParams);
                }

            }
            else  {
                Button close_btn = mView.findViewById(R.id.window_close);
                close_btn.setText("Clear");
                close_btn.setTag("clear");
                FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);
                overlay_layout.setVisibility(View.VISIBLE);
            }

            logTime("Screen blur");
        } catch (Exception e) {
            Log.d("Error1",e.toString());
        }

    }

    public void close() {

        try {

            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            } else {
                am.setStreamMute(AudioManager.STREAM_MUSIC, false);
            }

            // remove the view from the window
            //((WindowManager)context.getSystemService(WINDOW_SERVICE)).removeView(mView);
            // invalidate the view
            //mView.invalidate();
            // remove all views
            FrameLayout overlay_layout = mView.findViewById(R.id.overlay_layout);
            overlay_layout.setVisibility(View.GONE);
            //((ViewGroup)mView).removeView(overlay_layout);

            Button close_btn = mView.findViewById(R.id.window_close);
            close_btn.setText("Blur");
            close_btn.setTag("blur");

            logTime("Screen clear");
            //mView.invalidate();

            //mWindowManager.addView(mView, mParams);


            // the above steps are necessary when you are adding and removing
            // the view simultaneously, it might give some exceptions


        } catch (Exception e) {
            Log.d("Error2",e.toString());
        }
    }

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



    private void logTime( String status) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(calendar.getTime());

        SharedPreferences.Editor sp = context.getSharedPreferences("prefs" , MODE_PRIVATE).edit();

        SharedPreferences sp_reader = context.getSharedPreferences("prefs", MODE_PRIVATE);

        sp.putString("logs" , sp_reader.getString("logs" , "") + "\n"+time+" "+ status);

        sp.commit();
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
