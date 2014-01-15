package com.motorola.quicknote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

 public class QNActivity extends Activity {
     final static int   _INVALID_VIEW_ID        = -1;
     
     
     protected boolean            _b_content_changed = false;
     protected int                _viewroot = _INVALID_VIEW_ID;
     protected ViewTreeObserver   _vt_observer = null;
     protected _QNGlobalLayoutListener  _listener = new _QNGlobalLayoutListener();
     protected View               _prevV = null;
     
     // check view is really match orientation
     private boolean _does_view_match_orientation(View viewroot) {
         // NOTE !!! 
         // !!! We should with viewroot's width value. !!!
         // Due to IME and status bar, height value may lead unexpected result when we decide!!!
         // NOTE !!!
         //  Why this is needed!!!
         //  if we call "setContentView" in "onConfigurationChanged" to support changing orientation, 
         // sometimes you can see that change progress is done with two steps.
         //  Step one is "View is changed by automatic view change.
         //  And second step is changing into our goal (view set by setContentView()).
         //  To aviud, this kind of walk-around is needed.
         //  And there is also one more advantage.
         //  Activity can know calculated size of view in _OnViewRoot_layouted()!!!...
         /// (This is sometimes very useful!!!)
         int mid = (QNUtil.screen_height(this) + QNUtil.screen_width(this))/2;
         return ( QNUtil.is_landscape(this) && viewroot.getWidth()>mid )
                 || ( !QNUtil.is_landscape(this) && viewroot.getWidth()<mid );
     }
     
     protected class _QNGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
         
         public void onGlobalLayout() {
             if(_INVALID_VIEW_ID != _viewroot) {
                 View v = findViewById(_viewroot);
                 
                 // This "condition-checking" filters "changing orientation"
                 if( _does_view_match_orientation(v) 
                     && _b_content_changed  &&  v != _prevV ) {
                     // Changing orientation -- highest priority!!!
                     QNDev.log("QNActivity : ViewRoot (" + v.getWidth() + ", " + v.getHeight() + ")");
                     _onContent_layouted();
                     _prevV = v;
                     _b_content_changed  = false;
                 }
                 /* -- We can check changing layout with this...
                 else if (!_b_content_changed && v == _prevV) 
                            && _viewroot_prevh != v.getHeight()) {
                     // usually, when soft keyboard(IME) is shown.
                     _viewroot_prevh = v.getHeight();
                     _OnViewRoot_layouted();
                 }
                 */
             } else {
                 _b_content_changed  = false;
             }
         }
     };
     
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
     }
     
     // For feedback internally...
     public void force_onActivityResult (int requestCode, int resultCode, Intent intent) {
         onActivityResult(requestCode, resultCode, intent);
     }

     /**
      * This should be called before activity content is set!.
      * @param rid
      */
     protected void _viewroot(int rid) {
         _viewroot = rid;
     }
     
     
     protected void _init_view_tree_observer() {
         if(_INVALID_VIEW_ID != _viewroot) {
             final View v = findViewById(_viewroot);
             _vt_observer = v.getViewTreeObserver();
             _vt_observer.addOnGlobalLayoutListener(_listener);
             QNDev.log("QNActivity : add view tree observer...");
         }
     }
     
     protected void _onContent_layouted() {}
     
     @Override
     public void onContentChanged() {
         super.onContentChanged();
         QNDev.log("QNActivity : onContentChanged");
         _init_view_tree_observer();
         _b_content_changed  = true;
     }     
     
     /** This function matchs "getLastNonConfigurationInstance()" **/
     @Override
     public void onConfigurationChanged(Configuration newConfig) {
         super.onConfigurationChanged(newConfig);
         // What should be done here????
     }
 }