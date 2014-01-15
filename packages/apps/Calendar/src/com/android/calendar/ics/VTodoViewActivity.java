package com.android.calendar.ics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.calendar.R;
import com.motorola.calendarcommon.vcal.VCalUtils;
import com.motorola.calendarcommon.vcal.common.CalendarEvent;

public class VTodoViewActivity extends Activity{
    //Percentage constants
    private static final int VALUE_PERCENTAGE_START = 0;
    private static final int VALUE_PERCENTAGE_END = 10;
    public static long OFFSET_TIME_IN_MILLSECONDS = TimeZone.getDefault().getRawOffset();
    
    private  static final long INVALID_MIILISEC = -1L;
    private static final String TAG = "VTodoViewActivity";
    
    /**
     * get a date string. for example 2011/04/18 will be formatted to Mon,Apr18,2011
     */
    private String formatDateString(long mills){
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
        | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH
        | DateUtils.FORMAT_ABBREV_WEEKDAY;
        String date = DateUtils.formatDateTime(this, mills, flags);
        return date;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_vtodo);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        Uri fileUri = intent.getData();
        if (fileUri == null){
            Log.e(TAG, "can not get the ics file");
            finish();
            return;
        }
        Log.d(TAG,"start async task to handle");
        //start async task to parse the file
        new VTodoParserAsyncTask().execute(fileUri);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            //finish the activity when press home up
            finish();
            return true;
        }
        return false;
    }
    private CalendarEvent  parseVTodo(Uri fileUri){
        CalendarEvent vcalEvent = null;
        try {
            vcalEvent = VCalUtils.parseVcal(this, fileUri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to open the file " + fileUri.toString());
        } catch (IOException e) {
            Log.e(TAG, "I/O error when reading the file " + fileUri.toString());
        } catch (SecurityException e){
            Log.e(TAG, "no permisson  " + fileUri.toString());
        }
        return vcalEvent;
    }
    private void updateViewFromCalendarEvent(CalendarEvent ce){
        //disable  edit buttons
        CheckBox completedCheckBox = (CheckBox)findViewById(R.id.completed);
        CheckBox starred =  (CheckBox)findViewById(R.id.starred_checkbox);
        View tagContainerView = findViewById(R.id.tag_container);
        completedCheckBox.setClickable(false);
        starred.setClickable(false);
        tagContainerView.setClickable(false);
        //hide account info
        findViewById(R.id.account_container).setVisibility(View.GONE);
        //TODO:check if following is available in extend property
        findViewById(R.id.note_taking_container).setVisibility(View.GONE);
        findViewById(R.id.repeat_container).setVisibility(View.GONE);
        findViewById(R.id.reminder_container).setVisibility(View.GONE);

        //set title
        TextView title = (TextView) findViewById(R.id.title);
        if (!TextUtils.isEmpty(ce.summary)){
            title.setText(ce.summary);
        }
        //set title background to cyan
        findViewById(R.id.title_container).setBackgroundColor(0xff00ffff);
        //set des
        TextView  des = (TextView) findViewById(R.id.description);
        View desContainer = findViewById(R.id.description_container);
        if (TextUtils.isEmpty(ce.description)){
            desContainer.setVisibility(View.GONE);
        }else{
            desContainer.setVisibility(View.VISIBLE);
            des.setText(ce.description);
        }
        //set priority,  we did not parse percentage , so pass 0 for percentage
        setPriorityPercentage(ce.priority, 0);

        //set due date and start date
        setTime(ce.dtStart,ce.due);
        //set overdue flag
        View overDue = findViewById(R.id.overdue_container);
        if (isOverdue(ce.due)){
            overDue.setVisibility(View.VISIBLE);
        }else{
            overDue.setVisibility(View.GONE);
        }
        //set tag
        TextView tagView = (TextView)findViewById(R.id.tag_names);
        if (TextUtils.isEmpty(ce.categories)){
            tagView.setText(R.string.no_tag_label);
        }else {
            tagView.setText(ce.categories);
        }
        
    }
    private void setTime(long start, long end) {
        SpannableStringBuilder startTime = null, endTime = null;
        SpannableStringBuilder text;
        View timeContainer = findViewById(R.id.time_container);
        TextView timeView = (TextView)findViewById(R.id.start_end_time);
        Resources res = getResources();
        if (start == INVALID_MIILISEC && end == INVALID_MIILISEC){
            timeContainer.setVisibility(View.GONE);
            return;

        }

        if (start != INVALID_MIILISEC){
            startTime = buildBoldString(res.getString(R.string.start)+res.getString(R.string.space),formatDateString(start));
        }
        if (end != INVALID_MIILISEC ){
            endTime = buildBoldString(res.getString(R.string.end)+res.getString(R.string.space),formatDateString(end));
        }
        if (start != INVALID_MIILISEC && end != INVALID_MIILISEC){
            text = new SpannableStringBuilder(startTime);
            text.append('\n');
            text.append(endTime);
        }else if (start!= INVALID_MIILISEC ){
            text = startTime;
        }else{
            text = endTime;
        }

        timeView.setText(text);
        timeContainer.setVisibility(View.VISIBLE);
    }
    private boolean  isOverdue(long time){
        if (time == INVALID_MIILISEC){
            return false;
        }
        if (((time + OFFSET_TIME_IN_MILLSECONDS)/DateUtils.DAY_IN_MILLIS) <
                ((System.currentTimeMillis()  + OFFSET_TIME_IN_MILLSECONDS) /DateUtils.DAY_IN_MILLIS)){
            return true;
        }
        return false;
    }
    /*
     * convenient method to build a TextAppearanceSpan
     * first string will set to bold and second string to normal
     */
    private SpannableStringBuilder buildBoldString(String bold,String normal){
        SpannableStringBuilder bld = new SpannableStringBuilder();
        TextAppearanceSpan span = new TextAppearanceSpan(this,R.style.TextAppearance_ViewEvent_ItemLabel);
        bld.append(bold);
        bld.setSpan(span,0,bold.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
        bld.append(normal);
        return bld;
    }
    private void setPriorityPercentage(int priority, int percentage){
        TextView label = (TextView)findViewById(R.id.priority_percentage_data);
        Resources res = getResources();
        String text = "";

        ImageView priorityImage = (ImageView)findViewById(R.id.priority_image);
        if (priority == 1){
            priorityImage.setImageResource(R.drawable.ic_list_importance_high);
            text = res.getString(R.string.priority_high);
            priorityImage.setVisibility(View.VISIBLE);
        }else if (priority > 3){
            priorityImage.setImageResource(R.drawable.ic_list_importance_low);
            text = res.getString(R.string.priority_low);
            priorityImage.setVisibility(View.VISIBLE);
        }else{
            priorityImage.setVisibility(View.GONE);
        }

        if (percentage < VALUE_PERCENTAGE_END && percentage > VALUE_PERCENTAGE_START ){
            //append the percentage to the priority text
            if (TextUtils.isEmpty(text)){
                text = String.format(getString(R.string.percent_complete),percentage*10);
            }else {
                text = String.format(getString(R.string.priority_percent_complete), text,percentage*10);
            }
        }
        if (!TextUtils.isEmpty(text)){
            label.setText(text);
            label.setVisibility(View.VISIBLE);
        }else{
            label.setVisibility(View.GONE);
        }
        //set the container to gone if no values
        View container = findViewById(R.id.priority_percentage);
        if (label.getVisibility()== View.GONE && priorityImage.getVisibility()== View.GONE){
            container.setVisibility(View.GONE);
        }else{
            container.setVisibility(View.VISIBLE);
        }
    }
    
    class VTodoParserAsyncTask extends AsyncTask<Uri, Void, CalendarEvent>{

        @Override
        protected CalendarEvent doInBackground(Uri... params) {
            
            return parseVTodo(params[0]);
        }
        @Override
        protected void onPostExecute(CalendarEvent ca){
            if (ca == null){
                Log.e(TAG,"parse error");
                finish();
                return;
            }

            if (!ca.isVtodo) {
                Log.e(TAG,"Not VTODO type");
                finish();
                return;
            }
            updateViewFromCalendarEvent(ca);
            return;
        }
    }
}
