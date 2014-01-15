/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.motorola.mmsp.motohomex;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;
/*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
import android.widget.Toast;
/*ended by ncqp34*/
import com.motorola.mmsp.motohomex.R;

import java.io.IOException;
import java.util.ArrayList;
/*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
import android.widget.Button;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
/*ended by ncqp34*/

/*Added by amt_chenjing at 20120615 for NPM.B-1239*/
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
/*ended by amt_chenjing*/
public class WallpaperChooserDialogFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {

    private static final String TAG = "Launcher.WallpaperChooserDialogFragment";
    private static final String EMBEDDED_KEY = "com.motorola.mmsp.motohomex."
            + "WallpaperChooserDialogFragment.EMBEDDED_KEY";

    private boolean mEmbedded;
    private Bitmap mBitmap = null;

    private ArrayList<Integer> mThumbs;
    private ArrayList<Integer> mImages;
    private WallpaperLoader mLoader;
    private WallpaperDrawable mWallpaperDrawable = new WallpaperDrawable();
    /*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
    private boolean mIsSetting = false;
    private Button mSetButton;
    private Button mResetButton;
    private BroadcastReceiver mWallpaperReceiver;
    /*ended by ncqp34*/
    public static WallpaperChooserDialogFragment newInstance() {
        WallpaperChooserDialogFragment fragment = new WallpaperChooserDialogFragment();
        fragment.setCancelable(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(EMBEDDED_KEY)) {
            mEmbedded = savedInstanceState.getBoolean(EMBEDDED_KEY);
        } else {
            mEmbedded = isInLayout();
        }
	/*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
	IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        mWallpaperReceiver = new WallpaperObserver();
        getActivity().registerReceiver(mWallpaperReceiver, filter);
	/*ended by ncqp34*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EMBEDDED_KEY, mEmbedded);
    }

    private void cancelLoader() {
        if (mLoader != null && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel(true);
            mLoader = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        cancelLoader();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelLoader();

	/*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
        if (mWallpaperReceiver != null) {
            getActivity().unregisterReceiver(mWallpaperReceiver);
        }
	/*ended by ncqp34*/
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        /* On orientation changes, the dialog is effectively "dismissed" so this is called
         * when the activity is no longer associated with this dying dialog fragment. We
         * should just safely ignore this case by checking if getActivity() returns null
         */
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    /* This will only be called when in XLarge mode, since this Fragment is invoked like
     * a dialog in that mode
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        findWallpapers();

        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        findWallpapers();

        /* If this fragment is embedded in the layout of this activity, then we should
         * generate a view to display. Otherwise, a dialog will be created in
         * onCreateDialog()
         */
        if (mEmbedded) {
            View view = inflater.inflate(R.layout.wallpaper_chooser, container, false);
            view.setBackground(mWallpaperDrawable);

            final Gallery gallery = (Gallery) view.findViewById(R.id.gallery);
            gallery.setCallbackDuringFling(false);
            gallery.setOnItemSelectedListener(this);
            gallery.setAdapter(new ImageAdapter(getActivity()));

            View setButton = view.findViewById(R.id.set);
            setButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectWallpaper(gallery.getSelectedItemPosition());
                }
            });
	    /*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
	    mSetButton = (Button)view.findViewById(R.id.set);
	    mResetButton = (Button)view.findViewById(R.id.reset);
    	    /*ended by ncqp34*/

	    /*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
	    view.findViewById(R.id.reset).setOnClickListener(new OnClickListener() {
		@Override
		public void onClick(View v) {
		    Log.d(TAG,"reset to default---Flex.mDefaultPos==" + Flex.mDefaultPos);
		    Context ctx = getActivity();
		    if(ctx == null) return;
		    if(Flex.mDefaultPos == -1){
			try {
    				WallpaperManager wm;
				// Try to restore the system default wallpaper
				wm = WallpaperManager.getInstance(ctx);
				wm.clear();
				//Show a toast to inform the user that the default wallpaper
				// was restored and finish this activity
				Toast.makeText(ctx,R.string.wallpaper_default_restored,
						Toast.LENGTH_SHORT).show();
 			} catch (IOException e) {
				// Failed restoring default wallpaper
				Log.e(TAG, "Failed to restore default wallpaper: " + e);
			}
		    }else{
			Log.i(TAG, "reset to default---!= -1---position==" + Flex.mDefaultPos);
			selectWallpaper(Flex.mDefaultPos);
			Toast.makeText(ctx,R.string.wallpaper_default_restored,
				Toast.LENGTH_SHORT).show();
		    }

          	    Activity activity = getActivity();
            	    activity.setResult(Activity.RESULT_OK);
            	    activity.finish();
		}
	     });
	    /*ended by ncqp34*/
            return view;
        }
        return null;
    }

    private void selectWallpaper(int position) {
	Log.d(TAG,"selectWallpaper---enter---position == " + position);
	/*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
	if (position < 0) {
	    return ;
	}
	/*ended by ncqp34*/
	/*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
	mIsSetting = true;
        mSetButton.setEnabled(false);
	mResetButton.setEnabled(false);
	/*ended by ncqp34*/
	/*Added by amt_chenjing at 20120615 for NPM.B-1239*/
		FileInputStream fileInput = null;
		InputStream inputStream = null;
        try {
            WallpaperManager wpm = (WallpaperManager) getActivity().getSystemService(
                    Context.WALLPAPER_SERVICE);
	   /*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
	   Log.d(TAG,"position == " + position   + "Flex.getHiddenhasWallpaperCount() ==" + Flex.getHiddenhasWallpaperCount());
	   if (position < Flex.getHiddenhasWallpaperCount()) {
				File bitmapFile = new File(
						Flex.mImagesFromAPHidden.get(position));
				fileInput = new FileInputStream(bitmapFile);
				inputStream = new BufferedInputStream(fileInput);
				if (inputStream != null) {
					wpm.setStream(inputStream);
					inputStream.close();
					fileInput.close();
				}

				else {
					wpm.setBitmap(BitmapFactory
							.decodeFile(Flex.mImagesFromAPHidden.get(position)));
				}
				/*ended by amt_chenjing*/
	    } else{
                wpm.setResource(mImages.get(position));
	    }
	   /*ended by ncqp34*/
            Activity activity = getActivity();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        } catch (IOException e) {
            Log.e(TAG, "Failed to set wallpaper: " + e);
	    /*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
	    mIsSetting = false;
	    mSetButton.setEnabled(true);
	    mResetButton.setEnabled(true);
	    /*ended by ncqp34*/

        }
    }

    // Click handler for the Dialog's GridView
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectWallpaper(position);
    }

    // Selection handler for the embedded Gallery view
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mLoader != null && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel();
        }
        mLoader = (WallpaperLoader) new WallpaperLoader().execute(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void findWallpapers() {
        mThumbs = new ArrayList<Integer>(24);
        mImages = new ArrayList<Integer>(24);

        final Resources resources = getResources();
        // Context.getPackageName() may return the "original" package name,
        // com.motorola.mmsp.motohomex; Resources needs the real package name,
        // com.motorola.mmsp.motohomex. So we ask Resources for what it thinks the
        // package name should be.
        final String packageName = resources.getResourcePackageName(R.array.wallpapers);
	/*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
	String xmlstr = "";
	boolean needCDA = false;
	xmlstr = Flex.getWallpaperFlex(getActivity());
	if (!xmlstr.trim().equals("")) {
		needCDA = true;
	}
	if(needCDA == false){
		addWallpapers(resources, packageName, R.array.wallpapers);
		Log.d(TAG, "Using resource wallpapers");
	}
	Log.d(TAG, "Hidden has wallpaper");
	Flex.addWallpapersFromHidden(xmlstr ,mThumbs ,mImages);
        //addWallpapers(resources, packageName, R.array.wallpapers);
	/*ended by ncqp34*/
        addWallpapers(resources, packageName, R.array.extra_wallpapers);
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        final String[] extras = resources.getStringArray(list);
        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0) {
                final int thumbRes = resources.getIdentifier(extra + "_small",
                        "drawable", packageName);

                if (thumbRes != 0) {
                    mThumbs.add(thumbRes);
                    mImages.add(res);
                    // Log.d(TAG, "add: [" + packageName + "]: " + extra + " (" + res + ")");
                }
            }
        }
    }

    private class ImageAdapter extends BaseAdapter implements ListAdapter, SpinnerAdapter {
        private LayoutInflater mLayoutInflater;

        ImageAdapter(Activity activity) {
            mLayoutInflater = activity.getLayoutInflater();
        }

        public int getCount() {
            return mThumbs.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.wallpaper_item, parent, false);
            } else {
                view = convertView;
            }

            ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);
	    /*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
	    Drawable retVal = null;
	    if (position < Flex.getHiddenhasWallpaperCount()&& Flex.isHiddenhasWallpaper() == true) {
			retVal = Drawable.createFromPath(Flex.mThumbsFromAPHidden
					.get(position));
			image.setImageDrawable(retVal);
	    }else{ 	   
                int thumbRes = mThumbs.get(position);
                image.setImageResource(thumbRes);
                Drawable thumbDrawable = image.getDrawable();
                if (thumbDrawable != null) {
                   thumbDrawable.setDither(true);
                } else {
                  Log.e(TAG, "Error decoding thumbnail resId=" + thumbRes + " for wallpaper #"
                        + position);
              }
	    }
	    /*ended by ncqp34*/
            return view;
        }
    }

    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        BitmapFactory.Options mOptions;

        WallpaperLoader() {
            mOptions = new BitmapFactory.Options();
            mOptions.inDither = false;
            mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            if (isCancelled()) return null;
            try {
		/*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
		Log.d(TAG, "params[0]=" + params[0]);
		if (params[0] < Flex.getHiddenhasWallpaperCount()) {
		    return BitmapFactory.decodeFile(Flex.mImagesFromAPHidden
							.get(params[0]));
		} else{
	        /*ended by ncqp34*/   
                /*Added by cdg638 at 2012-09-20 for SWITCHUI-2871: Home stop when rotate quickly*/
                   if(isAdded()){
                    return BitmapFactory.decodeResource(getResources(),
                            mImages.get(params[0]), mOptions);
                   }else{
                    return null;
                   }
                /*Ended by cdg638*/
		}
            } catch (OutOfMemoryError e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b == null) return;

            if (!isCancelled() && !mOptions.mCancel) {
                // Help the GC
                if (mBitmap != null) {
                    mBitmap.recycle();
                }

                View v = getView();
                if (v != null) {
                    mBitmap = b;
                    mWallpaperDrawable.setBitmap(b);
                    v.postInvalidate();
                } else {
                    mBitmap = null;
                    mWallpaperDrawable.setBitmap(null);
                }
                mLoader = null;
            } else {
               b.recycle();
            }
        }

        void cancel() {
            mOptions.requestCancelDecode();
            super.cancel(true);
        }
    }

    /**
     * Custom drawable that centers the bitmap fed to it.
     */
    static class WallpaperDrawable extends Drawable {

        Bitmap mBitmap;
        int mIntrinsicWidth;
        int mIntrinsicHeight;

        /* package */void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            if (mBitmap == null)
                return;
            mIntrinsicWidth = mBitmap.getWidth();
            mIntrinsicHeight = mBitmap.getHeight();
        }

        @Override
        public void draw(Canvas canvas) {
            if (mBitmap == null) return;
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int x = (width - mIntrinsicWidth) / 2;
            int y = (height - mIntrinsicHeight) / 2;
            canvas.drawBitmap(mBitmap, x, y, null);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }

        @Override
        public void setAlpha(int alpha) {
            // Ignore
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // Ignore
        }
    }
    /*Added by ncqp34 at May-21-2012 for IPD.B-2612*/
    class WallpaperObserver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
	    Log.d(TAG,"WALLPAPER_CHANGED: mIsSetting =" + mIsSetting );
 	    if(mIsSetting){
	        mIsSetting = false;
		mSetButton.setEnabled(true);
	        mResetButton.setEnabled(true);
	    }
        }
    }
    /*ended by ncqp34*/
}
