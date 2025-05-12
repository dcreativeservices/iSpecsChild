package com.ispecs.child.helper;

public class DummyByteArrayGenerator {

    public static final byte[] getDummyByteArray(){

        byte[] value = {
                (byte) 0x19,   // sensor1
                (byte) 0x1E,   // sensor2
                (byte) 0x64,   // acc_x
                (byte) 0x96,   // acc_y
                (byte) 0xC8,   // acc_z
                (byte) 0x0F,   // day
                (byte) 0x0B,   // month
                (byte) 0x17,   // year
                (byte) 0x0C,   // hour
                (byte) 0x2D,   // minute
                (byte) 0x1E,   // second
                (byte) 0x55,   // battery
                (byte) 0x01    // status
        };

        return value;
    }
}
