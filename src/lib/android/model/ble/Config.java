package lib.android.model.ble;

import android.security.keystore.KeyProperties;

import java.util.UUID;

/**
 * Created by rongyu.yan on 2017/9/1.
 */

public interface Config {
    UUID SERVICE_UUID = UUID.fromString("8235a8e8-c6cb-48ea-bae2-3d2431384478");
    UUID CHARACTERISTIC_UUID_GET_WIFI_LIST = UUID.fromString("c0df6540-40d1-4bbe-8a8a-1dde7b26d639");
    //    UUID CHARACTERISTIC_UUID_GET_WIFI_LIST = UUID.fromString("b356962b-8a0f-4b3f-81d6-111d61fd105c");
    UUID CHARACTERISTIC_UUID_CONFIG_WIFI = UUID.fromString("ea8229fd-92ed-49aa-8c9c-1aca5c989d73");
    UUID CHARACTERISTIC_UUID_CONNECTIVITY = UUID.fromString("c5816f6f-4b00-47ad-8fd6-8915ffe262b3");
    UUID CHARACTERISTIC_UUID_QUIT = UUID.fromString("163456BD-0F5A-80F2-EC0A-B6E04A9E7B4D");
    UUID CHARACTERISTIC_UUID_CONFIG_WIFI_V2 = UUID.fromString("21a0e1dd-9ee1-4c89-ad72-bc5324e161b9");
    UUID CHARACTERISTIC_UUID_GET_DEVICE_INFO = UUID.fromString("9477bb5f-cf34-4b00-9781-dfd24a473ef8");

    String AES_KEY = "ffe5484db0d4498b";
    String AES_IV = "ecda10b17c724c26";
    byte[] AES_KEY_BYTES = new byte[]{102, 102, 101, 53, 52, 56, 52, 100, 98, 48, 100, 52, 52, 57, 56, 98};
    byte[] AES_IV_BYTES = new byte[]{101, 99, 100, 97, 49, 48, 98, 49, 55, 99, 55, 50, 52, 99, 50, 54};
    String AES_TRANSFORMATION = String.format("%s/%s/%s", KeyProperties.KEY_ALGORITHM_AES, KeyProperties.BLOCK_MODE_CBC, KeyProperties.ENCRYPTION_PADDING_PKCS7);

    byte STATE_DISCONNECTED = 1;
    byte STATE_ASSOCIATING = 2;
    byte STATE_CONNECTED = 3;
    byte STATE_RESTRICTED_CONNECTED = 4;
    byte STATE_PENDING_CONNECTED = 5;
    byte STATE_ADD_NETWORK_FAIL = 6;
    byte STATE_MQTT_SUCCESS = 20;

    String CHARSET = "UTF-8";
    String AP_SSID = "Phicomm_R1";

    int AP_PORT = 8989;

    long BT_SCAN_TIMEOUT = 30000;
    long AP_SCAN_TIMEOUT = 30000;
    long REQUEST_TIMEOUT = 30000;
    long BIND_DEVICE_TIMEOUT = 30000;

    byte[] START_SIGNAL = new byte[]{83};

    int WIFI_PAGE_SIZE = 30;

    ////// 功能//////////////
    String AROUSE_WORD = "小讯小讯";
    int FM_DATA_PAGE_SIZE = 50;


    ////////其他/////////////
    long COMMON_REQUEST_TIMEOUT = 10000;
    int ERR_CODE_TIMEOUT = -1;
    int ERR_CODE_DATA_ERROR = -2;
}
