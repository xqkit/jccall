package com.kidosc.jcvideo;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.kidosc.jcvideo.JCWrapper.JCManager;
import com.kidosc.jcvideo.Toos.Utils;


public class JCApplication extends Application {

    private static final String TAG = "MainActivity";
    private int mFrontActivityCount;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        String processName = Utils.getCurProcessName(this);
        if (TextUtils.equals(processName, getPackageName())) {
            Log.d(TAG,"equals process name");
            JCManager.getInstance().initialize(this);
            setupCheckForeground();
        }
    }

    private void setupCheckForeground() {
        mFrontActivityCount = 0;
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (mFrontActivityCount == 0) {
                    JCManager.getInstance().client.setForeground(true);
                }
                mFrontActivityCount++;
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                mFrontActivityCount--;
                if (mFrontActivityCount == 0) {
                    JCManager.getInstance().client.setForeground(false);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

}
