package com.tech.embdes.embdesdemo.scan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.tech.embdes.embdesdemo.Ble_Pojo;
import com.tech.embdes.embdesdemo.DeviceScanAcitivity2;
import com.tech.embdes.embdesdemo.R;
import com.tech.embdes.embdesdemo.data.Constants;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.DbService;

import java.util.UUID;

import io.fabric.sdk.android.Fabric;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity implements OnClick {
    public final static UUID UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 30 * 1000;
    private static final String TAG = DeviceScanActivity.class.getSimpleName();
    Button add;
    RecyclerView listView;
    PowerManager.WakeLock wakeLock;
    ProgressDialog pDialog;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private DevicesAdapter mDeviceListAdapter;
    private Boolean exit = false;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    Log.e(TAG, "Scanned device:name-->"+device.getName()+"::address-->"+device.getAddress());
                    runOnUiThread(() -> {
                        BleDevice bleDevice = new BleDevice();
                        bleDevice.setName(device.getName());
                        bleDevice.setAddress(device.getAddress());
                        mDeviceListAdapter.addDevice(bleDevice);
                    });
                }
            };
    public static boolean active = false;

    public void statuscheck() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            builderalertmessage();
        }
    }

    private void builderalertmessage() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Location is not Enabled");
        b.setMessage("Would you like to Enable ?");
        b.setCancelable(false);
        b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent1);
            }
        });
        AlertDialog a = b.create();
        a.show();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                return;
        }
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        getActionBar().setTitle(R.string.title_devices);
        setContentView(R.layout.devicexml);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        statuscheck();
        add = findViewById(R.id.button);
        listView = findViewById(R.id.listview);
        mHandler = new Handler();
        pDialog = new ProgressDialog(DeviceScanActivity.this);
        pDialog.setTitle("");
        pDialog.setMessage("Connecting, Please wait..");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        add.setOnClickListener(view -> {
            if (mDeviceListAdapter.getSelectedDevice().size() > 0) {
                Intent dbService = new Intent(DeviceScanActivity.this, DbService.class);
                dbService.setAction(DbContract.ACTION_INSERT_MULTI_DEVICES);
                dbService.putExtra(DbContract.DEVICES, mDeviceListAdapter.getSelectedDevice());
                startService(dbService);
                //	registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                final Intent intent = new Intent(DeviceScanActivity.this, DeviceScanAcitivity2.class);
                intent.putExtra(Constants.IS_FROM_SPLASH_SCREEN, true);
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    Log.e(TAG, "Stopped Scan (onCreate)");
                    mScanning = false;
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } else {
                AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanActivity.this);
                build.setMessage("Select device before clicking add");
                build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog a = build.create();
                a.show();
            }
        });
        int permissioncheck = ContextCompat.checkSelfPermission(DeviceScanActivity.this, Manifest.permission.READ_PHONE_STATE);

        if (permissioncheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DeviceScanActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 10);
        }
        if (ActivityCompat.checkSelfPermission(DeviceScanActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DeviceScanActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                }, 10);
            }
        }
        // Initializes list view adapter.
        mDeviceListAdapter = new DevicesAdapter(this);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(mDeviceListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_sync).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.adddevice).setVisible(false);
            menu.findItem(R.id.addeddevices).setVisible(false);
        } else {
            menu.findItem(R.id.menu_sync).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
            menu.findItem(R.id.adddevice).setVisible(false);
            menu.findItem(R.id.addeddevices).setVisible(false);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (exit) {
            finish(); // finish activity
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //wakeLock.acquire();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    Log.e(TAG, "Stopped Scan (scanLeDevice -> runnable)");
                }
            }, SCAN_PERIOD);

            mScanning = true;
            UUID[] uuids = new UUID[1];
            uuids[0] = UUID_HEART_RATE_SERVICE;
            mDeviceListAdapter.getDevices().clear();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            Log.e(TAG, "Started Scan (scanLeDevice)");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            Log.e(TAG, "Stopped Scan (scanLeDevice)");
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onClick(int position, boolean selected) {
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            Log.e(TAG, "Stopped Scan (onClick(int position, boolean selected))");
            mScanning = false;
        }
    }
}