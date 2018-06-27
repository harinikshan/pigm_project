package com.tech.embdes.embdesdemo.data;

import java.io.Serializable;

/**
 * Created by sakkam2 on 1/28/2018.
 */

public class Device implements Serializable {
    private long id;
    private String name, address;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
