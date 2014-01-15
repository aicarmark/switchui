package com.motorola.mmsp.motohomex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.motorola.mmsp.motohomex.AppsCustomizePagedView.CustomizeSearchString;
import com.motorola.mmsp.motohomex.apps.MotoAppsCustomizePagedView;
import com.motorola.mmsp.motohomex.R;
/**
 * 2012/05/10 Custom search bar shown when search icon in the tab is touched.
 * @author amt_moto_wangpeipei
 *
 */
public class AppsCustomizeSearchBar extends RelativeLayout implements TextWatcher, View.OnClickListener {

	private AppsCustomizeTabHost mAppsCustomizeTabHost;
	private EditText mSearchEditText;
	private ImageView clearIcon;
	//added by amt_wangpeipei 2012/07/31 for SWITCHUI-2507 begin.
	private ImageView mVoiceButton;
	private Context mContext;
	//added by amt_wangpeipei 2012/07/31 for SWITCHUI-2507 end.
	private AppsCustomizeFilter myFilter;
	
	/* modified by amt_wangpeipei 2012/07/31 for SWITCHUI-2507 begin.*/
	public AppsCustomizeSearchBar(Context context) {
		super(context);
		mContext = context;
	}

	public AppsCustomizeSearchBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public AppsCustomizeSearchBar(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	/* modified by amt_wangpeipei 2012/07/31 for SWITCHUI-2507 end.*/
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mSearchEditText = (EditText)findViewById(R.id.apps_customize_search_key);
		clearIcon = (ImageView)findViewById(R.id.clear_icon);
		clearIcon.setOnClickListener(this);
		setEditHint();
		myFilter = new AppsCustomizeFilter();
	}
	
	//added by amt_wangpeipei 2012/07/31 for SWITCHUI-2515 begin.
	public void showVoiceButton(){
		mVoiceButton = (ImageView) findViewById(R.id.apps_customize_voice_search_icon);
		if (isVoiceSearchExist()) {
			mVoiceButton.setVisibility(View.VISIBLE);
		} else {
			mVoiceButton.setVisibility(View.GONE);
		}
	}
	
	//added by amt_wangpeipei 2012/07/31 for SWITCHUI-2507 begin.
	private boolean isVoiceSearchExist() {
		Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
		List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(intent, 0);
		if(list == null || list.size() == 0){
			return false;
		}
		return true;
	}
	//added by amt_wangpeipei 2012/07/31 for SWITCHUI-2507 end.
	
	private void setEditHint(){
		mSearchEditText.setHint(R.string.app_menu_search_hint);
		mSearchEditText.setHintTextColor(Color.DKGRAY);
	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		if(!s.toString().isEmpty()){
			clearIcon.setVisibility(View.VISIBLE);
		}
		else{
			clearIcon.setVisibility(View.GONE);
			setEditHint();
		}
		if(mAppsCustomizeTabHost == null){
			return;
		}
		myFilter.filter(s);
	}

	public void setAppsCustomizeTabHost(AppsCustomizeTabHost tabHost){
		mAppsCustomizeTabHost = tabHost;
	}
	
	private class AppsCustomizeFilter extends Filter{
		private static final String FILTER_RESULT_WIDGET = "widget";
		private static final String FILTER_RESULT_APP = "app";
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			HashMap<String, ArrayList> mapResult = new HashMap<String, ArrayList>(2); 
			mapResult.put(FILTER_RESULT_APP, filterApps(constraint));
			mapResult.put(FILTER_RESULT_WIDGET, filterWidgets(constraint));
			
			FilterResults results = new FilterResults();
			results.values = mapResult;
			results.count = mapResult.size();
			return results;
		}

		private ArrayList<Object> filterWidgets(CharSequence prefix) {
			AppsCustomizePagedView customizePane = mAppsCustomizeTabHost.getAppsCustomizePane();
			if (prefix == null || prefix.length() == 0) {
				return customizePane.getOriginalWidgets();
			} else {
				//modified by amt_wangpeipei 2012/08/15 for switchui-2547 begin
				HashMap<CustomizeSearchString, Object> mWidgetNameMap = customizePane.getWidgetMap();
                String prefixString = prefix.toString().toUpperCase();
                final int count = mWidgetNameMap.size();
                final ArrayList<Object> newItems = new ArrayList<Object>(count);
                Set<Entry<CustomizeSearchString, Object>> set = mWidgetNameMap.entrySet();
                for (Entry<CustomizeSearchString, Object> entry : set) {
                	//ambiguous match
                	if(entry!= null && isAmbiguousMatch(entry.getKey(), prefixString)){
                		newItems.add(entry.getValue());
                	}
                    
                }
                //modified by amt_wangpeipei 2012/08/15 for switchui-2547 end
                return newItems;
            }
		}

		private ArrayList<ApplicationInfo> filterApps(CharSequence prefix) {
			AppsCustomizePagedView customizePane = mAppsCustomizeTabHost.getAppsCustomizePane();
			if (prefix == null || prefix.length() == 0) {
				return customizePane.getOriginalApps();
			} else {
				//modified by amt_wangpeipei 2012/08/15 for switchui-2547 begin
				HashMap<CustomizeSearchString, ApplicationInfo> mAppNameMap = customizePane.getAppMap();
                String prefixString = prefix.toString().toLowerCase();
                final int count = mAppNameMap.size();
                final ArrayList<ApplicationInfo> newItems = new ArrayList<ApplicationInfo>(count);
                Set<Entry<CustomizeSearchString, ApplicationInfo>> set = mAppNameMap.entrySet();
                if(set != null && set.size() > 0){
                	for (Entry<CustomizeSearchString, ApplicationInfo> entry : set) {
                		//ambiguous match
                		if(entry!= null && isAmbiguousMatch(entry.getKey(), prefixString)){
                			newItems.add(entry.getValue());
                		}
                		
                	}
                }
                //modified by amt_wangpeipei 2012/08/15 for switchui-2547 end
                return newItems;
			}
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			AppsCustomizePagedView appsCustomizePagedView = mAppsCustomizeTabHost.getAppsCustomizePane();
			HashMap<String, ArrayList> resultMap= (HashMap<String, ArrayList>)results.values;
			ArrayList<ApplicationInfo> appResult = (ArrayList<ApplicationInfo>)resultMap.get(FILTER_RESULT_APP);
			ArrayList<Object> widgetResult = (ArrayList<Object>)resultMap.get(FILTER_RESULT_WIDGET);
			// make sure an empty page can be shown when filter result is empty and avoid NullPointerException when update indicator position.
			if(appResult.size() == 0){
				appResult.add(null);
			}
			if(widgetResult.size() == 0){
				widgetResult.add(null);
			}
			//modified by amt_wangpeipei 2012/07/16 for switchui-2205 begin
			if (appsCustomizePagedView instanceof MotoAppsCustomizePagedView) {
				((MotoAppsCustomizePagedView) appsCustomizePagedView)
						.refreshFilterResult(appResult, widgetResult,
								mAppsCustomizeTabHost.getCurrentTab());
			}
			else{
				appsCustomizePagedView.refreshFilterResult(appResult,
						widgetResult, mAppsCustomizeTabHost.getCurrentTab());
			}
			//modified by amt_wangpeipei 2012/07/16 for switchui-2205 end
		}
		
	}

	public void clearSearchText() {
		mSearchEditText.setText("");
	}
	
	/**
	 * check if input string is ambiguous match source string.
	 * specific compare method: if input string has space, then split it and check if each of the splited item is contained in source string.
	 * 							else check if input string is contained in source string.
	 * @param source 	be matched string.
	 * @param input		input string.
	 * @return
	 */
	//modified by amt_wangpeipei 2012/08/15 for switchui-2547 begin
	public boolean isAmbiguousMatch(CustomizeSearchString searchString, String input) {
		String source = searchString.key;
		//modified by amt_wangpeipei 2012/07/16 for switchui-2547 end
		input = input.trim().toUpperCase();
		if(input.contains(" ")){
			String[] items = input.split(" ");
			for(int i = 0; i < items.length; i++){
				if(!source.contains(items[i])){
					return false;
				}
			}
			return true;
		}
		else if(source.contains(input)){
			return true;
		}
		return false;
	}


	public void hideInputMethod() {  
        InputMethodManager inputMethodManager =
            (InputMethodManager)mAppsCustomizeTabHost.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
    }
	
	public void showInputMethod(){
	    mSearchEditText.requestFocus();
            InputMethodManager inputMethodManager =
                (InputMethodManager)mAppsCustomizeTabHost.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	    /*Added by ncqp34 at Jul-13-2012 for switchui-2127*/
	    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
	    /*ended by ncqp34*/
            inputMethodManager.showSoftInput(mSearchEditText, 0);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.clear_icon:
			clearSearchText();
			break;
		default:
			break;
		}
	}

	public void addTextChangedListener() {
		mSearchEditText.addTextChangedListener(this);
	}
	
	public void removeTextChangedListener(){
		mSearchEditText.removeTextChangedListener(this);
	}
}
