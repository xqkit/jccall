package com.kidosc.jcvideo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * Desc:    接收重启的广播
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/1/31 10:05
 */

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "VideoCallBoot";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "video call receiver has received");
        Intent service = new Intent(context, MyService.class);
        context.startService(service);
    }
}
