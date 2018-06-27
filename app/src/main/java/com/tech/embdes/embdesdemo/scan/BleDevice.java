package com.tech.embdes.embdesdemo.scan;

import com.tech.embdes.embdesdemo.data.Device;

/**
 * Created by sakkam2 on 1/29/2018.
 */

public class BleDevice extends Device {
    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
