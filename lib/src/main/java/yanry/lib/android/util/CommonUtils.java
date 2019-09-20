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
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import yanry.lib.android.entity.MainHandler;
import yanry.lib.android.interfaces.BooleanSupplier;
import yanry.lib.android.interfaces.Consumer;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;

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

    public <T> T getRunOnUiProxy(T target, Class<T> targetInterface, Consumer<Exception> exceptionHandler) {
        Class<?>[] interfaces = {targetInterface};
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), interfaces, new RunOnUiHandler(target, exceptionHandler));
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

    public static void runOnUiThread(Runnable task) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            task.run();
        } else {
            Singletons.get(MainHandler.class).removeCallbacks(task);
            Singletons.get(MainHandler.class).post(task);
        }
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

    public static void retryOnFail(int tryTimes, long interval, BooleanSupplier action, Runnable onFail) {
        if (!action.get()) {
            if (--tryTimes > 0) {
                int finalTryTimes = tryTimes;
                Singletons.get(MainHandler.class).postDelayed(() -> retryOnFail(finalTryTimes, interval, action, onFail), interval);
            } else {
                onFail.run();
            }
        }
    }

    public static void scheduleTimeout(Runnable task, long delay) {
        MainHandler mainHandler = Singletons.get(MainHandler.class);
        mainHandler.removeCallbacks(task);
        mainHandler.postDelayed(task, delay);
    }

    public static void cancelPendingTimeout(Runnable task) {
        Singletons.get(MainHandler.class).removeCallbacks(task);
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
                    }
                }
            } catch (Exception e) {
                Logger.getDefault().catches(e);
            }
        }
    }
}
