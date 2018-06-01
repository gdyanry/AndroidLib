/**
 * 
 */
package lib.android.view;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Submit button is switched to enable data_state only when no input field is empty.
 * @author yanry
 *
 *         2016年3月7日
 */
public abstract class SubmitButtonStateAgent {
	private int inputFlag;
	private int fullValue;

	public SubmitButtonStateAgent(EditText... editTexts) {
		for (int i = 0; i < editTexts.length; i++) {
			EditText et = editTexts[i];
			final int flag = 1 << i;
			fullValue += flag;
			et.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO Auto-generated method stub

				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// TODO Auto-generated method stub

				}

				@Override
				public void afterTextChanged(Editable s) {
					handleButtonState(s, flag);
				}
			});
		}
	}
	
	public boolean isEnable() {
		return inputFlag == fullValue;
	}

	private void handleButtonState(Editable s, int flag) {
		if (s.length() > 0) {
			inputFlag |= flag;
			if (inputFlag == fullValue) {
				changeButtonState(true);
			}
		} else {
			boolean isEnable = inputFlag == fullValue;
			inputFlag &= ~flag;
			if (isEnable) {
				isEnable = false;
				changeButtonState(false);
			}
		}
	}

	protected abstract void changeButtonState(boolean enable);
}
