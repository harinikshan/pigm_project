package com.tech.embdes.embdesdemo;

import android.widget.ProgressBar;

/**
 * Created by Aakash on 22-08-2017.
 */

public class Ble_Pojo {
    String name,address;
    String connection;
    String connected;

    public String getConnected() {
        return connected;
    }

    public void setConnected(String connected) {
        this.connected = connected;
    }

    public String getDisconnected() {
        return disconnected;
    }

    public void setDisconnected(String disconnected) {
        this.disconnected = disconnected;
    }

    String disconnected;

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
