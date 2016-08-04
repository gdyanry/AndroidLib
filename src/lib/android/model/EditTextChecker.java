/**
 * 
 */
package lib.android.model;

import android.widget.EditText;
import lib.common.model.StringMatcher;

/**
 * @author yanry
 *
 *         2015年12月30日
 */
public class EditTextChecker {
	private boolean fail;

	public EditTextChecker check(EditText et, StringMatcher matcher) {
		if (!fail) {
			String text = et.getText().toString();
			fail |= !matcher.test(text);
		}
		return this;
	}

	public boolean isPass() {
		boolean pass = !fail;
		fail = false;
		return pass;
	}
}
