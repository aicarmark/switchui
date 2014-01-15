package com.motorola.mmsp.motohomex.apps;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.SoundEffectConstants;
import android.widget.Spinner;

import com.motorola.mmsp.motohomex.Launcher;
import com.motorola.mmsp.motohomex.LauncherApplication;

public class AllAppsDropDownGroup extends Spinner {

    float mTextDimen;
    private OnClickListenerSpinner mOnClickListener;

    public AllAppsDropDownGroup(Context context, int mode) {
        super(context, mode);
    }

    public AllAppsDropDownGroup(Context context) {
        super(context);
    }

    public AllAppsDropDownGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AllAppsDropDownGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AllAppsDropDownGroup(Context context, AttributeSet attrs, int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // You need to call the super to not get
        // an exception
        super.onSaveInstanceState();

        // Lets AllAppsPage handles everything related
        // to save/restore instance.
        // Return null to block the onRestoreIntanceState
        return null;
    }

    @Override
    public boolean performClick() {
        // Close the folder on apps tray
        if (mOnClickListener != null) {
            /*2012-7-16, add by bvq783 for switchui-2237*/
            if (mOnClickListener instanceof AllAppsPage) {
                AllAppsPage page = (AllAppsPage)mOnClickListener;
                if (!page.checkShowSpinner()) {
                    return true;
                }
            }
            /*2012-7-16, add end*/
            mOnClickListener.onClickSpinner();
        }
        //added by amt_wangpeipei 2012/07/21 for switchui-2397 begin
        LauncherApplication application = (LauncherApplication)getContext().getApplicationContext();
        Launcher launcher = application.getLauncher();
        AllAppsDropDownMenu menu = (AllAppsDropDownMenu)launcher.getOverFlowMenu();
        if(menu != null){
        	menu.dismiss();
        }
        else{
        	Menu bottomMenu = launcher.getAllAppsPage().getMenu();
        	if(bottomMenu != null){
        		bottomMenu.close();
        	}
        }
        //added by amt_wangpeipei 2012/07/21 for switchui-2397 end.
        super.performClick();
        playSoundEffect(SoundEffectConstants.CLICK);
        return true;
    }

    public void setOnClickListenerSpinner(OnClickListenerSpinner listener) {
        mOnClickListener = listener;
    }
}

