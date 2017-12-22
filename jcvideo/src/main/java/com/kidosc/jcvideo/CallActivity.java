package com.kidosc.jcvideo;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.juphoon.cloud.JCCall;
import com.juphoon.cloud.JCCallItem;
import com.juphoon.cloud.JCMediaDevice;
import com.juphoon.cloud.JCMediaDeviceVideoCanvas;
import com.kidosc.jcvideo.JCWrapper.JCCallUtils;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCEvent;
import com.kidosc.jcvideo.JCWrapper.JCManager;
import com.kidosc.jcvideo.Toos.Utils;
import com.kidosc.jcvideo.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Desc:
 * Email:   frank.xiong@kidosc.com
 * Date:    2017/12/21 18:38
 */

public class CallActivity extends Activity {

    @BindView(R.id.layoutCall)
    public ConstraintLayout mContentView;
    @BindView(R.id.layoutAudioIn)
    public View mAudioIn;
    @BindView(R.id.layoutVideoIn)
    public View mVideoIn;
    @BindView(R.id.layoutVideoInCall)
    public View mVideoInCall;
    @BindView(R.id.txtUserId)
    public TextView mTxtUserId;
    @BindView(R.id.txtCallInfo)
    public TextView mTxtCallInfo;
    @BindView(R.id.txtNetStatus)
    public TextView mTxtNetStatus;

    private boolean mFullScreen;
    private JCMediaDeviceVideoCanvas mLocalCanvas;
    private JCMediaDeviceVideoCanvas mRemoteCanvas;
    private AlertDialog mAlertAnswer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_page);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        JCManager.getInstance().call.call("a666aa", true);
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
//            stopCallInfoTimer();
            removeCanvas();
            finish();
        } else {
//            startCallInfoTimer();
            JCCallItem item = JCCallUtils.getActiveCall();
            boolean singleCall = callItems.size() == 1;
            mTxtUserId.setVisibility(singleCall ? View.VISIBLE : View.INVISIBLE);
            mTxtCallInfo.setVisibility(singleCall ? View.VISIBLE : View.INVISIBLE);
            mTxtNetStatus.setVisibility(singleCall ? View.VISIBLE : View.INVISIBLE);
            if (singleCall) {
                mTxtUserId.setText(item.getDisplayName());
            }
            boolean needAnswer = item.getDirection() == JCCall.DIRECTION_IN && item.getState() == JCCall.STATE_PENDING;
            boolean video = item.getVideo();
            mAudioIn.setVisibility(!video && needAnswer ? View.VISIBLE : View.INVISIBLE);
            mVideoIn.setVisibility(video && needAnswer ? View.VISIBLE : View.INVISIBLE);
            mVideoInCall.setVisibility(video && !needAnswer ? View.VISIBLE : View.INVISIBLE);
            if (video) {
                dealCanvas(item);
//                updateVideoInCallViews(item);
            } else {
                removeCanvas();
//                updateAudioInCallViews(item);
            }
            dealNeedAnswerCall();
        }
    }

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
            mLocalCanvas = JCManager.getInstance().mediaDevice.startCameraVideo(JCMediaDevice.RENDER_FULL_SCREEN);
            mLocalCanvas.getVideoView().setZOrderMediaOverlay(true);
            mLocalCanvas.getVideoView().setId(View.generateViewId());
            mContentView.addView(mLocalCanvas.getVideoView(), 0);
            change = true;
        } else if (mLocalCanvas != null && !item.getUploadVideoStreamSelf()) {
            JCManager.getInstance().mediaDevice.stopVideo(mLocalCanvas);
            mContentView.removeView(mLocalCanvas.getVideoView());
            mLocalCanvas = null;
            change = true;
        }

        if (item.getState() == JCCall.STATE_TALKING) {
            if (mRemoteCanvas == null && item.getUploadVideoStreamOther()) {
                mRemoteCanvas = JCManager.getInstance().mediaDevice.startVideo(item.getRenderId(), JCMediaDevice.RENDER_FULL_SCREEN);
                mRemoteCanvas.getVideoView().setId(View.generateViewId());
                mContentView.addView(mRemoteCanvas.getVideoView(), 0);
                change = true;
            } else if (mRemoteCanvas != null && !item.getUploadVideoStreamOther()) {
                JCManager.getInstance().mediaDevice.stopVideo(mRemoteCanvas);
                mContentView.removeView(mRemoteCanvas.getVideoView());
                mRemoteCanvas = null;
                change = true;
            }
        }

        // 处理视频窗口大小
        if (change) {
            if (mLocalCanvas != null && mRemoteCanvas != null) {
                mContentView.removeView(mLocalCanvas.getVideoView());
                mContentView.removeView(mRemoteCanvas.getVideoView());
                mContentView.addView(mRemoteCanvas.getVideoView());
//                mContentView.addView(mRemoteCanvas.getVideoView(), 0);
//                mContentView.addView(mLocalCanvas.getVideoView(), 1);
//                ConstraintSet constraintSet = new ConstraintSet();
//                constraintSet.clone(mContentView);
//                constraintSet.constrainWidth(mLocalCanvas.getVideoView().getId(), Utils.dip2px(this, 80));
//                constraintSet.constrainHeight(mLocalCanvas.getVideoView().getId(), Utils.dip2px(this, 120));
//                constraintSet.connect(mLocalCanvas.getVideoView().getId(),
//                        ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, Utils.dip2px(this, 8));
//                constraintSet.connect(mLocalCanvas.getVideoView().getId(),
//                        ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, Utils.dip2px(this, 24));
//                constraintSet.applyTo(mContentView);
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

//    private void updateAudioInCallViews(JCCallItem item) {
//        mBtnSpeakerAudio.setSelected(JCManager.getInstance().mediaDevice.isSpeakerOn());
//        if (item.getState() == JCCall.STATE_TALKING) {
//            mBtnMuteAudio.setEnabled(true);
//            mBtnHoldAudio.setEnabled(!item.getHeld());
//            mBtnHoldAudio.setSelected(item.getHold());
//            mBtnMuteAudio.setSelected(item.getMute());
//        } else {
//            mBtnMuteAudio.setEnabled(false);
//            mBtnHoldAudio.setEnabled(false);
//        }
//    }

//    private void updateVideoInCallViews(JCCallItem item) {
//        mBtnSpeakerVideo.setSelected(JCManager.getInstance().mediaDevice.isSpeakerOn());
//        if (item.getState() == JCCall.STATE_TALKING) {
//            mBtnCameraVideo.setEnabled(true);
//            mBtnMuteVideo.setEnabled(true);
//            mBtnMuteVideo.setSelected(item.getMute());
//            mBtnCameraVideo.setSelected(item.getUploadVideoStreamSelf());
//        } else {
//            mBtnMuteVideo.setEnabled(false);
//            mBtnHoldAudio.setEnabled(false);
//            mBtnCameraVideo.setEnabled(false);
//        }
//    }


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

    public void onFullScreen(View view) {
        mFullScreen = !mFullScreen;
        switchFullScreen();
    }

    private void switchFullScreen() {
        Utils.showSystemUI(this, !mFullScreen);
        Utils.setActivityFullScreen(this, mFullScreen);
        JCCallItem item = JCCallUtils.getActiveCall();
        if (item != null && item.getVideo()) {
            mTxtUserId.setVisibility(mFullScreen ? View.INVISIBLE : View.VISIBLE);
            mTxtCallInfo.setVisibility(mFullScreen ? View.INVISIBLE : View.VISIBLE);
            mTxtNetStatus.setVisibility(mFullScreen ? View.INVISIBLE : View.VISIBLE);
            if (mFullScreen) {
                mVideoIn.setVisibility(View.INVISIBLE);
                mVideoInCall.setVisibility(View.INVISIBLE);
            } else {
                if (item.getDirection() == JCCall.DIRECTION_IN && (item.getState() < JCCall.STATE_CONNECTING || item.getState() > JCCall.STATE_OK)) {
                    mVideoIn.setVisibility(View.VISIBLE);
                } else {
                    mVideoInCall.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
