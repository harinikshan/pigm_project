package com.tech.embdes.embdesdemo.data;

import android.provider.BaseColumns;

/**
 * Created by sakkam2 on 1/28/2018.
 */

public interface DbContract {
    String ACTION_CREATE_DEVICE = "com.tech.embdes.embdesdemo.ACTION_CREATE_DEVICE";
    String ACTION_CREATE_ANALYSIS = "com.tech.embdes.embdesdemo.ACTION_CREATE_ANALYSIS";
    String ACTION_DELETE_DEVICE = "com.tech.embdes.embdesdemo.ACTION_UPDATED_DEVICES";
    String ACTION_DELETE_DEVICES = "com.tech.embdes.embdesdemo.ACTION_DELETE_DEVICES";
    String ACTION_DELETE_ANALYSIS = "com.tech.embdes.embdesdemo.ACTION_DELETE_ANALYSIS";
    String ACTION_GET_ALL_DEVICES = "com.tech.embdes.embdesdemo.ACTION_GET_ALL_DEVICES";
    String ACTION_GET_ALL_ANALYSIS = "com.tech.embdes.embdesdemo.ACTION_GET_ALL_ANALYSIS";
    String ACTION_GET_DEVICE = "com.tech.embdes.embdesdemo.ACTION_GET_DEVICE";
    String ACTION_GET_FILTERED_ANALYSIS = "com.tech.embdes.embdesdemo.ACTION_GET_FILTERED_ANALYSIS";
    String ACTION_UPDATE_DEVICE = "com.tech.embdes.embdesdemo.ACTION_UPDATE_DEVICE";
    String ACTION_INSERT_MULTI_ANALYSIS = "com.tech.embdes.embdesdemo.ACTION_INSERT_MULTI_ANALYSIS";
    String ACTION_INSERT_MULTI_DEVICES = "com.tech.embdes.embdesdemo.ACTION_INSERT_MULTI_DEVICES";
    String ACTION_EXPORT_DB = "com.tech.embdes.embdesdemo.ACTION_EXPORT_DB";
    String ACTION_EXPORT_ALL_LOGS = "com.tech.embdes.embdesdemo.ACTION_EXPORT_ALL_LOGS";
    String ACTION_NO_DATA_TO_EXPORT = "com.tech.embdes.embdesdemo.ACTION_NO_DATA_TO_EXPORT";

    String FROM_TIME = "FROM_TIME";
    String TO_TIME = "TO_TIME";
    String ANALYSIS = "ANALYSIS";
    String DEVICES = "DEVICES";

    interface DeviceEntry extends BaseColumns {
        String TABLE_NAME = "devices";
        String COLUMN_NAME = "name";
        String COLUMN_ADDRESS = "address";
        String COLUMN_MAJOR_CLASS = "majorclass";
        String COLUMN_MINOR_CLASS = "minorclass";
    }

    interface AnalysisEntry extends BaseColumns {
        String TABLE_NAME = "analysis";
        String COLUMN_DEVICE = "device";
        String COLUMN_TIME = "analyzed_time";
        String COLUMN_HEART_RATE = "heart_rate";
        String COLUMN_SPO = "spo2";
        String COLUMN_TEMPERATURE = "temperature";
        String COLUMN_X_AXIS = "x_axis";
        String COLUMN_Y_AXIS = "y_axis";
        String COLUMN_Z_AXIS = "z_axis";
    }
}
