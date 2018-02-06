package com.kidosc.jcvideo;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.kidosc.jcvideo.JCWrapper.JCEvent.JCEvent;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCLoginEvent;
import com.kidosc.jcvideo.JCWrapper.JCManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Desc:    后台服务
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/2/1 10:29
 */

public class MyService extends Service {

    private static final String TAG = MyService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
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
        }
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
                /*requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                                , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                                , Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE
                                , Manifest.permission.RECEIVE_BOOT_COMPLETED}
                        , 1);*/
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i("Kathy", "onDestroy - Thread ID = " + Thread.currentThread().getId());
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
