package com.tech.embdes.embdesdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.constraint.Group;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.tech.embdes.embdesdemo.data.Analysis;
import com.tech.embdes.embdesdemo.data.Constants;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.DbService;
import com.tech.embdes.embdesdemo.data.Device;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static com.tech.embdes.embdesdemo.BluetoothLeService.EXTRA_INTERVAL;
import static com.tech.embdes.embdesdemo.BluetoothLeService.UUID_HEART_RATE_MEASUREMENT;

public class Values extends Activity {
    public final static UUID UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_DEVICE_INFORMATION_SERVICE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_ENABLE_BT = 1;
    public static boolean active = false;
    static boolean opp;
    private static BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            check1 = true;

            ArrayList<Device> device = new ArrayList<>();
            device.add(mDevice);
            mBluetoothLeService.addDevices(device);
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
//            }
////           //  Automatically connects to the device upon successful start-up initialization.
//            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // mBluetoothLeService = null;
        }
    };
    TextView xAxis, yAxis, zAxis, heartRate;
    String mDeviceAddress;
    ArrayList<Ble_Pojo> arrayList;
    TextView adIntervalVal, wakeTimeVal;
    TextView spo2, temp;
    TextView powerTransVal;
    Button set;
    BluetoothAdapter mBluetoothAdapter;
    String[] arrey;
    String flag;
    boolean flag1;
    boolean flag2;
    ProgressBar progressBar;
    boolean flag3;
    String a, b, c, d;
    ArrayList<Values_pojo> arraylist;
    String name;
    Values_pojo values_pojo;
    PrintWriter pw = null;
    Calendar calender;
    Date d1;
    int advertise = 0;
    int data = 0;
    SeekBar advIntervalSeek, wakeTimeSeek, powerTransSeek;
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    com.jjoe64.graphview.GraphView graphView;
    BluetoothGattCharacteristic mNotifyCharacteristic;
    ArrayList<BluetoothGattCharacteristic> mcharcterstics = new ArrayList<BluetoothGattCharacteristic>();
    ArrayList<String> servicelist;
    Handler mHandler = new Handler();
    Runnable mtimer;
    boolean check, check1;
    boolean check2 = true;
    private Group mAccelerometerGroup, mAnalyticsGroup, mGraphGroup, mConfigGroup;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                System.out.println("Connected");
                mAnalyticsGroup.setVisibility(View.VISIBLE);

                progressBar.setVisibility(View.INVISIBLE);
                mConfigGroup.setVisibility(View.GONE);
                mAccelerometerGroup.setVisibility(View.GONE);
                graphView.setVisibility(View.INVISIBLE);

                Toast.makeText(Values.this, "Connected", 300).show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                System.out.println("Disconnected");
                progressBar.setVisibility(View.VISIBLE);

                mAnalyticsGroup.setVisibility(View.GONE);
                mConfigGroup.setVisibility(View.GONE);
                mAccelerometerGroup.setVisibility(View.GONE);
                graphView.setVisibility(View.GONE);

                Toast.makeText(Values.this, "Disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                System.out.println("Discovered");
                check = true;
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                /*try {
                    main();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                data();*/
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                System.out.println("History");
                flag = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (flag != null) {
                    switch (flag) {
                        case "209":
                            Log.d("CDheck", "onReceive: ");
                            displayconfigure(intent.getStringExtra("G"), (intent.getStringExtra("H")));
                            break;
                        case "12":
                            Toast.makeText(Values.this, "No Data Avaliable", Toast.LENGTH_SHORT).show();
                            break;
                        case "11":
                            // restrict to save to db..
                            break;
                        case "161":
                            Toast.makeText(Values.this, "Time Updated", Toast.LENGTH_SHORT).show();
                            break;
                        case "1001":
                            displayallvalues((Analysis) intent.getSerializableExtra(DbContract.ANALYSIS));
                            break;
                        case "1000":
//                            displayallvalues((Analysis) intent.getSerializableExtra(DbContract.ANALYSIS));
                            break;
                        default:
                            check2 = false;
                            Log.d(TAG, "onReceive: ");
                            break;
                    }
                }
            } else if (DbContract.ACTION_EXPORT_DB.equals(action)) {
                String path = intent.hasExtra(Constants.FILE_PATH) ? intent.getStringExtra(Constants.FILE_PATH) : null;
                String fileName = intent.hasExtra(Constants.FILE_NAME) ? intent.getStringExtra(Constants.FILE_NAME) : null;
                if (path != null && fileName != null) {
                    File file = new File(path, fileName);
                    Uri uri = Uri.fromFile(file);
                    Intent pdfOpenintent = new Intent(Intent.ACTION_VIEW);
                    pdfOpenintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    pdfOpenintent.setDataAndType(uri, "text/xlsx");
                    try {
                        startActivity(pdfOpenintent);
                    } catch (ActivityNotFoundException ex) {
                        Log.d("Check", "onOptionsItemSelected: " + ex.getMessage());
                    }
                    Toast.makeText(Values.this, "Exported..", Toast.LENGTH_SHORT).show();
                }
            } else if (DbContract.ACTION_NO_DATA_TO_EXPORT.equals(action)) {
                Toast.makeText(Values.this, "No Data Available..", Toast.LENGTH_SHORT).show();
            }
        }
    };
    LineGraphSeries series, series1, series2;
    double graphxvalue = 2d;
    int advertisement = 0;
    int dataconfigureblevalue = 0;
    int powertrasmissson = 0;
    PowerManager.WakeLock wakeLock;
    String mName;
    private Intent gattServiceIntent;
    private boolean isBound;
    private Device mDevice;
    private long value;
    private SeekBar activeTimeSeek;
    private TextView activeTime;
    private int activeTimeVal;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(DbContract.ACTION_EXPORT_DB);
        intentFilter.addAction(DbContract.ACTION_NO_DATA_TO_EXPORT);
        intentFilter.addAction(DbContract.ACTION_EXPORT_ALL_LOGS);
        return intentFilter;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void displayallvalues(Analysis analysis) {
        heartRate.setText(analysis.getHeartRate());
        spo2.setText(analysis.getSpo2());
        temp.setText(analysis.getTemperature());
        xAxis.setText(analysis.getxAxis());
        yAxis.setText(analysis.getyAxis());
        zAxis.setText(analysis.getzAxis());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.values_activity);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyWakelockTag");

        mAccelerometerGroup = findViewById(R.id.accelerometer);
        mAnalyticsGroup = findViewById(R.id.analytics);
        mConfigGroup = findViewById(R.id.config);

        xAxis = findViewById(R.id.x_axis);
        yAxis = findViewById(R.id.y_axis);
        zAxis = findViewById(R.id.z_axis);
        heartRate = findViewById(R.id.heart_rate);
        advIntervalSeek = findViewById(R.id.ad_interval_seek);
        wakeTimeSeek = findViewById(R.id.wake_time_seek);
        powerTransSeek = findViewById(R.id.power_trans_seek);
        activeTimeSeek = findViewById(R.id.active_time_seek);
        activeTime = findViewById(R.id.active_time);
        adIntervalVal = findViewById(R.id.ad_interval);
        wakeTimeVal = findViewById(R.id.wake_time);
        set = findViewById(R.id.config_submit);
        spo2 = findViewById(R.id.spo2);
        temp = findViewById(R.id.temperature);
        powerTransVal = findViewById(R.id.power_trans);
        progressBar = findViewById(R.id.progressBar);
        servicelist = new ArrayList<>();
        arrayList = new ArrayList<Ble_Pojo>();
        graphView = (com.jjoe64.graphview.GraphView) findViewById(R.id.graphview);
        arraylist = new ArrayList<Values_pojo>();
        values_pojo = new Values_pojo();
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
        if (preferences != null)
            value = preferences.getLong("Timevalue", 5 * 60 * 1000);
        arrey = new String[3];
        series = new LineGraphSeries();
        series1 = new LineGraphSeries();
        series2 = new LineGraphSeries();

        //
        series1.setColor(Color.BLUE);
        series1.setDrawDataPoints(true);
        series1.setDataPointsRadius(4);
        series1.setThickness(2);
        series1.setTitle("Spo2");
        //
        series2.setColor(Color.GRAY);
        series2.setDrawDataPoints(true);
        series2.setDataPointsRadius(4);
        series2.setThickness(2);
        series2.setTitle("Temperature");

        series.setColor(Color.CYAN);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(4);
        series.setThickness(2);
        series.setTitle("Heart_Rate");
        //
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMaxY(150);

        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(4);
        graphView.getViewport().setMaxX(80);
        //
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScalableY(true);
        graphView.getLegendRenderer().setVisible(true);
        graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        //

        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(100);
        graphView.addSeries(series);
//        graphView.addSeries(series1);
//        graphView.addSeries(series2);
//        graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
//            @Override
//            public String formatLabel(double value, boolean isValueX) {
//                if (isValueX){
//                    return sdf.format(new Date((long) value));
//                }
//                return super.formatLabel(value, isValueX);
//            }
//        });

        graphView.getGridLabelRenderer().setHumanRounding(false);
        graphView.getGridLabelRenderer().setNumVerticalLabels(14);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int waketimevalue = Integer.parseInt(wakeTimeVal.getText().toString());
                int advertisementtime = Integer.parseInt(adIntervalVal.getText().toString());
                int power = Integer.parseInt(powerTransVal.getText().toString());
                int activeTimeValue = Integer.parseInt(activeTime.getText().toString());
                Log.d(TAG, "onClick: read" + waketimevalue + "advIntervalSeek" + advertisementtime);
                Log.d("check", "onClick: ");
                byte[] data = new byte[7];
                data[0] = (byte) (0xA4);
                Log.d("Check", "onClick: " + data[0]);
                data[1] = 0x06;
                Log.d(TAG, "onClick: " + data[1]);
                data[2] = (byte) (advertisementtime >> 8);
                Log.d("Check", "onClick: " + data[2]);
                data[3] = (byte) advertisementtime;
                Log.d("Check", "onClick: " + data[3]);
                data[4] = (byte) waketimevalue;
                Log.d(TAG, "onClick:" + data[4]);
                data[5] = (byte) activeTimeValue;
                /*data[5] = (byte) power;
                Log.d(TAG, "onClick: " + data[5]);*/
                data[6] = (byte) 0xEE;
                Log.d(TAG, "onClick: " + data[6]);
                mBluetoothLeService.writeToHeartRateCharacteristic(data);
                data();
                Toast.makeText(Values.this, "Advertisement and Data Interval Updated", Toast.LENGTH_SHORT).show();
                Toast.makeText(Values.this, "Advertisement and Data Interval Updated", Toast.LENGTH_SHORT).show();

            }
        });
        powerTransSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                i = i - 40;
                powertrasmissson = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                powerTransVal.setText("" + powertrasmissson);
            }
        });
        advIntervalSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                i = i + 100;
                advertisement = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                adIntervalVal.setText("" + advertisement);
            }
        });
        activeTimeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                activeTimeVal = i + 5;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                activeTime.setText(""+activeTimeVal);
            }
        });
        wakeTimeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                i = i + 1;
                dataconfigureblevalue = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                wakeTimeVal.setText("" + dataconfigureblevalue);
            }
        });
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        mcharcterstics = new ArrayList<BluetoothGattCharacteristic>();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            if (gattService.getUuid().equals(UUID_HEART_RATE_SERVICE)) {
                mcharcterstics.add(gattService.getCharacteristic(UUID_HEART_RATE_MEASUREMENT));
                uuid = gattService.getCharacteristic(UUID_HEART_RATE_MEASUREMENT).getUuid().toString();
                servicelist.add(SampleGattAttributes.lookup(uuid));
            }
//            else if (gattService.getUuid().equals(UUID_DEVICE_INFORMATION_SERVICE)){
//                mcharcterstics.add(gattService.getCharacteristic(UUID_DEVICE_INFORMATION));
//                uuid  = gattService.getCharacteristic(UUID_DEVICE_INFORMATION).getUuid().toString();
//                servicelist.add(SampleGattAttributes.lookup(uuid));
//            }
        }

    }
//    public void displayHeartrate(String data) {
//        if (data != null) {
//            heartRate.setText(data);
//            values_pojo.setH1(data);
//
//        }
//    }
//    public void dispalySpo2(String data) {
//        if (data != null) {
//            heartRate.setText(data);
//            values_pojo.setS1(data);
//        }
//    }
//    public void displaytemperature(String data) {
//        if (data != null) {
//            heartRate.setText(data);
//            values_pojo.setT1(data);
//        }
//    }

    private void displayconfigure(String adv, String read) {
        this.advertise = Integer.parseInt(adv);
        this.data = Integer.parseInt(read);
        Toast.makeText(this, "Current Advertiment Interval = " + advertise + "\n" + "Current Read Time Interval = " + data, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "displayconfigure: " + advertise + " " + data);
    }

    private void displayallvalues(String stringExtra, String b, String a, String c, String d5, String d6, String d7) {
        //myDb.Heart_Rate(c,stringExtra,b,a,d5,d6,d7);
        this.a = stringExtra;
        this.b = b;
        this.c = a;
        heartRate.setText(stringExtra);
        spo2.setText(b);
        temp.setText(a);
        xAxis.setText(d5);
        yAxis.setText(d6);
        zAxis.setText(d7);
        Log.d(TAG, "displayallvalues: " + name);
        Log.d(TAG, "displayallvalues: " + mDeviceAddress);
//        if (flag1){
//            Log.d("check", "displayallvalues: 12"+flag1);
//
//        }else if (flag2){
//            Log.d("check", "displayallvalues:2 "+flag2);
//            heartRate.setText(b);
//        }else if (flag3){
//            Log.d("check", "displayallvalues:3 "+flag3);
//            heartRate.setText(a);
//        }

    }

    private void displayAccelrometer(String data, String data2, String data3) {
        if (data != null) {
            xAxis.setText(data);
            yAxis.setText(data2);
            zAxis.setText(data3);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //wakeLock.acquire();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        Bundle bundle = getIntent().getExtras();
        mDevice = (Device) bundle.getSerializable(DbContract.DEVICES);
        if (mDevice != null) {
            name = mDevice.getName();
            mDeviceAddress = mDevice.getAddress();
            getActionBar().setTitle(name);
        }
//        Cursor c = database.query("Bluetooth",null,null,null,null,null,null);
//        if(c.getCount()>0) {
//            while (c.moveToNext()) {
//                Ble_Pojo ble_pojo = new Ble_Pojo();
//                mDeviceAddress = c.getString(c.getColumnIndex("address"));
//                mName = c.getString(c.getColumnIndex("name"));
//                ble_pojo.setAddress(mDeviceAddress);
//                ble_pojo.setName(mName);
//                configuredDevices.add(ble_pojo);
////               mLeDeviceListAdapter.addDevice(mDeviceAddress);
////               mLeDeviceListAdapter.notifyDataSetChanged();
//            }
//        }
        gattServiceIntent = new Intent(Values.this, BluetoothLeService.class);
        isBound = bindService(gattServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);
        gattServiceIntent.setAction(BluetoothLeService.ACTION_READ_DEVICE);
        gattServiceIntent.putExtra(BluetoothLeService.EXTRA_ADRESS, mDevice);
        gattServiceIntent.putExtra( EXTRA_INTERVAL, value);
        startService(gattServiceIntent);
    }

    private double getTemp() {
        String data = "0";
        if (c != null) {
            data = c;
        }
        return Double.parseDouble(data);
    }

    private double getSpo2() {
        String data = "0";
        if (b != null) {
            data = b;
        }
        return Double.parseDouble(data);
    }

    private double getHeart() {
        String data = "0";
        if (a != null) {
            data = a;
        }
        return Double.parseDouble(data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.menu_sync).setVisible(false);
        menu.findItem(R.id.accel).setVisible(true);
        menu.findItem(R.id.heartrate).setVisible(true);
        // menu.findItem(R.id.menuspo2).setVisible(true);
        //menu.findItem(R.id.temmperature).setVisible(true);
        menu.findItem(R.id.history1).setVisible(true);
        menu.findItem(R.id.log).setVisible(true);
        menu.findItem(R.id.menu_stop).setVisible(false);
        menu.findItem(R.id.menu_scan).setVisible(false);
        menu.findItem(R.id.graph123).setVisible(true);
        menu.findItem(R.id.rename).setVisible(true);
        menu.findItem(R.id.configure).setVisible(true);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.history1:
                Intent i = new Intent(Values.this, History.class);
                i.putExtra(DbContract.DEVICES, mDevice);
                startActivity(i);
                finish();
                break;
            case R.id.accel:
                mAccelerometerGroup.setVisibility(View.VISIBLE);
                mAnalyticsGroup.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                mConfigGroup.setVisibility(View.GONE);
                graphView.setVisibility(View.GONE);
                break;
            case R.id.heartrate:
                mAccelerometerGroup.setVisibility(View.GONE);
                mAnalyticsGroup.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                mConfigGroup.setVisibility(View.GONE);
                graphView.setVisibility(View.GONE);
//                mBluetoothLeService.initializeRead();
                break;
            case R.id.menuspo2:
                Log.d("Check", "onOptionsItemSelected:b ");
                flag1 = false;
                flag3 = false;
                flag2 = true;
                mAccelerometerGroup.setVisibility(View.GONE);
                mAnalyticsGroup.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                mConfigGroup.setVisibility(View.GONE);
                graphView.setVisibility(View.GONE);
                break;
            case R.id.temmperature:
                Log.d("Check", "onOptionsItemSelected:a ");
                flag3 = true;
                flag1 = false;
                flag2 = false;
                mAccelerometerGroup.setVisibility(View.GONE);
                mAnalyticsGroup.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                mConfigGroup.setVisibility(View.GONE);
                graphView.setVisibility(View.VISIBLE);
                break;
            case R.id.log:
//                if (mcharcterstics != null) {
//                    for (int i =0; i<mcharcterstics.size();i++){
//                        final BluetoothGattCharacteristic characteristic =
//                                mcharcterstics.get(i);
//                        final int charaProp = characteristic.getProperties();
////                        if (((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) |
////                                (charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
////                            Log.d("chh", "onClick: "+characteristic.getUuid());
////                            mBluetoothLeService.writeToHeartRateCharacteristic();
////                            //mBluetoothLeService.writeToHeartRateCharacteristic(characteristic.getValue());
////                        }
//
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            // If there is an active notification on a characteristic, clear
//                            // it first so it doesn't update the data field on the user interface.
//                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
//                                mNotifyCharacteristic = null;
//                            }
//                            mBluetoothLeService.readCharacteristic(characteristic);
//                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                            mNotifyCharacteristic = characteristic;
//                            mBluetoothLeService.setCharacteristicNotification(
//                                    characteristic, true);
//                        }
//                    }
//                }
                exportDb();
                break;
            case R.id.graph123:
                flag2 = false;
                flag3 = false;
                flag1 = true;
                mAccelerometerGroup.setVisibility(View.GONE);
                mAnalyticsGroup.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                mConfigGroup.setVisibility(View.GONE);
                graphView.setVisibility(View.VISIBLE);
                break;
            case R.id.configure:
                byte[] request = new byte[4];
                request[0] = (byte) 0xA2;
                request[1] = 0x01;
                request[2] = 0x0B;
                request[3] = (byte) 0xEE;
                if (check) {
                    Log.d("Check", "onOptionsItemSelected: ");
                    mBluetoothLeService.writeToHeartRateCharacteristic(request);
                    data();
                } else {
                    Toast.makeText(this, "Device not connected", Toast.LENGTH_SHORT).show();
                }
                mAccelerometerGroup.setVisibility(View.GONE);
                mAnalyticsGroup.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                mConfigGroup.setVisibility(View.VISIBLE);
                graphView.setVisibility(View.GONE);
                break;
            case R.id.rename:
                AlertDialog.Builder bui = new AlertDialog.Builder(Values.this);
                bui.setTitle("Time");
                final View dialog = getLayoutInflater().inflate(R.layout.timepopup, null);
                final EditText na = (EditText) dialog.findViewById(R.id.edittext2);
                na.setHint("Enter Device Name");
                bui.setView(dialog);
                bui.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = na.getText().toString();
                        if (!name.isEmpty()) {
                            String ref = 'E' + name;
                            mBluetoothLeService.writeToHeartRateCharacteristic(ref);
                            if (mDevice != null) {
                                Intent renameIntent = new Intent(Values.this, DbService.class);
                                renameIntent.setAction(DbContract.ACTION_UPDATE_DEVICE);
                                renameIntent.putExtra(DbContract.DeviceEntry._ID, mDevice.getId());
                                renameIntent.putExtra(DbContract.DeviceEntry.COLUMN_NAME, name);
                                startService(renameIntent);
                                getActionBar().setTitle(name);
                            }
                        } else {
                            Toast.makeText(Values.this, "Invalid Name!", Toast.LENGTH_SHORT).show();
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

        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 7:
                if (resultCode == RESULT_OK) {
                    Uri uri = null;
                    if (data != null) {
                        uri = data.getData();
                        Log.i("Check", "Uri: " + uri.toString());
                    }
                    break;
                }

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //scanLeDevice(false);
//        mBluetoothLeService.disconnect(mDeviceAddress);
//        stopService(gattServiceIntent);
        mHandler.removeCallbacks(mtimer);
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
        if (isBound)
            unbindService(mServiceConnection);
        stopService(gattServiceIntent);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //  mBluetoothLeService.close();
        opp = true;
        DeviceScanAcitivity2.click = true;
        Intent i = new Intent(Values.this, DeviceScanAcitivity2.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    public void write() {
        if (mcharcterstics != null) {
            Log.d(TAG, "data: " + mcharcterstics.size());
            for (int i = 0; i < mcharcterstics.size(); i++) {
                Log.d(TAG, "data: ");
                final BluetoothGattCharacteristic characteristic =
                        mcharcterstics.get(i);
                final int charaProp = characteristic.getProperties();
                if (((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) |
                        (charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                    Log.d("chh", "onClick: " + characteristic.getUuid());
                    mBluetoothLeService.initializeRead();
                }
            }
        }
    }

    public void data() {
        if (mcharcterstics != null) {
            Log.d(TAG, "data: " + mcharcterstics.size());
            for (int i = 0; i < mcharcterstics.size(); i++) {
                Log.d(TAG, "data: ");
                final BluetoothGattCharacteristic characteristic =
                        mcharcterstics.get(i);
                final int charaProp = characteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        mNotifyCharacteristic = null;
                    }
                    mBluetoothLeService.readCharacteristic(characteristic);
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifyCharacteristic = characteristic;
                    mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                }
                if (((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) |
                        (charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                    Log.d("chh", "onClick: " + characteristic.getUuid());
                    mBluetoothLeService.initializeRead();
                }
            }
        }
    }

    private void exportDb() {
        Toast.makeText(this, "Exporting database..", Toast.LENGTH_SHORT).show();
        Intent exportIntent = new Intent(this, DbService.class);
        exportIntent.setAction(DbContract.ACTION_EXPORT_DB);
        exportIntent.putExtra(DbContract.AnalysisEntry.COLUMN_DEVICE, mDevice != null ? mDevice.getId() : 0);
        startService(exportIntent);
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
