/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * 
 * ***************************************************************************************
 * [QuickNote] Image Content Viewer main
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created - template .
 * 
 * 
 *****************************************************************************************/


package com.motorola.quicknote.content;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.provider.MediaStore.Images.Media;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Matrix;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Region;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;

import com.motorola.quicknote.QNActivity;
import com.motorola.quicknote.QNDev;
import com.motorola.quicknote.QNUtil;
import com.motorola.quicknote.QNSetReminder;
import com.motorola.quicknote.R;

import java.util.Calendar;   

/**
 * Package Private!!
 */
class QNContent_Image extends QNContent
{
    private Bitmap      _bm = null;
   // private Bitmap     thumBmp = null;

    private static final String TAG               = "QNContent_Image";
    private int mWidth = 0;
    private int mHeight = 0;

    
    private Rect _Pure_image_bound(ImageView iv, Bitmap bm) {
        // Assume that "ImageView" shows real image on it's center and scale/shrink it if needed.!!
        // (in Eclair, it is true)
        int[] out = new int[2];
        int sample_size = 1;
        QNUtil.shrink_fixed_ratio(iv.getWidth(), iv.getHeight(), 
                                  bm.getWidth(), bm.getHeight(), out);
        QNDev.log("(" + iv.getWidth() + ", " + iv.getHeight() + ") / (" + bm.getWidth() + ", " + bm.getHeight() + ") / (" + out[0] + ", " + out[1] + ")");
        int wgap = iv.getWidth() - out[0];
        wgap = (wgap>0)? wgap: -wgap;
        int hgap = iv.getHeight() - out[1];
        hgap = (hgap>0)? hgap: -hgap;
        
        return new Rect(wgap/2, hgap/2, out[0]+wgap/2, out[1]+hgap/2);        
    }    
    
    
    QNContent_Image(String mimetype, Uri uri) { 
        super(mimetype, uri);
        QNDev.qnAssert(null != uri && uri.getScheme().equals("file"));
    }

    /************************
     * Implement QNContent_Interface
     */

    @Override
    public boolean close() {
        if(null != _bm) { _bm.recycle(); }
   //     if (null != thumBmp) { thumBmp.recycle();}
        return true;
    }
    
    @Override
    public NotestateE state() { return NotestateE.IDLE; }

    /* -- obsolete
    @Override
    public int Type() { return TYPE_IMAGE; }
     */

    @Override
    public SharingTypeE[] shared_by() {
        SharingTypeE[] st = new SharingTypeE[2];
        st[0] = SharingTypeE.MESSAGE;
        st[1] = SharingTypeE.EMAIL;
        return st;
    }

    @Override
    public boolean isBGColor() { return false; }

   
    @Override
    public View noteView(Context context, ViewGroup parent) {
        QNDev.qnAssert(null != context && null != parent);
        if (null == context || null == parent ) { return null;}

        if(null != _bm) { _bm.recycle(); } // memory...
        
        try {
          String filePath = QNUtil.transFilePath(uri().toString());
          mWidth = context.getResources().getInteger(R.integer.image_width);
          mHeight = context.getResources().getInteger(R.integer.image_height);
          _bm = QNUtil.decode_image_file(filePath, mWidth, mHeight, false);
          if (_bm == null) {
            fUsable = false;
            return null;
          }

           // decoding based on screen size is enough!!
           
           ImageView ret = (ImageView)parent.findViewById(R.id.image_content);
           ret.setImageBitmap(_bm);
           fUsable = true;
           return ret;
        } catch  (Exception ex) {
            QNDev.log(TAG+"noteView catch error = "+ex);
            return null;
        } catch (java.lang.OutOfMemoryError e) {
            QNDev.log(TAG+" noteView catch out of memory error = "+e);
            return null;
        }
    }

    @Override
    public View detailView(Context context, ViewGroup parent, Uri fileUri) {
        QNDev.qnAssert(null != context && null != parent);
        if (null == context || null == parent ) { return null;}

        if(null != _bm) { _bm.recycle(); } // memory...

        try {
          String filePath = QNUtil.transFilePath(fileUri.toString());
          int width = context.getResources().getInteger(R.integer.detail_image_width);
          int height = context.getResources().getInteger(R.integer.detail_image_height);
          _bm = QNUtil.decode_image_file(filePath, width, height, false);

          if (_bm == null) {
            fUsable = false;
            return null;
          }

           // decoding based on screen size is enough!!
           ImageView ret = (ImageView)parent.findViewById(R.id.image_detail);
           ret.setImageBitmap(_bm);
           fUsable = true;
           return ret;
        } catch  (Exception ex) {
            QNDev.log(TAG+"detailView catch error = "+ex);
            return null;
        } catch (java.lang.OutOfMemoryError e) {
            QNDev.log(TAG+" noteView catch out of memory error = "+e);
            return null;
        }
    }


    @Override
    public View updateNoteView(Context context, ViewGroup parent, Uri newFileUri) {
        QNDev.qnAssert(null != context && null != parent);
        if (null == context || null == parent ) { return null;}

        if(null != _bm) { _bm.recycle(); } // memory...
        
        try {
          String filePath = QNUtil.transFilePath(newFileUri.toString());
          _bm = QNUtil.decode_image_file(filePath, mWidth, mHeight, false);
          if (_bm == null) {
            fUsable = false;
            return null;
          }

           // decoding based on screen size is enough!!
           
           ImageView ret = (ImageView)parent.findViewById(R.id.image_content);
           ret.setImageBitmap(_bm);
           fUsable = true;
           return ret;
        } catch  (Exception ex) {
            QNDev.log(TAG+"detailView catch error = "+ex);
            return null;
        } catch (java.lang.OutOfMemoryError e) {
            QNDev.log(TAG+"detailView catch out of memory error = "+e);
            return null;
        }

    }


    private Bitmap resizeBitmap (Bitmap srcBmp, int width, int height) {
        int imgWidth = srcBmp.getWidth();
        int imgHeight = srcBmp.getHeight();
        int targetWidth = imgWidth;
        int targetHeight = imgHeight;
        Bitmap tempBmp = null;
        boolean noChange = false;

        QNDev.log("imgWidth="+ imgWidth + " imgHeight="+ imgHeight);
        try {
            float ratioContainer = (float)width/ (float)height;
            float ratioSource = (float)imgWidth/ (float)imgHeight;
            QNDev.log("width=" + width + " height=" + height);
            QNDev.log("ratioContainer="+ ratioContainer + " ratioSource="+ ratioSource);
            if (ratioSource > ratioContainer) {
                QNDev.log(TAG+"ratioSource >, resize srcBmp");
                targetWidth = (int)(imgHeight * ratioContainer);
                targetHeight = imgHeight;
                QNDev.log("ratioSource > ratioContainer " + " targetWidth="+ targetWidth + " targetHeight="+ targetHeight);
                tempBmp = Bitmap.createBitmap(srcBmp,
                                                                     (imgWidth - targetWidth) / 2,
                                                                     0,
                                                                     (imgWidth + targetWidth) / 2,
                                                                     targetHeight);
            } else if (ratioSource < ratioContainer) {
                 QNDev.log(TAG+"ratioSource <, resize srcBmp");
                targetWidth = imgWidth;
                targetHeight = (int)(imgWidth / ratioContainer);
                QNDev.log("ratioSource < ratioContainer " + "targetWidth="+ targetWidth + " targetHeight="+ targetHeight);
                tempBmp = Bitmap.createBitmap(srcBmp,
                                                                     0,
                                                                     (imgHeight - targetHeight) / 2,
                                                                     targetWidth,
                                                                     (imgHeight + targetHeight) / 2);
            }  else {
                QNDev.log(TAG+"ratioSource =, return srcBmp ");
                noChange = true;
            }

            Matrix matrix = new Matrix();
            matrix.postScale( ((float)width) / targetWidth, ((float)height) / targetHeight);
            QNDev.log("targetWidth="+ targetWidth + " targetHeight="+ targetHeight + " matrix="+ matrix.toString());

            if (noChange) {
                tempBmp = Bitmap.createBitmap(srcBmp, 0, 0, targetWidth, targetHeight, matrix, true);
            } else {
				/*2012-9-6, add by amt_sunzhao for T810T_P003933 */ 
             //tempBmp = Bitmap.createBitmap(tempBmp, 0, 0, targetWidth, targetHeight, matrix, true);
            	final Bitmap newTmpBmp = Bitmap.createBitmap(tempBmp, 0, 0, targetWidth, targetHeight, matrix, true);
            	/*2012-11-26, add by amt_sunzhao for SWITCHUITWO-96 */ 
            	if((null != tempBmp)
            			&& (tempBmp != newTmpBmp)) {
            		/*2012-11-26, add end*/
            		tempBmp.recycle();
            	}
            	tempBmp = newTmpBmp;
				/*2012-9-6, add end*/ 
            }

            return tempBmp;

        } catch (Exception ex) {
            return null;
        } catch (java.lang.OutOfMemoryError e) {
            return null;
        }
    }


     @Override
//     public Bitmap widgetThumbBitmap(ContentResolver cr, Long origId, Uri thumbUri, int width, int height) {
     public Bitmap widgetThumbBitmap(ContentResolver cr, Long origId, int width, int height) {

       // if (null != thumBmp) { thumBmp.recycle(); }
        Bitmap thumBmp = null;
        
        /**
          Because micro thumbnail will cause severe distortion of the image after enlarging, 
          we directly use mini thumbnail here
         */ 
        //try to get the bmp from big thumbnail 512 x 384
        try {
//               thumBmp = Media.getBitmap(cr, thumbUri);
			/*2012-9-6, add by amt_sunzhao for T810T_P003933 */ 
        	QNDev.log("getThumbnail start.");
            thumBmp = Thumbnails.getThumbnail(cr, origId, Thumbnails.MINI_KIND, null);
            QNDev.log("getThumbnail end.");

            if (null != thumBmp) {
               QNDev.log("widgetThumbBitmap"+"Get the mini thumbnail!!!");
               //return resizeBitmap(thumBmp, width, height);
               final Bitmap bmp = resizeBitmap(thumBmp, width, height);
               if(bmp != thumBmp) {
            	   thumBmp.recycle();
               }
               return bmp;
			/*2012-9-6, add end*/ 
            }
 
            QNDev.log("widgetThumbBitmap"+"Cannot get the thumbnail!");
            return null;
         } catch (Exception ex) {
            return null;
         } catch (java.lang.OutOfMemoryError e) {
            return null;
         }

     /*   if (null != origId) {
           try {
              //try to get the micro thumbnal:96*96
              thumBmp = Thumbnails.getThumbnail(cr, origId, Thumbnails.MICRO_KIND, null);
           } catch (Exception e) {
              QNDev.log("widgetThumbBitmap"+"Error to get micro thumb: e = "+e);
             //do nothing
           }
        }

        if (null != thumBmp ) {
            QNDev.log("widgetThumbBitmap"+"Get the micro thumbnail!!!");
            return resizeBitmap(thumBmp, width, height);
            //since the micro thumbnail is 96*96, how about to directly return it without resizing it?
        } else {
      
            //try to get the bmp from big thumbnail 512 x 384
            if (null != thumbUri) {
               try {
                  thumBmp = Media.getBitmap(cr, thumbUri);
               } catch (Exception e) {
                  //do nothing
               }
            }
            if (null != thumBmp) {
               QNDev.log("widgetThumbBitmap"+"Get the mini thumbnail!!!");
               return resizeBitmap(thumBmp, width, height);
            }
            QNDev.log("widgetThumbBitmap"+"Cannot get the thumbnail!");
            return null;
        } 
     */
   }

    
    @Override
    public Bitmap widgetBitmap(Context context, int width, int height) {
        QNDev.qnAssert(null != context);
        if (null == context ) { return null;}
        QNDev.log("width="+ width + " height="+ height);
        
       // if(null != _bm) { _bm.recycle(); }
        Bitmap bmp = null;

        try {
            if (mBitmapType == BitmapTypeE.SCREEN_WIDGET_BITMAP) {
                QNDev.logd("QuickNote","create screen widget type bitmap");
                String filePath = QNUtil.transFilePath(uri().toString());
                bmp = QNUtil.decode_image_file(filePath, width, height, false);
            } else {  
                //get the size of the bitmap
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                opts.inSampleSize = 1; 
                String filePath = QNUtil.transFilePath(uri().toString());
                BitmapFactory.decodeFile(filePath, opts);
                /*2012-12-12, add by amt_sunzhao for SWITCHUITWO-226 */
                final int doubles = 2;
                // request a reasonably sized output image, 
                while (opts.outHeight > height || opts.outWidth > width) {
                	/*
                	 * To improve the quality of bitmap, bitmap can not be small.
                	 * If the width/height of bitmap are smaller than target
                	 *  width/height, when invoke resizeBitmap, the bitmap will
                	 *  fuzzy.
                	 */
                	if((height >= opts.outHeight/ doubles)
                			&& (width >= opts.outWidth/ doubles)) {
                		break;
                	}
                	/*2012-12-12, add end*/
                    opts.outHeight /= 2;
                    opts.outWidth /= 2;
                    opts.inSampleSize *= 2;
                }

                //get the image for real now
                QNDev.log("QNContent_Image"+"inSampleSize = "+opts.inSampleSize);
                opts.inJustDecodeBounds = false;
                bmp = BitmapFactory.decodeFile(filePath, opts);
            }
        } catch (Exception ex) {
            return null;
        } catch (java.lang.OutOfMemoryError e) {
            return null;
        }
        

        if(bmp == null) {
            return null;
        } else {
			/*2012-9-6, add by amt_sunzhao for T810T_P003933 */ 
            //return resizeBitmap(bmp, width, height);
        	final Bitmap retBmp = resizeBitmap(bmp, width, height);
        	if(retBmp != bmp) {
        		bmp.recycle();
        	}
        	return retBmp;
			/*2012-9-6, add end*/ 
        }

/*
        Canvas canvas = new Canvas();
        Bitmap returnBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        returnBitmap.eraseColor(Color.TRANSPARENT);
        canvas.setBitmap(returnBitmap);
        
        canvas.clipRect((float)(width*0.9), (float)(height*0.916), (float)width, (float)height);
        canvas.clipRect(0, 0, width, height, Region.Op.REVERSE_DIFFERENCE );

        Matrix matrixCanvas = new Matrix();
        canvas.drawBitmap(tempBmp, matrixCanvas, null);
  */              
/*
        float startX = 0, startY = 0;
        float endX = 0, endY = 0;

        if(imgWidth >= width && imgHeight >= height){
            startX = (imgWidth - width) / 2.0f;
            startY = (imgHeight - height) / 2.0f;
            endX = (imgWidth + width) / 2.0f;
            endY = (imgHeight + height) / 2.0f;

            tempBmp = Bitmap.createBitmap(_bm, (int)startX, (int)startY, (int)(endX-startX), (int)(endY-startY));
        }
        else if(imgWidth >= width){
            startX = (imgWidth - width) / 2.0f;
            startY = 0;

            tempBmp = Bitmap.createBitmap(_bm, (int)startX, (int)startY, width, imgHeight);
        }
        else if(imgHeight >= height){
            startX = 0;
            startY = (imgHeight - height) / 2.0f;

            tempBmp = Bitmap.createBitmap(_bm, (int)startX, (int)startY, imgWidth, height);
        }
        else if((float)imgHeight/height >= (float)imgWidth/width){
            float offset = (float)height / imgHeight;
            tempBmp = Bitmap.createScaledBitmap(_bm, (int)(offset*imgWidth), (int)(offset*imgHeight), true);
        }
        else if((float)imgHeight/height < (float)imgWidth/width){
            float offset = (float)width / imgWidth;
            tempBmp = Bitmap.createScaledBitmap(_bm, (int)(offset*imgWidth), (int)(offset*imgHeight), true);
        }*/
        
      //  return returnBitmap; 
    }


    @Override
    public Bitmap widgetBitmap(Context context, int width, int height, int bgcolor)
    {
       return widgetBitmap(context, width, height);
    }

     @Override
    public CharSequence getTextNoteContent()
    {return "";}
}
