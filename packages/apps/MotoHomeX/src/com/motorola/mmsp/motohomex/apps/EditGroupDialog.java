package com.motorola.mmsp.motohomex.apps;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.apps.AllAppsPage.AppsDialog;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;

public class EditGroupDialog implements AppsDialog, OnClickListener {

    private Dialog mDialog;
    private AllAppsPage mAllAppsPage;
    private Toast mToast;

    EditGroupDialog(AllAppsPage allAppsPage){
        mAllAppsPage = allAppsPage;

        // Create the dialog
        mDialog = new EditDialog(mAllAppsPage.getContext());
        // Set the layout
        mDialog.setContentView(R.layout.all_apps_dialog_edit_group);
        // Set the title
        mDialog.setTitle(R.string.edit_group);

        // Set the listener
        mDialog.findViewById(R.id.all_apps_dialog_edit_group_ok).setOnClickListener(this);
        mDialog.findViewById(R.id.all_apps_dialog_edit_group_cancel).setOnClickListener(this);
        mDialog.findViewById(R.id.all_apps_dialog_edit_group_icon).setOnClickListener(this);
    }

    public void show() {
        GroupItem editGroupItem = mAllAppsPage.getEditGroupItem();
        //2012-07-26, ChenYidong for SWITCHUI-2472
        if(editGroupItem == null){
            return;
        }
        // Set the title
        EditText textView = (EditText) mDialog.findViewById(R.id.all_apps_dialog_edit_group_text);
        //remove it.
        //textView.setText(editGroupItem.getName(mAllAppsPage.getContext()));
        //2012-07-26, end

        // Set the icon
        ImageView editIconView = (ImageView) mDialog.findViewById(R.id.all_apps_dialog_edit_group_icon);
        //modified by amt_wangpeipei 2012/07/25 for switchui-2461 begin	
	/*Added by ncqp34 at Jul-25-2012 for switchui-2462*/
        //modify by amt_zhouxin for SWITCHUI-2096 start
        //editIconView.setImageDrawable(mAllAppsPage.getGroupIcon(editGroupItem.getIconSet()));
        if(editGroupItem == null){
        	textView.setText("");
        	editIconView.setImageResource(GroupItem.sEditIconIds[Groups.ICON_SET_USER]);
        }
        else{
        	textView.setText(editGroupItem.getName(mAllAppsPage.getContext()));
        	editIconView.setImageDrawable(editGroupItem.getEditIcon(mAllAppsPage.getContext()));
        }
        //2012-07-26, chenYidong for SWITCHUI-2468
        textView.setSelection(textView.getText().length());
        //modify by amt_zhouxin for SWITCHUI-2096 end
	/*ended by ncqp34*/
        //modified by amt_wangpeipei 2012/07/25 for switchui-2461 end.

        // Make it so
        mAllAppsPage.mAppsDialog = this; // it is better a setter
        mAllAppsPage.showDialog(mDialog);
    }

    @Override
    public void onClick(View v) {
        GroupItem editGroupItem = null;
        switch(v.getId()) {
            case R.id.all_apps_dialog_edit_group_ok:
                editGroupItem = mAllAppsPage.getEditGroupItem();
                /*2012-07-19, ChenYidong for SWITCHUI-2325*/
                try{
                    editGroupItem.setName(((TextView)mDialog.findViewById(R.id.all_apps_dialog_edit_group_text)).getText().toString().trim());
    
                    // Verify user entered a name is empty
                    final CharSequence name = editGroupItem.getName(mAllAppsPage.getContext());
                    if (name == null || TextUtils.isEmpty(name)) {
                        if (mToast == null) {
                            mToast = Toast.makeText(mAllAppsPage.getContext(), R.string.empty_group_name, Toast.LENGTH_SHORT);
                        }
                        mToast.show();
                        break;
                    }
                } catch(NullPointerException e){
                }
                /*2012-07-19, end*/
                // Now try to save it.
                mAllAppsPage.saveGroup(true);
                mDialog.dismiss();
                break;
            case R.id.all_apps_dialog_edit_group_cancel:
                mDialog.dismiss();
                break;
            case R.id.all_apps_dialog_edit_group_icon:
                editGroupItem = mAllAppsPage.getEditGroupItem();
                editGroupItem.setName(((TextView)mDialog.findViewById(R.id.all_apps_dialog_edit_group_text)).getText());
                mAllAppsPage.setNextDialog(AllAppsPage.DIALOG_SET_ICON);
                mDialog.dismiss();
                break;
        }

    }

    @Override
    public int getId() {
        return AllAppsPage.DIALOG_EDIT_GROUP;
    }

    @Override
    public void saveState(Bundle state) {}

    @Override
    public void restoreState(Bundle state) {}

    @Override
    public void dismiss() {
        // If you press back many times while the toast
        // is on the screen, the toast gets stuck in the 
        // screen. So release it.
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
    }

    class EditDialog extends Dialog {

        public EditDialog(Context context) {
            super(context);
        }

        protected EditDialog(Context context, boolean cancelable,
                OnCancelListener cancelListener) {
            super(context, cancelable, cancelListener);
        }

        public EditDialog(Context context, int theme) {
            super(context, theme);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            mAllAppsPage.post(new Runnable(){
                public void run(){
                    InputMethodManager imm = (InputMethodManager) mAllAppsPage.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(mDialog.findViewById(R.id.all_apps_dialog_edit_group_text), InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }
    }
}

