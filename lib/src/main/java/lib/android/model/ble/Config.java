package lib.android.model.ble;

import android.security.keystore.KeyProperties;

import java.util.UUID;

/**
 * Created by rongyu.yan on 2017/9/1.
 */

public interface Config {

    String CHARSET = "UTF-8";

    byte[] START_SIGNAL = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 83};

}
