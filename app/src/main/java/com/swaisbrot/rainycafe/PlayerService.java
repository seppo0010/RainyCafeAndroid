package com.swaisbrot.rainycafe;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerService extends Service {
    public static final int STATUS_CAFE = 1;
    public static final int STATUS_RAIN = 2;

    private MediaPlayer cafePlayer = null;
    private MediaPlayer rainPlayer = null;

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_UPDATE = 3;
    static final int MSG_TOGGLE_CAFE = 4;
    static final int MSG_TOGGLE_RAIN = 5;
    static final int MSG_FETCH_STATUS = 6;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    final Messenger mMessenger = new Messenger(new IncomingHandler());

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

    private MediaPlayer playAudio(String name) {
        try {
            AssetFileDescriptor afd = getAssets().openFd(name);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.setLooping(true);
            player.start();
            return player;
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), e.getMessage());
            return null;
        }
    }

    private void stopAudio(MediaPlayer player) {
        if (player != null) {
            player.stop();
        }
    }

    public void playCafe() {
        cafePlayer = playAudio("cafe.mp3");
    }

    public void stopCafe() {
        stopAudio(cafePlayer);
        cafePlayer = null;
    }

    public boolean isPlayingCafe() {
        return cafePlayer != null && cafePlayer.isPlaying();
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
        rainPlayer = playAudio("rain.mp3");
    }

    public void stopRain() {
        stopAudio(rainPlayer);
        rainPlayer = null;
    }

    public boolean isPlayingRain() {
        return rainPlayer != null && rainPlayer.isPlaying();
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
