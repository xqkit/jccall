package com.kidosc.jcvideo.JCWrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.juphoon.cloud.JCCall;
import com.juphoon.cloud.JCCallCallback;
import com.juphoon.cloud.JCCallItem;
import com.juphoon.cloud.JCClient;
import com.juphoon.cloud.JCClientCallback;
import com.juphoon.cloud.JCMediaChannel;
import com.juphoon.cloud.JCMediaChannelCallback;
import com.juphoon.cloud.JCMediaChannelParticipant;
import com.juphoon.cloud.JCMediaChannelQueryInfo;
import com.juphoon.cloud.JCMediaDevice;
import com.juphoon.cloud.JCMediaDeviceCallback;
import com.juphoon.cloud.JCMessageChannel;
import com.juphoon.cloud.JCMessageChannelCallback;
import com.juphoon.cloud.JCMessageChannelItem;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCConfMessageEvent;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCConfQueryEvent;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCJoinEvent;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCLoginEvent;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCMessageEvent;
import com.kidosc.jcvideo.JCWrapper.JCEvent.JCEvent;
import com.kidosc.jcvideo.R;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 本类主要是对Juphoon Cloud SDK 的简单封装
 */
public class JCManager implements JCClientCallback, JCCallCallback,
        JCMediaChannelCallback, JCMediaDeviceCallback, JCMessageChannelCallback {

    public static JCManager getInstance() {
        return JCManagerHolder.INSTANCE;
    }

    private static final String APP_KEY = "c126e6b01521363ef9d44097";
    private static final String TAG = "JCManager";

    public Boolean pstnMode = false; // 会议的Pstn落地模式

    private Context mContext;
    public JCClient client;
    public JCCall call;
    public JCMediaDevice mediaDevice;
    public JCMediaChannel mediaChannel;
    public JCMessageChannel messageChannel;

    public List<JCMessageChannelItem> listMessages = new ArrayList<>();

    public boolean initialize(Context context) {
        mContext = context;
        client = JCClient.create(context, APP_KEY, this, null);
        mediaDevice = JCMediaDevice.create(client, this);
        mediaChannel = JCMediaChannel.create(client, mediaDevice, this);
        call = JCCall.create(client, mediaDevice, this);
        messageChannel = JCMessageChannel.create(client, this);
        generateDefaultConfig(context);

        client.displayName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.cloud_setting_key_display_name), "");
        client.setConfig(JCClient.CONFIG_KEY_SERVER_ADDRESS,
                PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.cloud_setting_key_server), ""));
        call.maxCallNum = Integer.valueOf(
                PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.cloud_setting_key_call_max_num), ""));
        call.setConference(
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.cloud_setting_key_call_audio_conference), false));
        mediaChannel.setConfig(JCMediaChannel.CONFIG_CAPACITY,
                PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.cloud_setting_key_conference_max_num), ""));

        // 本程序设置为固定方向
        mediaDevice.autoRotate = false;

        return true;
    }

    @Override
    public void onLogin(boolean result, @JCClient.ClientReason int reason) {
        EventBus.getDefault().post(new JCLoginEvent(result, reason));
    }

    @Override
    public void onLogout(@JCClient.ClientReason int reason) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.LOGOUT));
        saveLastLogined("", "");
    }

    @Override
    public void onClientStateChange(@JCClient.ClientState int state, @JCClient.ClientState int oldState) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CLIENT_STATE_CHANGE));
    }

    @Override
    public void onCallItemAdd(JCCallItem item) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CALL_ADD));
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CALL_UI));
    }

    @Override
    public void onCallItemRemove(JCCallItem item, @JCCall.CallReason int reason) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CALL_REMOVE));
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CALL_UI));
    }

    @Override
    public void onCallItemUpdate(JCCallItem item) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CALL_UPDATE));
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CALL_UI));
    }

    @Override
    public void onMediaChannelStateChange(@JCMediaChannel.MediaChannelState int state, @JCMediaChannel.MediaChannelState int oldState) {

    }

    @Override
    public void onMediaChannelPropertyChange() {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CONFERENCE_PROP_CHANGE));
    }

    @Override
    public void onJoin(boolean result, @JCMediaChannel.MediaChannelReason int reason, String channelId) {
        EventBus.getDefault().post(new JCJoinEvent(result, reason, channelId));
        Log.d(TAG,"onJoin result : " + result + " , reason : " + reason + ",channelID : " + channelId);
        if (result && pstnMode) {
            if (mediaChannel.inviteSipUser(channelId) == -1) {
                mediaChannel.leave();
            }
        }
    }

    @Override
    public void onLeave(@JCMediaChannel.MediaChannelReason int reason, String channelId) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CONFERENCE_LEAVE));
    }

    @Override
    public void onQuery(int operationId, boolean result, @JCMediaChannel.MediaChannelReason int reason, JCMediaChannelQueryInfo queryInfo) {
        EventBus.getDefault().post(new JCConfQueryEvent(operationId, result, reason, queryInfo));
    }

    @Override
    public void onParticipantJoin(JCMediaChannelParticipant participant) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CONFERENCE_PARTP_JOIN));
        if (pstnMode) {
            mediaChannel.enableAudioOutput(true);
        }
    }

    @Override
    public void onParticipantLeft(JCMediaChannelParticipant participant) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CONFERENCE_PARTP_LEAVE));
        if (pstnMode) {
            mediaChannel.leave();
        }
    }

    @Override
    public void onParticipantUpdate(JCMediaChannelParticipant participant) {
        EventBus.getDefault().post(new JCEvent(JCEvent.EventType.CONFERENCE_PARTP_UPDATE));
    }

    @Override
    public void onMessageReceive(String type, String content, String fromUserId) {
        EventBus.getDefault().post(new JCConfMessageEvent(type, content, fromUserId));
    }

    @Override
    public void onInviteSipUserResult(int operationId, boolean result, int reason) {
        if (pstnMode && !result) {
            mediaChannel.leave();
        }
    }

    @Override
    public void onCameraUpdate() {

    }

    @Override
    public void onAudioOutputTypeChange(boolean speaker) {

    }

    // 用于自动登录上次登录着的账号
    public boolean loginIfLastLogined() {
        String userId = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(mContext.getString(R.string.cloud_setting_last_login_user_id), null);
        if (TextUtils.isEmpty(userId)) {
            return false;
        }
        String password = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(mContext.getString(R.string.cloud_setting_last_login_password), null);
        return client.login(userId, password);
    }

    // 保存最后一次登录账号信息
    public void saveLastLogined(String userId, String password) {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .putString(mContext.getString(R.string.cloud_setting_last_login_user_id), userId)
                .putString(mContext.getString(R.string.cloud_setting_last_login_password), password)
                .apply();
    }

    // 生成默认配置
    private void generateDefaultConfig(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        String value = sp.getString(context.getString(R.string.cloud_setting_key_display_name), "");
        if (TextUtils.isEmpty(value)) {
            editor.putString(context.getString(R.string.cloud_setting_key_display_name), "");
        }
        value = sp.getString(context.getString(R.string.cloud_setting_key_server), "");
        if (TextUtils.isEmpty(value)) {
            editor.putString(context.getString(R.string.cloud_setting_key_server), client.getConfig(JCClient.CONFIG_KEY_SERVER_ADDRESS));
        }
        value = sp.getString(context.getString(R.string.cloud_setting_key_call_max_num), "");
        if (TextUtils.isEmpty(value)) {
            editor.putString(context.getString(R.string.cloud_setting_key_call_max_num), String.valueOf(call.maxCallNum));
        }
        value = sp.getString(context.getString(R.string.cloud_setting_key_conference_max_num), "");
        if (TextUtils.isEmpty(value)) {
            editor.putString(context.getString(R.string.cloud_setting_key_conference_max_num), mediaChannel.getConfig(JCMediaChannel.CONFIG_CAPACITY));
        }
        editor.apply();
    }

    @Override
    public void onMessageSendUpdate(JCMessageChannelItem jcMessageChannelItem) {
        EventBus.getDefault().post(new JCMessageEvent(true, jcMessageChannelItem));
    }

    @Override
    public void onMessageRecv(JCMessageChannelItem jcMessageChannelItem) {
        EventBus.getDefault().post(new JCMessageEvent(false, jcMessageChannelItem));
    }

    private static final class JCManagerHolder {
        private static final JCManager INSTANCE = new JCManager();
    }
}
