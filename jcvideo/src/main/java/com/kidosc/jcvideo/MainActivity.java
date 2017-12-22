package com.kidosc.jcvideo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.kidosc.jcvideo.JCWrapper.JCEvent.JCEvent;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCLoginEvent;
import com.kidosc.jcvideo.JCWrapper.JCManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        initPermissions();
        EventBus.getDefault().register(this);
        JCManager.getInstance().client.login("a666a", "199319xqk");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 1000);
            }
        }
    }

    public void onDial(View view) {
        startActivity(new Intent(this, CallActivity.class));
    }

    @Subscribe
    public void onEvent(JCEvent event) {
        if (event.getEventType() == JCEvent.EventType.LOGOUT) {
            Log.d(TAG, "log out");
        } else if (event.getEventType() == JCEvent.EventType.LOGIN) {
            JCLoginEvent login = (JCLoginEvent) event;
            if (!login.result) {
                Log.d(TAG, "start login");
            }
        } else if (event.getEventType() == JCEvent.EventType.CLIENT_STATE_CHANGE) {
//            updateTitle();
        } else if (event.getEventType() == JCEvent.EventType.CALL_ADD) {
            Log.d(TAG, "add a call");
            startActivity(new Intent(this, CallActivity.class));
        } else if (event.getEventType() == JCEvent.EventType.CALL_REMOVE) {
            Log.d(TAG, "call remove");
        } else if (event.getEventType() == JCEvent.EventType.CONFERENCE_JOIN) {

        } else if (event.getEventType() == JCEvent.EventType.CONFERENCE_LEAVE) {

        } else if (event.getEventType() == JCEvent.EventType.Exit) {
            finish();
        }
    }
}
