package com.tech.embdes.embdesdemo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sakkam2 on 1/28/2018.
 */

public class DbHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static String DB_NAME = "pigs";
    private static String INTEGER = " INTEGER";
    private static String TEXT = " TEXT";
    private static String COMMA_SEP = ",";

    private static String CREATE_DEVICE_TABLE =
            "create table " + DbContract.DeviceEntry.TABLE_NAME + "("
                    + DbContract.DeviceEntry._ID + INTEGER + " primary key" + COMMA_SEP
                    + DbContract.DeviceEntry.COLUMN_NAME + TEXT + COMMA_SEP
                    + DbContract.DeviceEntry.COLUMN_ADDRESS + TEXT + COMMA_SEP
                    + DbContract.DeviceEntry.COLUMN_MAJOR_CLASS + TEXT + COMMA_SEP
                    + DbContract.DeviceEntry.COLUMN_MINOR_CLASS + TEXT + ")";

    private static String CREATE_ANALYSIS_TABLE =
            "create table " + DbContract.AnalysisEntry.TABLE_NAME + "("
                    + DbContract.AnalysisEntry._ID + INTEGER + " primary key,"
                    + DbContract.AnalysisEntry.COLUMN_DEVICE + INTEGER + " REFERENCES " + DbContract.DeviceEntry.TABLE_NAME + " ON DELETE CASCADE" + COMMA_SEP
                    + DbContract.AnalysisEntry.COLUMN_TIME + INTEGER + COMMA_SEP
                    + DbContract.AnalysisEntry.COLUMN_HEART_RATE + TEXT + COMMA_SEP
                    + DbContract.AnalysisEntry.COLUMN_SPO + TEXT + COMMA_SEP
                    + DbContract.AnalysisEntry.COLUMN_TEMPERATURE + TEXT + COMMA_SEP
                    + DbContract.AnalysisEntry.COLUMN_X_AXIS + TEXT + COMMA_SEP
                    + DbContract.AnalysisEntry.COLUMN_Y_AXIS + TEXT + COMMA_SEP
                    + DbContract.AnalysisEntry.COLUMN_Z_AXIS + TEXT + ")";

    private static DbHelper mInstance = null;

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public synchronized static DbHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DbHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DEVICE_TABLE);
        db.execSQL(CREATE_ANALYSIS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (!db.isReadOnly())
            db.setForeignKeyConstraintsEnabled(true);
    }
}
