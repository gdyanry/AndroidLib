/**
 * 
 */
package lib.android.widget;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.PopupWindow;

import lib.android.entity.enums.Alignment;
import lib.android.entity.enums.Orientation;

/**
 * @author yanry
 *
 *         2016年3月2日
 */
public class ConvenientPopupWindow extends PopupWindow {

	public ConvenientPopupWindow(View contentView, int width, int height) {
		super(contentView, width, height);
		setFocusable(true);
		setBackgroundDrawable(new ColorDrawable(0x00000000));
	}

	public void showOutside(View anchor, Orientation relativeLocation, Alignment alignment, int padding) {
		showAsDropDown(anchor, getXOff(anchor, relativeLocation, alignment, padding, false),
				getYOff(anchor, relativeLocation, alignment, padding, false));
	}
	
	public void showInside(View anchor, Orientation relativeLocation, Alignment alignment, int padding) {
		showAsDropDown(anchor, getXOff(anchor, relativeLocation, alignment, padding, true),
				getYOff(anchor, relativeLocation, alignment, padding, true));
	}

	private int getXOff(View anchor, Orientation relativeLocation, Alignment alignment, int padding, boolean isInside) {
		switch (relativeLocation) {
		case TOP:
		case BOTTOM:
			if (alignment == Alignment.CENTER) {
				return (anchor.getWidth() - getWidth()) / 2;
			}
			if (alignment == Alignment.END) {
				return anchor.getWidth() - getWidth();
			}
		case LEFT:
			return isInside ? padding : -getWidth() - padding;
		case RIGHT:
			return isInside ? anchor.getWidth() - getWidth() - padding : anchor.getWidth() + padding;
		}
		return 0;
	}

	private int getYOff(View anchor, Orientation relativeLocation, Alignment alignment, int padding, boolean isInside) {
		switch (relativeLocation) {
		case TOP:
			return isInside ? padding - anchor.getHeight() : -getHeight() - anchor.getHeight() - padding;
		case BOTTOM:
			return isInside ? -padding : padding;
		case LEFT:
		case RIGHT:
			if (alignment == Alignment.CENTER) {
				return -(getHeight() + anchor.getHeight()) / 2;
			}
			if (alignment == Alignment.END) {
				return -getHeight();
			}
			return -anchor.getHeight();
		}
		return 0;
	}
}
