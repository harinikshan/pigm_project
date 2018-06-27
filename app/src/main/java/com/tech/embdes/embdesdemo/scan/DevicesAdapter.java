package com.tech.embdes.embdesdemo.scan;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tech.embdes.embdesdemo.R;
import com.tech.embdes.embdesdemo.data.Device;

import java.util.ArrayList;

/**
 * Created by sakkam2 on 1/29/2018.
 */

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> implements OnClick {
    private final OnClick mCallback;
    private ArrayList<BleDevice> devices = new ArrayList<>();

    public DevicesAdapter(OnClick callback) {
        this.mCallback = callback;
    }

    @Override
    public void onClick(int position, boolean selected) {
        if (devices.size() > position) {
            devices.get(position).setSelected(selected);
            notifyItemChanged(position);
        }
        mCallback.onClick(position, selected);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_device, parent, false), this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindView(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public ArrayList<BleDevice> getDevices() {
        return devices;
    }

    public ArrayList<BleDevice> getSelectedDevice() {
        ArrayList<BleDevice> selectedDevices = new ArrayList<>();
        for (BleDevice device : devices) {
            if (device.isSelected())
                selectedDevices.add(device);
        }
        return selectedDevices;
    }

    public void clear() {
        devices.clear();
        notifyDataSetChanged();
    }

    public void addDevice(BleDevice bleDevice) {
        int insertPosition = -1;
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            if (device.getAddress().equals(bleDevice.getAddress())){
                insertPosition = i;
                break;
            }
        }
        if (insertPosition != -1)
            devices.set(insertPosition, bleDevice);
        else
            devices.add(bleDevice);

        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceAddress;
        final TextView deviceName;
        private final OnClick mCallBack;

        ViewHolder(View view, OnClick callBack) {
            super(view);
            this.mCallBack = callBack;
            deviceAddress = view.findViewById(R.id.device_address);
            deviceName = view.findViewById(R.id.device_name);
        }

        void bindView(BleDevice device) {
            if (device != null) {
                if (device.getName() != null)
                    deviceName.setText(device.getName());
                else
                    deviceName.setText(itemView.getContext().getString(R.string.unknown_device));
                if (device.getAddress() != null)
                    deviceAddress.setText(device.getAddress());

                itemView.setBackgroundColor(device.isSelected() ? itemView.getResources().getColor(R.color.darkGray) : 0);
                itemView.setOnClickListener(view -> mCallBack.onClick(getAdapterPosition(), !device.isSelected()));
            }
        }
    }
}
