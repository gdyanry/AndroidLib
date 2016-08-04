/**
 * 
 */
package lib.android.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import lib.android.util.CommonUtils;

/**
 * @author yanry
 *
 *         2016年6月11日
 */
public abstract class UiTaskProxyFactory {

	/**
	 * 
	 * @param target
	 *            the object that needs proxy.
	 * @param targetInterface
	 *            the interface type that needs proxy.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxy(T target, Class<T> targetInterface) {
		Class<?>[] interfaces = { targetInterface };
		return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), interfaces, new RunOnUiHandler(target));
	}

	protected abstract void handlerException(Exception e);

	private class RunOnUiHandler implements InvocationHandler, Runnable {
		private Object target;
		private Method method;
		private Object[] args;

		private RunOnUiHandler(Object target) {
			this.target = target;
		}

		@Override
		public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			this.method = method;
			this.args = args;
			CommonUtils.runOnUiThread(this);
			// TODO return value is not handled
			return null;
		}

		@Override
		public synchronized void run() {
			if (method != null) {
				try {
					method.invoke(target, args);
				} catch (Exception e) {
					handlerException(e);
				}
				method = null;
				args = null;
			}
		}

	}
}
