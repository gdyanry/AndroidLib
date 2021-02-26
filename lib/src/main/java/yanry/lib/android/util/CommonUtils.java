/**
 *
 */
package yanry.lib.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.util.HexUtil;
import yanry.lib.java.util.IOUtil;

/**
 * @author yanry
 *
 *         2014年8月11日 下午2:18:58
 */
public class CommonUtils {

    public static boolean launchApp(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            return false;
        }
        context.startActivity(intent);
        return true;
    }

    public static void installApk(Context ctx, File apk) {
        if (apk.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        }
    }

    public static boolean requestCamera(Activity ctx, File out, int requestCode) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(ctx.getPackageManager()) != null) {
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(out));
            ctx.startActivityForResult(cameraIntent, requestCode);
            return true;
        }
        Logger.getDefault().ee("no system camera found.");
        return false;
    }

    /**
     * 记得在manifest file中添加"android.permission.READ_PHONE_STATE"权限
     */
    public static String getPhoneNo(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1Number();
    }

    /**
     * 获取mac地址（适配所有Android版本）
     *
     * @return
     */
    public static String getMac(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Android 6.0 之前（不包括6.0）获取mac地址
            // 必须的权限 <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"></uses-permission>
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            if (info != null) {
                return info.getMacAddress();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            try {
                return IOUtil.streamToString(Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address").getInputStream(), "utf-8");
            } catch (IOException e) {
                Logger.getDefault().catches(e);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                if (networkInterfaces != null) {
                    while (networkInterfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = networkInterfaces.nextElement();
                        if ("wlan0".equalsIgnoreCase(networkInterface.getName())) {
                            return HexUtil.bytesToHex(":", networkInterface.getHardwareAddress());
                        }
                    }
                }
            } catch (SocketException e) {
                Logger.getDefault().catches(e);
            }
        }
        Logger.getDefault().concat(LogLevel.Error, "fail to get mac address for sdk version: ", Build.VERSION.SDK_INT);
        return null;
    }

    public static int dip2px(float dipValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue,
                Resources.getSystem().getDisplayMetrics());
    }

    public static int getWindowDimension(boolean width) {
        return width ? Resources.getSystem().getDisplayMetrics().widthPixels
                : Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static File getDiskCacheDir(Context context) {
        File dir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            dir = context.getExternalCacheDir();
        }
        if (dir == null) {
            dir = context.getCacheDir();
        }
        return dir;
    }

    public static Bitmap drawText(String text, Paint paint) {
        Bitmap bm = Bitmap.createBitmap((int) Math.ceil(paint.measureText(text)),
                (int) Math.ceil(paint.descent() - paint.ascent()), Config.ALPHA_8);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bm);
        canvas.drawText(text, 0, 0, paint);
        return bm;
    }

    public static Activity getActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext());
        } else {
            return null;
        }
    }

    /**
     * 修复InputMethodManager导致的内存泄露，在界面关闭时调用此方法。
     *
     * @param leakedView
     */
    public static void fixInputMethodMemoryLeak(View leakedView) {
        if (leakedView == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) leakedView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null) {
            return;
        }
        String[] viewArr = new String[]{"mCurRootView", "mServedView", "mNextServedView"};
        for (String view : viewArr) {
            try {
                Field field = inputMethodManager.getClass().getDeclaredField(view);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object fieldObj = field.get(inputMethodManager);
                if (fieldObj != null && fieldObj instanceof View) {
                    View fieldView = (View) fieldObj;
                    if (fieldView == leakedView) {
                        field.set(inputMethodManager, null);
                        Logger.getDefault().dd("fix memory leak from InputMethodManager: ", view, "=", fieldView);
                    }
                }
            } catch (Exception e) {
                Logger.getDefault().catches(e);
            }
        }
    }

    public static void fixInputMethodMemoryLeak(Activity activity) {
        if (activity == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null) {
            return;
        }
        String[] viewArr = new String[]{"mCurRootView", "mServedView", "mNextServedView"};
        for (String view : viewArr) {
            try {
                Field field = inputMethodManager.getClass().getDeclaredField(view);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object fieldObj = field.get(inputMethodManager);
                if (fieldObj != null && fieldObj instanceof View) {
                    View fieldView = (View) fieldObj;
                    if (fieldView.getContext() == activity) {
                        field.set(inputMethodManager, null);
                        Logger.getDefault().dd("fix memory leak from InputMethodManager: ", view, "=", fieldView);
                    }
                }
            } catch (Exception e) {
                Logger.getDefault().catches(e);
            }
        }
    }
}
