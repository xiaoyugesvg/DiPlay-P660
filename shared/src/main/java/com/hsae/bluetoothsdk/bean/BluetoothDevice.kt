package com.hsae.bluetoothsdk.bean

import android.os.Parcel
import android.os.Parcelable

data class BluetoothDevice(
    val name: String?, val mac: String?, val status: Int, val rssi: Int, val cod: Int,
) : Parcelable {
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name); dest.writeString(mac); dest.writeInt(status); dest.writeInt(rssi); dest.writeInt(cod)
    }
    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<BluetoothDevice> {
        override fun createFromParcel(source: Parcel) = BluetoothDevice(source.readString(), source.readString(), source.readInt(), source.readInt(), source.readInt())
        override fun newArray(size: Int): Array<BluetoothDevice?> = arrayOfNulls(size)
    }
}
