package com.motorola.mmsp.motohomex.apps;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.motorola.mmsp.motohomex.ApplicationInfo;
import com.motorola.mmsp.motohomex.AppsCustomizePagedView;
import com.motorola.mmsp.motohomex.AppsCustomizeTabHost;
import com.motorola.mmsp.motohomex.LauncherApplication;
import com.motorola.mmsp.motohomex.LauncherModel;
import com.motorola.mmsp.motohomex.LauncherSettings;
import com.motorola.mmsp.motohomex.PagedViewCellLayout;
import com.motorola.mmsp.motohomex.PagedViewIcon;
import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;
import com.motorola.mmsp.motohomex.util.FilteredArrayList;
/*2012-7-12 add by Hu ShuAn for switchui-2061 start*/
import com.motorola.mmsp.motohomex.HolographicOutlineHelper;
/*2012-7-12 add by Hu ShuAn end */

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
/*2012-7-12 add by Hu ShuAn for switchui-2061 start*/
import android.view.LayoutInflater;
/*2012-7-12 add by Hu ShuAn end */
/*Added by ncqp34 at Jul-25-2012 for menu-order flex*/
import com.motorola.mmsp.motohomex.Flex;
/*ended by ncqp34*/
public class MotoAppsCustomizePagedView extends AppsCustomizePagedView {

    /** Model manages apps groups and other data used by applications on device. */
    AppsModel mAppsModel;
    GroupItem mCurrentGroupItem;
    ArrayList<ApplicationInfo> mAllApps;
    /*2012-7-12 add by Hu ShuAn for switchui-2061 start*/
    HolographicOutlineHelper mHolographicOutlineHelper;
    LayoutInflater mLayoutInflater;
    /*2012-7-12 add by Hu ShuAn end */

    public MotoAppsCustomizePagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAllApps = new ArrayList<ApplicationInfo>();
        /*2012-7-12 add by Hu ShuAn for switchui-2061 start*/
        mLayoutInflater = LayoutInflater.from(getContext());
        mHolographicOutlineHelper = new HolographicOutlineHelper();
        /*2012-7-12 add by Hu ShuAn end */
    }

    @Override
    protected void init() {
        super.init();
        mAppsModel = ((LauncherApplication) getContext().getApplicationContext()).getAppsModel();
    }

    public void setGroup(GroupItem groupItem){
        mCurrentGroupItem = groupItem;
    }

    void refreshPage(){
        if (mAllApps.size() > 0){
            mApps = rebuildFilteredApps(mAllApps);
            // You need update the page count first
            updatePageCounts();
            // then you invalidate the pages.
            // The invalidade calls syncPage to
            // set the right number
            // of pages for apps and widgets
            //modified by amt_wangpeipei 2012/07/24 for switchui-2435 begin
            String tabTag = getTabHost().getCurrentTabTag();
            if(AppsCustomizeTabHost.WIDGETS_TAB_TAG.equals(tabTag)){
            	getTabHost().setCurrentTabByTag(AppsCustomizeTabHost.APPS_TAB_TAG);
            }
            else{
            	invalidatePageData(0, true);
            }
            //modified by amt_wangpeipei 2012/07/24 for switchui-2435 end.
        }
    }

    public AllAppsPage getAllAppsPage(){
        return (AllAppsPage)getParent();
    }

    private ArrayList<ApplicationInfo> rebuildFilteredApps(ArrayList<ApplicationInfo> apps) {
        FilteredArrayList<ApplicationInfo> filteredApps = new FilteredArrayList<ApplicationInfo>();

        // Filter and sort the apps according to the group settings
        if (null != mCurrentGroupItem) {
            filteredApps.setFilter(mAppsModel.getMatchingIndexes(apps, mCurrentGroupItem));
        }
        filteredApps.setArray(apps);
        return filteredApps.getFilterList();//filteredApps;
    }

    public static final Comparator<ApplicationInfo> APP_NAME_COMPARATOR
            = new Comparator<ApplicationInfo>() {
                    @Override
                    public final int compare(ApplicationInfo a, ApplicationInfo b) {
                        return Collator.getInstance().compare(a.title.toString(), b.title.toString());
                    }
            };

    @Override
    public void setApps(ArrayList<ApplicationInfo> list) {
        // clear the arrays
        mAllApps.clear();
        mApps.clear();

        // Add the applications
        mAllApps.addAll(list);
        // Now, sort
	/*Added by ncqp34 at Jul-24-2012 for menu-order flex*/
	/*Added by ncqp34 at Mar-23-2012 for fake app*/
        LauncherApplication app = (LauncherApplication)getContext().getApplicationContext();
	//ArrayList<ApplicationInfo> fakeList = app.getFakeModel().getAppList(getContext(), list);
	//if(fakeList != null){
            //mAllApps.addAll(fakeList);
	//}
	/*ended by ncqp34*/
	/*Added by ncqp34 at Jan-06-2012 for menuOrder flex update*/
    //Collections.sort(mAllApps, LauncherModel.APP_NAME_COMPARATOR);
	Collections.sort(mAllApps, Flex.getMenuOrderComparator(getContext()));
	/*ended by ncqp34*/
	/*ended by ncqp34*/

        doBindAppsAdded();
        mApps = rebuildFilteredApps(mAllApps);

        //modified by amt_wangpeipei 2012/06/04 to save initial apps and initialize mAppNameMap
        //begin.
        mOriginalApps = mApps;
        setAppMap(mApps.size());
        //modified by amt_wangpeipei 2012/06/04 end.

        updatePageCounts();

      // The next layout pass will trigger data-ready if both widgets and apps are set, so
      // request a layout to do this test and invalidate the page data when ready.
        
		// modified by amt_wangpeipei 2012/08/20 for switchui-2555 begin
		invalidateOnDataChange();
		// modified by amt_wangpeipei 2012/08/20 for switchui-2555 end
    }

    @Override
    protected boolean testDataReady() {
        // Use mAllApps instead of mApps.
        return !mAllApps.isEmpty() && !mWidgets.isEmpty();
    }

    @Override
    protected void updatePageCounts() {
        super.updatePageCounts();
        // The number of pages cannot be 0, but a group can have no
        // applications
        mNumAppsPages = mNumAppsPages < 1 ? 1 : mNumAppsPages;
    }

    /**
     * Helper function used by callbacks to process newly-added apps.
     */
    private void doBindAppsAdded() {
        final int N = mAllApps.size();
        if (N == 0) {
            return;
        }
        // Get the system folders
        for (int i = 0; i < N; ++i) {
            final ApplicationInfo item = mAllApps.get(i);
            long folderId = AppItem.INVALID_ID;
            int index = Collections.binarySearch(mApps, item, LauncherModel.APP_NAME_COMPARATOR);
            if (index < 0) {
                index = -(index+1);
            }
            // Add the applications to the folders if needed
            AppItem appItem = new AppItem(item.componentName, !item.isSystem, folderId);
            mAppsModel.addToAppMap(item.componentName, appItem);
        }
    }

    protected void refreshFrequentGroup() {
        // Refresh the page if the group is frequent
        // because an application was launched
        if (mCurrentGroupItem != null &&
            mCurrentGroupItem.getType() == Groups.TYPE_FREQUENTS){
            // refreshPage needs to be called on main thread
            post(new Runnable()
                {
                    public void run()
                    {
                        refreshPage();
                    }
                });
        }
    }

    public void setObscure(boolean obscure){
        setAlpha(obscure ? 0.5f:1f);
    }

    public int getAppsListSize(){
        return mAllApps.size();
    }
    
    public ApplicationInfo getAppsListItem(int index){
        return mAllApps.get(index);
    }

    public boolean groupAppsListContains(ApplicationInfo info){
       return mApps.contains(info);
    }

    public ApplicationInfo getAppInfo(ComponentName component) {
        final int N = mApps.size();
        for (int i=0; i<N; i++) {
            ApplicationInfo appInfo = mApps.get(i);
            if (appInfo.intent.getComponent().equals(component)) {
                return appInfo;
            }
        }
        return null;
    }

    @Override
    public boolean onLongClick(View v) {
        boolean retVal = false;
        if (v instanceof PagedViewIcon){
            // Applications
            retVal = getAllAppsPage().onLongClick(v);
            if (!retVal) {
                retVal = super.onLongClick(v);
            }
        } else {
            // Widgets
            retVal = super.onLongClick(v);
        }
        return retVal;
    }

    @Override
    public void onClick(View v) {
        // When we have exited all apps or are in transition, disregard clicks
        if (!mLauncher.isAllAppsCustomizeOpen() ||
                mLauncher.getWorkspace().isSwitchingState()) return;

/*      IKYTZWE-762  improve KPI of launch app from main menu, below code looks for not used feature
        if (!getAllAppsPage().onBackPressed()){
            if (v instanceof PagedViewIcon) {
                // Animate some feedback to the click
                final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();
                if (appInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SYSTEM_FOLDER){
                    final Rect outRect = new Rect();
                    v.getHitRect(outRect);
                    animateClickFeedback(v, new Runnable() {
                        @Override
                        public void run() {
                            openFolder((SystemFolderInfo)appInfo, outRect);
                        }
                    });

                    return;
                }
            }
        }
*/
        super.onClick(v);
    }

    @Override
    public boolean beginDragging(View v) {
        getAllAppsPage().onBackPressed();
        return super.beginDragging(v);
    }

    @Override
    protected void removeAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // loop through all the apps and remove apps that have the same component
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            int removeIndex = findAppByComponent(mAllApps, info);
            if (removeIndex > -1) {
                mAllApps.remove(removeIndex);
            }
        }
        // Now update the indexes
        mApps = rebuildFilteredApps(mAllApps);
    }

    @Override
    protected void addAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // We add it in place, in alphabetical order
        int count = list.size();
        for (int i = 0; i < count; ++i) {
            ApplicationInfo info = list.get(i);
	    /*Added by ncqp34 at Jan-06-2012 for menuOrder flex update*/
            //int index = Collections.binarySearch(mAllApps, info, LauncherModel.APP_NAME_COMPARATOR);
            int index = Collections.binarySearch(mAllApps, info, Flex.getMenuOrderComparator(getContext()));
            /*ended by ncqp34*/
            if (index < 0) {
                mAllApps.add(-(index + 1), info);
            }
        }
        // Now update the indexes
        mApps = rebuildFilteredApps(mAllApps);
    }

    /**
     * Override because some groups can have no applications to diplay.
     */
    @Override
    public void syncAppsPageItems(int page, boolean immediate) {
        super.syncAppsPageItems(page, immediate);

        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mApps.size());

        // If there is no app to be displayed, display the empty message
        //modified by amt_wangpeipei 2012/07/11 for switchui-2050 begin
		if (page == 0 && startIndex == 0
				&& (endIndex == 0 || (endIndex == 1 && mApps.get(0) == null))) {
	    //modified by amt_wangpeipei 2012/07/11 for switchui-2050 end
			 PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);
			/* 2012-7-12 add by Hu ShuAn for switchui-2061 start */
			// TextView text = new TextView(getContext());
			// text.setText(getContext().getResources().getString(mCurrentGroupItem.getEmptyGroupMessageId()));
			// text.setTextSize(getContext().getResources().getDimension(R.dimen.appicon_text_size));
			// text.setGravity(Gravity.CENTER_HORIZONTAL);
			// ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
			// params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			// params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			// layout.addView(text, params);

			PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
					R.layout.apps_customize_application, layout, false);
			ApplicationInfo info = new ApplicationInfo();
			//added by amt_wangpeipei 2012/07/17 for SWITCHUI-2264 begin
			int originalAppSize = mOriginalApps.size();
			if (mLauncher.isAppsCustomizeSearchStatus()
					&& (originalAppSize > 1 || (originalAppSize == 1 && mOriginalApps
							.get(0) != null))) {
				info.title = "";
			} else {
				info.title = getContext().getResources().getString(
						mCurrentGroupItem.getEmptyGroupMessageId());
			}
			//added by amt_wangpeipei 2012/07/17 for SWITCHUI-2264 end
			icon.applyFromApplicationInfo(info, true, this);
			// coordinate x=0, y=0; span x=4,y=4
			layout.addViewToCellLayout(icon, -1, 0,
					new PagedViewCellLayout.LayoutParams(0, 0, 4, 4));
			/* 2012-7-12 add by Hu ShuAn end */
        }
    }

    
    boolean isApplicationTab() {
        AppsCustomizeTabHost tabHost = getTabHost();
        if (null == tabHost)
            return false;
        String tag = tabHost.getCurrentTabTag();
        if (tag != null) {
            if (tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void setCurrentPage(int currentPage) {
        super.setCurrentPage(currentPage);
        // We reset the save index when we change pages so that
        // it will be recalculated on next rotation
        mSaveInstanceStateItemIndex = -1;
    }

}

