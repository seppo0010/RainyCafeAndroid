package com.swaisbrot.rainycafe;

import android.app.Service;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerService extends Service {
    public static final int STATUS_CAFE = 1;
    public static final int STATUS_RAIN = 2;

    private SoundPool soundPool;
    private int cafeSound;
    private int cafeStream = 0;
    private boolean cafePaused = false;
    private int rainSound;
    private int rainStream = 0;
    private boolean rainPaused = false;

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_UPDATE = 3;
    static final int MSG_TOGGLE_CAFE = 4;
    static final int MSG_TOGGLE_RAIN = 5;
    static final int MSG_FETCH_STATUS = 6;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        soundPool = new SoundPool.Builder().setMaxStreams(2).build();
        try {
            cafeSound = soundPool.load(getAssets().openFd("cafe.mp3"), 1);
            rainSound = soundPool.load(getAssets().openFd("rain.mp3"), 1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendUpdate() {
        int status = (isPlayingCafe() ? STATUS_CAFE : 0) + (isPlayingRain() ? STATUS_RAIN : 0);
        for (int i= mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_UPDATE, status, 0));
            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }

    public void playCafe() {
        if (cafeStream == 0) {
            cafeStream = soundPool.play(cafeSound, 100, 100, 0, -1, 1.0f);
        } else {
            soundPool.resume(cafeStream);
            cafePaused = false;
        }
    }

    public void stopCafe() {
        if (cafeStream != 0) {
            soundPool.pause(cafeStream);
            cafePaused = true;
        }
    }

    public boolean isPlayingCafe() {
        return cafeStream != 0 && !cafePaused;
    }

    public void toggleCafe() {
        if (isPlayingCafe()) {
            stopCafe();
        } else {
            playCafe();
        }
        sendUpdate();
    }

    public void playRain() {
        if (rainStream == 0) {
            rainStream = soundPool.play(rainSound, 100, 100, 0, -1, 1.0f);
        } else {
            soundPool.resume(rainStream);
            rainPaused = false;
        }
    }

    public void stopRain() {
        if (rainStream != 0) {
            soundPool.pause(rainStream);
            rainPaused = true;
        }
    }

    public boolean isPlayingRain() {
        return rainStream != 0 && !rainPaused;
    }

    public void toggleRain() {
        if (isPlayingRain()) {
            stopRain();
        } else {
            playRain();
        }
        sendUpdate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_TOGGLE_CAFE:
                    toggleCafe();
                    break;
                case MSG_TOGGLE_RAIN:
                    toggleRain();
                    break;
                case MSG_FETCH_STATUS:
                    sendUpdate();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
