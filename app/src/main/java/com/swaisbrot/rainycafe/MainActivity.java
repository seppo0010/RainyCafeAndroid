package com.swaisbrot.rainycafe;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class MainActivity extends AppCompatActivity {
    private boolean mBound = false;
    private Messenger mService;

    private void setupToggle(int id, final int message) {
        ((CheckBox)findViewById(id)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mService != null) {
                    try {
                        mService.send(Message.obtain(null, message));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateView(0);

        setupToggle(R.id.cafe, PlayerService.MSG_TOGGLE_CAFE);
        setupToggle(R.id.rain, PlayerService.MSG_TOGGLE_RAIN);
        bindService(new Intent(this, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void updateView(int status) {
        ((CheckBox)findViewById(R.id.cafe)).setChecked((status & PlayerService.STATUS_CAFE) > 0);
        ((CheckBox)findViewById(R.id.rain)).setChecked((status & PlayerService.STATUS_RAIN) > 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = new Messenger(binder);
            try {
                mMessenger.send(Message.obtain(null, PlayerService.MSG_FETCH_STATUS));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PlayerService.MSG_UPDATE:
                    updateView(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
