package com.hsae.bluetoothsdk;
interface IBluetoothCallback {
    void onBatterLevelChanged(int level);
    void onBtStateChanged(int state, String address);
    void onConnectStateChanged(int profile, int state, int reason);
    void onDeviceConnectRequest(String name, String mac, int type, int reserved);
    void onDeviceInquiried(String name, String mac, int rssi, int cod, boolean finished);
    void onDeviceNameChanged(String name);
    void onDevicePairRequest(String mac, String name, boolean request);
    void onPairStateChanged(String mac, int state);
    void onPowerStateChanged(int state);
    void onServiceConnected();
    void onServiceDisconnected();
    void onSignelLevelChanged(int level);
}
