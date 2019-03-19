package yanry.lib.android.model.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by rongyu.yan on 2017/9/18.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class BleServer extends BluetoothGattServerCallback {

    private BluetoothManager manager;
    private BluetoothAdapter adapter;
    private BluetoothGattServer gattServer;
    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;
    private HashMap<BluetoothDevice, ByteArrayOutputStream> outputStreams;

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BleServer() {
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            }

            @Override
            public void onStartFailure(int errorCode) {
            }
        };
        outputStreams = new HashMap<>();
    }

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BluetoothState init(Context context, BluetoothGattService... services) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = manager.getAdapter();
            if (adapter != null) {
                if (adapter.isEnabled()) {
                    gattServer = manager.openGattServer(context, this);
                    AdvertiseData.Builder builder = new AdvertiseData.Builder().setIncludeTxPowerLevel(true);
                    for (BluetoothGattService service : services) {
                        builder.addServiceUuid(new ParcelUuid(service.getUuid()));
                        gattServer.addService(service);
                    }

                    if (adapter.isMultipleAdvertisementSupported()) {
                        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                                .setConnectable(true)
                                .build();
                        AdvertiseData advertiseData = builder.build();
                        AdvertiseData scanResponse = new AdvertiseData.Builder()
                                .setIncludeDeviceName(true)
                                .build();
                        advertiser = adapter.getBluetoothLeAdvertiser();
                        advertiser.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback);
                    }
                }
                return adapter.isEnabled() ? BluetoothState.ENABLE : BluetoothState.DISABLE;
            }
        }
        return BluetoothState.NOT_EXIST;
    }

    public boolean updateCharacteristic(BluetoothDevice device, UUID serviceId, UUID characteristicId, byte[] value) {
        BluetoothGattService service = gattServer.getService(serviceId);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
            if (characteristic != null) {
                characteristic.setValue(value);
                List<BluetoothDevice> connectedDevices = manager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
                if (device == null) {
                    for (BluetoothDevice d : connectedDevices) {
                        synchronized (d) {
                            gattServer.notifyCharacteristicChanged(d, characteristic, true);
                        }
                    }
                } else {
                    synchronized (device) {
                        gattServer.notifyCharacteristicChanged(device, characteristic, true);
                    }
                }
                return true;
            }
        }
        return false;
    }

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stop() {
        if (gattServer != null) {
            gattServer.close();
        }
        if (adapter.isEnabled() && advertiser != null) {
            // If stopAdvertising() gets called before close() a null
            // pointer exception is raised.
            advertiser.stopAdvertising(advertiseCallback);
        }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        String response = processRequest(device, characteristic.getUuid(), null);
        if (response != null) {
            sendResponse(device, characteristic, response);
        }
    }

    private boolean sendResponse(BluetoothDevice device, BluetoothGattCharacteristic characteristic, String response) {
        if (response != null) {
            try {
                byte[] bytes = response.getBytes(Config.CHARSET);
                long time = System.currentTimeMillis();
                synchronized (device) {
                    characteristic.setValue(Config.START_SIGNAL);
                    if (gattServer.notifyCharacteristicChanged(device, characteristic, false)) {
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                        byte[] buf = new byte[bytes.length > 19 ? 20 : bytes.length + 1];
                        while (inputStream.read(buf, 1, buf.length - 1) != -1) {
                            int available = inputStream.available();
                            int remainPackage = available / 19 + ((available % 19 > 0) ? 1 : 0);
                            byte flag = (byte) remainPackage;
                            if (flag == 0 && remainPackage != 0) {
                                flag = Byte.MIN_VALUE;
                            }
                            buf[0] = flag;
                            characteristic.setValue(buf);
                            if (available > 19) {
                                buf = new byte[20];
                            } else if (available > 0) {
                                buf = new byte[available + 1];
                            }
                            // 休息一会以防止对方接收速度跟不上而丢包
                            Thread.sleep(2);
                        }
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public synchronized void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                          BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                          int offset, byte[] value) {
        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        if (Arrays.equals(value, Config.START_SIGNAL)) {
            outputStreams.put(device, new ByteArrayOutputStream());
        } else {
            ByteArrayOutputStream outputStream = outputStreams.get(device);
            if (outputStream != null) {
                try {
                    outputStream.write(value, 1, value.length - 1);
                    if (value[0] == 0) {
                        String requestData = outputStream.toString(Config.CHARSET);
                        sendResponse(device, characteristic, processRequest(device, characteristic.getUuid(), requestData));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected abstract String processRequest(BluetoothDevice device, UUID characteristicId, String requestData);
}
