package com.tech.embdes.embdesdemo;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.tech.embdes.embdesdemo.data.Analysis;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.DbService;
import com.tech.embdes.embdesdemo.data.Device;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static com.tech.embdes.embdesdemo.GraphView.hexStringToByteArray;
import static com.tech.embdes.embdesdemo.SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLeService extends android.app.Service {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static UUID UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_BATTERY_LEVEL =
            UUID.fromString(SampleGattAttributes.BATTERY_LEVEL);
    public final static UUID UUID_DEVICE_INFORMATION = UUID.fromString(SampleGattAttributes.DEVICE_NAME);
    public static final String ACTION = "action";
    public static final String ACTION_READ_DEVICES = "ACTION_READ_DEVICES";
    public static final String ACTION_READ_DEVICE = "ACTION_READ_DEVICE";
    public static final String ACTION_UPDATED_DEVICES = "ACTION_UPDATED_DEVICES";
    public static final String ACTION_INCREASED_TIME = "ACTION_INCREASED_TIME";
    public static final String ACTION_RERUN_DEVICE = "ACTION_RERUN_DEVICE";
    public static final String ACTION_RERUN = "ACTION_RERUN";
    public static final String ACTION_STOP_TIMER = "ACTION_STOP_TIMER";
    public static final String ACTION_DISCONNECT = "ACTION_DISCONNECT";
    public static final String EXTRA_INTERVAL = "EXTRA_INTERVAL";
    public static final String EXTRA_DATA_AVAILABLE = "EXTRA_DATA_AVAILABLE";
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public static final int STATE_IDLE = 0;
    public static final int STATE_DISCONNECTED = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_ENABLE_NOTIFICATION = 4;
    public static final int STATE_SYNC_TIME = 5;
    public static final int STATE_READ_REQUESTED = 6;
    public static final int STATE_PACKETS_COUNTED = 7;
    public static final int STATE_READING_IN_PROGRESS = 8;
    public static final int STATE_READ = 9;
    private static final String EXTRA_DISCONNECT_INDEX = "EXTRA_DISCONNECT_INDEX";
    public static final String ANALYSIS_DATA = "1000";
    public static final String ANALYSIS_LIVE_DATA = "1001";
    public static boolean check;
    public static BluetoothLeService Service;
    public static boolean
            endpacket;
    public static String EXTRA_ADRESS =
            "com.example.bluetooth.le.EXTRA_ADRESS";
    public static String EXTRA_NAME =
            "com.example.bluetooth.le.EXTRA_NAME";
    static String Adress;
    static String CurrentDeviceAdress;
    static String Name;
    static Boolean isUserDisconnected = false;
    static Boolean Retry = false;
    private final Handler mHandler;
    private final IBinder mBinder = new LocalBinder();
    public boolean isrunning = false;
    public Handler mDisconnectHandler = new Handler();
    BluetoothDevice gattdevice;
    BluetoothDevice device;
    int spo2, temp;
    PrintWriter pw = null;
    BluetoothGatt mBluetoothGatt;
    //Service UUID
    BluetoothGattCharacteristic mLedCharacteristic, mButtonChar;
    PendingIntent repeatPendingIntent;
    AlarmManager alarmManager;
    private boolean mOperationInProgress = true; // Initially true to block operations before services are discovered.
    private boolean mInitInProgress;
    private int mConnectionState = STATE_IDLE;
    private ArrayList<BluetoothGattCharacteristic> mCharacteristics = new ArrayList<>();
    private boolean isReadAll;
    private ArrayList<BluetoothGattCharacteristic> mCharacteristicsInQueue = new ArrayList<>();
    private boolean mIsReadInProgress;
    private int mActiveDeviceIndex;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    //00001523-1212-efde-1523-785feabcd123
//    public final static UUID LED_Button_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");//service
//    public final static UUID Button_Charecterstic = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");//read
//    public final static UUID LED_Charecterstic = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");//write
//    public final static UUID LINKLOSS_SERVICE_UUID = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    private String mBluetoothDeviceAddress;
    private ArrayList<Device> devices = new ArrayList<>();
    private ArrayList<Analysis> analyses = new ArrayList<>();
    //    private void getAdd() {
//        MyDb myDb = new MyDb(getApplicationContext());
//        myDb.open();
//        Log.d(TAG, "getAdd: "+add);
//        SQLiteDatabase database = myDb.myHelper.getReadableDatabase();
//        Cursor c = database.query("Bluetooth",null,null,null,null,null,null);
//        if(c.getCount()>0) {
//            if (c.moveToNext()) {
//                add = c.getString(c.getColumnIndex("address"));
//                Log.d(TAG, "getAdd: "+add);
//               // mName = c.getString(c.getColumnIndex("name"));
////               mLeDeviceListAdapter.addDevice(mDeviceAddress);
////               mLeDeviceListAdapter.notifyDataSetChanged();
//            }
//        }
//    }
    private int availablePacketsToRead = 0;
    private int mPacketReceived = 0;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//        	 String intentAction;
//			  gattdevice = gatt.getDevice();
//			  Adress = gattdevice.getAddress();
//			  Name = gattdevice.getName();
//			if (newState == BluetoothProfile.STATE_CONNECTED) {
//				intentAction = ACTION_GATT_CONNECTED;
//				broadcastUpdate(intentAction, Adress, Name);
//				mBluetoothGatt.discoverServices();
//			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//
//
//				System.out.println("user disconnected");
//			    intentAction = ACTION_GATT_DISCONNECTED;
//			    //mConnectionState = STATE_DISCONNECTED;
//				broadcastUpdate(intentAction, Adress,  Name);
//				mBluetoothGatt.disconnect();
//				mBluetoothGatt.close();
//				mBluetoothGatt = null;
////				if(isUserDisconnected){
////				    System.out.println("user disconnected");
////				    intentAction = ACTION_GATT_DISCONNECTED;
////					broadcastUpdate(intentAction, Adress,  Name);
////					mBluetoothGatt.close();
////					mBluetoothGatt = null;  //clear characteristics & mBluetoothGatt = null at disconnect & unbindservice & bindservice in onresume
////
////				}
////				else
////				{
////					 intentAction = ACTION_GATT_DISCONNECTED;
////					broadcastUpdate(intentAction, Adress,  Name);
////				 }
//
//			}
            String intentAction;
            gattdevice = gatt.getDevice();
            Adress = gattdevice.getAddress();
            Name = gattdevice.getName();
            Log.d(TAG, "onConnectionStateChange: " + gattdevice.getAddress());
            System.out.println("Connecting" + Name);
            System.out.println("Connecting" + Adress);
            System.out.println("Connecting" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                System.out.println("Current mode: user connected");
                if (disconnectingPendingIntent != null)
                    alarmManager.cancel(disconnectingPendingIntent);
                intentAction = ACTION_GATT_CONNECTED;
                if (mConnectionState == STATE_CONNECTING) {
                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(intentAction, Adress, Name);
                    Log.e(TAG, "broadcast connected");
                    final boolean bonded = gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED;
                    final int delay = bonded ? 1600 : 0; // around 1600 ms is required when connection interval is ~45ms.
                    if (delay > 0)
                        Log.d("ERROR", "wait(" + delay + ")");
                    mHandler.postDelayed(() -> {
                        // Some proximity tags (e.g. nRF PROXIMITY) initialize bonding automatically when connected.
                        if (gattdevice.getBondState() != BluetoothDevice.BOND_BONDING) {
                            Log.d("Error", "Discovering Services...");
                            Log.d("ERROR", "gatt.discoverServices()");
                            if (mBluetoothGatt == null) {
                                return;
                            }
                            mBluetoothGatt.discoverServices();
                        }
                    }, delay);
                }

                /*if (isReadingInProgress())
                    disconnectIfTimeOut(mActiveDeviceIndex);*/
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //if(isUserDisconnected){
                System.out.println("Current mode: user disconnected");
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
                //isrunning =false;
//					mBluetoothGatt.disconnect();
//					mBluetoothGatt.close();
//					mBluetoothGatt = null;  //clear characteristics & mBluetoothGatt = null at disconnect & unbindservice & bindservice in onresume
                //}
//				else
//				{
////					 intentAction = ACTION_GATT_DISCONNECTED;
////					 broadcastUpdate(intentAction, Adress,  Name);
//					intentAction = ACTION_GATT_LINKLOSS;
//					broadcastUpdate(intentAction, Adress,  Name);
//				 }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                System.out.println("Current mode: discovered");
                Log.e(TAG, "broadcast onServiceDiscovered before " + mConnectionState);
                if (mConnectionState == STATE_CONNECTED) {
                    mConnectionState = STATE_ENABLE_NOTIFICATION;
                    sendNotification();
                }
                Log.e(TAG, "broadcast onServiceDiscovered after " + mConnectionState);
            } else {
                Log.w(TAG, "onServicesDiscovered: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead: ");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(UUID_HEART_RATE_MEASUREMENT)) {
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d("Check", "Heart rate format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d("Check", "Heart rate format UINT32.");
                }
                int offset0 = characteristic.getIntValue(format, 0);
                Log.v(TAG, "onCharacteristic" + offset0);
                if (offset0 == 1) {
                    // Nothing to do handled in broadcast
                    mPacketReceived++;
                    if (mPacketReceived == availablePacketsToRead) {
                        Log.v(TAG, "onCharacteristicChanged insided read " + mPacketReceived);
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                        mConnectionState = STATE_READ;

                        Intent saveAnalysis = new Intent(BluetoothLeService.this, DbService.class);
                        saveAnalysis.setAction(DbContract.ACTION_INSERT_MULTI_ANALYSIS);
                        saveAnalysis.putExtra(DbContract.ANALYSIS, analyses);
                        startService(saveAnalysis);
                        analyses.clear();

                        disconnectAndReadNext();
                        return;
                    } else {
                        Log.v(TAG, "onCharacteristicChanged insided read else " + mPacketReceived);
                        mConnectionState = STATE_READING_IN_PROGRESS;
                    }
                } else {
                    Log.v(TAG, "onCharacteristicChanged Total packet size " + availablePacketsToRead + " receivedPacket " + mPacketReceived + " isReadAll " + isReadAll);
                    Log.v(TAG, "onCharacteristicChanged Connection State " + mConnectionState);
                    if (mConnectionState == STATE_SYNC_TIME) {
                        // Nothing to do. handled in send broadcast
                    } else if (mConnectionState == STATE_READ_REQUESTED) {
                        Log.e(TAG, "onCharacteristicChanged Packet length received");
                        mPacketReceived = 0;
                    } else if (mConnectionState == STATE_PACKETS_COUNTED || mConnectionState == STATE_READING_IN_PROGRESS) {
                        mPacketReceived++;
                        if (mPacketReceived == availablePacketsToRead) {
                            Log.v(TAG, "onCharacteristicChanged insided read " + mPacketReceived);
                            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                            mConnectionState = STATE_READ;

                            for (Analysis analysis : analyses) {
                                Log.e(TAG, "Analysis Before Save:" + analysis.toString());
                            }
                            Intent saveAnalysis = new Intent(BluetoothLeService.this, DbService.class);
                            saveAnalysis.setAction(DbContract.ACTION_INSERT_MULTI_ANALYSIS);
                            saveAnalysis.putExtra(DbContract.ANALYSIS, analyses);
                            startService(saveAnalysis);
                            analyses.clear();

                            disconnectAndReadNext();
                            return;
                        } else {
                            Log.v(TAG, "onCharacteristicChanged insided read else " + mPacketReceived);
                            mConnectionState = STATE_READING_IN_PROGRESS;
                        }
                    }
                }
                Log.e(TAG, "onCharacteristicChanged " + mConnectionState);
            }
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.getCharacteristic().getUuid().equals(UUID_HEART_RATE_MEASUREMENT)) {
                    if (mConnectionState == STATE_ENABLE_NOTIFICATION) {
                        Log.e(TAG, "broadcast sync time");
                        mConnectionState = STATE_SYNC_TIME;
                        syncTime();
                    }
                }
            }
        }
    };

    private void sendNotification() {
        mConnectionState = STATE_ENABLE_NOTIFICATION;
        setNotificationState(true);
    }

    private Handler mReadDeviceHandler = new Handler();
    private long mIntervalToReadDevices = 0;
    private PendingIntent disconnectingPendingIntent;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    public BluetoothLeService() {
        mHandler = new Handler();
    }

    public void initializeRead() {
        writeToHeartRateCharacteristic(hexStringToByteArray("0D"));
    }

    private void syncTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        long time = cal.getTimeInMillis() / 1000;
        Log.d("Check", "main: " + time);
        byte[] timearray = new byte[7];
        timearray[0] = (byte) 0xA0;
        timearray[1] = (byte) 0x04;
        timearray[2] = (byte) (time >> 24);
        timearray[3] = (byte) (time >> 16);
        timearray[4] = (byte) (time >> 8);
        timearray[5] = (byte) time;
        timearray[6] = (byte) 0xEE;
        writeToHeartRateCharacteristic(timearray);
    }

    private void readCharacteristicInTheQueue(BluetoothGatt gatt) {
        gatt.readCharacteristic(gatt.getService(UUID_HEART_RATE_SERVICE)
                .getCharacteristic(UUID_HEART_RATE_MEASUREMENT));
    }

    public boolean isReadingInProgress() {
        Log.d(TAG, "Current mode:" + mConnectionState);
        return !(mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_IDLE);
    }

    private void disconnectAndReadNext() {
        if (devices != null && devices.size() > 0 && mIsReadInProgress && isReadAll) {
            close();
            mConnectionState = STATE_DISCONNECTED;
            broadcastUpdate(ACTION_GATT_DISCONNECTED);
            mActiveDeviceIndex++;
            waitIdle(100);
            if (mActiveDeviceIndex < devices.size()) {
                connect(devices.get(mActiveDeviceIndex).getAddress());
            } else {
                mIsReadInProgress = false;
            }
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, String adress, String name) {
        final Intent intent = new Intent(action);
        Service = BluetoothLeService.this;
        intent.putExtra(EXTRA_ADRESS, adress);
        intent.putExtra(EXTRA_NAME, name);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, byte[] batt) {
        final Intent intent = new Intent(action);
//		System.out.println("In rrrrrrssssiiii"+ batt);
        byte batyint[] = new byte[2];
        batyint[0] = batt[0];
        batyint[1] = batt[1];
        System.out.println("byte0.. : " + batyint[0] + "..byte1.. : " + batyint[1]);
        intent.putExtra("BATT", batt);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
//        Log.v(TAG, "characteristic.getStringValue(0) = " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
//        intent.putExtra(EXTRA_DATA, String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)));
//        sendBroadcast(intent);
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d("Check", "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d("Check", "Heart rate format UINT32.");
            }
            int type = characteristic.getIntValue(format, 0);
            Log.d(TAG, "broadcastUpdate:123 " + type);
            String hex = Integer.toHexString(type);
            System.out.println("Hex value: " + hex + " isReading in progress:" + (mConnectionState == STATE_READING_IN_PROGRESS));
            if (mConnectionState == STATE_READING_IN_PROGRESS || type == 1) {
                int offset1 = characteristic.getIntValue(format, 1);
                int offset2 = characteristic.getIntValue(format, 2);
                int offset3 = characteristic.getIntValue(format, 3);
                int offset4 = characteristic.getIntValue(format, 4);
                offset1 = (offset1 << 24) | (offset2 << 16) | (offset3 << 8) | offset4;
                Log.d(TAG, "broadcastUpdate: " + offset1);
                long date = Long.parseLong(String.valueOf(offset1)) * 1000;
                Date expiry = new Date(date);
                Log.d(TAG, "broadcastUpdate: " + expiry);
                SimpleDateFormat sp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
//                String date = sp.format(new Date(String.valueOf(expiry)));
                Log.d("Date", "broadcastUpdate:Time " + date);
                int heartRate = characteristic.getIntValue(format, 6);
                Log.d(TAG, "broadcastUpdate:1 " + heartRate);
                int spo2 = characteristic.getIntValue(format, 8);
                Log.d(TAG, "broadcastUpdate:2 " + spo2);
                int temp = characteristic.getIntValue(format, 10);
                Log.d(TAG, "broadcastUpdate:3 " + temp);
                int x1 = characteristic.getIntValue(format, 11);
                Log.d(TAG, "broadcastUpdate: x1" + x1);
                Log.d(TAG, "broadcastUpdate: x1:hex" + x1);
                int x2 = characteristic.getIntValue(format, 12);
                Log.d(TAG, "broadcastUpdate: x2" + x2);
                /*if ((x2 & (1 << 2)) == 1 << 2) {
                    x2 = x2 & (0x03);
                }*/

                float x = convertToGravity(x2);

                //  if ((x2<<16)==(1<<16)){
                //  x2= x2&(0x03ff);
                // }
                Log.d(TAG, "broadcastUpdate:x " + x);
                int y1 = characteristic.getIntValue(format, 13);
                Log.d(TAG, "broadcastUpdate: y1" + y1);
                int y2 = characteristic.getIntValue(format, 14);
                Log.d(TAG, "broadcastUpdate: y2" + y2);
                float y = convertToGravity(y2);
                Log.d(TAG, "broadcastUpdate:y " + y);
                int z1 = characteristic.getIntValue(format, 15);
                Log.d(TAG, "broadcastUpdate: z1 " + z1);
                int z2 = characteristic.getIntValue(format, 16);
                Log.d(TAG, "broadcastUpdate: z2 " + z2);
                float z = convertToGravity(z2);
                Log.d(TAG, "broadcastUpdate: z " + z);

                Analysis analysis = new Analysis();
                analysis.setHeartRate(String.valueOf(heartRate));
                analysis.setSpo2(String.valueOf(spo2));
                analysis.setTemperature(String.valueOf(temp));
                analysis.setTime(date);
                analysis.setxAxis(String.valueOf(x));
                analysis.setyAxis(String.valueOf(y));
                analysis.setzAxis(String.valueOf(z));
//                if (mActiveDeviceIndex < devices.size())
                analysis.setDevice(devices.get(mActiveDeviceIndex).getId());
                Log.e(TAG, "Analysis:OnRead: " + analysis);

                if (type == 1) {
                    intent.putExtra(EXTRA_DATA, ANALYSIS_LIVE_DATA);
                    intent.putExtra(DbContract.ANALYSIS, analysis);
                } else {
                    analyses.add(analysis);
                    intent.putExtra(EXTRA_DATA, ANALYSIS_DATA);
                    intent.putExtra(DbContract.ANALYSIS, analysis);
                }
            } else {
                switch (hex) {
                    case "b": {
                        int offset1 = characteristic.getIntValue(format, 1);
                        int offset2 = characteristic.getIntValue(format, 2);
                        int length = (offset1 << 8) | (offset2);
                        availablePacketsToRead = length / 16;
                        mConnectionState = STATE_PACKETS_COUNTED;
                        intent.putExtra(EXTRA_DATA, String.valueOf(type));
                        intent.putExtra("length", String.valueOf(length / 16));
                    }
                    break;
                    case "c": {
                        mConnectionState = STATE_READ;
                        intent.putExtra(EXTRA_DATA, String.valueOf(type));
                        sendBroadcast(intent);
                        disconnectAndReadNext();
                    }
                    return;
                    case "a1": {
                        intent.putExtra(EXTRA_DATA, String.valueOf(type));
                        sendBroadcast(intent);
                        mConnectionState = STATE_READ_REQUESTED;
                        initializeRead();
                        return;
                    }
                    case "d1": {
                        int offset1 = characteristic.getIntValue(format, 1);
                        int offset2 = characteristic.getIntValue(format, 2);
                        int offset3 = characteristic.getIntValue(format, 3);
                        int adv = offset1 << 8 | offset2;
                        int read = characteristic.getIntValue(format, 4);
                        int readtime = offset3 << 8 | read;
                        intent.putExtra(EXTRA_DATA, String.valueOf(type));
                        intent.putExtra("G", String.valueOf(adv));
                        intent.putExtra("H", String.valueOf(readtime));
                    }
                    break;
                    default:
                        return;
                }
            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            Log.d(TAG, "broadcastUpdate: Hexvalues" + data);
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private float convertToGravity(int input) {
        if (input >> 7 == 1) {
            // 1s complement
            input = ~input & 0xff;
            input += 1;

            input *= 16;
            input *= -1;
        } else {
            input *= 16;
        }
        return ((float) input / 1000f) * 3f;
    }

    public int getActiveDeviceIndex() {
        return mActiveDeviceIndex;
    }

    private boolean internalWriteCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
            return false;

        return gatt.writeCharacteristic(characteristic);
    }

    public void writeToHeartRateCharacteristic(byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(UUID_HEART_RATE_SERVICE);
        BluetoothGattCharacteristic charcterstic = service.getCharacteristic(UUID_HEART_RATE_MEASUREMENT);
        charcterstic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        charcterstic.setValue(value);
        mBluetoothGatt.setCharacteristicNotification(charcterstic, true);
        mBluetoothGatt.writeCharacteristic(charcterstic);
    }

    public void writeToHeartRateCharacteristic(String time) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(UUID_HEART_RATE_SERVICE);
        BluetoothGattCharacteristic charcterstic = service.getCharacteristic(UUID_HEART_RATE_MEASUREMENT);
        charcterstic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        charcterstic.setValue(time);
        mBluetoothGatt.setCharacteristicNotification(charcterstic, true);
        mBluetoothGatt.writeCharacteristic(charcterstic);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        /*if (!isReadAll) {
            triggerLoopDevices(mIntervalToReadDevices);
        }*/
        if (repeatPendingIntent != null)
            alarmManager.cancel(repeatPendingIntent);
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        //getAdd();
//        connect(add);
        return true;
    }

//public void writeToHeartRateCharacteristic(BluetoothGattCharacteristic characteristic) {
//    if (mBluetoothGatt == null) {
//        Log.d(TAG, "writeToHeartRateCharacteristic:writing ");
//        return;
//    }
//    mBluetoothGatt.writeToHeartRateCharacteristic(characteristic);
//}

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
//        if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_IDLE) {
        Log.d(TAG, "connect: " + address);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
//        if (DeviceScanAcitivity2.checking){}
//        mBluetoothGatt.connect();}
        //  Previously connected device.  Try to reconnect.
        Log.d(TAG, "connect: " + mBluetoothDeviceAddress);
        if (mBluetoothDeviceAddress != null && address.matches(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mBluetoothDeviceAddress = address;
                mConnectionState = STATE_CONNECTING;
                System.out.println("connect");
                broadcastUpdate(ACTION_GATT_CONNECTING, address);
                return true;
            } else {
                System.out.println("connect false");
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback, 2);
        } else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        broadcastUpdate(ACTION_GATT_CONNECTING, address);
        return true;
//        }
//        return false;
    }

    private void broadcastUpdate(String action, String address) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADRESS, address);
        sendBroadcast(intent);
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
//    public void disconnect() {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.disconnect();
//    }
    public void disconnect() {
        //isUserDisconnected = true;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            System.out.println("disconnect gatt" + mBluetoothGatt);
            return;
        }
        mBluetoothGatt.disconnect();
        mConnectionState = STATE_DISCONNECTED;
//		else{
//			System.out.println("on disconnect bluegatt is null");
//		}

    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mConnectionState = STATE_IDLE;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     * <p>
     * public void close() {
     * if (mBluetoothGatt == null) {
     * return;
     * }
     * mBluetoothGatt.close();
     * mBluetoothGatt = null;
     * }
     * <p>
     * /**
     * Rehquest a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (mBluetoothGatt.readCharacteristic(characteristic)) {
            check = true;
        }
        Log.d(TAG, "readCharacteristic: " + mBluetoothGatt.readCharacteristic(characteristic));
        Log.d(TAG, "readCharacteristic: " + characteristic.getUuid().toString());
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void readDeviceInfo() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        // This is specific to Heart Rate Measurement.
        Log.d(TAG, "setCharacteristicNotification:212 " + characteristic.getUuid());
        Log.d(TAG, "setCharacteristicNotification: 123" + UUID_HEART_RATE_SERVICE);
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            Log.d(TAG, "setCharacteristicNotification: ");
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                if (enabled) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Log.d(TAG, "setCharacteristicNotification:true ");
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    Log.d(TAG, "setCharacteristicNotification:false ");
                }
                mBluetoothGatt.writeDescriptor(descriptor);
                Log.d(TAG, "setCharacteristicNotification: " + mBluetoothGatt.writeDescriptor(descriptor));
            }
        }
    }

    protected void onDeviceReady() {
        mBluetoothGatt.getDevice();
    }

    public boolean setNotificationState(final boolean enable) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null) {
            return false;
        }

        final BluetoothGattService service = gatt.getService(UUID_HEART_RATE_SERVICE);
        if (service == null)
            return false;

        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_HEART_RATE_MEASUREMENT);
        if (characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return false;

        gatt.setCharacteristicNotification(characteristic, enable);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {
            String mLogSession = "Error";
            if (enable) {
                Log.e(TAG, "broadcast send notifica " + enable);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Log.d(mLogSession, "Enabling battery level notifications...");
                Log.d(mLogSession, "Enabling notifications for " + UUID_HEART_RATE_MEASUREMENT);
                Log.d(mLogSession, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG + ", value=0x0100)");
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                Log.d(mLogSession, "Disabling battery level notifications...");
                Log.d(mLogSession, "Disabling notifications for " + UUID_HEART_RATE_MEASUREMENT);
                Log.d(mLogSession, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG + ", value=0x0000)");

            }
            return internalWriteDescriptorWorkaround(descriptor);
        }
        return false;
    }

    public boolean internalWriteDescriptorWorkaround(final BluetoothGattDescriptor descriptor) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || descriptor == null)
            return false;

        final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
        final int originalWriteType = parentCharacteristic.getWriteType();
        parentCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        final boolean result = gatt.writeDescriptor(descriptor);
        parentCharacteristic.setWriteType(originalWriteType);
        Log.d(TAG, "internalWriteDescriptorWorkaround: " + result);
        return result;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    //inform to server on start of image data and end of image data
    public void loadImageCMD(int i) {
        //Log.w(TAG, "byte array"+i);
        //buttonchar
        //System.out.println("cmd");
        if (mLedCharacteristic != null) {
            //System.out.println("cmd"+i);
            mLedCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mLedCharacteristic.setValue(i, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGatt.writeCharacteristic(mLedCharacteristic);
        }
    }

    //load the image data to server (20 byte packet)
    public void loadImageDATA(byte[] trans) {
        if (mLedCharacteristic != null) {
            //for(int i=0;i<trans.length;i++){
            //System.out.println("mdata"+trans[0]);
            //}
            mLedCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mLedCharacteristic.setValue(trans);
            mBluetoothGatt.setCharacteristicNotification(mLedCharacteristic, true);
            mBluetoothGatt.writeCharacteristic(mLedCharacteristic);
            Log.d(TAG, "loadImageDATA: " + mBluetoothGatt.writeCharacteristic(mLedCharacteristic));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "broadcast onStartCommand");
        initialize();
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_READ_DEVICES:
                        isReadAll = true;
                        mIntervalToReadDevices = intent.getLongExtra(EXTRA_INTERVAL, 0);
                        break;
                    case ACTION_READ_DEVICE:
                        isReadAll = false;
                        Device device = intent.hasExtra(EXTRA_ADRESS)
                                ? (Device) intent.getSerializableExtra(EXTRA_ADRESS) : null;
                        mIntervalToReadDevices = intent.getLongExtra(EXTRA_INTERVAL, 0);
                        if (device != null) {
                            devices.clear();
                            devices.add(device);
                            if (repeatPendingIntent != null)
                                alarmManager.cancel(repeatPendingIntent);
                            /*disconnect();
                            close();*/
//                            waitIdle(100);
                            mIsReadInProgress = false;
                            mConnectionState = STATE_CONNECTING;
                            mActiveDeviceIndex = 0;
                            connect(device.getAddress());
//                            triggerLoopDevice();
                        }
                        break;
                    case ACTION_RERUN_DEVICE:
                        close();
                        initiateReadFromAllDevices();
                        break;
                    case ACTION_RERUN:
                        isReadAll = true;
                        close();
                        initiateReadFromAllDevices();
                        break;
                    case ACTION_INCREASED_TIME:
                    case ACTION_UPDATED_DEVICES:
                        mIsReadInProgress = false;
                        triggerLoopDevices(intent.getLongExtra(EXTRA_INTERVAL, 0));
                        break;
                    case ACTION_DISCONNECT:
                        int deviceId = intent.getIntExtra(EXTRA_DISCONNECT_INDEX, -1);
                        if (deviceId != -1) {
//                            disconnect();
                            close();
                            mActiveDeviceIndex++;
                            connectDeviceByActiveIndex();
                        }
                        break;
                    case ACTION_STOP_TIMER:
                        if (repeatPendingIntent != null)
                            alarmManager.cancel(repeatPendingIntent);
                        mConnectionState = STATE_IDLE;
                        break;
                }
            }
        }
        return START_STICKY;
    }

    private void triggerLoopDevice() {
        if (repeatPendingIntent != null)
            alarmManager.cancel(repeatPendingIntent);
        // For calling after a particular time
        Intent alarmIntent = new Intent(this, BluetoothLeService.class);
        alarmIntent.setAction(ACTION_RERUN_DEVICE);
        repeatPendingIntent = PendingIntent.getService(this, 1, alarmIntent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                mIntervalToReadDevices, repeatPendingIntent);
    }

    public void triggerLoopDevices(long intervalToReadAllDevices) {
        if (isReadAll && !mIsReadInProgress) {
            close();
            Log.e(TAG, "Interval from ui " + intervalToReadAllDevices);
            mIntervalToReadDevices = intervalToReadAllDevices;
            if (repeatPendingIntent != null)
                alarmManager.cancel(repeatPendingIntent);
            // For calling after a particular time
            Intent alarmIntent = new Intent(this, BluetoothLeService.class);
            alarmIntent.setAction(ACTION_RERUN);
            repeatPendingIntent = PendingIntent.getService(this, 1, alarmIntent, 0);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    intervalToReadAllDevices, repeatPendingIntent);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    }

    public synchronized void initiateReadFromAllDevices() {
        if (devices != null && devices.size() > 0) {
            mActiveDeviceIndex = 0;
            mIsReadInProgress = true;
            connectDeviceByActiveIndex();
        } else {
            alarmManager.cancel(repeatPendingIntent);
            stopSelf();
        }
    }

    private void connectDeviceByActiveIndex() {
        if (devices != null && devices.size() > mActiveDeviceIndex) {
            connect(devices.get(mActiveDeviceIndex).getAddress());
            disconnectIfTimeOut(mActiveDeviceIndex);
        } else {
            mIsReadInProgress = false;
        }
    }

    private void disconnectIfTimeOut(int index) {
        Intent alarmIntent = new Intent(this, BluetoothLeService.class);
        alarmIntent.setAction(ACTION_DISCONNECT);
        alarmIntent.putExtra(EXTRA_DISCONNECT_INDEX, index);
        disconnectingPendingIntent = PendingIntent.getService(this, 1, alarmIntent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (10 * 1000),
                disconnectingPendingIntent);
    }

    public boolean waitIdle(int i) {
        i /= 10;
        while (--i > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return i > 0;
    }

    public void addDevices(ArrayList<Device> configuredDevices) {
        devices.clear();
        devices.addAll(configuredDevices);
    }

    public int getCurrentState() {
        return mConnectionState;
    }

    //    public class LocalBinder extends Binder {
//		BluetoothLeService getService() {
//			System.out.println("ble service getservice");
//			return BluetoothLeService.this;
//		}
//	}
//
//	@Override
//	public IBinder onBind(Intent intent) {
//		return mBinder;
//	}
//
//	@Override
//	public boolean onUnbind(Intent intent) {
//		// After using a given device, you should make sure that BluetoothGatt.close() is called
//		// such that resources are cleaned up properly.  In this particular example, close() is
//		// invoked when the UI is disconnected from the Service.
//		//  close();
//		//mBluetoothGatt.close();
//		return super.onUnbind(intent);
//
//	}
//
//	private final IBinder mBinder = new LocalBinder();
//
//	/**
//	 * Initializes a reference to the local Bluetooth adapter.
//	 *
//	 * @return Return true if the initialization is successful.
//	 */
//	public boolean initialize() {
//		// For API level 18 and above, get a reference to BluetoothAdapter through
//		// BluetoothManager.
//		if (mBluetoothManager == null) {
//			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//			if (mBluetoothManager == null) {
//				Log.e(TAG, "Unable to initialize BluetoothManager.");
//				return false;
//			}
//		}
//		mBluetoothAdapter = mBluetoothManager.getAdapter();
//		if (mBluetoothAdapter == null) {
//			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
//			return false;
//		}
//		return true;
//	}
//
//	/**
//	 * Connects to the GATT server hosted on the Bluetooth LE device.
//	 *
//	 * @param address The device address of the destination device.
//	 *
//	 * @return Return true if the connection is initiated successfully. The connection result
//	 *         is reported asynchronously through the
//	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
//	 *         callback.
//	 */
//	public boolean connect(final String address) {
//	//	Retry = true;
//		CurrentDeviceAdress = address;
//	 	isUserDisconnected = false;
//	 	//re-initialize the adapter
//	 	mBluetoothAdapter = mBluetoothManager.getAdapter();
//
//		if (mBluetoothAdapter == null || address == null) {
//			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
//			return false;
//		}
//
//		device = mBluetoothAdapter.getRemoteDevice(address);
//		if (device == null) {
//			Log.w(TAG, "Device not found.  Unable to connect.");
//			return false;
//		}
//
//		if(mBluetoothGatt == null){
//
//			mBluetoothGatt = device.connectGatt(BluetoothLeService.this, false, mGattCallback);
//		}
//		else{
//			mBluetoothGatt.connect();
//		}
//		Log.d(TAG, "Trying to create a new connection.");
//		return true;
//	}
//
//
//	/**
//	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
//	 * is reported asynchronously through the
//	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
//	 * callback.
//	 */
//	public void disconnect(String devieAdress) {
//		isUserDisconnected = true;
//		if(mBluetoothGatt!=null){
//			System.out.println("disconnect gatt" + mBluetoothGatt );
//		mBluetoothGatt.disconnect();
//
//		}
//		else{
//			System.out.println("on disconnect bluegatt is null");
//		}
//
//	}
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    class Task implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i <= 5; i++) {
                final int value = i;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            loadImageDATA(new byte[]{0x03});
            for (int i = 0; i <= 5; i++) {
                final int value = i;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            loadImageDATA(new byte[]{0x07});
        }
    }

}