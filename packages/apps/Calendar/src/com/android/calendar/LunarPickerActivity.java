package com.android.calendar;

import com.android.calendar.CalendarController.EventType;
import com.android.calendar.R;


import android.app.Activity;
import android.app.ActionBar;
import android.app.DatePickerDialog.OnDateSetListener;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.res.Resources;

public class LunarPickerActivity extends Activity{

     private Time mCurrentDate;
     private TextView today_solar_date;
     private TextView today_lunar_date;
     private TextView checked_day;
     private TextView checked_lunar_date;
     private int flags ;
     private DatePicker mDatePicker;
     private Button mCheckDate;
     private Time mChangedDate;
     private CalendarController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lunar_picker);

        mController = CalendarController.getInstance(this);
        mDatePicker = (DatePicker) findViewById(R.id.datePicker);
        today_solar_date = (TextView)findViewById(R.id.today_solar_date);
        today_lunar_date = (TextView)findViewById(R.id.today_lunar_date);
        checked_lunar_date = (TextView)findViewById(R.id.checked_lunar_date);
        checked_day = (TextView)findViewById(R.id.checked_day);
        mCurrentDate = new Time();
        mChangedDate = new Time();
        long now = System.currentTimeMillis();
        mCurrentDate.set(now);
        // modified by amt_sunli 2012-12-05 SWITCHUITWO-169 begin
        flags = DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_SHOW_WEEKDAY
        | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_WEEKDAY;
        // modified by amt_sunli 2012-12-05 SWITCHUITWO-169 end
        String dateRange =DateUtils.formatDateRange(LunarPickerActivity.this, now, now, flags);
        
        dateRange = dateRange.replace("","");

        today_solar_date.setText(dateRange);
        
        String lunarDate = Utils.lunarDate(this.getBaseContext(), mCurrentDate,true);
        checked_lunar_date.setText(lunarDate);
        // modified by amt_sunli 2012-12-05 SWITCHUITWO-169 begin
        String dayOfWeek = DateUtils.formatDateRange(LunarPickerActivity.this , now, now, DateUtils.FORMAT_SHOW_WEEKDAY| DateUtils.FORMAT_ABBREV_WEEKDAY);
        // modified by amt_sunli 2012-12-05 SWITCHUITWO-169 end
        dayOfWeek = dayOfWeek.replace("","");
        checked_day.setText(dayOfWeek);
        
        Resources res = getResources();
        today_lunar_date.setText(res.getString(R.string.goto_today) + " " + lunarDate );
        
        
//        Button b = (Button) findViewById(R.id.create_event);
//           
//        b.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//            	mController.sendEventRelatedEvent(
//                        this, EventType.CREATE_EVENT, -1, mChangedDate.toMillis(true), 0, 0, 0, -1);
//            	finish();
//            }
//        });

        mDatePicker.init(mCurrentDate.year, mCurrentDate.month, mCurrentDate.monthDay, new OnDateChangedListener(){
            public void onDateChanged(DatePicker view, int year,
                    int monthOfYear, int dayOfMonth) {
                if(year>=2038||year<1902){
                    checked_lunar_date.setText("");
                }else{
                	mChangedDate.set(dayOfMonth, monthOfYear, year);
                    long millis = mChangedDate.toMillis(false);
                    String dateRange =DateUtils.formatDateRange(LunarPickerActivity.this, millis, millis, flags);
                    checked_lunar_date.setText(Utils.lunarDate(LunarPickerActivity.this, mChangedDate,true));
                    // modified by amt_sunli 2012-12-05 SWITCHUITWO-169 begin
                    String dayOfWeek = DateUtils.formatDateRange(LunarPickerActivity.this , millis, millis, DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY);
                    // modified by amt_sunli 2012-12-05 SWITCHUITWO-169 end
                    dayOfWeek = dayOfWeek.replace("","");
                    checked_day.setText(dayOfWeek);
                }
            }
        });

    }

     private class DateListener implements OnDateSetListener {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Cache the member variables locally to avoid inner class overhead.

                if(year>=2038||year<1902){
                    checked_lunar_date.setText("");
                }else{
                    mChangedDate.set(dayOfMonth, monthOfYear, year);
                    long millis = mChangedDate.toMillis(false);
                    String dateRange =DateUtils.formatDateRange(LunarPickerActivity.this, millis, millis, flags);
                    checked_lunar_date.setText(Utils.lunarDate(LunarPickerActivity.this, mChangedDate,true));
                }
            }
        }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBar()
                .setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_event_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.returnToCalendarHome(this);
                return true;
            case R.id.create_event_lunar:
                mController.sendEventRelatedEvent(
                        this, EventType.CREATE_EVENT, -1, mChangedDate.toMillis(true), 0, 0, 0, -1);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

