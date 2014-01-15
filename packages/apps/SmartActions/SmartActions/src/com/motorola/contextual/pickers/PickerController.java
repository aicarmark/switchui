/*
 * @(#)PickerController.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * crgw47        2012/04/23  NA               Initial version
 *
 */

package com.motorola.contextual.pickers;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.motorola.contextual.smartrules.R;

/**
 * The base class for trigger and action pickers.
 * <code><pre>
 * CLASS:
 *
 * RESPONSIBILITIES:
 *     Constructs the UI for a action or trigger
 *     picker based on a list of picker items
 *
 * COLLABORATORS:
 *      Picker.java - Holds and creates the controller
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PickerController {

    @SuppressWarnings("unused")
	private final static String TAG = "PickerController";
    private final Context mContext;
    private final DialogInterface mDialogInterface;

    private CharSequence mTitle;
    private PickerListView mListView;
    private View mContentView;
    private Button mButtonPositive;
    private CharSequence mButtonPositiveText;
    private Message mButtonPositiveMessage;
    private Button mButtonNegative;
    private CharSequence mButtonNegativeText;
    private Message mButtonNegativeMessage;
    private Button mButtonNeutral;
    private CharSequence mButtonNeutralText;
    private Message mButtonNeutralMessage;
    private ScrollView mScrollView;
    private int mIconId = -1;
    private ImageView mIconView;
    private TextView mTitleView;
    // cjd - this field is set, but never read, cannot be read by extension class, what is it's purpose?
    private ListAdapter mAdapter;
    // cjd - this field is set, but never read, cannot be read by extension class, what is it's purpose?
    private int mCheckedItem = -1;
    private boolean mHasButtons;

    private final int mPickerDialogLayout;
    private int mSingleChoiceItemLayout;
    private int mListItemLayout;

    private final Handler mHandler;


    View.OnClickListener mButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            Message m = null;
            if (v == mButtonPositive && mButtonPositiveMessage != null) {
                m = Message.obtain(mButtonPositiveMessage);
            } else if (v == mButtonNegative && mButtonNegativeMessage != null) {
                m = Message.obtain(mButtonNegativeMessage);
            } else if (v == mButtonNeutral && mButtonNeutralMessage != null) {
                m = Message.obtain(mButtonNeutralMessage);
            }
            if (m != null) {
                m.sendToTarget();
            }

            // Post a message so we dismiss after the above handlers are executed
            mHandler.obtainMessage(ButtonHandler.MSG_DISMISS_DIALOG, mDialogInterface)
                    .sendToTarget();
        }
    };

    private static final class ButtonHandler extends Handler {
        // Button clicks have Message.what as the BUTTON{1,2,3} constant
        private static final int MSG_DISMISS_DIALOG = 1;

        private final WeakReference<DialogInterface> mDialog;

        public ButtonHandler(DialogInterface dialog) {
            mDialog = new WeakReference<DialogInterface>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case DialogInterface.BUTTON_POSITIVE:
                case DialogInterface.BUTTON_NEGATIVE:
                case DialogInterface.BUTTON_NEUTRAL:
                    ((DialogInterface.OnClickListener) msg.obj).onClick(mDialog.get(), msg.what);
                    break;

                case MSG_DISMISS_DIALOG:
                    ((DialogInterface) msg.obj).dismiss();
            }
        }
    }

    /**
     * Returns centering mode for single button implmentations.
     * NB - Currently always "true"
     *
     * @return true if single button should center
     */
    private static boolean shouldCenterSingleButton(Context context) {
        return true;
    }


    public PickerController(Context context, DialogInterface di) {
        mContext = context;
        mDialogInterface = di;
        mHandler = new ButtonHandler(di);
        mPickerDialogLayout = R.layout.picker;
    }

    /**
     * Creates internal View based on number of buttons required.
     *
     * @param P Setup parameters
     */
    private void setupView(Params P) {
        boolean hasTitle = setupTitle();
        mHasButtons = setupButtons();

        if (mHasButtons) {
            enableButtonIfItemSelected(!P.mIsNoChoice, this,
                    DialogInterface.BUTTON_POSITIVE, P.mIsBottomButtonAlwaysEnabled);
        } else {
            View buttonPanel = getView().findViewById(R.id.buttonPanel);
            buttonPanel.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the title View to use the title text, if found.
     *
     * @return true if the picker has title text
     */
    private boolean setupTitle() {

        final boolean hasTextTitle = !TextUtils.isEmpty(mTitle);
        mTitleView = (TextView) getView().findViewById(R.id.prompt);

        /* Display the title if a title is supplied, else hide it */
        if (hasTextTitle) {
            mTitleView.setText(mTitle);
        } else {
            mTitleView.setVisibility(View.GONE);
        }

        return hasTextTitle;
    }

    /**
     * Sets up the control buttons (yes/no/cancel) based set up strings.
     *
     * @return true if any buttons were set up
     */
    private boolean setupButtons() {
        int BIT_BUTTON_POSITIVE = 1;
        int BIT_BUTTON_NEGATIVE = 2;
        int BIT_BUTTON_NEUTRAL = 4;
        int whichButtons = 0;
        mButtonPositive = (Button) getView().findViewById(R.id.button1);
        mButtonPositive.setOnClickListener(mButtonHandler);

        if (TextUtils.isEmpty(mButtonPositiveText)) {
            mButtonPositive.setVisibility(View.GONE);
        } else {
            mButtonPositive.setText(mButtonPositiveText);
            mButtonPositive.setVisibility(View.VISIBLE);
            whichButtons = whichButtons | BIT_BUTTON_POSITIVE;
        }

        mButtonNegative = (Button) getView().findViewById(R.id.button2);
        mButtonNegative.setOnClickListener(mButtonHandler);

        if (TextUtils.isEmpty(mButtonNegativeText)) {
            mButtonNegative.setVisibility(View.GONE);
        } else {
            mButtonNegative.setText(mButtonNegativeText);
            mButtonNegative.setVisibility(View.VISIBLE);

            whichButtons = whichButtons | BIT_BUTTON_NEGATIVE;
        }

        mButtonNeutral = (Button) getView().findViewById(R.id.button3);
        mButtonNeutral.setOnClickListener(mButtonHandler);

        if (TextUtils.isEmpty(mButtonNeutralText)) {
            mButtonNeutral.setVisibility(View.GONE);
        } else {
            mButtonNeutral.setText(mButtonNeutralText);
            mButtonNeutral.setVisibility(View.VISIBLE);

            whichButtons = whichButtons | BIT_BUTTON_NEUTRAL;
        }

        if (shouldCenterSingleButton(mContext)) {
            /*
             * If we only have 1 button it should be centered on the layout and
             * expand to fill 50% of the available space.
             */
            if (whichButtons == BIT_BUTTON_POSITIVE) {
                centerButton(mButtonPositive);
            } else if (whichButtons == BIT_BUTTON_NEGATIVE) {
                centerButton(mButtonNeutral);
            } else if (whichButtons == BIT_BUTTON_NEUTRAL) {
                centerButton(mButtonNeutral);
            }
        }

        return whichButtons != 0;
    }

    /**
     * Centers the given button horizontally.
     *
     * @param button The button to be centered
     */
    private void centerButton(Button button) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.weight = 0.5f;
        button.setLayoutParams(params);
        View leftSpacer = getView().findViewById(R.id.leftSpacer);
        if (leftSpacer != null) {
            leftSpacer.setVisibility(View.VISIBLE);
        }
        View rightSpacer = getView().findViewById(R.id.rightSpacer);
        if (rightSpacer != null) {
            rightSpacer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Enable button depending on a condition.
     * In single or multiple choice mode, the condition is that at least one
     * selection has been made.
     *
     * @param isChoiceMode True if single or multiple choice mode. Otherwise false.
     * @param pickerController Picker controller
     * @param whichButton Button to enable or disable
     */
    static void enableButtonIfItemSelected(boolean isChoiceMode, PickerController pickerController,
            int whichButton, boolean isAlwaysEnabled) {
        if (isChoiceMode && !isAlwaysEnabled) {
            enableButtonIfItemSelected(pickerController, whichButton);
        }
    }

    /**
     * Enable a specific button if the picker has any items selected
     *
     * @param pickerController The picker to be checked for selected items.
     * @param whichButton The button to be enabled.
     */
    private static void enableButtonIfItemSelected(PickerController pickerController, int whichButton) {
        Button button = pickerController.getButton(whichButton);
        if (button != null && pickerController.getListView() != null) {
            button.setEnabled(pickerController.getListView().getCheckedItemCount() > 0);
        }
    }


    public static class Params {
    	// cjd - I don't really like to see all these public attributes, means nothing is really encapsulated and setting any one of
    	//        these has no influence on any of the others. A simple example, context here is set presumably once, and read
    	//        everywhere it's needed. It could be passed in the constructor, then encapsulated to ensure no method anywhere
    	//        sets it to null either indavertently or otherwise, and a getter could be used to wrap it, allowing
        //         any class needing it, to gain read access.
        public final Context mContext;
        public final LayoutInflater mInflater;

        public int mIconId = 0;
        public CharSequence mTitle;
        public CharSequence mPositiveButtonText;
        // cjd - Are these ever actually registered or just referenced?
        //         If registered, it might be nice to deregister these listeners in this class rather than forcing the implementer to do so. That is,
        //         if the context is always an Activity or whatever, could be captured in the constructor, then deregistered on
        //         some standard call to this method to "close" the use of the instance of this class.
        public DialogInterface.OnClickListener mPositiveButtonListener;
        public CharSequence mNegativeButtonText;
        public DialogInterface.OnClickListener mNegativeButtonListener;
        public CharSequence mNeutralButtonText;
        public DialogInterface.OnClickListener mNeutralButtonListener;
        public boolean mCancelable;
        public DialogInterface.OnCancelListener mOnCancelListener;
        public DialogInterface.OnKeyListener mOnKeyListener;
        public CharSequence[] mItems;
        //An array of list items or a collection of list items
        //can be used to build the list
        public ListItem[] mListItemsArray;
        public List<ListItem> mListItemsCollection;
        public ListAdapter mAdapter;
        public DialogInterface.OnClickListener mOnClickListener;
        public AdapterView.OnItemLongClickListener mOnItemLongClickListener = null;
        public boolean[] mCheckedItems;
        public boolean mIsMultiChoice;
        public boolean mIsSingleChoice;
        public boolean mIsNoChoice = false;
        // cjd - perhaps mIsFirstItemSelectingAll ?
        public boolean mFirstItemSelectsAll = false;
        //Bottom button can always be enabled on some screens, this boolean
        //has to be set in those pickers
        public boolean mIsBottomButtonAlwaysEnabled = false;
        public int mListItemLayoutResId=R.layout.list_item_tap_text;
        public int mCheckedItem = -1;
        public DialogInterface.OnMultiChoiceClickListener mOnCheckboxClickListener;
        // cjd - perhaps mHasCursor?
        public boolean hasCursor = false;
        public Cursor mCursor;
        public String mLabelColumn;
        public String mIsCheckedColumn;
        public AdapterView.OnItemSelectedListener mOnItemSelectedListener;
        public OnPrepareListViewListener mOnPrepareListViewListener;
        public boolean mRecycleOnMeasure = true;

        /** List item description cursor column */
        public String mDescCol = null;

        /** List item icon Uri cursor column */
        public String mIconCol = null;

        /** Text area click listener */
        public View.OnClickListener mTextClickListener;

        /**
         * Interface definition for a callback to be invoked before the ListView
         * will be bound to an adapter.
         */
         public interface OnPrepareListViewListener {

             /**
              * Called before the ListView is bound to an adapter.
              * @param listView The ListView that will be shown in the dialog.
              */
             void onPrepareListView(ListView listView);
         }

         public Params(Context context) {
             mContext = context;
             mCancelable = true;
             mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         }

        /**
         * Apply all of the various text strings, icons, list items, etc for this Picker
         * to the relevant dialog.
         *
         * @param dialog The dialog which will have the various settings applied to it.
         */
         public void apply(PickerController dialog) {
             if (mTitle != null) {
                 dialog.setTitle(mTitle);
             }
             if (mIconId >= 0) {
                 dialog.setIcon(mIconId);
             }
             if (mPositiveButtonText != null) {
                 dialog.setButton(DialogInterface.BUTTON_POSITIVE, mPositiveButtonText,
                         mPositiveButtonListener, null);
             }
             if (mNegativeButtonText != null) {
                 dialog.setButton(DialogInterface.BUTTON_NEGATIVE, mNegativeButtonText,
                         mNegativeButtonListener, null);
             }
             if (mNeutralButtonText != null) {
                 dialog.setButton(DialogInterface.BUTTON_NEUTRAL, mNeutralButtonText,
                         mNeutralButtonListener, null);
             }

             createContentView(dialog);
             // For a list, the client can either supply an array of items or an
             // adapter or a cursor or a list
             if ((mListItemsArray != null) || (mItems != null) || hasCursor
                     || (mAdapter != null) || mListItemsCollection != null) {
                 createListView(dialog);
             }

             /*
                dialog.setCancelable(mCancelable);
                dialog.setOnCancelListener(mOnCancelListener);
                if (mOnKeyListener != null) {
                    dialog.setOnKeyListener(mOnKeyListener);
                }
              */
         }

        /**
         * Creates the picker's ListView by creating a custom list adapter for the
         * picker's list of items.
         *
         * @param dialog The dialog which will have the various settings applied to it.
         */
         public void createListView(final PickerController dialog) {

             final PickerListView listView = (PickerListView)dialog.getView().findViewById(R.id.list);

             if ((listView != null)) {
                 final int layout = mIsSingleChoice ? dialog.mSingleChoiceItemLayout : dialog.mListItemLayout;

                 final ListAdapter adapter;

                 if (mListItemsArray != null){
                     adapter = new CustomListAdapter(mContext, mListItemsArray,
                             (mIsMultiChoice) ? mCheckedItems : null, mListItemLayoutResId);
                 }else if (mListItemsCollection != null) {
                     adapter = new CustomListAdapter(mContext, mListItemsCollection,
                             (mIsMultiChoice) ? mCheckedItems : null, mListItemLayoutResId);
                 }
                 else if (hasCursor) {
                     adapter = new CustomCursorAdapter(mContext,
                                     R.layout.list_item_tap_text,
                                     mCursor, 0 /*flags*/,
                                     R.id.list_item_label,
                                     mLabelColumn,
                                     R.id.list_item_desc,
                                     mDescCol,
                                     R.id.list_item_icon,
                                     mIconCol,
                                     mTextClickListener,
                                     R.id.list_item_text_area,
                                     R.id.divider_line
                     );
                 }
                 else {
                     adapter = new ArrayAdapter<CharSequence>(mContext, layout, R.id.text1, mItems);
                 }

                 if (adapter != null) {
                     listView.setAdapter(adapter);
                     listView.initializeParams(dialog, this);

                     // Set choice mode and default selection
                     if (mIsSingleChoice) {
                         listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                         if (mCheckedItem > -1) {
                             listView.setItemChecked(mCheckedItem, true);
                             listView.setSelection(mCheckedItem);
                         }
                     } else if (mIsMultiChoice) {
                         listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                         if (mCheckedItems != null) {
                             for (int i = 0; i < mCheckedItems.length; i++) {
                                 if (mCheckedItems[i]) {
                                     listView.setItemChecked(i, true);
                                 }
                             }
                         }
                     } else if (mIsNoChoice) {
                         listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
                     }

                     // cjd - method is pretty clean, but rather long, perhaps it could be refactored, just move this out.
                     listView.setOnItemClickListener(new OnItemClickListener() {
                         public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                             try {
                            	 if (mFirstItemSelectsAll) {
                            		 if (position == 0) {
                            			 // The first item is an "All items" entry, so select/deselect everything
                            			 // based on its state
                            			 final boolean selected = listView.isItemChecked(0);
                            			 for (int i = 1; i < listView.getCount(); i++) {
                            				 listView.setItemChecked(i, selected);
                            				 if (mCheckedItems != null) {
                                				 // cjd - how do you know if the [i] instance exists? this is a public variable.
                            					 mCheckedItems[i] = selected;
                            				 }
                            			 }
                            		 } else {
                            			 // Set the state of the "All items" entry based on whether everything
                            			 // is selected
                            			 boolean allSelected = true;
                            			 for (int i = 1; i < listView.getCount(); i++) {
                            				 if (!listView.isItemChecked(i)) {
                            					 allSelected = false;
                            					 break;
                            				 }
                            			 }
                            			 listView.setItemChecked(0, allSelected);
                            			 if (mCheckedItems != null)
                            				 // cjd - how do you know if the [0] instance exists? this is a public variable.
                            				 mCheckedItems[0] = allSelected;
                            		 }
                            	 }

                            	 enableButtonIfItemSelected(!mIsNoChoice, dialog, DialogInterface.BUTTON_POSITIVE, mIsBottomButtonAlwaysEnabled);

                                 if (mCheckedItems != null) {
                    				 // cjd - how do you know if the [position] instance exists? this is a public variable.
                                     mCheckedItems[position] = listView.isItemChecked(position);
                                 }

                                 if (mOnClickListener != null) {
                                     mOnClickListener.onClick(dialog.mDialogInterface, position);
                                     if (!mIsSingleChoice) {
                                         dialog.mDialogInterface.dismiss();
                                     }
                                 }
                                 else if (mOnCheckboxClickListener != null) {
                                         mOnCheckboxClickListener.onClick(dialog.mDialogInterface, position, listView.isItemChecked(position));
                                 }
                             }catch(ArrayIndexOutOfBoundsException aibe) {
                                 //If the num of list items change then there's a chance of
                                 //mCheckedItems throwing this exception, although picker
                                 //responsible for this list should update the mCheckedItems
                                 //array when the list is updated, this is only a last stop measure
                                 //to prevent unexpected Force closes due to old reference being held
                                 aibe.printStackTrace();
                             }
                         }
                     });

                     // Attach a given OnItemSelectedListener to the ListView
                     if (mOnItemSelectedListener != null) {
                         listView.setOnItemSelectedListener(mOnItemSelectedListener);
                     }

                     // Attach given View.OnLongClickListener to the ListView
                     if (mOnItemLongClickListener != null) {
                         listView.setOnItemLongClickListener(mOnItemLongClickListener);
                     }

                     dialog.mCheckedItem = mCheckedItem;
                     dialog.mListView = listView;
                     dialog.mAdapter = adapter;
                 }
             }
         }

        /**
         * Inflates the picker's main view.
         *
         * @param dialog The dialog in which the view will be created.
         */
        public View createContentView(PickerController dialog) {
             View contentView = mInflater.inflate(dialog.mPickerDialogLayout, null);
             if (contentView != null) {
                 dialog.mContentView = contentView;
             }
             return contentView;
         }
    }

    public void installContent(Params P) {
            setupView(P);
    }

    /**
     * Returns the full picker View
     */
    public View getView() {
        return mContentView;
    }

    /**
     * Sets the picker title.
     * @param title The title string to be used
     */
    public void setTitle(CharSequence title) {
        mTitle = title;
        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    /**
     * Sets a click listener or a message to be sent when the button is clicked.
     * You only need to pass one of {@code listener} or {@code msg}.
     *
     * @param whichButton Which button, can be one of
     *            {@link DialogInterface#BUTTON_POSITIVE},
     *            {@link DialogInterface#BUTTON_NEGATIVE}, or
     *            {@link DialogInterface#BUTTON_NEUTRAL}
     * @param text The text to display in positive button.
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @param msg The {@link Message} to be sent when clicked.
     */
    public void setButton(int whichButton, CharSequence text,
            DialogInterface.OnClickListener listener, Message msg) {

        if (msg == null && listener != null) {
            msg = mHandler.obtainMessage(whichButton, listener);
        }

        switch (whichButton) {

            case DialogInterface.BUTTON_POSITIVE:
                mButtonPositiveText = text;
                mButtonPositiveMessage = msg;
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                mButtonNegativeText = text;
                mButtonNegativeMessage = msg;
                break;

            case DialogInterface.BUTTON_NEUTRAL:
                mButtonNeutralText = text;
                mButtonNeutralMessage = msg;
                break;

            default:
                throw new IllegalArgumentException("Button does not exist");
        }
    }

    /**
     * Set the icon based on a resourceId
     * @param resId The resourceId of the drawable to use as the icon, or 0
     * if you don't want an icon.
     */
    public void setIcon(int resId) {
        mIconId = resId;
        if (mIconView != null) {
            if (resId > 0) {
                mIconView.setImageResource(mIconId);
            } else if (resId == 0) {
                mIconView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Returns the picker internal ListView.
     */
    public ListView getListView() {
        return mListView;
    }

    /**
     * Returns the relevant member button.
     * @param whichButton Which button, can be one of
     *            {@link DialogInterface#BUTTON_POSITIVE},
     *            {@link DialogInterface#BUTTON_NEGATIVE}, or
     *            {@link DialogInterface#BUTTON_NEUTRAL}
     */
    public Button getButton(int whichButton) {
        switch (whichButton) {
            case DialogInterface.BUTTON_POSITIVE:
                return mButtonPositive;
            case DialogInterface.BUTTON_NEGATIVE:
                return mButtonNegative;
            case DialogInterface.BUTTON_NEUTRAL:
                return mButtonNeutral;
            default:
                return null;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mScrollView != null && mScrollView.executeKeyEvent(event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mScrollView != null && mScrollView.executeKeyEvent(event);
    }
}
