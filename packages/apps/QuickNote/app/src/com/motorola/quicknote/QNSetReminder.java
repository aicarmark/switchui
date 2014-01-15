package com.motorola.quicknote;

import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
//import android.util.Log;

import com.motorola.quicknote.R;
import com.motorola.quicknote.QuickNotesDB.QNColumn;

import java.util.Date;
import java.util.Calendar;

public class QNSetReminder extends PreferenceActivity {

	private static final String TAG = "QNSetReminder";

	private EditTextPreference mLabel;
	private CheckBoxPreference mSetPref;
	private Preference mDatePref;
	private Preference mTimePref;

	private int mId;
	private boolean mEnabled = false;
	private Calendar mCalendar;
	private int mYear = 0;
	private int mMonth = 0;
	private int mDayOfMonth = 0;
	private int mHourOfDay = 0;
	private int mHour = 0;
	private int mAmPm = 1;
	private int mMinute = 0;

	private long mDBReminder = 0L;
	private Menu mMenu = null;
	private String mFrom = null;
	private Uri mContentUri = null;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		addPreferencesFromResource(R.xml.reminder_prefs);

		mSetPref = (CheckBoxPreference) findPreference("set_date_time");
		mDatePref = findPreference("set_date");
		mTimePref = findPreference("set_time");

		if ((mSetPref == null) || (mDatePref == null) || (mTimePref == null)) {
			return;
		}

		mCalendar = Calendar.getInstance();

		// need to get the old setting from intent
		Intent i = getIntent();
		mFrom = i.getStringExtra("from");
		long currentReminder = i.getLongExtra("currentReminder", 0L);
		QNDev.log(TAG + "reminder: currentReminder = " + currentReminder);
		mDBReminder = i.getLongExtra("dbReminderSet", 0L);
		QNDev.log(TAG + "reminder, mDBReminder = " + mDBReminder);

		setResult(RESULT_CANCELED);

		// From the intent we can judge whether is has reminder or not
		// if it is a new reminder, then we show current date & time
		if (null == mFrom) {
			// cannot know whether the request from, return
			return;
		} else if (mFrom.equals("QNNoteView")) {
			// it is from QNNoteView
			String uri_string = i.getStringExtra("content_uri");
			QNDev.log(TAG + " uri_string from intent = " + uri_string);
			if (null == uri_string) {
				return;
			} else {
				mContentUri = QNUtil.buildUri(uri_string);
			}

			QNDev.log(TAG + "case 4, use db values= " + mDBReminder);
			if (0L != mDBReminder) {
				mSetPref.setChecked(true);
				mCalendar.setTimeInMillis(mDBReminder);
			} else {
				mSetPref.setChecked(false);
			}
		} else {
			// error case, just return;
			return;
		}

		mYear = mCalendar.get(Calendar.YEAR);
		mMonth = mCalendar.get(Calendar.MONTH);
		mDayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);

		// get the current time
		mHourOfDay = mCalendar.get(Calendar.HOUR_OF_DAY);
		mHour = mCalendar.get(Calendar.HOUR);
		mAmPm = mCalendar.get(Calendar.AM_PM);
		mMinute = mCalendar.get(Calendar.MINUTE);

		// display the data & time
		updateDateDisplay();
		updateTimeDisplay();

	}

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.xml.action_bar, menu);
    mMenu = menu;
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.btn_ok:
        saveReminder();
        finish();
        break;
      case R.id.btn_cancel:
        finish();
        break;
      default:
        finish();
    }
    return true;
  }

  @Override
  public void onConfigurationChanged (Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if(mMenu != null) {
      try {
        mMenu.clear();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.xml.action_bar, mMenu);
      } catch(Exception e) {
        QNDev.log(TAG + "Exception in configChanged: " + e);
      }
    }
  }

	private void updateDateDisplay() {
		Date myDate = mCalendar.getTime();
		java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(this);

		mDatePref.setSummary(shortDateFormat.format(myDate));
	}

	private void updateTimeDisplay() {
		Date myDate = mCalendar.getTime();
		mTimePref.setSummary(DateFormat.getTimeFormat(this).format(myDate));
	}

	public static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}


    // the callback received when the user "sets" the date in the dialog
    final private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, 
                                      int monthOfYear, int dayOfMonth) {
                     mYear = year;
                     mMonth = monthOfYear;
                     mDayOfMonth = dayOfMonth;
                     mCalendar.set(Calendar.YEAR, mYear);
                     mCalendar.set(Calendar.MONTH, mMonth);
                     mCalendar.set(Calendar.DAY_OF_MONTH, mDayOfMonth);
                     updateDateDisplay();
                     if (-1 == DateTimeCheck()) {
                       //show a toast indicating it is a past time
                       popWarnToast();
                    }
                }
            };

    // the callback received when the user "sets" the time in the dialog
    final private TimePickerDialog.OnTimeSetListener mTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
               mHourOfDay = hourOfDay;
               mMinute = minute;
               mCalendar.set(Calendar.HOUR_OF_DAY, mHourOfDay);
               mCalendar.set(Calendar.MINUTE, mMinute);
               mCalendar.set(Calendar.SECOND, 0);
               mCalendar.set(Calendar.MILLISECOND, 0);

               updateTimeDisplay();
               if (-1 == DateTimeCheck()) {
                 //show a toast indicating it is a past time
                 popWarnToast();
              }
        }
    };


    //Check the date & time
    private int DateTimeCheck () {
         Calendar nowCalendar = Calendar.getInstance();
         return mCalendar.compareTo(nowCalendar);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mDatePref) {
            new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDayOfMonth).show();
        } else if (preference == mTimePref) {
            new TimePickerDialog(this, mTimeSetListener, mHourOfDay, mMinute,
                    DateFormat.is24HourFormat(this)).show();

        }


        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

 
    //set the reminder flag in edit mode
    public static void setReminderFlag(Context context, TextView timeView, Calendar c) {
       StringBuilder timeString = formatTimeDate(context, c); 
       timeView.setText(timeString);          
     }



    //save the reminder when the usr back from set reminder
    @Override
    public void onBackPressed() {
        // saveReminder();
        finish();
    }

    private void saveReminder() {
      mEnabled = mSetPref.isChecked();
 
      Intent intent = new Intent();
      QNDev.log(TAG+"QNSetReminder: mEnabled = "+mEnabled+" time = "+mCalendar.getTimeInMillis());
      intent.putExtra("ReminderEnable", mEnabled);
      intent.putExtra("ReminderSetTime",mCalendar.getTimeInMillis());

      setResult(RESULT_OK, intent);
      return;
    }


   static public StringBuilder formatTimeDate(Context context, Calendar calendar) {
       Date myDate = calendar.getTime();
       java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(context);

       StringBuilder timeString =  new StringBuilder();
       timeString.append(DateFormat.getTimeFormat(context).format(myDate))
                   .append(", ")
                   .append(shortDateFormat.format(myDate));
                    
        QNDev.log(TAG+"reminder: timeString = "+timeString);
        return timeString;
   }

    /**
     * Display a toast that tells the user the date/time set just now is invalid.
     */
    private void popWarnToast() {
        StringBuilder timeString = formatTimeDate(this, mCalendar);
        Toast.makeText(this, getString(R.string.waring_toast, timeString), Toast.LENGTH_LONG).show();
    }

    /**
     * Display a toast that tells the user the set is failed because date/time set just now is invalid.
     */
    private void popFailureToast() {
        StringBuilder timeString = formatTimeDate(this, mCalendar);
        Toast.makeText(this, getString(R.string.failure_toast, timeString), Toast.LENGTH_LONG).show();
    }

}
