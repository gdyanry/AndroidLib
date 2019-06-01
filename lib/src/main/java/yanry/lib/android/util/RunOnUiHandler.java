package yanry.lib.android.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import yanry.lib.android.interfaces.Consumer;

class RunOnUiHandler implements InvocationHandler, Runnable {
    private Object target;
    private Consumer<Exception> exceptionHandler;
    private Method method;
    private Object[] args;
    private Object result;

    RunOnUiHandler(Object target, Consumer<Exception> exceptionHandler) {
        this.target = target;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        this.method = method;
        this.args = args;
        CommonUtils.runOnUiThread(this);
        return result;
    }

    @Override
    public synchronized void run() {
        if (method != null) {
            try {
                result = method.invoke(target, args);
            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(e);
                }
            }
            method = null;
            args = null;
        }
    }

}
