package com.motorola.mmsp.sinaweather.app;

public class WeatherScreenConfig {
    
    public static final int PORTRAIT_ONLY = 0;
    public static final int PROTRAIT_AND_LANDSCAPE = 1;
    public static final int LANDSCAPE_ONLY = 2;
    
    public static final int ANIMATION_RES_ONLY = 0;
    public static final int STILL_IMAGE_ONLY = 1;

    public static final int DETAIL_VIEW_INDEX = 0;
    public static final int SETTING_VIEW_INDEX = 1;
    public static final int CITIES_VIEW_INDEX = 2;
    public static final int SEARCHCITY_VIEW_INDEX = 3;

    public static final int BG_DEDLAY_TIME = 0;
    
    
    private static final int GENERIC_PROTRAIT_OR_LANDSCAPE = PROTRAIT_AND_LANDSCAPE;
public static final  int[] PROTRAIT_OR_LANDSCAPE = new int[]{PORTRAIT_ONLY};//new int[]{PROTRAIT_AND_LANDSCAPE,PROTRAIT_AND_LANDSCAPE,PROTRAIT_AND_LANDSCAPE,PROTRAIT_AND_LANDSCAPE};
    
    
    public static int getResType() {
    	return STILL_IMAGE_ONLY;
    }
    
    public static int getWeatherScreenConfig(int whichScreen) {
    	int result = GENERIC_PROTRAIT_OR_LANDSCAPE;
    	if (PROTRAIT_OR_LANDSCAPE != null && whichScreen < PROTRAIT_OR_LANDSCAPE.length) {
    		result = PROTRAIT_OR_LANDSCAPE[whichScreen];
    	}
    	
    	if (result < 0) {
    		result = GENERIC_PROTRAIT_OR_LANDSCAPE;
    	}
    	return result;
    }
}

