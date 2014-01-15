package com.motorola.mmsp.weather.locationwidget.small;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.motorola.mmsp.plugin.widget.PluginWidget;
import com.motorola.mmsp.plugin.widget.PluginWidgetHostView;
import com.motorola.mmsp.plugin.widget.PluginWidgetModel;
import com.motorola.mmsp.weather.R;

public class WeatherProviderUsePlugin extends PluginWidget 
{
	private static final int MAX_NUM = 8;
	private Toast mToast;
	
	@Override
	public PluginWidgetModel createModel(Context context)
	{
		WeatherWidgetModel model = new WeatherWidgetModel(pkgContext);
		return model; 
	}

	@Override
	public void deleteView(View view)
	{
		((WeatherWidgetLayout)view).release();
		
		int widgetId = ((PluginWidgetHostView) view.getParent()).getPluginWidgetHostId();
		((WeatherWidgetModel)this.mModel).deleteWidget(widgetId);
		
		super.deleteView(view);
	}

	@Override
	public View createView(Context context, int widgetId)
	{
		Log.i("Weather","WeatherNewsWidgetProvider create view widgetId = "	+ widgetId);
		if(this.mViews.size() < MAX_NUM)
		{
			WeatherWidgetLayout view = new WeatherWidgetLayout(pkgContext,
					context);
			return view;
		}else{
			String tips = pkgContext.getResources().getString(R.string.add_widget_number_tip);
			if (mToast == null){
				mToast = Toast.makeText(context, tips, Toast.LENGTH_LONG);
			}
			mToast.show();
			return null;
		}
		
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);		
	}
}
