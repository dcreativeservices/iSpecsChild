package com.ispecs.child.models;

public class Device {

    private String name;
    private String address;
    private String rssi;

    public Device(String name, String address, String rssi) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public String getRssi() {
        return rssi;
    }
}
