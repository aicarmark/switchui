package com.motorola.quicknote;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

public class MyImageView extends ImageView {
	private Bitmap mBm = null;
	public MyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		// TODO Auto-generated method stub
		if(null != mBm) {
			mBm.recycle();
		}
		mBm = bm;
		super.setImageBitmap(bm);
	}
	
	/*2012-10-10, add by amt_sunzhao for SWITCHUITWOV-190 */ 
	/*@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		if(null != mBm) {
			mBm.recycle();
		}
		super.finalize();
	}*/
	/*2012-10-10, add end*/ 
}
