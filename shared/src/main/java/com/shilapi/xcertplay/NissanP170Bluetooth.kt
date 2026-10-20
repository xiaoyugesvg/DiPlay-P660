package com.shilapi.xcertplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.hsae.bluetoothsdk.IBluetoothManager
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Nissan/HSAE head units (P170, and the P660/P760 family that runs the same
 * `com.hsae.bluetoothservice`) keep the paired phones in the vendor Bluetooth service, so
 * Android's own `bondedDevices` is empty whenever the car stack owns the radio.
 */
object NissanP170Bluetooth {
    private const val TAG = "NissanP170BT"
    private const val PKG = "com.hsae.bluetoothservice"
    private const val SERVICE = "com.hsae.bluetoothservice.service.BluetoothService"
    private const val ACTION = "com.hsae.bluetoothsdk.IBluetoothManager"
    data class Phone(val name: String, val address: String, val status: Int, val rssi: Int)

    private fun bind(context: Context): Pair<IBluetoothManager, ServiceConnection> {
        val app = context.applicationContext
        val latch = CountDownLatch(1)
        var manager: IBluetoothManager? = null
        var failure: Throwable? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                try { manager = IBluetoothManager.Stub.asInterface(service) } catch (t: Throwable) { failure = t }
                latch.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName) = Unit
            override fun onNullBinding(name: ComponentName) { failure = IOException("P170 BluetoothService returned null binding"); latch.countDown() }
            override fun onBindingDied(name: ComponentName) { failure = IOException("P170 BluetoothService binding died"); latch.countDown() }
        }
        // HSAE's own clients bind by the IBluetoothManager action. Keep the
        // package restriction so Android cannot resolve an unrelated service.
        // Fall back to the concrete exported service component on older P170 builds.
        val actionIntent = Intent(ACTION).setPackage(PKG)
        val componentIntent = Intent().setComponent(ComponentName(PKG, SERVICE))
        val ok = try {
            app.bindService(actionIntent, connection, Context.BIND_AUTO_CREATE)
        } catch (t: Throwable) {
            Log.w(TAG, "Action bind failed; trying P170 service component", t)
            try {
                app.bindService(componentIntent, connection, Context.BIND_AUTO_CREATE)
            } catch (t2: Throwable) {
                failure = t2
                false
            }
        }
        if (!ok) throw IOException("Cannot bind Nissan P170 BluetoothService")
        if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
            runCatching { app.unbindService(connection) }
            throw IOException("Timed out binding Nissan P170 BluetoothService")
        }
        failure?.let { runCatching { app.unbindService(connection) }; throw IOException("Cannot use Nissan P170 BluetoothService", it) }
        return manager!! to connection
    }

    fun phones(context: Context): List<Phone> = runCatching {
        val (manager, connection) = bind(context)
        try {
            manager.getPairedDevices().mapNotNull { d ->
                val mac = d.mac?.trim().orEmpty()
                if (mac.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")))
                    Phone(d.name?.ifBlank { "iPhone" } ?: "iPhone", mac, d.status, d.rssi)
                else null
            }
        } finally { runCatching { context.applicationContext.unbindService(connection) } }
    }.onFailure { Log.w(TAG, "getPairedDevices failed", it) }.getOrDefault(emptyList())

    fun prepareForCarPlay(context: Context, address: String, name: String = "iPhone"): Boolean {
        val normalized = address.trim()
        if (!normalized.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))) return false
        return runCatching {
            val (manager, connection) = bind(context)
            try {
                Log.i(TAG, "prepare phone=$name address=$normalized")
                manager.connect(name, normalized)
                val deadline = SystemClock.elapsedRealtime() + 2500L
                while (SystemClock.elapsedRealtime() < deadline) {
                    if (manager.getConnectDeviceAddress()?.equals(normalized, true) == true) break
                    SystemClock.sleep(100)
                }
                runCatching { manager.connectA2dp(normalized) }
                runCatching { manager.connectHfp(normalized) }
                true
            } finally { runCatching { context.applicationContext.unbindService(connection) } }
        }.onFailure { Log.w(TAG, "prepareForCarPlay failed", it) }.getOrDefault(false)
    }
}
