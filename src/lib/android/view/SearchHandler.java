/**
 * 
 */
package lib.android.view;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import lib.common.model.json.JSONArray;

/**
 * @author yanry
 *
 *         2015年12月8日
 */
public abstract class SearchHandler implements OnFocusChangeListener, TextWatcher, OnEditorActionListener {
	private static final String PREF_KEY_SEARCH_HISTORY = SearchHandler.class.getName() + ".history";

	private EditText etSearch;
	private List<String> tempList;
	private JSONArray history;
	private SharedPreferences pref;
	private String currentSearch;
	private Activity activity;

	public SearchHandler(EditText etSearch, SharedPreferences pref, boolean useEditorAction, Activity activity) {
		this.etSearch = etSearch;
		this.pref = pref;
		this.activity = activity;
		tempList = new LinkedList<String>();
		String saved = pref.getString(PREF_KEY_SEARCH_HISTORY, null);
		if (saved == null) {
			history = new JSONArray();
		} else {
			history = new JSONArray(saved);
			for (int i = 0; i < history.length(); i++) {
				tempList.add(0, history.getString(i));
			}
		}

		etSearch.setOnFocusChangeListener(this);
		etSearch.addTextChangedListener(this);
		if (useEditorAction) {
			etSearch.setOnEditorActionListener(this);
		}
	}

	public List<String> getTempList() {
		return tempList;
	}

	public void clickSearch() {
		if (currentSearch != null && currentSearch.trim().length() > 0) {
			// 防止重复插入
			for (int j = 0; j < history.length(); j++) {
				if (history.getString(j).equals(currentSearch)) {
					history.remove(j);
				}
			}
			history.put(currentSearch);
			pref.edit().putString(PREF_KEY_SEARCH_HISTORY, history.toString()).commit();
			// 隐藏键盘
			hideKeyboard();
			showList(false);
			// 搜索
			onSearch(currentSearch);
		} 
	}

	public void clickListItem(int position) {
		currentSearch = tempList.get(position);
		// 需要改变位置
		tempList.remove(position);
		tempList.add(0, currentSearch);
		// 显示当前搜索的内容
		setSearchText(currentSearch);
		clickSearch();
	}

	public void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
	}

	public void setSearchText(String text) {
		etSearch.setText(text);
		etSearch.requestFocus();
		etSearch.setSelection(text.length());
	}

	public void clearList() {
		while (history.length() > 0) {
			history.remove(0);
		}
		pref.edit().putString(PREF_KEY_SEARCH_HISTORY, history.toString()).commit();
		tempList.clear();
		onListContentChange();
		showList(false);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		showList(hasFocus && tempList.size() > 0);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTextChanged(Editable s) {
		currentSearch = s.toString();
		tempList.clear();
		for (int i = history.length() - 1; i >= 0; i--) {
			String str = history.getString(i);
			if (str.indexOf(currentSearch) != -1) {
				tempList.add(str);
			}
		}
		onListContentChange();
		showList(tempList.size() > 0);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEARCH) {
			clickSearch();
		}
		return true;
	}

	public abstract void showList(boolean show);

	protected abstract void onListContentChange();

	protected abstract void onSearch(String key);
}
