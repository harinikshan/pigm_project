package com.tech.embdes.embdesdemo.data;

import java.io.Serializable;
import java.text.SimpleDateFormat;

/**
 * Created by sakkam2 on 1/28/2018.
 */

public class Analysis implements Serializable {
    private long id, device, time;
    private String heartRate, spo2, temperature, xAxis, yAxis, zAxis;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDevice() {
        return device;
    }

    public void setDevice(long device) {
        this.device = device;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(String heartRate) {
        this.heartRate = heartRate;
    }

    public String getSpo2() {
        return spo2;
    }

    public void setSpo2(String spo2) {
        this.spo2 = spo2;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getxAxis() {
        return xAxis;
    }

    public void setxAxis(String xAxis) {
        this.xAxis = xAxis;
    }

    public String getyAxis() {
        return yAxis;
    }

    public void setyAxis(String yAxis) {
        this.yAxis = yAxis;
    }

    public String getzAxis() {
        return zAxis;
    }

    public void setzAxis(String zAxis) {
        this.zAxis = zAxis;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("time:");
        builder.append(time);
        builder.append("|");
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        builder.append("formatted time:");
        builder.append(format.format(time));
        builder.append("|");
        builder.append("id:");
        builder.append(id);
        builder.append("|");
        builder.append("device:");
        builder.append(device);
        builder.append("|");
        builder.append("heartRate:");
        builder.append(heartRate);
        builder.append("|");
        builder.append("spo2:");
        builder.append(spo2);
        builder.append("|");
        builder.append("temperature:");
        builder.append(temperature);
        builder.append("|");
        builder.append("xAxix:");
        builder.append(xAxis);
        builder.append("|");
        builder.append("yAxis:");
        builder.append(yAxis);
        builder.append("|");
        builder.append("zAxis:");
        builder.append(zAxis);
        builder.append("\n");
        return builder.toString();
    }
}
