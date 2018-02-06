package com.kidosc.jcvideo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.juphoon.cloud.JCCall;
import com.juphoon.cloud.JCCallItem;
import com.juphoon.cloud.JCMediaDevice;
import com.juphoon.cloud.JCMediaDeviceVideoCanvas;
import com.kidosc.jcvideo.JCWrapper.JCCallUtils;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCEvent;
import com.kidosc.jcvideo.JCWrapper.JCManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Desc:    发起通话
 * Email:   frank.xiong@kidosc.com
 * Date:    2017/12/21 18:38
 */

public class CallActivity extends Activity {

    private static final String TAG = CallActivity.class.getSimpleName();
    @BindView(R.id.layoutCall)
    public RelativeLayout mContentView;

    @BindView(R.id.call_in_answercall)
    public ImageView mCallInAnswer;
    @BindView(R.id.call_in_endcall)
    public ImageView mCallInEnd;
    @BindView(R.id.call_in_name)
    public TextView mCallInName;

    @BindView(R.id.call_out_name)
    public TextView mCallOutName;
    @BindView(R.id.call_out_term)
    public ImageView mCallOutTerm;

    @BindView(R.id.rl_call_in)
    public RelativeLayout mRlCallIn;
    @BindView(R.id.rl_call_out)
    public RelativeLayout mRlCallOut;

    private boolean mFullScreen;
    private JCMediaDeviceVideoCanvas mLocalCanvas;
    private JCMediaDeviceVideoCanvas mRemoteCanvas;
    private AlertDialog mAlertAnswer;
    private ImageView mOnTerm;
    private String mName;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.call_page);
        ButterKnife.bind(this);
        Log.d(TAG, "onCreate");
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
        Intent intent = getIntent();
        String uid = intent.getStringExtra("contact_id");
        mName = intent.getStringExtra("name");
        Log.d(TAG, "uid = " + uid + ",name = " + mName);
        mCallOutName.setText(mName);
        mCallInName.setText(mName);
        boolean flag = JCManager.getInstance().call.call("kido_" + uid + "_kido", true);
        Log.d(TAG, flag ? "成功" : "失败");
        if (!flag) {
            Toast.makeText(this, "视频失败，对方未登陆", Toast.LENGTH_SHORT).show();
        }
        updateUI();
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
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        EventBus.getDefault().unregister(this);
        removeCanvas();
    }

    @Subscribe
    public void onEvent(JCEvent jcEvent) {
        if (jcEvent.getEventType() == JCEvent.EventType.CALL_UI) {
            updateUI();
        }
    }

    private void updateUI() {
        List<JCCallItem> callItems = JCManager.getInstance().call.getCallItems();
        if (callItems.size() == 0) {
            Log.e(TAG, "item is 0,will very soon finished");
            removeCanvas();
            finish();
        } else {
            JCCallItem item = JCCallUtils.getActiveCall();
            boolean singleCall = callItems.size() == 1;
            boolean needAnswer = item.getDirection() == JCCall.DIRECTION_IN && item.getState() == JCCall.STATE_PENDING;
            boolean video = item.getVideo();
            Log.d(TAG, "video " + video + " , needAnswer " + needAnswer);
            mRlCallIn.setVisibility(video && needAnswer ? View.VISIBLE : View.INVISIBLE);
            mRlCallOut.setVisibility(video && !needAnswer ? View.VISIBLE : View.INVISIBLE);
            if (video) {
                dealCanvas(item);
            } else {
                removeCanvas();
            }
            dealNeedAnswerCall();
        }
    }

    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_POWER:
                onTerm(mContentView);
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }*/

    private void dealNeedAnswerCall() {
        if (mAlertAnswer == null) {
            final JCCallItem itemNeedAnswerNotActive = JCCallUtils.getNeedAnswerNotActiveCall();
            if (itemNeedAnswerNotActive != null) {
                mAlertAnswer = new AlertDialog.Builder(this)
                        .setTitle(R.string.tip)
                        .setMessage(R.string.tip_answer_call)
                        .setPositiveButton(R.string.answer, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                JCManager.getInstance().call.answer(itemNeedAnswerNotActive, false);
                                mAlertAnswer = null;
                            }
                        })
                        .setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                JCManager.getInstance().call.term(itemNeedAnswerNotActive, JCCall.REASON_BUSY, "");
                                mAlertAnswer = null;
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void dealCanvas(JCCallItem item) {
        // 是否有视频窗口变化
        boolean change = false;
        if (mLocalCanvas == null && item.getUploadVideoStreamSelf()) {
            Log.d(TAG, "mLocalCanvas == null ");
            mLocalCanvas = JCManager.getInstance().mediaDevice.startCameraVideo(JCMediaDevice.RENDER_FULL_SCREEN);
            mLocalCanvas.getVideoView().setZOrderMediaOverlay(true);
            mLocalCanvas.getVideoView().setId(View.generateViewId());
            mContentView.addView(mLocalCanvas.getVideoView(), 0);
            change = true;
        } else if (mLocalCanvas != null && !item.getUploadVideoStreamSelf()) {
            Log.d(TAG, "mLocalCanvas != null ");
            JCManager.getInstance().mediaDevice.stopVideo(mLocalCanvas);
            mContentView.removeView(mLocalCanvas.getVideoView());
            mLocalCanvas = null;
            change = true;
        }

        if (item.getState() == JCCall.STATE_TALKING) {
            Log.d(TAG, "STATE_TALKING ");
            if (mRemoteCanvas == null && item.getUploadVideoStreamOther()) {
                Log.d(TAG, "mRemoteCanvas == null");
                mRemoteCanvas = JCManager.getInstance().mediaDevice.startVideo(item.getRenderId(), JCMediaDevice.RENDER_FULL_SCREEN);
                mRemoteCanvas.getVideoView().setId(View.generateViewId());
                mContentView.addView(mRemoteCanvas.getVideoView(), 0);
                change = true;
            } else if (mRemoteCanvas != null && !item.getUploadVideoStreamOther()) {
                Log.d(TAG, "mRemoteCanvas != null");
                JCManager.getInstance().mediaDevice.stopVideo(mRemoteCanvas);
                mContentView.removeView(mRemoteCanvas.getVideoView());
                mRemoteCanvas = null;
                change = true;
            }
        }

        // 处理视频窗口大小
        if (change) {
            Log.d(TAG, "change");
            if (mLocalCanvas != null && mRemoteCanvas != null) {
                mContentView.removeView(mLocalCanvas.getVideoView());
                mContentView.removeView(mRemoteCanvas.getVideoView());
                mContentView.addView(mRemoteCanvas.getVideoView());
                View view = LayoutInflater.from(this).inflate(R.layout.on_call, null);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams
                        (RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                TextView mOnName = view.findViewById(R.id.call_on_name);
                if (mName != null){
                    mOnName.setText(mName);
                }else{
                    mOnName.setText(item.getDisplayName());
                }
                mOnTerm = view.findViewById(R.id.call_on_term);
                mOnTerm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnTerm.setImageAlpha(255);
                        onTerm(v);
                    }
                });
                mRlCallOut.setVisibility(View.INVISIBLE);
                mRlCallIn.setVisibility(View.INVISIBLE);
                mContentView.addView(view, layoutParams);
                mContentView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnTerm.setImageAlpha(255);
                        mHandler.sendEmptyMessageDelayed(0, 3000);
                    }
                });
                mHandler.sendEmptyMessageDelayed(0, 3000);
            } else if (mLocalCanvas != null) {
                mContentView.removeView(mLocalCanvas.getVideoView());
                mContentView.addView(mLocalCanvas.getVideoView(), 0,
                        new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            } else if (mRemoteCanvas != null) {
                mContentView.removeView(mRemoteCanvas.getVideoView());
                mContentView.addView(mRemoteCanvas.getVideoView(), 0,
                        new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mOnTerm.setImageAlpha(0);
        }
    };

    private void removeCanvas() {
        if (mLocalCanvas != null) {
            mContentView.removeView(mLocalCanvas.getVideoView());
            JCManager.getInstance().mediaDevice.stopVideo(mLocalCanvas);
            mLocalCanvas = null;
        }
        if (mRemoteCanvas != null) {
            mContentView.removeView(mRemoteCanvas.getVideoView());
            JCManager.getInstance().mediaDevice.stopVideo(mRemoteCanvas);
            mRemoteCanvas = null;
        }
    }

    public void onAudioAnswer(View view) {
        JCManager.getInstance().call.answer(JCCallUtils.getActiveCall(), false);
    }

    public void onVideoAnswer(View view) {
        JCManager.getInstance().call.answer(JCCallUtils.getActiveCall(), true);
    }

    public void onTerm(View view) {
        JCManager.getInstance().call.term(JCCallUtils.getActiveCall(), JCCall.REASON_NONE, null);
    }

    public void onHold(View view) {
        JCManager.getInstance().call.hold(JCCallUtils.getActiveCall());
    }

    public void onMute(View view) {
        JCManager.getInstance().call.mute(JCCallUtils.getActiveCall());
    }

    public void onSwitchCamera(View view) {
        JCManager.getInstance().mediaDevice.switchCamera();
    }

    public void onOpenCloseCamera(View view) {
        JCManager.getInstance().call.enableUploadVideoStream(JCCallUtils.getActiveCall());
    }

    /*public void onFullScreen(View view) {
        mFullScreen = !mFullScreen;
        switchFullScreen();
    }

    private void switchFullScreen() {
        Utils.showSystemUI(this, !mFullScreen);
        Utils.setActivityFullScreen(this, mFullScreen);
        JCCallItem item = JCCallUtils.getActiveCall();
        if (item != null && item.getVideo()) {
            mCallOnName.setVisibility(mFullScreen ? View.INVISIBLE : View.VISIBLE);
            if (mFullScreen) {
                mRlCallIn.setVisibility(View.INVISIBLE);
            } else {
                if (item.getDirection() == JCCall.DIRECTION_IN && (item.getState() < JCCall.STATE_CONNECTING || item.getState() > JCCall.STATE_OK)) {
                    mRlCallIn.setVisibility(View.VISIBLE);
                } else {
                }
            }
        }
    }*/
}
