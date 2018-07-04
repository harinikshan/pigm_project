package com.tech.embdes.embdesdemo.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tech.embdes.embdesdemo.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by sakkam2 on 1/28/2018.
 */

public class DbService extends IntentService {
    private static final String SEPERATOR = "|||||";

    public DbService() {
        super(DbService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case DbContract.ACTION_CREATE_DEVICE:
                    insertDevice((Device) intent.getSerializableExtra(DbContract.DEVICES));
                    break;
                case DbContract.ACTION_CREATE_ANALYSIS:
                    insertAnalysis((Analysis) intent.getSerializableExtra(DbContract.ANALYSIS));
                    break;
                case DbContract.ACTION_DELETE_DEVICES:
                    deleteAllDevices();
                    break;
                case DbContract.ACTION_DELETE_ANALYSIS:
                    deleteAllAnalysis();
                    break;
                case DbContract.ACTION_DELETE_DEVICE:
                    deleteDevice(intent.getLongExtra(DbContract.AnalysisEntry.COLUMN_DEVICE, 0));
                    break;
                case DbContract.ACTION_GET_ALL_DEVICES:
                    getAllDevicesAndSend();
                    break;
                case DbContract.ACTION_GET_ALL_ANALYSIS:
                    getAllAnalysis(intent.getLongExtra(DbContract.AnalysisEntry.COLUMN_DEVICE, 0));
                    break;
                case DbContract.ACTION_GET_DEVICE:
                    getDevice(intent.getLongExtra(DbContract.DeviceEntry._ID, 0));
                    break;
                case DbContract.ACTION_GET_FILTERED_ANALYSIS:
                    getAnalysis(intent.getLongExtra(DbContract.AnalysisEntry.COLUMN_DEVICE, 0),
                            intent.getLongExtra(DbContract.FROM_TIME, 0),
                            intent.getLongExtra(DbContract.TO_TIME, 0));
                    break;
                case DbContract.ACTION_UPDATE_DEVICE:
                    updateDevice(intent.getLongExtra(DbContract.DeviceEntry._ID, 0),
                            intent.getStringExtra(DbContract.DeviceEntry.COLUMN_NAME));
                    break;
                case DbContract.ACTION_INSERT_MULTI_ANALYSIS:
                    insertAnalysis((ArrayList<Analysis>) intent.getSerializableExtra(DbContract.ANALYSIS));
                    break;
                case DbContract.ACTION_INSERT_MULTI_DEVICES:
                    insertDevices((ArrayList<Device>) intent.getSerializableExtra(DbContract.DEVICES));
                    break;
                case DbContract.ACTION_EXPORT_DB:
                    exportDbAndRespond(intent.getLongExtra(DbContract.AnalysisEntry.COLUMN_DEVICE, 0));
                    break;
                case DbContract.ACTION_EXPORT_ALL_LOGS:
                    exportAllDb();
                    break;
            }
        }
    }

    private void exportAllDb() {
        ArrayList<Device> devices = getAllDevices();
        boolean dataAvailable = true;
        for (Device device : devices) {
            String dbAvailable = exportDb(device.getId());
            dataAvailable = dataAvailable && dbAvailable != null;
        }
        if (dataAvailable) {
            Intent intent = new Intent(DbContract.ACTION_EXPORT_ALL_LOGS);
            sendBroadcast(intent);
        } else {
            Intent intent = new Intent(DbContract.ACTION_NO_DATA_TO_EXPORT);
            sendBroadcast(intent);
        }
    }

    private void exportDbAndRespond(long deviceId) {
        String data = exportDb(deviceId);
        if (data != null) {
            String[] pathAndName = data.split(Pattern.quote(SEPERATOR));
            if (pathAndName.length == 2) {
                Intent intent = new Intent(DbContract.ACTION_EXPORT_DB);
                intent.putExtra(Constants.FILE_PATH, pathAndName[0]);
                intent.putExtra(Constants.FILE_NAME, pathAndName[1]);
                sendBroadcast(intent);
            }
        } else {
            Intent intent = new Intent(DbContract.ACTION_NO_DATA_TO_EXPORT);
            sendBroadcast(intent);
        }
    }

    private String exportDb(long deviceId) {
        Device device = readDevice(deviceId);
        if (device != null) {
            ArrayList<Analysis> analyses = readAnalysisData(deviceId);

            if (analyses != null && analyses.size() == 0)
                return null;

            String path = Environment.getExternalStorageDirectory().getPath() + "/embdes_pigm/logs/";
            File exploeDir = new File(path);
            if (!exploeDir.exists()) {
                exploeDir.mkdirs();
            }
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedDate = df.format(Calendar.getInstance().getTime());
            String fileName = device.getName() + "_" + formattedDate + "-Emb-Pig.csv";
            File file = new File(exploeDir, fileName);
            try {
                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                String names[] = {"Time", "Heart rate", "spo2", "temperature", "x_axis", "y_axis", "z_axis"};
                csvWrite.writeNext(names);
                for (Analysis analysis : analyses) {
                    SimpleDateFormat sp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    String analysisdata[] = {
                            sp.format(new Date(analysis.getTime())),
                            analysis.getHeartRate(),
                            analysis.getSpo2(),
                            analysis.getTemperature(),
                            analysis.getxAxis(),
                            analysis.getyAxis(),
                            analysis.getzAxis()
                    };
                    csvWrite.writeNext(analysisdata);
                }
                csvWrite.close();

                return path + SEPERATOR + fileName;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                Log.d("Check", "exportDb: " + ex.getMessage(), ex);
            }
        }
        return null;
    }

    private void deleteDevice(long deviceId) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getWritableDatabase();
            db.beginTransaction();
            try {
                String[] whereArgs = {String.valueOf(deviceId)};
                db.delete(DbContract.DeviceEntry.TABLE_NAME, DbContract.DeviceEntry._ID + "=?", whereArgs);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
            db.close();
        }
    }

    private void insertDevice(Device device) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(DbContract.DeviceEntry.COLUMN_NAME, device.getName());
                contentValues.put(DbContract.DeviceEntry.COLUMN_ADDRESS, device.getAddress());

                long insertId = db.insert(DbContract.DeviceEntry.TABLE_NAME, null, contentValues);
                if (insertId != -1) {
                    db.setTransactionSuccessful();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
    }

    private void insertAnalysis(Analysis analysis) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(DbContract.AnalysisEntry.COLUMN_DEVICE, analysis.getDevice());
                contentValues.put(DbContract.AnalysisEntry.COLUMN_TIME, analysis.getTime());
                contentValues.put(DbContract.AnalysisEntry.COLUMN_HEART_RATE, analysis.getHeartRate());
                contentValues.put(DbContract.AnalysisEntry.COLUMN_SPO, analysis.getSpo2());
                contentValues.put(DbContract.AnalysisEntry.COLUMN_TEMPERATURE, analysis.getTemperature());
                contentValues.put(DbContract.AnalysisEntry.COLUMN_X_AXIS, analysis.getxAxis());
                contentValues.put(DbContract.AnalysisEntry.COLUMN_Y_AXIS, analysis.getyAxis());
                contentValues.put(DbContract.AnalysisEntry.COLUMN_Z_AXIS, analysis.getzAxis());

                long insertId = db.insertOrThrow(DbContract.AnalysisEntry.TABLE_NAME, null, contentValues);
                if (insertId != -1)
                    db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
    }

    private void deleteTable(String tableName) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete(tableName, null, null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
            db.close();
        }
    }

    private void deleteAllDevices() {
        deleteTable(DbContract.DeviceEntry.TABLE_NAME);
    }

    private void deleteAllAnalysis() {
        deleteTable(DbContract.AnalysisEntry.TABLE_NAME);
    }

    private void getAllDevicesAndSend() {
        if (getApplicationContext() != null) {
            Intent intent = new Intent(DbContract.ACTION_GET_ALL_DEVICES);
            intent.putExtra(DbContract.DEVICES, getAllDevices());
            sendBroadcast(intent);
        }
    }

    private ArrayList<Device> getAllDevices() {
        SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getReadableDatabase();
        ArrayList<Device> devices = new ArrayList<>();

        String[] projection = {
                DbContract.DeviceEntry._ID,
                DbContract.DeviceEntry.COLUMN_NAME,
                DbContract.DeviceEntry.COLUMN_ADDRESS
        };

        Cursor cursor = db.query(DbContract.DeviceEntry.TABLE_NAME, projection, null, null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                Device device = new Device();
                device.setId(cursor.getLong(cursor.getColumnIndex(DbContract.DeviceEntry._ID)));
                device.setName(cursor.getString(cursor.getColumnIndex(DbContract.DeviceEntry.COLUMN_NAME)));
                device.setAddress(cursor.getString(cursor.getColumnIndex(DbContract.DeviceEntry.COLUMN_ADDRESS)));
                devices.add(device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        db.close();
        return devices;
    }

    private void getAllAnalysis(long deviceId) {
        ArrayList<Analysis> analyses = readAnalysisData(deviceId);
        if (analyses != null) {
            Intent intent = new Intent(DbContract.ACTION_GET_FILTERED_ANALYSIS);
            intent.putExtra(DbContract.ANALYSIS, analyses);
            sendBroadcast(intent);
        }
    }

    private ArrayList<Analysis> readAnalysisData(long deviceId) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getReadableDatabase();
            ArrayList<Analysis> analyses = new ArrayList<>();

            String[] projection = {
                    DbContract.AnalysisEntry._ID,
                    DbContract.AnalysisEntry.COLUMN_DEVICE,
                    DbContract.AnalysisEntry.COLUMN_TIME,
                    DbContract.AnalysisEntry.COLUMN_HEART_RATE,
                    DbContract.AnalysisEntry.COLUMN_SPO,
                    DbContract.AnalysisEntry.COLUMN_TEMPERATURE,
                    DbContract.AnalysisEntry.COLUMN_X_AXIS,
                    DbContract.AnalysisEntry.COLUMN_Y_AXIS,
                    DbContract.AnalysisEntry.COLUMN_Z_AXIS
            };
            String selection = DbContract.AnalysisEntry.COLUMN_DEVICE + "=?";
            String[] selectionArgs = {String.valueOf(deviceId)};

            Cursor cursor = db.query(DbContract.AnalysisEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
            while (cursor.moveToNext()) {
                try {
                    Analysis analysis = new Analysis();
                    analysis.setId(cursor.getLong(cursor.getColumnIndex(DbContract.AnalysisEntry._ID)));
                    analysis.setDevice(cursor.getLong(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_DEVICE)));
                    analysis.setTime(cursor.getLong(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_TIME)));
                    analysis.setHeartRate(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_HEART_RATE)));
                    analysis.setSpo2(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_SPO)));
                    analysis.setTemperature(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_TEMPERATURE)));
                    analysis.setxAxis(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_X_AXIS)));
                    analysis.setyAxis(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_Y_AXIS)));
                    analysis.setzAxis(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_Z_AXIS)));

                    analyses.add(analysis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
            db.close();
            return analyses;
        }
        return null;
    }

    private void getDevice(long deviceId) {
        Device device = readDevice(deviceId);

        if (device != null) {
            Intent intent = new Intent(DbContract.ACTION_GET_ALL_DEVICES);
            intent.putExtra(DbContract.DEVICES, device);
            sendBroadcast(intent);
        }
    }

    private Device readDevice(long deviceId) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getReadableDatabase();
            Device device = new Device();

            String[] projection = {
                    DbContract.DeviceEntry._ID,
                    DbContract.DeviceEntry.COLUMN_NAME,
                    DbContract.DeviceEntry.COLUMN_ADDRESS
            };
            String selection = DbContract.DeviceEntry._ID + "=?";
            String[] selectionArgs = {String.valueOf(deviceId)};

            Cursor cursor = db.query(DbContract.DeviceEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
            while (cursor.moveToNext()) {
                device.setId(cursor.getLong(cursor.getColumnIndex(DbContract.DeviceEntry._ID)));
                device.setName(cursor.getString(cursor.getColumnIndex(DbContract.DeviceEntry.COLUMN_NAME)));
                device.setAddress(cursor.getString(cursor.getColumnIndex(DbContract.DeviceEntry.COLUMN_ADDRESS)));
            }
            cursor.close();
            db.close();
            return device;
        }
        return null;
    }

    private void getAnalysis(long deviceId, long fromTime, long toTime) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getReadableDatabase();
            ArrayList<Analysis> analyses = new ArrayList<>();

            String[] projection = {
                    DbContract.AnalysisEntry._ID,
                    DbContract.AnalysisEntry.COLUMN_DEVICE,
                    DbContract.AnalysisEntry.COLUMN_TIME,
                    DbContract.AnalysisEntry.COLUMN_HEART_RATE,
                    DbContract.AnalysisEntry.COLUMN_SPO,
                    DbContract.AnalysisEntry.COLUMN_TEMPERATURE,
                    DbContract.AnalysisEntry.COLUMN_X_AXIS,
                    DbContract.AnalysisEntry.COLUMN_Y_AXIS,
                    DbContract.AnalysisEntry.COLUMN_Z_AXIS
            };
            String selection = DbContract.AnalysisEntry.COLUMN_DEVICE + "=? AND " + DbContract.AnalysisEntry.COLUMN_TIME + ">=? AND " + DbContract.AnalysisEntry.COLUMN_TIME + "<=?";
            String[] selectionArgs = {String.valueOf(deviceId), String.valueOf(fromTime), String.valueOf(toTime)};

            Cursor cursor = db.query(DbContract.AnalysisEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
            while (cursor.moveToNext()) {
                try {
                    Analysis analysis = new Analysis();
                    analysis.setId(cursor.getLong(cursor.getColumnIndex(DbContract.AnalysisEntry._ID)));
                    analysis.setDevice(cursor.getLong(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_DEVICE)));
                    analysis.setTime(cursor.getLong(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_TIME)));
                    analysis.setHeartRate(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_HEART_RATE)));
                    analysis.setSpo2(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_SPO)));
                    analysis.setTemperature(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_TEMPERATURE)));
                    analysis.setxAxis(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_X_AXIS)));
                    analysis.setyAxis(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_Y_AXIS)));
                    analysis.setzAxis(cursor.getString(cursor.getColumnIndex(DbContract.AnalysisEntry.COLUMN_Z_AXIS)));

                    analyses.add(analysis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
            db.close();

            Intent intent = new Intent(DbContract.ACTION_GET_FILTERED_ANALYSIS);
            intent.putExtra(DbContract.ANALYSIS, analyses);
            sendBroadcast(intent);
        }
    }

    private void updateDevice(long deviceId, String newName) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(this).getWritableDatabase();

            db.beginTransaction();
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbContract.DeviceEntry.COLUMN_NAME, newName);

            String whereClause = DbContract.DeviceEntry._ID + "=?";
            String[] whereClauseArgs = {String.valueOf(deviceId)};

            int updatedId = db.update(DbContract.DeviceEntry.TABLE_NAME, contentValues, whereClause, whereClauseArgs);
            if (updatedId != 0)
                db.setTransactionSuccessful();

            db.endTransaction();
            db.close();
        }
    }

    private void insertAnalysis(ArrayList<Analysis> analyses) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getWritableDatabase();
            db.beginTransaction();
            try {
                for (Analysis analysis : analyses) {
                    if (!getAnalysis(db, analysis.getTime(), analysis.getDevice())) {
                        Log.i("hari","data analysis entry to database" );
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_DEVICE, analysis.getDevice());
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_TIME, analysis.getTime());
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_HEART_RATE, analysis.getHeartRate());
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_SPO, analysis.getSpo2());
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_TEMPERATURE, analysis.getTemperature());
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_X_AXIS, analysis.getxAxis());
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_Y_AXIS, analysis.getyAxis());
                        contentValues.put(DbContract.AnalysisEntry.COLUMN_Z_AXIS, analysis.getzAxis());

                        db.insertOrThrow(DbContract.AnalysisEntry.TABLE_NAME, null, contentValues);
                    }
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
    }

    private boolean getAnalysis(SQLiteDatabase db, long time, long deviceId) {
        if (getApplicationContext() != null) {
            String[] projection = {
                    DbContract.AnalysisEntry.COLUMN_DEVICE,
                    DbContract.AnalysisEntry.COLUMN_TIME
            };
            String selection = DbContract.AnalysisEntry.COLUMN_DEVICE + "=? AND " + DbContract.AnalysisEntry.COLUMN_TIME + "=?";
            String[] selectionArgs = {String.valueOf(deviceId), String.valueOf(time)};

            Cursor cursor = db.query(DbContract.AnalysisEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
            int count = cursor.getCount();
            cursor.close();
            return count > 0;
        }
        return false;
    }

    private void insertDevices(ArrayList<Device> devices) {
        if (getApplicationContext() != null) {
            SQLiteDatabase db = DbHelper.getInstance(getApplicationContext()).getWritableDatabase();
            db.beginTransaction();
            try {
                for (Device device : devices) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DbContract.DeviceEntry.COLUMN_NAME, device.getName());
                    contentValues.put(DbContract.DeviceEntry.COLUMN_ADDRESS, device.getAddress());

                    db.insert(DbContract.DeviceEntry.TABLE_NAME, null, contentValues);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
    }
}
