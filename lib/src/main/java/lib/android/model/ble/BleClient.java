package lib.android.model.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import lib.android.model.PendingAction;
import lib.android.util.CommonUtils;
import lib.common.interfaces.Loggable;

/**
 * @author rongyu.yan
 * @date 2017/9/14
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleClient extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback, Loggable {
    private static final long ACTION_TIMEOUT = 8000;
    private static final int RETRY_TIMES_ON_FAIL = 5;
    private static final long RETRY_INTERVAL = 300;
    private BluetoothAdapter adapter;
    private Context context;
    private BluetoothGatt gatt;
    private HashMap<UUID, ByteArrayOutputStream> outputStreams;
    private HashMap<UUID, LinkedList<ByteArrayInputStream>> inputStreams;
    private BroadcastReceiver receiver;
    private boolean initState;
    private HashMap<BluetoothGattCharacteristic, PendingAction> pendingActions;

    public BleClient() {
        inputStreams = new HashMap<>();
        outputStreams = new HashMap<>();
        pendingActions = new HashMap<>();
    }

    public void init(Context context) {
        this.context = context;
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = manager.getAdapter();
        }
        if (adapter == null) {
        } else {
            initState = adapter.isEnabled();
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    if (state == BluetoothAdapter.STATE_ON) {
                        onBluetoothStateChange(true);
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        onBluetoothStateChange(false);
                    }
                }
            };
            context.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }
    }

    public BluetoothState getState() {
        if (adapter != null) {
            return adapter.isEnabled() ? BluetoothState.ENABLE : BluetoothState.DISABLE;
        }
        return BluetoothState.NOT_EXIST;
    }

    public void enable() {
        adapter.enable();
    }

    public boolean startScanning() {
        if (adapter.startLeScan(this)) {
            log("start BLE scanning...");
            onScanStateChange(true);
            return true;
        }
        return false;
    }

    public void stopScanning() {
        adapter.stopLeScan(BleClient.this);
        log("stop BLE scanning.");
        onScanStateChange(false);
    }

    public boolean connect(BluetoothDevice device) {
        if (adapter != null) {
            outputStreams.clear();
            inputStreams.clear();
            log("Trying to create a new connection.");
            // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
            gatt = new BleConnectionCompat(context).connectGatt(device, false, this);
            return gatt != null;
        }
        return false;
    }

    public void sendData(UUID serviceId, UUID characteristicId, String data) {
        if (gatt == null) {
            log("gatt is null.");
            onConnectionError();
        } else {
            BluetoothGattService service = gatt.getService(serviceId);
            if (service == null) {
                log("get service fail: %s", serviceId);
                onConnectionError();
            } else {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
                if (characteristic == null) {
                    log("get characteristic fail: %s", characteristicId);
                    onConnectionError();
                } else if (gatt.setCharacteristicNotification(characteristic, true)) {
                    if (data == null) {
                        log("trying to read characteristic: %s.", characteristic.getUuid());
                        CommonUtils.retryOnFail(RETRY_TIMES_ON_FAIL, RETRY_INTERVAL, () -> handleCharacteristic(gatt, characteristic, true), () -> {
                            log("read characteristic fail.");
                            onConnectionError();
                        });
                    } else {
                        log("sending data (%s): %s", characteristicId.toString().substring(0, 3), data);
                        try {
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(Config.CHARSET));
                            synchronized (characteristicId) {
                                LinkedList<ByteArrayInputStream> queue = inputStreams.get(characteristicId);
                                if (queue == null) {
                                    queue = new LinkedList<>();
                                    inputStreams.put(characteristicId, queue);
                                }
                                queue.offer(inputStream);
                                characteristic.setValue(Config.START_SIGNAL);
                                writeCharacteristic(gatt, characteristic);
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    log("set characteristic notification fail.");
                    onConnectionError();
                }
            }
        }
    }

    private void writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        CommonUtils.retryOnFail(RETRY_TIMES_ON_FAIL, RETRY_INTERVAL, () -> handleCharacteristic(gatt, characteristic, false), () -> {
            log("write characteristic fail.");
            onConnectionError();
        });
    }

    private boolean handleCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean isRead) {
        if (isRead ? gatt.readCharacteristic(characteristic) : gatt.writeCharacteristic(characteristic)) {
            getPendingAction(characteristic).setup(ACTION_TIMEOUT);
            return true;
        }
        return false;
    }

    private PendingAction getPendingAction(BluetoothGattCharacteristic characteristic) {
        PendingAction pendingAction = pendingActions.get(characteristic);
        if (pendingAction == null) {
            pendingAction = new PendingAction() {
                @Override
                protected void onFinish(boolean isTimeout) {
                    if (isTimeout) {
                        onConnectionError();
                    }
                }

                @Override
                public void log(String msg, Object... args) {
                    BleClient.this.log(msg, args);
                }
            };
            pendingActions.put(characteristic, pendingAction);
        }
        return pendingAction;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onBtConnectionLoss(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (gatt != null) {
            log("disconnect gatt.");
            gatt.disconnect();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void release() {
        closeGatt();
        if (context != null && receiver != null) {
            context.unregisterReceiver(receiver);
            receiver = null;
            context = null;
        }
        if (adapter != null) {
            if (initState && !adapter.isEnabled()) {
                adapter.enable();
            } else if (!initState && adapter.isEnabled()) {
                adapter.disable();
            }
        }
    }

    private void closeGatt() {
        inputStreams.clear();
        outputStreams.clear();
        pendingActions.clear();
        if (gatt != null) {
            log("close gatt.");
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                log("bluetooth is connected.");
                BleClient.this.onConnectionStateChange(ConnectionState.Connected);
                // Attempts to discover services after successful connection.
                log("Attempting to start service discovery:" + gatt.discoverServices());
                break;
            case BluetoothProfile.STATE_CONNECTING:
                BleClient.this.onConnectionStateChange(ConnectionState.Connecting);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                log("bluetooth is disconnected.");
                closeGatt();
                BleClient.this.onConnectionStateChange(ConnectionState.Disconnected);
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                BleClient.this.onConnectionStateChange(ConnectionState.Disconnecting);
                break;
            default:
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            List<BluetoothGattService> services = gatt.getServices();
            log("discover %s services.", services.size());
            BleClient.this.onDiscoverServices(services);
        } else {
            log("discover services fail: %s.", status);
            onConnectionError();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        PendingAction pendingAction = getPendingAction(characteristic);
        if (pendingAction.isPending()) {
            pendingAction.knock(null);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("read characteristic(%s) fail: %s.", characteristic.getUuid(), status);
                onConnectionError();
            }
        } else {
            log("receive read characteristic result beyond timeout: %s!", status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        PendingAction pendingAction = getPendingAction(characteristic);
        if (pendingAction.isPending()) {
            pendingAction.knock(null);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                UUID uuid = characteristic.getUuid();
                synchronized (uuid) {
                    LinkedList<ByteArrayInputStream> queue = inputStreams.get(uuid);
                    if (queue != null) {
                        ByteArrayInputStream peek = queue.peek();
                        if (peek != null) {
                            int available = peek.available();
                            byte[] buf = new byte[available > 19 ? 20 : available + 1];
                            peek.read(buf, 1, buf.length - 1);
                            available = peek.available();
                            int remainPackage = available / 19 + ((available % 19 > 0) ? 1 : 0);
                            byte flag = (byte) remainPackage;
                            if (flag == 0 && remainPackage != 0) {
                                flag = Byte.MIN_VALUE;
                            }
                            buf[0] = flag;
                            if (available == 0) {
                                inputStreams.remove(uuid);
                            }
                            characteristic.setValue(buf);
                            // 写入下一个包
                            writeCharacteristic(gatt, characteristic);
                        }
                    }
                }
            } else {
                log("write characteristic(%s) fail: %s.", characteristic.getUuid(), status);
                onConnectionError();
            }
        } else {
            log("receive write characteristic result beyond timeout: %s!", status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == BleClient.this.gatt) {
            byte[] value = characteristic.getValue();
            UUID uuid = characteristic.getUuid();
            log("receive %s: %s", uuid.toString().substring(0, 3), Arrays.toString(value));
            if (Arrays.equals(value, Config.START_SIGNAL)) {
                outputStreams.put(uuid, new ByteArrayOutputStream());
            } else if (outputStreams.containsKey(uuid)) {
                ByteArrayOutputStream outputStream = outputStreams.get(uuid);
                outputStream.write(value, 1, value.length - 1);
                if (value[0] == 0) {
                    try {
                        String data = outputStream.toString(Config.CHARSET);
                        log("receive data: " + data);
                        onReceiveData(uuid, data);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    outputStreams.remove(uuid);
                }
            } else {
                onValueChanged(characteristic);
            }
        }
    }

    protected abstract void onBluetoothStateChange(boolean isEnabled);

    /**
     * Called on main thread.
     *
     * @param isScanning
     */
    protected abstract void onScanStateChange(boolean isScanning);

    /**
     * Called on Binder thread.
     *
     * @param state
     */
    protected abstract void onConnectionStateChange(ConnectionState state);

    /**
     * Called on Binder thread.
     *
     * @param services
     */
    protected abstract void onDiscoverServices(List<BluetoothGattService> services);

    /**
     * Called on Binder thread.
     *
     * @param characteristicId
     * @param data
     */
    protected abstract void onReceiveData(UUID characteristicId, String data);

    /**
     * Called on Binder thread.
     *
     * @param characteristic
     */
    protected abstract void onValueChanged(BluetoothGattCharacteristic characteristic);

    /**
     * Called on Binder thread.
     */
    protected abstract void onConnectionError();

}
