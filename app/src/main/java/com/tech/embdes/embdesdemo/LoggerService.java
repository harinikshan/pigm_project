package com.tech.embdes.embdesdemo;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tech.embdes.embdesdemo.data.Constants;
import com.tech.embdes.embdesdemo.scan.DeviceScanActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

/**
 * Created by sakkam2 on 1/31/2018.
 */

public class LoggerService extends IntentService {
    public LoggerService() {
        super(LoggerService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Constants.LOG_FILES:
                    try {
                        Process process = Runtime.getRuntime().exec("logcat");
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));

                        StringBuilder log = new StringBuilder();
                        String line = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            log.append(line);
                            if (!isAppRunning())
                                break;
                        }
                        String path = Environment.getExternalStorageDirectory().getPath() + "/Embdes/";
                        File exploeDir = new File(path);
                        if (!exploeDir.exists()) {
                            exploeDir.mkdirs();
                        }
                        String timeStamp = new SimpleDateFormat("dd_MM_yyyy").format(System.currentTimeMillis());
                        File file = new File(exploeDir, "log_" + timeStamp + ".txt");
                        Log.e(LoggerService.class.getSimpleName(), "log file:"+file.getAbsolutePath());
                        file.createNewFile();
                        FileWriter out = new FileWriter(file.getAbsolutePath(), true);
                        out.write("---------------------------------------------------------------------------------------------------------------------");
                        try {
                            out.write(log.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    public boolean isAppRunning() {
        return SplashScreen.active || DeviceScanActivity.active || DeviceScanAcitivity2.active || Values.active || BaseActivity.active;
    }
}
