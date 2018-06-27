package com.tech.embdes.embdesdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.DbService;
import com.tech.embdes.embdesdemo.data.Device;
import com.tech.embdes.embdesdemo.scan.DeviceScanActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;

import static com.tech.embdes.embdesdemo.data.Constants.IS_FROM_SPLASH_SCREEN;

/**
 * Created by Aakash on 21-08-2017.
 */

public class DeviceScanAcitivity2 extends Activity {
    // Stops scanning after 10 seconds.
    static final int GATT_TIMEOUT = 100;
    private static final int REQUEST_ENABLE_BT = 1;
    public static boolean active = false;
    public static BluetoothLeService mBluetoothLeService;
    static boolean click = false;
    static int j = 0;
    static int k = 0;
    static boolean checking = false;
    static String connection;
    static boolean flag2 = false;
    static boolean flag3 = false;
    static boolean flag4 = false;
    private static String TAG = "Check";
    EditText name, address;
    Button connect;
    boolean timercheck = true;
    boolean servicecheck;
    ListView listservice;
    ArrayList<String> servicelist;
    EditText time;
    Calendar calender;
    String flag;
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    Button ok;
    boolean check, check1;
    ArrayList<Device> configuredDevices;
    ArrayList<Integer> names;
    ListView listView;
    Timer timer;
    int lengthsample;
    Handler handler;
    TextView mDataField;
    int length;
    boolean isDeleted, isAdded = false;
    long value = 5 * 60 * 1000;
    boolean flag1 = true;
    Intent i;
    Toast toast;
    int count = 0;
    ProgressDialog pDialog;
    ViewHolder viewh = new ViewHolder();
    BluetoothGattCharacteristic mNotifyCharacteristic;
    String connectionsate;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            connectionsate = "Connected";
            mBluetoothLeService.addDevices(configuredDevices);
            mBluetoothLeService.triggerLoopDevices(value);
            Log.e(TAG, "onServiceConnected");
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
//            }
//           //  Automatically connects to the device upon successful start-up initialization.
//            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            connectionsate = "Disconnected";
            // mBluetoothLeService = null;
        }
    };
    ArrayList<BluetoothGattCharacteristic> mcharcterstics = new ArrayList<BluetoothGattCharacteristic>();
    PowerManager.WakeLock wakeLock;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private boolean mConnected = true;
    //private boolean checkflag;
    private boolean isBound;
    private int size = 0;
    private Intent gattServiceIntent;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint({"WrongConstant", "ShowToast"})
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                System.out.println("Connected");
                mConnected = true;
                connectionsate = "Connected";
                flag2 = true;
                Log.d("postion", "onReceive:987 " + length);
                mLeDeviceListAdapter.updatetext(length - 1);
                Log.d("postion", "onReceive:345 " + length);
                mLeDeviceListAdapter.notifyDataSetChanged();
                String name = getSelectedDevice() != null ? getSelectedDevice().getName() : "";
                Toast.makeText(DeviceScanAcitivity2.this, "Device Connected:" + name, 300).show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                String name = getSelectedDevice() != null ? getSelectedDevice().getName() : "";
                Toast.makeText(DeviceScanAcitivity2.this, "Device DisConnected", 300).show();
                invalidateOptionsMenu();
                flag2 = false;
                k++;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                String name = getSelectedDevice() != null ? getSelectedDevice().getName() : "";
                Toast.makeText(DeviceScanAcitivity2.this, "Services Discovered" + name, 300).show();
            } else if (BluetoothLeService.ACTION_GATT_CONNECTING.equals(action)) {
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADRESS);
                Toast.makeText(DeviceScanAcitivity2.this, "Connecting to " + address, 300).show();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                System.out.println("History");
                flag = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (flag != null) {
                    String hex = Integer.toHexString(Integer.parseInt(flag));
                    switch (hex) {
                        case "a1":
                            Toast.makeText(DeviceScanAcitivity2.this, "Time Updated", 300).show();
                            break;
                        case "b":
                            lengthsample = Integer.parseInt(intent.getStringExtra("length"));
                            Log.d("TIMEUPDATE", "onReceive: " + lengthsample);
                            break;
                        case "c":
                            Toast.makeText(DeviceScanAcitivity2.this, "No Data Avaliable", 300).show();
                            break;
                        case "3e8":
                            check1 = true;
                            Log.e(TAG, "Hex:Reading data");
                            String name = getSelectedDevice() != null ? getSelectedDevice().getName() : "";
                            toast = Toast.makeText(DeviceScanAcitivity2.this, "Reading Data:" + name, 300);
                            toast.show();
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    toast.cancel();
                                }
                            }, 5);
                            break;
                    }
                }
            } else if (DbContract.ACTION_GET_ALL_DEVICES.equals(action)) {
                ArrayList<Device> devices = (ArrayList<Device>) intent.getSerializableExtra(DbContract.DEVICES);
                configuredDevices.clear();
                configuredDevices.addAll(devices);

                mLeDeviceListAdapter = new LeDeviceListAdapter(configuredDevices);
                listView.setAdapter(mLeDeviceListAdapter);
                //scanLeDevice(true);
                mLeDeviceListAdapter.notifyDataSetChanged();

                if (configuredDevices.size() == 0) {
                    Intent scanActivity = new Intent(DeviceScanAcitivity2.this, DeviceScanActivity.class);
                    startActivity(scanActivity);
                    finish();
                } else if (isAdded || isDeleted) {
                    Intent mIntent = new Intent(DeviceScanAcitivity2.this, BluetoothLeService.class);
                    mIntent.setAction(BluetoothLeService.ACTION_UPDATED_DEVICES);
                    startService(mIntent);
                    isAdded = isDeleted = false;
                } else {
                    if (isFromSplashScreen) {
                        startBleService();
                    } else {
                        mBleServiceStartHandler.postDelayed(mBleServiceStartRunnable, 45 * 1000);
                    }
                    isFromSplashScreen = false;
                }
            } else if (DbContract.ACTION_EXPORT_ALL_LOGS.equals(action)) {
                Toast.makeText(DeviceScanAcitivity2.this, "Data Exported.", Toast.LENGTH_SHORT).show();
            } else if (DbContract.ACTION_NO_DATA_TO_EXPORT.equals(action)) {
                Toast.makeText(DeviceScanAcitivity2.this, "No Data Available..", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private boolean isRegistered = false;
    private boolean isFromSplashScreen;

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

    public Device getSelectedDevice() {
        if (mBluetoothLeService != null && mBluetoothLeService.getActiveDeviceIndex() < configuredDevices.size()) {
            return configuredDevices.get(mBluetoothLeService.getActiveDeviceIndex());
        }
        return null;
    }

    // Device scan callback.
//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//
//                            Cursor c = database.query("Bluetooth",null,"address = '"+device.getAddress().toString()+"'",null,null,null,null);
//                            if(c.getCount()>0) {
//                                while(c.moveToNext()) {
//                                    mLeDeviceListAdapter.addDevice(device);
//                                    mLeDeviceListAdapter.notifyDataSetChanged();
//                                }
//                            }
//                        }
//                    });
//                }
//            };
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(DbContract.ACTION_GET_ALL_DEVICES);
        intentFilter.addAction(DbContract.ACTION_NO_DATA_TO_EXPORT);
        intentFilter.addAction(DbContract.ACTION_EXPORT_DB);
        intentFilter.addAction(DbContract.ACTION_EXPORT_ALL_LOGS);
        return intentFilter;
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//         finish();
//    }\

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void statuscheck() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            builderalertmessage();
        }
    }

    public boolean locationcheck() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void builderalertmessage() {
        AlertDialog.Builder b = new AlertDialog.Builder(DeviceScanAcitivity2.this);
        b.setTitle("Location is not Enabled");
        b.setMessage("Would you like to Enable ?");
        b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent1);
                dialog.cancel();
            }
        });
        b.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(DeviceScanAcitivity2.this, "Enable Location And Bluetooth for Multiple Connection", Toast.LENGTH_SHORT).show();
                dialog.cancel();
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            isFromSplashScreen = getIntent().getBooleanExtra(IS_FROM_SPLASH_SCREEN, false);
        }
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.device2);
        getActionBar().setTitle("Device List");
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyWakelockTag");

        statuscheck();
        listView = (ListView) findViewById(R.id.listview2);
        servicelist = new ArrayList<>();
        mHandler = new Handler();
        handler = new Handler();
        configuredDevices = new ArrayList<Device>();
        names = new ArrayList<Integer>();
        time = findViewById(R.id.edittext2);
        pDialog = new ProgressDialog(DeviceScanAcitivity2.this);
        pDialog.setTitle("");
        pDialog.setMessage("Connecting, Please wait..");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        Date d3 = calendar.getTime();
        Log.d(TAG, "onCreate: " + d3);
        Log.d(TAG, "onCreate: " + d3.getTime());
        SharedPreferences preferences = getSharedPreferences("Time", MODE_PRIVATE);
        if (preferences != null) {
            value = preferences.getLong("Timevalue", 5 * 60 * 1000);
            Log.d("TIME", "onCreate: " + value);
        }
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        /// mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        //mGattServicesList.setOnChildClickListener(servicesListClickListner);
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
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

        gattServiceIntent = new Intent(DeviceScanAcitivity2.this, BluetoothLeService.class);
//        isBound = bindService(gattServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Intent intent = new Intent(DeviceScanAcitivity2.this,Values.class);
//                intent.putExtra("Device_Address",configuredDevices.get(i).getAddress());
//                intent.putExtra("Device_Name",configuredDevices.get(i).getName());
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
                if (mBluetoothLeService == null || !mBluetoothLeService.isReadingInProgress()) {
                    check = true;
//                mBluetoothLeService.disconnect(mDeviceAddress);
//                stopService(gattServiceIntent);

//                gattServiceIntent.putExtra("add",mDeviceAddress);
//                startService(gattServiceIntent);
                    Intent intet = new Intent(DeviceScanAcitivity2.this, Values.class);
                    intet.putExtra(DbContract.DEVICES, configuredDevices.get(i));
                    intet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intet.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intet);

                } else {
                    AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    StringBuilder messageBuilder = new StringBuilder("Wait until the Process Finishes");
                    if (getCurrentState() != null) {
                        messageBuilder.append("\n");
                        messageBuilder.append("Current state: \n");
                        messageBuilder.append(getCurrentState());
                    }
                    build.setMessage(messageBuilder.toString());
                    build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog a = build.create();
                    a.show();
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                build.setTitle("You Have to Delete the Selected Device");
                build.setMessage("Device : " + configuredDevices.get(i).getName());
                build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        isDeleted = true;
                        Intent deleteService = new Intent(DeviceScanAcitivity2.this, DbService.class);
                        deleteService.setAction(DbContract.ACTION_DELETE_DEVICE);
                        deleteService.putExtra(DbContract.AnalysisEntry.COLUMN_DEVICE, i);
                        startService(deleteService);

                        Intent deviceService = new Intent(DeviceScanAcitivity2.this, DbService.class);
                        deviceService.setAction(DbContract.ACTION_GET_ALL_DEVICES);
                        startService(deviceService);

                        dialogInterface.cancel();
                    }
                });
                build.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog a = build.create();
                a.show();
                return true;
            }
        });

        int permissioncheck = ContextCompat.checkSelfPermission(DeviceScanAcitivity2.this, Manifest.permission.READ_PHONE_STATE);

        if (permissioncheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DeviceScanAcitivity2.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 10);
        }
        if (ActivityCompat.checkSelfPermission(DeviceScanAcitivity2.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DeviceScanAcitivity2.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                }, 10);
            }

        }
    }

    private String getCurrentState() {
        if (mBluetoothLeService != null) {
            switch (mBluetoothLeService.getCurrentState()) {
                case BluetoothLeService.STATE_IDLE:
                case BluetoothLeService.STATE_DISCONNECTED:
                case BluetoothLeService.STATE_READ:
                    return null;
                case BluetoothLeService.STATE_CONNECTING:
                    if (getSelectedDevice() != null)
                        return "Connecting to " + getSelectedDevice().getName();
                    else
                        return "Connecting";
                case BluetoothLeService.STATE_CONNECTED:
                    if (getSelectedDevice() != null)
                        return "Connected to " + getSelectedDevice().getName();
                    else
                        return "Connected";
                case BluetoothLeService.STATE_ENABLE_NOTIFICATION:
                    if (getSelectedDevice() != null)
                        return "Enabling Notification for " + getSelectedDevice().getName();
                    else
                        return "Enabling Notification";
                case BluetoothLeService.STATE_SYNC_TIME:
                    if (getSelectedDevice() != null)
                        return "Syncing Time with " + getSelectedDevice().getName();
                    else
                        return "Syncing Time";
                case BluetoothLeService.STATE_READ_REQUESTED:
                    if (getSelectedDevice() != null)
                        return "Requesting data from " + getSelectedDevice().getName();
                    else
                        return "Requesting data";
                case BluetoothLeService.STATE_READING_IN_PROGRESS:
                    if (getSelectedDevice() != null)
                        return "Reading data from " + getSelectedDevice().getName();
                    else
                        return "Reading data data";
            }
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_sync).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.back).setVisible(true);
            menu.findItem(R.id.history1).setVisible(true);
            menu.findItem(R.id.configurabletime).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.adddevice).setVisible(true);
            menu.findItem(R.id.stoptimer).setVisible(true);
            menu.findItem(R.id.graphview2).setVisible(true);
        } else {
            menu.findItem(R.id.menu_sync).setVisible(true);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.back).setVisible(true);
            menu.findItem(R.id.stoptimer).setVisible(true);
            menu.findItem(R.id.history1).setVisible(true);
            menu.findItem(R.id.adddevice).setVisible(true);
            menu.findItem(R.id.configurabletime).setVisible(true);
            menu.findItem(R.id.graphview2).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);

        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync:
                if (mBluetoothLeService != null && !mBluetoothLeService.isReadingInProgress()) {
                    Toast.makeText(this, "Exporting DB...", Toast.LENGTH_SHORT).show();
                    Intent syncDevice = new Intent(DeviceScanAcitivity2.this, DbService.class);
                    syncDevice.setAction(DbContract.ACTION_EXPORT_ALL_LOGS);
                    startService(syncDevice);
                } else {
                    AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    StringBuilder messageBuilder = new StringBuilder("Wait until the Process Finishes");
                    if (getCurrentState() != null) {
                        messageBuilder.append("\n");
                        messageBuilder.append("Current state: \n");
                        messageBuilder.append(getCurrentState());
                    }
                    build.setMessage(messageBuilder.toString());

                    build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog a = build.create();
                    a.show();
                }
                break;
            case R.id.menu_scan:
                // mLeDeviceListAdapter.clear();
                // scanLeDevice(true);
                // mLeDeviceListAdapter.notifyDataSetChanged();
                break;
            case R.id.menu_stop:
                //scanLeDevice(false);
                // mLeDeviceListAdapter.notifyDataSetChanged();
                break;
            case R.id.back:
                AlertDialog.Builder build12 = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                build12.setTitle("All the Device Information Erased");
                build12.setMessage("Are you sure to erase");
                build12.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent deleteService = new Intent(DeviceScanAcitivity2.this, DbService.class);
                        deleteService.setAction(DbContract.ACTION_DELETE_DEVICES);
                        startService(deleteService);
                        if (mBluetoothLeService != null) {
                            mBluetoothLeService.disconnect();
                            mBluetoothLeService.close();
                        }
                        try {
                            unregisterReceiver(mGattUpdateReceiver);
                            if (isBound)
                                unbindService(mServiceConnection);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        stopService(gattServiceIntent);

                        Intent in = new Intent(DeviceScanAcitivity2.this, DeviceScanActivity.class);
                        in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        in.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(in);
                        //unbindService(mServiceConnection);
                        finish();
                        dialogInterface.cancel();
                    }
                });
                build12.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog ab = build12.create();
                ab.show();
                break;
            case R.id.history1:
                if (!flag3) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    builder.setTitle("List of Devices Added");
                    @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.addedeviceslist, null);
                    ListView listView12 = view.findViewById(R.id.devicesaded);
                    builder.setView(view);

                    DeviceListAdapter listAdapter = new DeviceListAdapter();
                    listView12.setAdapter(listAdapter);
                    listAdapter.notifyDataSetChanged();
                    listView12.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Intent intent = new Intent(DeviceScanAcitivity2.this, History.class);
                            intent.putExtra(DbContract.DEVICES, configuredDevices.get(position));
                            startActivity(intent);
                            finish();
                        }
                    });
                    builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    StringBuilder messageBuilder = new StringBuilder("Wait until the Process Finishes\n and stop the Timer2");
                    if (getCurrentState() != null) {
                        messageBuilder.append("\n");
                        messageBuilder.append("Current state: \n");
                        messageBuilder.append(getCurrentState());
                    }
                    build.setMessage(messageBuilder.toString());
                    build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog a = build.create();
                    a.show();
                }
                break;
            case R.id.configurabletime:
                if (mBluetoothLeService != null && !mBluetoothLeService.isReadingInProgress()) {
                    AlertDialog.Builder bui = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    bui.setTitle("Time");
                    final View dialog = getLayoutInflater().inflate(R.layout.timepopup, null);
                    final EditText na = (EditText) dialog.findViewById(R.id.edittext2);
                    na.setText("" + TimeUnit.MILLISECONDS.toMinutes(value));
                    bui.setView(dialog);
                    bui.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                value = Integer.parseInt(na.getText().toString());
                                if (value > 0) {
                                    value = (value * 60) * 1000;
                                    Log.d(TAG, "onClick: " + value);
                                    SharedPreferences.Editor editor = (SharedPreferences.Editor) getSharedPreferences("Time", MODE_PRIVATE).edit();
                                    editor.putLong("Timevalue", value);
                                    editor.apply();
                                    editor.commit();

                                    Intent intent = new Intent(DeviceScanAcitivity2.this, BluetoothLeService.class);
                                    intent.setAction(BluetoothLeService.ACTION_INCREASED_TIME);
                                    intent.putExtra(BluetoothLeService.EXTRA_INTERVAL, value);
                                    startService(intent);
                                } else {
                                    Toast.makeText(DeviceScanAcitivity2.this, "Invalid Time..", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    bui.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog abc = bui.create();
                    abc.show();
                } else {
                    AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    StringBuilder messageBuilder = new StringBuilder("Wait until the Process Finishes\n and stop the Timer 1");
                    if (getCurrentState() != null) {
                        messageBuilder.append("\n");
                        messageBuilder.append("Current state: \n");
                        messageBuilder.append(getCurrentState());
                    }
                    build.setMessage(messageBuilder.toString());

                    build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog a = build.create();
                    a.show();
                }
                break;
            case R.id.adddevice:
                AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                build.setTitle("Enter Device Information");
                final View dialogview = getLayoutInflater().inflate(R.layout.customlist, null);
                final EditText name = (EditText) dialogview.findViewById(R.id.devicename);
                final EditText address1 = (EditText) dialogview.findViewById(R.id.deviceaddress1);
                final EditText address2 = (EditText) dialogview.findViewById(R.id.deviceaddress2);
                final EditText address3 = (EditText) dialogview.findViewById(R.id.deviceaddress3);
                final EditText address4 = (EditText) dialogview.findViewById(R.id.deviceaddress4);
                final EditText address5 = (EditText) dialogview.findViewById(R.id.deviceaddress5);
                final EditText address6 = (EditText) dialogview.findViewById(R.id.deviceaddress6);
                build.setView(dialogview);
                build.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String nam = name.getText().toString();
                        String add = address1.getText().toString() + ":" + address2.getText().toString() + ":" + address3.getText().toString() + ":" +
                                address4.getText().toString() + ":" + address5.getText().toString() + ":" + address6.getText().toString();
                        Log.d(TAG, "onClick: " + add);
                        Log.d(TAG, "onClick: " + add.length());
                        if (add.length() == 17) {
                            Device device = new Device();
                            device.setName(nam);
                            device.setAddress(add);
                            isAdded = true;
                            Intent addDevice = new Intent(DeviceScanAcitivity2.this, DbService.class);
                            addDevice.setAction(DbContract.ACTION_CREATE_DEVICE);
                            addDevice.putExtra(DbContract.DEVICES, device);
                            startService(addDevice);

                            Intent getDevices = new Intent(DeviceScanAcitivity2.this, DbService.class);
                            getDevices.setAction(DbContract.ACTION_GET_ALL_DEVICES);
                            startService(getDevices);
                            address1.getText().clear();
                            address2.getText().clear();
                            address3.getText().clear();
                            address4.getText().clear();
                            address5.getText().clear();
                            address6.getText().clear();
                            name.getText().clear();
                            dialogInterface.cancel();
                        } else {
                            AlertDialog.Builder build = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                            build.setMessage("You Entered a Wrong Address");
                            build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    address1.getText().clear();
                                    address2.getText().clear();
                                    address3.getText().clear();
                                    address4.getText().clear();
                                    address5.getText().clear();
                                    address6.getText().clear();
                                    name.getText().clear();
                                    dialogInterface.cancel();
                                }
                            });
                            AlertDialog a = build.create();
                            a.show();
                        }
                    }
                });
                build.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog a = build.create();
                a.show();
                break;
            case R.id.stoptimer:
                AlertDialog.Builder bu = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                bu.setMessage(" Are you Sure to Stop Timer ?");
                bu.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent stopTimerIntent = new Intent(DeviceScanAcitivity2.this, BluetoothLeService.class);
                        stopTimerIntent.setAction(BluetoothLeService.ACTION_STOP_TIMER);
                        startService(stopTimerIntent);
                        Toast.makeText(DeviceScanAcitivity2.this, "Timer has been cancelled \n Wait until Remaining Process Finishes", Toast.LENGTH_SHORT).show();
                        dialogInterface.cancel();
                    }

                });
                bu.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog abcd = bu.create();
                abcd.show();
                break;
            case R.id.graphview2:
                if (!flag3) {
                    AlertDialog.Builder builder12 = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    builder12.setTitle("List of Devices Added");
                    @SuppressLint("InflateParams") final View view12 = getLayoutInflater().inflate(R.layout.addedeviceslist, null);
                    ListView listView2 = view12.findViewById(R.id.devicesaded);
                    builder12.setView(view12);
                    DeviceListAdapter listAdapter12 = new DeviceListAdapter();
                    listView2.setAdapter(listAdapter12);
                    listAdapter12.notifyDataSetChanged();
                    listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Intent intent = new Intent(DeviceScanAcitivity2.this, GraphView.class);
                            intent.putExtra(DbContract.DEVICES, configuredDevices.get(position));
                            startActivity(intent);
                        }
                    });
                    builder12.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog alertDialog12 = builder12.create();
                    alertDialog12.show();
                } else {
                    AlertDialog.Builder build11 = new AlertDialog.Builder(DeviceScanAcitivity2.this);
                    StringBuilder messageBuilder = new StringBuilder("Wait until the Process Finishes\n and stop the Timer 3");
                    if (getCurrentState() != null) {
                        messageBuilder.append("\n");
                        messageBuilder.append("Current state: \n");
                        messageBuilder.append(getCurrentState());
                    }
                    build11.setMessage(messageBuilder.toString());
                    build11.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog a11 = build11.create();
                    a11.show();
                }
                break;
        }
        return true;
    }

    public boolean bluetooth() {

        return mBluetoothAdapter.isEnabled();
    }

    private Handler mBleServiceStartHandler = new Handler();
    private Runnable mBleServiceStartRunnable = new Runnable() {
        @Override
        public void run() {
            startBleService();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //wakeLock.acquire();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        //   registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Initializes list view adapter.
        if (configuredDevices.size() != 0) {
            configuredDevices.clear();
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (flag1 && locationcheck() && bluetooth()) {
            isRegistered = true;

            Intent dbService = new Intent(this, DbService.class);
            dbService.setAction(DbContract.ACTION_GET_ALL_DEVICES);
            startService(dbService);
        }
    }

    private void startBleService() {
        gattServiceIntent = new Intent(DeviceScanAcitivity2.this, BluetoothLeService.class);
        isBound = bindService(gattServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);
        gattServiceIntent.setAction(BluetoothLeService.ACTION_READ_DEVICES);
        gattServiceIntent.putExtra(BluetoothLeService.EXTRA_INTERVAL, value);
        startService(gattServiceIntent);
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
        //scanLeDevice(false);
        if (toast != null) {
            Log.d(TAG, "onPause: 123");
            toast.cancel();
        }

        try {
            unregisterReceiver(mGattUpdateReceiver);
            if (isBound)
                unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBleServiceStartHandler.removeCallbacks(mBleServiceStartRunnable);
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        //TextView connection;
        //ProgressBar progressBar;
    }

    private class DeviceListAdapter extends BaseAdapter {
        int id;
        private LayoutInflater mInflator;
        private String[] gName = new String[100];
        private String[] gAdd = new String[100];
        // BluetoothDevice dev;

        public DeviceListAdapter() {
            mInflator = DeviceScanAcitivity2.this.getLayoutInflater();
        }

        public Device getDevice(int position) {
            return configuredDevices.get(position);
        }

        public void clear() {
            configuredDevices.clear();
        }

        @Override
        public int getCount() {
            return configuredDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return configuredDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewh.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewh.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewh);
            } else {
                viewh = (ViewHolder) view.getTag();
            }
            viewh.deviceName.setText(configuredDevices.get(i).getName());
            viewh.deviceAddress.setText(configuredDevices.get(i).getAddress());
            Log.d(TAG, "getView: " + viewh.deviceName.getText().toString());
            return view;
        }
    }

    // Adapter for holding devices found through scanning.
    public class LeDeviceListAdapter extends BaseAdapter {
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_SEPARATOR = 1;
        private static final int TYPE_MAX_COUNT = TYPE_SEPARATOR + 1;
        ArrayList<Device> arrayList;
        //private ArrayList<String> mLeDevices;
        private LayoutInflater mInflator;

        // BluetoothDevice dev;
        public LeDeviceListAdapter(ArrayList<Device> arrayList) {
            // mLeDevices = new ArrayList<String>();
            this.arrayList = arrayList;
            mInflator = DeviceScanAcitivity2.this.getLayoutInflater();
            int[] progress;
        }

        //        public void disconnected(){
//            if (k<=length){
//            viewh.connection.setText(configuredDevices.get(k).getDisconnected());
//            k++;}
//        }
        public void deletetext(int index) {
            View v = listView.getChildAt(index);
            if (v == null) {
                return;
            }
            ;
            // TextView textView = v.findViewById(R.id.pbHeaderProgress);
            // textView.setText("DisConnected");
        }

        public void updatetext(int index) {
            View v = listView.getChildAt(index);
            if (v == null) {
                return;
            }
            ;
            if (listView.getFirstVisiblePosition() == 0) {
                //  TextView textView = v.findViewById(R.id.pbHeaderProgress);
                //textView.setText("Connected");}else {
                //  TextView textView = v.findViewById(R.id.pbHeaderProgress);
                //textView.setText("DisConnected");
            }
        }

        //        public void connected(){
//            if (j<=length){
//                Log.d("position", "connected: "+j+" "+length);
//            viewh.connection.setText(configuredDevices.get(j).getConnected());
//            j++;
//            notifyDataSetChanged();
//            }
//        }
        public void clear() {
            arrayList.clear();
        }

        @Override
        public int getCount() {
            return arrayList.size();
        }

        @Override
        public int getViewTypeCount() {
            return super.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }

        @Override
        public Object getItem(int i) {
            return arrayList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewh.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewh.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewh);
            } else {
                viewh = (ViewHolder) view.getTag();
            }
            length = i;
            final String deviceName = arrayList.get(i).getName();
            names.add(i);
            if (deviceName != null && deviceName.length() > 0) {
                viewh.deviceName.setText(deviceName);
                viewh.deviceAddress.setText(arrayList.get(i).getAddress());
                int position = getItemViewType(i);
                Log.d("postion", "getView:123 " + position);
//                if (flag2){
//                    flag2=false;
//                    Log.d("postion", "getView: "+position);
//                    if (k<=i){
//                    viewh.connection.setText(configuredDevices.get(i).getConnected());
//                    notifyDataSetChanged();
//                    view.invalidate();
//                    }else
//                    {
//                        Log.d("position", "getView:2 ");
//                        viewh.connection.setText(configuredDevices.get(i).getDisconnected());
//                    }
//                }
            } else {
                viewh.deviceName.setText(R.string.unknown_device);
                viewh.deviceAddress.setText(arrayList.get(i).getAddress());
            }
            return view;
        }
    }
}
