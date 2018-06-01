/**
 * 
 */
package lib.android.view;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;

/**
 * @author yanry
 *
 *         2016年3月8日
 */
public class PasswordVisibilitySwitcher {
	private EditText etPwd;
	private boolean isVisible;

	public PasswordVisibilitySwitcher(EditText etPwd) {
		this.etPwd = etPwd;
		isVisible = isVisibleAtStart();
	}

	/**
	 * 
	 * @return is password visible after switch.
	 */
	public boolean switchVisibility() {
		if (!isVisible) {
			etPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
			isVisible = true;
		} else {
			etPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
			isVisible = false;
		}
		if (etPwd.getText().length() > 0) {
			etPwd.setSelection(etPwd.getText().length());
		}
		return isVisible;
	}

	/**
	 * Password is initially invisible by default. Override this to return true
	 * if you want a different initialization.
	 * 
	 * @return
	 */
	protected boolean isVisibleAtStart() {
		return false;
	}
}
