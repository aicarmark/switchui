package com.motorola.numberlocation;

interface INumberLocationCallback {
	void showResult(int result); 
	
	void showProgress(int type);
	void setProgressMax(int type,int value);
	void updateProgress(int type,int progress);
	void dismissProgress(int type);
}
