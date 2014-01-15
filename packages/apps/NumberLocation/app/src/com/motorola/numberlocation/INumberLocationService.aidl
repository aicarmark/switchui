package com.motorola.numberlocation;

import com.motorola.numberlocation.INumberLocationCallback;

interface INumberLocationService {
	void registerCallback(INumberLocationCallback cb);
	void unregisterCallback(INumberLocationCallback cb);
	String getLocationByNumber(String number);
	String getNumberByCityLocation(String number);
	String getNumberByCountryLocation(String number);
	int updateNumberLocationDatabase();
	void cancelUpdateNumberLocationDatabase();
}
