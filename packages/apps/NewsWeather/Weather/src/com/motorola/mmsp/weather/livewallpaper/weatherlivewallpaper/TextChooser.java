package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.text.TextPaint;
import android.util.Log;
import android.widget.TextView;

public class TextChooser {
	public TextView initSmallText(TextView tv) {
		tv.setTextColor(Color.WHITE);
		tv.setTextSize(10);
		tv.setAlpha(0.6f);
		return tv;
	}

	public TextView initBigText(TextView tv) {
		android.util.Log.d("jiayy","initBigText begin");
		tv.setTextColor(Color.RED);
		tv.setTextSize(35);
		tv.setTypeface(Typeface.DEFAULT_BOLD);
		tv.setBackgroundColor(Color.GREEN);
		tv.setAlpha(0.5f);
		return tv;
	}

}
