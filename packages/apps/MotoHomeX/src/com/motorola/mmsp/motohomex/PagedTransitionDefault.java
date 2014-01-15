 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */
package com.motorola.mmsp.motohomex;

import android.view.View;

import android.view.ViewGroup;
/*
import com.motorola.mmsp.plugin.widget.PluginWidgetHostView;
import com.motorola.mmsp.plugin.widget.PluginWidgetScrollListener;
*/
import android.util.Log;
public class PagedTransitionDefault extends PagedTransition {
    private static final PagedTransitionDefault INSTANCE = new PagedTransitionDefault();

    public PagedTransitionDefault() {
        super();
    }

    public static PagedTransitionDefault getInstance() {
        return INSTANCE;
    }

    public void screenScrolled(PagedView pagedView, int screenCenter) {
        if (!(pagedView instanceof Workspace)) {
           if (overScrolled(pagedView, screenCenter))
               return;
        } else {
           if (c2!=null) c2.clearAnimation();
           if (overScrolled(pagedView, screenCenter)) return;
           /*add by bvq783 for plugin*/
           super.screenScrolled(pagedView, screenCenter);
           if (Math.abs(pro)>1.0f) return;

           if (left) {
            c1 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage);
            c2 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage+1);

            if (c1 != null) {
                c1.setCameraDistance(CAMERA_DISTANCE);
                ViewGroup vv = (ViewGroup)((ViewGroup) c1).getChildAt(0);
                for ( int j=0; j < vv.getChildCount(); j++) {
                    View vvv = vv.getChildAt(j);
                    /*
                    if (vvv != null && vvv instanceof PluginWidgetHostView) {
                        PluginWidgetScrollListener listen = (PluginWidgetScrollListener)((PluginWidgetHostView)vvv).getScrollListener();
                        if (listen != null) {
                            listen.scrolled(pro, 2, true);
                        }
                    }
                    */
                }
            }
            if (c2 != null) {
                c2.setCameraDistance(CAMERA_DISTANCE);
                ViewGroup vv = (ViewGroup)((ViewGroup) c2).getChildAt(0);
                for ( int j=0; j < vv.getChildCount(); j++) {
                    View vvv = vv.getChildAt(j);
                    /*
                    if (vvv != null && vvv instanceof PluginWidgetHostView) {
                        PluginWidgetScrollListener listen = (PluginWidgetScrollListener)((PluginWidgetHostView)vvv).getScrollListener();
                        if (listen != null) {
                            listen.scrolled(pro, 1, true);
                        }
                    }
                    */
                }
            }
        } else {
            c1 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage);
            c2 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage-1);

            if (c1 != null) {
                c1.setCameraDistance(CAMERA_DISTANCE);
                ViewGroup vv = (ViewGroup)((ViewGroup) c1).getChildAt(0);
                for ( int j=0; j < vv.getChildCount(); j++) {
                    View vvv = vv.getChildAt(j);
                    /*
                    if (vvv != null && vvv instanceof PluginWidgetHostView) {
                        PluginWidgetScrollListener listen = (PluginWidgetScrollListener)((PluginWidgetHostView)vvv).getScrollListener();
                        if (listen != null) {
                            listen.scrolled(pro, 4, true);
                        }
                    }
                    */
                }
            }

            if (c2 != null) {
                c2.setCameraDistance(CAMERA_DISTANCE);
                ViewGroup vv = (ViewGroup)((ViewGroup) c2).getChildAt(0);
                for ( int j=0; j < vv.getChildCount(); j++) {
                    View vvv = vv.getChildAt(j);
                    /*
                    if (vvv != null && vvv instanceof PluginWidgetHostView) {
                        PluginWidgetScrollListener listen = (PluginWidgetScrollListener)((PluginWidgetHostView)vvv).getScrollListener();
                        if (listen != null) {
                            listen.scrolled(pro, 3, true);
                        }
                    }
                    */
                }
            }
        }
        /*add by bvq783 end*/
       }
     }
}
