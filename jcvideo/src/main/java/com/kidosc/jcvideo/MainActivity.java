package com.kidosc.jcvideo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
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
        Log.d(TAG, "init permission");
        initPermissions();
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try {
            String imei = telephonyManager.getImei();
            EventBus.getDefault().register(this);
            Log.d(TAG, "imei = " + imei);
            boolean status = JCManager.getInstance().client.login("kido_" + imei + "_kido", imei);
            if (!status) Log.e(TAG, "login fail!");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onCreate");
        EventBus.getDefault().register(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initPermissions() {
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int resultR = pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, packageName);
            int resultW = pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, packageName);
            int resultC = pm.checkPermission(Manifest.permission.CAMERA, packageName);
            int resultRa = pm.checkPermission(Manifest.permission.RECORD_AUDIO, packageName);
            int resultP = pm.checkPermission(Manifest.permission.READ_PHONE_STATE, packageName);
            int resultRc = pm.checkPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED, packageName);
            if (resultR == -1 || resultW == -1 || resultC == -1 || resultRa == -1 || resultP == -1 || resultRc == -1) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                                , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                                , Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE
                                , Manifest.permission.RECEIVE_BOOT_COMPLETED}
                        , 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        int resultR = pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, packageName);
        int resultW = pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, packageName);
        int resultC = pm.checkPermission(Manifest.permission.CAMERA, packageName);
        int resultRa = pm.checkPermission(Manifest.permission.RECORD_AUDIO, packageName);
        int resultP = pm.checkPermission(Manifest.permission.READ_PHONE_STATE, packageName);
        int resultRc = pm.checkPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED, packageName);
        if (resultR == -1 || resultW == -1 || resultC == -1 || resultRa == -1 || resultP == -1 || resultRc == -1) {
            Log.e(TAG, "动态申请权限失败");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
