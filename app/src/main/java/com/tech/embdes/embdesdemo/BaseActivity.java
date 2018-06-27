package com.tech.embdes.embdesdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.tech.embdes.embdesdemo.data.Analysis;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.DbService;
import com.tech.embdes.embdesdemo.data.Device;
import com.tech.embdes.embdesdemo.scan.DeviceScanActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

import static android.content.ContentValues.TAG;

/**
 * Created by sakkam2 on 1/22/2018.
 */

public abstract class BaseActivity extends Activity {
    private String mName;
    private String mDeviceAddress;
    protected ArrayList<Analysis> mAnalyses = new ArrayList<>();
    private ArrayList<Device> mDevices;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint({"WrongConstant", "ShowToast"})
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mName = intent.getStringExtra(BluetoothLeService.EXTRA_NAME);
                mDeviceAddress = intent.getStringExtra(BluetoothLeService.EXTRA_ADRESS);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                System.out.println("Discovered");
            } else if (BluetoothLeService.ACTION_GATT_CONNECTING.equals(action)) {
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADRESS);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                System.out.println("History");
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null) {
                    String hex = Integer.toHexString(Integer.parseInt(data));
                    switch (hex) {
                        case "a1":
//                            Toast.makeText(History.this, "Time Updated", Toast.LENGTH_SHORT).show();
                            break;
                        case "b":

                            break;
                        case "c":
//                            Toast.makeText(History.this, "No Data Avaliable", 300).show();
                            break;
                        case "3e8":

                            break;
                    }
                }
            } else if (DbContract.ACTION_GET_FILTERED_ANALYSIS.equals(action)) {
                mAnalyses.clear();
                mAnalyses = (ArrayList<Analysis>) intent.getSerializableExtra(DbContract.ANALYSIS);
                onAnalysisAvailable();
            }else if (DbContract.ACTION_GET_ALL_DEVICES.equals(action)) {
                mDevices = (ArrayList<Device>) intent.getSerializableExtra(DbContract.DEVICES);
                startBleService();
            }
        }
    };

    private void startBleService() {
        gattServiceIntent = new Intent(this, BluetoothLeService.class);
        isBound = bindService(gattServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);
        gattServiceIntent.setAction(BluetoothLeService.ACTION_READ_DEVICES);
        gattServiceIntent.putExtra(BluetoothLeService.EXTRA_INTERVAL, value);
        startService(gattServiceIntent);
    }

    public static boolean active = false;

    public abstract void onAnalysisAvailable();

    private long value;
    private boolean isBound;
    private BluetoothAdapter mBluetoothAdapter;
    private Intent gattServiceIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences preferences = getSharedPreferences("Time", MODE_PRIVATE);
        if (preferences != null) {
            value = preferences.getLong("Timevalue", 20 * 1000);
            Log.d("TIME", "onCreate: " + value);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(locationcheck() && bluetooth()) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());


            Intent deviceService = new Intent(this, DbService.class);
            deviceService.setAction(DbContract.ACTION_GET_ALL_DEVICES);
            startService(deviceService);
        }
    }

    public boolean locationcheck() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean bluetooth() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(DbContract.ACTION_GET_FILTERED_ANALYSIS);
        intentFilter.addAction(DbContract.ACTION_GET_ALL_DEVICES);
        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.e(TAG, "ACTION_READ_NEXT:value"+value);
            Log.e(TAG, "onServiceConnected");
            mBluetoothLeService.addDevices(mDevices);
            mBluetoothLeService.triggerLoopDevices(value);
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
//            }
//           //  Automatically connects to the device upon successful start-up initialization.
//            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // mBluetoothLeService = null;
        }
    };

    public void readAnalysis(long startTime, long endTime, long deviceId) {
        Intent readAnalysis = new Intent(this, DbService.class);
        readAnalysis.setAction(DbContract.ACTION_GET_FILTERED_ANALYSIS);
        readAnalysis.putExtra(DbContract.AnalysisEntry.COLUMN_DEVICE, deviceId);
        readAnalysis.putExtra(DbContract.FROM_TIME, startTime);
        readAnalysis.putExtra(DbContract.TO_TIME, endTime);
        startService(readAnalysis);
    }

    public String getFormattedTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy \n HH:mm:ss");
        return formatter.format(new Date(time));
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
}
