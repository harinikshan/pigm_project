package com.tech.embdes.embdesdemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;
import com.tech.embdes.embdesdemo.data.Constants;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.DbService;
import com.tech.embdes.embdesdemo.data.Device;
import com.tech.embdes.embdesdemo.scan.DeviceScanActivity;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

//import android.support.v7.app.AppCompatActivity;

public class SplashScreen extends Activity {
    private BroadcastReceiver mBroadCastManager = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case DbContract.ACTION_GET_ALL_DEVICES:
                        ArrayList<Device> devices = (ArrayList<Device>) intent.getSerializableExtra(DbContract.DEVICES);
                        if (devices != null && devices.size() > 0) {
                            intent = new Intent(SplashScreen.this, DeviceScanAcitivity2.class);
                            intent.putExtra(Constants.IS_FROM_SPLASH_SCREEN, true);
                            startActivity(intent);
                            finish();
                        } else {
                            intent = new Intent(SplashScreen.this, DeviceScanActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        break;
                }
            }
        }
    };
    public static boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_splash_screen);
        Intent loggerService = new Intent(this, LoggerService.class);
        loggerService.setAction(Constants.LOG_FILES);
        startService(loggerService);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadCastManager, makeGattUpdateIntentFilter());

        Intent getDevices = new Intent(this, DbService.class);
        getDevices.setAction(DbContract.ACTION_GET_ALL_DEVICES);
        startService(getDevices);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadCastManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DbContract.ACTION_GET_ALL_DEVICES);
        return intentFilter;
    }
}
