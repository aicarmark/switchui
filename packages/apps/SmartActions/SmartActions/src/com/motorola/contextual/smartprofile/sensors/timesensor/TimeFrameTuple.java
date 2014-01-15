/*
 * @(#)TimeFrameTuple.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2011/02/18  NA                Initial Version
 *
 */
package com.motorola.contextual.smartprofile.sensors.timesensor;

import org.json.JSONObject;

import android.content.ContentValues;
/**
 * Abstracts the time frame table row
 *
 * <code><pre>
 * CLASS
 *  Implements @link {@link TimeFrameConstants}
 *
 * RESPONSIBILITIES
 *  Abstracts the time frame table row and exposes set methods for time frame fields
 *
 * COLLABORATORS
 *  None
 *
 * USAGE
 *  See each Method
 * </pre></code>
 *
 */
public class TimeFrameTuple implements TimeFrameConstants {
    private String mTimeFrameName;
    private String mTimeFrameInternalName;
    private String mAllDayFlag;
    private String mStartTime;
    private String mEndTime;
    private String mIsVisible;
    private int mDaysOfWeek;

    //private static final String TAG =  TimeFrameTuple.class.getSimpleName();
	
	/**
     * Constructor that fills the default values for few fields
     */
    public TimeFrameTuple() {
        mIsVisible  = FLAG_VISIBLE;
        mAllDayFlag = ALL_DAY_FLAG_FALSE;
    }
    
    /**
     * Constructor that creates TimeFrameTuple from JSON Object,
     * which can be used for inserting into tables
     * 
     * @param jsonObj - the JSON object to be converted to TimeFrame tuple
     */
    public TimeFrameTuple(JSONObject jsonObj) throws Exception
    {
    	mTimeFrameName = jsonObj.getString(TimeFrameTableColumns.NAME);
    	mTimeFrameInternalName = jsonObj.getString(TimeFrameTableColumns.NAME_INT);
    	mAllDayFlag = jsonObj.getString(TimeFrameTableColumns.ALL_DAY);
    	mStartTime = jsonObj.getString(TimeFrameTableColumns.START);
    	mEndTime = jsonObj.getString(TimeFrameTableColumns.END);
    	mIsVisible = jsonObj.getString(TimeFrameTableColumns.VISIBLE);
    	mDaysOfWeek = jsonObj.getInt(TimeFrameTableColumns.DAYS_OF_WEEK);
    }

    /**
     * Gets the time frame name
     *
     * @return mTimeFrameName - Name of the time frame
     */
    public String getName() {
        return mTimeFrameName;
    }

    /**
     * Gets the internal name of the time frame
     *
     * @return mTimeFrameInternalName - Internal name of the time frame
     */
    public String getInternalName() {
		return mTimeFrameInternalName;
    }

    /**
     * Gets the all day flag for the time frame
     *
     * @return mAllDayFlag - String indicating if the all day flag is true for the time frame
     */
    public String getAllDayFlag() {
        return mAllDayFlag;
    }

    /**
     * Gets the visibility  flag for the time frame
     *
     * @return mIsVisible - Flag indicating if the time frame should be visible to the user or not
     */
    public String getVisibility() {
        return mIsVisible;
    }

    /**
     * Gets the days of the week in which the time frame should repeat
     *
     * @return mDaysOfWeek - Bit mask representing the days of week
     */
    public int getDaysOfWeek() {
        return mDaysOfWeek;
    }

    /**
     * Gets the start time of the time frame
     *
     * @return mStartTime - Start time in hh:mm format
     */
    public String getStartTime() {
        return mStartTime;
    }

    /**
     * Gets the end time of the time frame
     *
     * @return mEndTime - End time in hh:mm format
     */
    public String getEndTime() {
        return mEndTime;
    }

    /**
     * Sets the time frame name
     *
     * @param name - Name of the time frame
     */
    public void setName(String name) {
        mTimeFrameName = name;
    }

    /**
     * Sets the internal name of the time frame
     *
     * @param name - Internal name of the time frame
     */
    public void setInternalName(String name) {
        mTimeFrameInternalName = name;
    }

    /**
     * Sets the all day flag for the time frame
     *
     * @param flag - Boolean indicating if the all day flag is true for the time frame
     */
    public void setAllDayFlag(boolean flag) {
        mAllDayFlag = flag ? ALL_DAY_FLAG_TRUE:ALL_DAY_FLAG_FALSE;
    }

    /**
     * Sets the visibility  flag for the time frame
     *
     * @param flag - Flag indicating if the time frame should be visible to the user or not
     */
    public void setVisibility(String flag) {
        mIsVisible = flag;
    }

    /**
     * Sets the days of the week in which the time frame should repeat
     *
     * @param daysOfWeek - Bit mask representing the days of week
     */
    public void setDaysOfWeek(int daysOfWeek) {
        mDaysOfWeek = daysOfWeek;
    }

    /**
     * Sets the start time of the time frame
     *
     * @param startTime - Start time in hh:mm format
     */
    public void setStartTime(String startTime) {
        mStartTime = startTime;
    }

    /**
     * Sets the end time of the time frame
     *
     * @param endTime - End time in hh:mm format
     */
    public void setEndTime(String endTime) {
        mEndTime = endTime;
    }

    /**
     * Converts the tuple to content values which can be used for inserting into tables
     *
     * @return - {@link ContentValues}
     */
    public ContentValues toContentValues() {
        ContentValues initialValues = new ContentValues();
        initialValues.put(TimeFrameTableColumns.NAME,mTimeFrameName);
        initialValues.put(TimeFrameTableColumns.START,mStartTime);
        initialValues.put(TimeFrameTableColumns.END,mEndTime);
        initialValues.put(TimeFrameTableColumns.NAME_INT, mTimeFrameInternalName);
        initialValues.put(TimeFrameTableColumns.ALL_DAY, mAllDayFlag);
        initialValues.put(TimeFrameTableColumns.DAYS_OF_WEEK, mDaysOfWeek);
        initialValues.put(TimeFrameTableColumns.VISIBLE, mIsVisible);
        return initialValues;
    }
}
