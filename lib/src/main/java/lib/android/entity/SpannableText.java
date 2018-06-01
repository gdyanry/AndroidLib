/**
 * 
 */
package lib.android.entity;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * @author yanry
 *
 *         2016年1月13日
 */
public class SpannableText {
	private SpannableStringBuilder ssb;

	public SpannableText() {
		ssb = new SpannableStringBuilder();
	}

	public SpannableText append(CharSequence plainText) {
		ssb.append(plainText);
		return this;
	}

	public SpannableText append(CharSequence text, Object span) {
		if (text.length() > 0) {
			int start = ssb.length();
			ssb.append(text);
			ssb.setSpan(span, start, start + text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		}
		return this;
	}

	public SpannableStringBuilder getBackedBuilder() {
		return ssb;
	}

	public void buildTo(TextView tv) {
		tv.setText(ssb);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setFocusable(false);
		tv.setClickable(false);
		tv.setLongClickable(false);
	}
}
