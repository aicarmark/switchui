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

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
/*2012-6-25, add by bvq783 for switchui-1581*/
import android.widget.Button;
/*2012-6-25, add end*/

public class DragPanel extends RelativeLayout{
	
//    private static final String Tag = "DragPanel";
    
    public static int ITEM_WIDTH_PORT = 105; // default item width pixels
    public static int ITEM_HEIGHT_PORT = 195;// default item height pixels
    
    public static int ITEM_WIDTH_LAND = 195; // default item width pixels
    public static int ITEM_HEIGHT_LAND = 105;// default item height pixels
    
    public static int TOP_MARGIN_PORT = 80; // default top margin pixels
    public static int BOTTOM_MARGIN_PORT = 110;// default bottom margin pixels
    
    public static int TOP_MARGIN_LAND = 42; // default top margin pixels
    public static int BOTTOM_MARGIN_LAND = 80;// default bottom margin pixels

    public static int LEFT_MARGIN_PORT = 35; // default left margin pixels
    public static int RIGHT_MARGIN_PORT = 35;// default right margin pixels
    
    public static int LEFT_MARGIN_LAND = 90; // default left margin pixels
    public static int RIGHT_MARGIN_LAND = 90;// default right margin pixels

    public int trash_area_height; // trash area height pixels
    public int trash_hit_area_height; // easy to hit if let hit area bigger than display
    public int taskbar_height; // task bar height pixels
    public int btnbar_height; //bottom button bar height pixels
    
    private int panel_width, panel_height;// panel contains all items, it's width and height pixels
    
    private static int item_width; // item width pixels
    private static int item_height; // item height pixels
    
    private int x_padding; // x direction padding pixels
    private int y_padding; // y direction padding pixels
    
    private int top = TOP_MARGIN_PORT; // top margin pixels
    private int bottom = BOTTOM_MARGIN_PORT; // bottom margin pixels
    private int left = LEFT_MARGIN_PORT; // left margin pixels
    private int right = RIGHT_MARGIN_PORT; // right margin pixels
    
    // Global variable store all items' ImageViews in order
    private ImageView [] items = new ImageView[9]; 

    private View layout_add; // layout contains add button
    private View layout_remove; // layout contains remove button
    private ImageButton imgbtn_add; // add button
    private ImageView imgbtn_remove;//remove button
    //private TextView tv_title; // TextView shows the title
    /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
    private TextView tv_remove; 
    private TextView tv_add; 
    /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
    /*2012-6-25, add by bvq783 for switchui-1581*/
    private Button setTran;
    /*2012-6-25, add end*/

    private int down_rel_x; // coordinate x relative to ImageView left top
    private int down_rel_y; // coordinate y relative to ImageView left top
    
    private boolean dragging = false; //if it is dragging mode
    private boolean inTrash = false; //if touch in trash area mode
    
    private int vacant = -1; //the index that is vacant when dragging
    private int drag = -1; //the index that is dragged;
    private int highlight = -1;
    
    private static final int MaxCount = 8; // max number of items count types
    private int count = MaxCount; // 0 - MaxCount, 0 stands for 1 item

    private int position2x[] = new int[5];//map position to x
    private int position2y[] = new int[5];//map position to y
    
    //there are 5 types position in x direction or y direction
    //define positions of each type of count(0-8)
    private int positions[][][] = new int[][][] {
       { {1,1} },
       { {3,1} , {4,1} },
       { {1,3} , {3,4} , {4,4} },
       { {3,3} , {4,3} , {3,4} , {4,4} },
       { {3,0} , {4,0} , {1,1} , {3,2} , {4,2} },
       { {3,0} , {4,0} , {3,1} , {4,1} , {3,2} , {4,2} },
       { {3,0} , {4,0} , {0,1} , {1,1} , {2,1} , {3,2} , {4,2} },
       { {0,0} , {1,0} , {2,0} , {3,1} , {4,1} , {0,2} , {1,2} , {2,2} },
       { {0,0} , {1,0} , {2,0} , {0,1} , {1,1} , {2,1} , {0,2} , {1,2} , {2,2} },
    };

    private DragPanelListener mListener;

    Handler handler = new Handler(){
        public void handleMessage(Message msg) {
        	imgbtn_add.setPressed(false);
        }
    };
    /*2012-03-31, add by Chen Yidong, for SWITCHUI-419*/
    private boolean mDragHightLightFlag = false;
    /*2012-03-31, end*/

    /*2012-3-28, add by bvq783 for switchui-257*/
    private boolean hasDown = false;
    /*2012-3-28, add end*/
    private int heightPixels;
    private int widthPixels;
    //compensate Y position when drag panel show/hide.
    private int compensateY;

    /*2012-07-03, added by Bai Jian SWITCHUI-1634*/
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        widthPixels = MeasureSpec.getSize(widthMeasureSpec);
        heightPixels = MeasureSpec.getSize(heightMeasureSpec);

        //determine item_width, item_height, top, bottom according to orientation
        if ( heightPixels > widthPixels ) {
        /* 2012-04-07 Modified by e13775 for MMCPPROJECT-28*/
            /*
            item_width = context.getResources().getDimensionPixelSize(R.dimen.item_width_port);
            item_height = context.getResources().getDimensionPixelSize(R.dimen.item_height_port);

            top = context.getResources().getDimensionPixelSize(R.dimen.top_margin_port);
            bottom = context.getResources().getDimensionPixelSize(R.dimen.bottom_margin_port);
            left = context.getResources().getDimensionPixelSize(R.dimen.left_margin_port);
            right = context.getResources().getDimensionPixelSize(R.dimen.right_margin_port);
            */
            /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
            item_width = (int) (widthPixels*0.25);
            item_height = (int) (heightPixels*0.25);
            top = (int) (trash_area_height + heightPixels*0.02);
            bottom = (int) (btnbar_height + heightPixels*0.02);
            /*2012-06-06, end*/
            right = left =(int) (widthPixels*0.07);
        /* 2012-04-07 Modified by e13775 for MMCPPROJECT-28*/
        } else {

            item_width = mContext.getResources().getDimensionPixelSize(R.dimen.item_width_land);
            item_height = mContext.getResources().getDimensionPixelSize(R.dimen.item_height_land);

            top = mContext.getResources().getDimensionPixelSize(R.dimen.top_margin_land);
            bottom = mContext.getResources().getDimensionPixelSize(R.dimen.bottom_margin_land);
            left = mContext.getResources().getDimensionPixelSize(R.dimen.left_margin_land);
            right = mContext.getResources().getDimensionPixelSize(R.dimen.right_margin_land);
        }

        //calculate panel width and height
        panel_width = (int) (widthPixels) - left -right;
        panel_height = (int) (heightPixels) - top - bottom;

        /*2012-07-19, added by Bai Jian SWITCHUI-2333*/
        if (((Launcher)getContext()).getSearchBar().getIsQSBarHide()) {
            compensateY = -4 + (int)((859-heightPixels)/3);
        } else {
            compensateY = 34;
        }
        /*2012-07-19, end*/

        //calculate
        x_padding = (panel_width - item_width*3)/2;
        y_padding = (panel_height - item_height*3)/2;

        // calculate the coordinate x y of each position
        position2x[1] = left + panel_width/2 - item_width/2;

        position2x[0] = position2x[1] - x_padding - item_width;
        position2x[2] = position2x[1] + x_padding + item_width;

        position2x[3] = left + panel_width/2 - x_padding/2 - item_width ;
        position2x[4] = left + panel_width/2 + x_padding/2;


        position2y[1] = top + panel_height/2 - item_height/2;

        position2y[0] = position2y[1] - y_padding - item_height;
        position2y[2] = position2y[1] + y_padding + item_height;

        position2y[3] = top + panel_height/2 - y_padding/2 - item_height ;
        position2y[4] = top + panel_height/2 + y_padding/2;

    }
    /*2012-07-03, end*/

	public DragPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
        LayoutInflater.from(context).inflate(R.layout.drag_panel, this);

        //get the layout contains add button and layout contains remove button
        layout_add = findViewById(R.id.layout_add);
        layout_remove = findViewById(R.id.layout_remove);
        
        //get the title TextView
        //tv_title = (TextView) findViewById(R.id.tv_title);

        /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
        tv_add = (TextView) findViewById(R.id.tv_add);
        tv_remove = (TextView) findViewById(R.id.tv_remove);
        tv_remove.setTextColor(Color.WHITE);
        /* 2012-04-07 Added end by e13775 for MMCPPROJECT-28*/
        imgbtn_remove = (ImageView) findViewById(R.id.imgbtn_remove);

        trash_area_height = context.getResources().getDimensionPixelSize(R.dimen.trash_area);
        trash_hit_area_height = trash_area_height+10;
        btnbar_height = context.getResources().getDimensionPixelSize(R.dimen.btnbar);
        taskbar_height = context.getResources().getDimensionPixelSize(R.dimen.taskbar);

        //get add button and set its on click listener
        imgbtn_add = (ImageButton) findViewById(R.id.imgbtn_add);

        /* 2012-04-07 Modified by e13775 for MMCPPROJECT-28*/
        OnClickListener listener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

            	handler.sendEmptyMessageDelayed(0, 500);
            	
            	// if count is not reach the max

            	if ( count != MaxCount) {
            		
                	//change count type
                    count++;

                    if (count == MaxCount) {
                        imgbtn_add.setEnabled(false);
                        tv_add.setTextColor(Color.GRAY);
                    }
                    
                    setHighlight(count/2);
                    
                    //show the added item
                    ImageView img = items[count];
                    img.setBackgroundResource(R.drawable.panel_box);
                    img.setImageBitmap(null);
                    LayoutParams lp = (LayoutParams) img.getLayoutParams();
                    lp.width = item_width;
                    lp.height = item_height;
                    lp.leftMargin = position2x[positions[count][count][0]];
                    lp.topMargin = position2y[positions[count][count][1]];
                    lp.rightMargin = panel_width - lp.leftMargin - item_width;
                    lp.bottomMargin = panel_height - lp.topMargin - item_height;
                    img.setLayoutParams(lp);
                    img.setVisibility(View.VISIBLE);
                    
                    //play animation
                    for (int i = 0; i < count; i++){
                        move(items[i], count, i);
                    }
                    
                    //callback
                    mListener.onAddItem();
            	}

            }
        };
        
        imgbtn_add.setOnClickListener(listener);
        tv_add.setOnClickListener(listener);

        /*2012-6-25, add by bvq783 for switchui-1581*/
        setTran = (Button)findViewById(R.id.transition_icon);
        OnClickListener btnListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //callback
                mListener.onLaunchTransition();
            }
        };
        setTran.setOnClickListener(btnListener);       
        /*2012-6-25, add end*/

        //initialize all image views
        for (int i = 0; i <= MaxCount ; i++){

        	//get a ImageView and store it in Global variable items
            ImageView img = new ImageView(context);
            img.setBackgroundResource(R.drawable.panel_box);
            LayoutParams lp = new LayoutParams(item_width, item_height);
            this.addView(img, lp);
            items[i] = img;
            
            img.setOnLongClickListener(new View.OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					// TODO Auto-generated method stub
                    /*2012-3-28, add by bvq783 for switchui-257*/
                    if(dragging) {
                        return false;
                    }
                    /*2012-3-28, add end*/

                	// no drag when there is only 1 item
                    if ( count == 0) {
                    	return false;
                    } else {
                        dragging = true;
                        //added by amt_wangpeipei 2012/06/29 for SWITCHUI-1718 begin
                        setTran.setEnabled(false);
                        //added by amt_wangpeipei 2012/06/29 for SWITCHUI-1718 end
                        layout_add.setVisibility(View.INVISIBLE);
                        layout_remove.setVisibility(View.VISIBLE);
                        v.getParent().bringChildToFront(v);
                        v.setBackgroundResource(R.drawable.panel_box_drag);
                        
                        // determine which item is touched
                        for ( int i = 0; i <= count ; i++) {
                        	if ( v == items[i] ) {
                        		drag = vacant = i;
                        	}
                        }
                        /*2012-03-20, add by Chen Yidong, for SWITCHUI-419.*/
                        mDragHightLightFlag = drag == highlight;
                        /*2012-03-20, end*/ 

                        /*2012-3-28, add by bvq783 for switchui-257*/
                        v.setTag(v.getId(), "true");
                        /*2012-3-28, add end*/

    					return true;
                    }
				}
			});
            
            //set item's on click listener
            img.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    /*2012-3-28, add by bvq783 for switchui-257*/
                    if (dragging) {
                        return;
                    }
                    /*2012-3-28, add end*/

                    // determine which item is touched
                    for ( int i = 0; i <= count ; i++) {
                    	if ( v == items[i] ) {
                            //callback
                            /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
                            setHighlight(i);
                            /*2012-06-06, end*/
                        	mListener.onClickItem(i);
                        	return;
                    	}
                    }

                }
            });
            
            //set item's on touch listener
            img.setOnTouchListener(new View.OnTouchListener() {
                
                public boolean onTouch(View v, MotionEvent event) {
                    /*2012-3-28, add by bvq783 for switchui-257*/
                    if (hasDown && !(v.getTag(v.getId())!=null && v.getTag(v.getId()).equals("true"))) {
                        /*2012-07-03, added by Bai Jian SWITCHUI-1634*/
                        if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            down_rel_x = (int) event.getX();
                            down_rel_y = (int) event.getY();
                        }
                        /*2012-07-03, end*/
                        return false;
                    }
                    /*2012-3-28, add end*/
                    
                    switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        
                    	//down_rel_x, down_rel_y
                        down_rel_x = (int) event.getX();
                        down_rel_y = (int) event.getY();
                        /*2012-3-28, add by bvq783 for switchui-257*/
                        hasDown = true;
                        /*2012-3-28, add end*/

                        return false;
                    case MotionEvent.ACTION_MOVE:
                    	
                    	// determine whether to drag mode
                        if (!dragging) {
                        	return false;
                        }
                        
                        // now in drag mode
                        
                        int raw_x = (int) (event.getRawX());
                        /*2012-07-03, added by Bai Jian SWITCHUI-1634*/
                        int raw_y = (int) (event.getRawY() - taskbar_height);
                        /*2012-07-03, end*/

                        // make the item follow the touch point
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        lp.leftMargin = raw_x - down_rel_x;
                        lp.topMargin = raw_y - down_rel_y;
                        lp.rightMargin = panel_width - lp.leftMargin - item_width;
                        lp.bottomMargin = panel_height - lp.topMargin - item_height;
                        v.setLayoutParams(lp);

                        if ( raw_y <= trash_hit_area_height ) {
                        	// if drag item to trash area

                            if (!inTrash) {
                            	// if not in trash mode

                            	// change to trash mode
                                inTrash = true;
                                
                                //highlight the trash area
                                imgbtn_remove.setImageResource(R.drawable.panel_trash_highlight);
                                /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
                                tv_remove.setTextColor(Color.RED);
                                /* 2012-04-07 Added end by e13775 for MMCPPROJECT-28*/

                               
                            } else {
                            	// if in trash mode
                            }
                        } else {
                        	// if drag item out of trash area

                            if (inTrash) {
                            	// if in trash mode
                            	
                            	// change to non-trash mode
                                inTrash = false;
                                
                                // not highlight the trash area
                                imgbtn_remove.setImageResource(R.drawable.panel_trash);
                                /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
                                tv_remove.setTextColor(Color.WHITE);
                                /* 2012-04-07 Added end  by e13775 for MMCPPROJECT-28*/
                            }
                        }
                        
                        // get item index that is bumped
                        int bump = getHitIndex(raw_x,raw_y);

                        if ( bump != -1 && bump != vacant ){
                        	// if bumped one
                        	
                        	// move the items with animation
                            if ( vacant > bump){
                                for ( int i = vacant - 1 ; i >= bump; i-- ){
                                    move(items[i], count, i+1);
                                    items[i+1] = items[i];
                                    /*2012-03-31, add by Chen Yidong, for SWITCHUI-419..*/
                                    if(!mDragHightLightFlag){
                                        if (highlight == i) highlight = i+1;
                                    }
                                    /*2012-03-20, end*/
                                }
                            } else {
                                for ( int i = vacant + 1 ; i <= bump; i++ ){
                                    move(items[i], count, i-1);
                                    items[i-1] = items[i];
                                    /*2012-03-31, add by Chen Yidong, for SWITCHUI-419*/
                                    if(!mDragHightLightFlag){
                                    	if (highlight == i) highlight = i-1;
                                    }
                                    /*2012-03-20, end*/
                                }
                            }

                            vacant = bump;
                        }

                        break;
                    /*2012-3-28, add by bvq783 for switchui-257*/
                    case MotionEvent.ACTION_CANCEL:
                    /*2012-3-28, add end*/
                    case MotionEvent.ACTION_UP:
                        /*2012-3-28, add by bvq783 for switchui-257*/
                        hasDown = false;
                        v.setTag(v.getId(), "false");
                        /*2012-3-28, add end*/
                    	
                        if (!dragging) {
                        	// if not in dragging mode
                            return false;
                        }

                        // now it is a touch event up in dragging mode
                        
                        // chanege to non-dragging mode
                        dragging = false;
                        //added by amt_wangpeipei 2012/06/29 for SWITCHUI-1718 begin
                        setTran.setEnabled(true);
                        //added by amt_wangpeipei 2012/06/29 for SWITCHUI-1718 end
                        // show the layout contains add button
                        layout_add.setVisibility(View.VISIBLE);
                        // hide the layout contains remove button
                        layout_remove.setVisibility(View.INVISIBLE);
                        
                        if (inTrash) {
                        	// if in trash mode
                        	
                        	// change to non-trash mode
                            inTrash = false;
                            
                        	items[highlight].setBackgroundResource(R.drawable.panel_box);
                        	
                        	// reorder items
                            for (int i = vacant; i < count; i++){
                                items[i] = items[i+1];
                            }
                            items[count] = (ImageView) v;
                            
                            //hide the delete one
                            v.setVisibility(View.INVISIBLE);
                            
                            //change count type
                            count--;
                            
                            //move items with animation
                            for (int i = 0; i <= count; i++){
                                move(items[i], count, i);
                            }
                            
                            setHighlight(count/2);
                            
                            //callback
                            mListener.onRemoveItem(drag);
                            
                            // not highlight the trash area
                            imgbtn_remove.setImageResource(R.drawable.panel_trash);
                            /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
                            tv_remove.setTextColor(Color.WHITE);
                            /* 2012-04-07 Added end by e13775 for MMCPPROJECT-28*/
                            
                            if (count == MaxCount - 1) {
                                imgbtn_add.setEnabled(true);
                               /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
                                tv_add.setTextColor(Color.WHITE);
                               /* 2012-04-07 Added end by e13775 for MMCPPROJECT-28*/
                            }
                            
                        } else {
                        	// if not in trash mode
                        	/*2012-03-31, add by Chen Yidong, for SWITCHUI-419*/
                        	if(mDragHightLightFlag){
                        		highlight = vacant;
                        	}
                        	/*2012-03-31, end*/
                        	// move the drag ImageView to vacant position with animation
                            move((ImageView) v, count, vacant);
                            items[vacant] = (ImageView) v;

                        	if ( vacant == highlight) {
                        		v.setBackgroundResource(R.drawable.panel_box_highlight);
                        	} else {
                        		v.setBackgroundResource(R.drawable.panel_box);
                        	}
                            
                            // it the order changed, do the callback
                            if ( drag != vacant) {
                            	setHighlight(count/2);
                            	mListener.onReorderItems(drag, vacant);
                            }
                        }
                        
                        break;
                    }
                    return true;
                }
            });
        }
	}

    /**
     * Move the ImageView to a specific position
     *
     * @param img The ImageView to move
     * @param to_count Move to which count type
     * @param to_position Move to which position of the to_count type
     */
    private void move(ImageView img, int to_count, int to_position){

    	img.clearAnimation();
        
        int from_x = img.getLeft();
        int from_y = img.getTop();
        
        int to_x = position2x[positions[to_count][to_position][0]];
        int to_y = position2y[positions[to_count][to_position][1]];
        
        if ( ( Math.abs(from_x - to_x) < 10) && 
        		( Math.abs(from_y - to_y) < 10) ) {
            LayoutParams lp = (LayoutParams) img.getLayoutParams();
            lp.leftMargin = to_x;
            lp.topMargin = to_y;
            lp.rightMargin = panel_width - lp.leftMargin - item_width;
            lp.bottomMargin = panel_height - lp.topMargin - item_height;
            img.setLayoutParams(lp);
            
        } else {
            LayoutParams lp = (LayoutParams) img.getLayoutParams();
            lp.leftMargin = to_x;
            lp.topMargin = to_y;
            lp.rightMargin = panel_width - lp.leftMargin - item_width;
            lp.bottomMargin = panel_height - lp.topMargin - item_height;
            img.setLayoutParams(lp);
			
            Animation am = new TranslateAnimation(from_x - to_x, 0,from_y - to_y, 0);
            am.setDuration(150);
            img.startAnimation(am);
        }

    }
    
    //get which item contains the x y 
    private int getHitIndex(int x, int y){
        
        for (int i = 0; i <= count; i++) {
            int xx = x - position2x[positions[count][i][0]];
            int yy = y - position2y[positions[count][i][1]];
            if ( (xx > 0) && (xx < item_width ) && (yy > 0) && (yy < item_height) ) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Update the bitmaps to items
     *
     * @param bmps ArrayList contains the bitmaps in order 
     */
    public void updateAll(ArrayList<Bitmap> bmps){
    	count = bmps.size()-1;
        if (count == MaxCount) {
            imgbtn_add.setEnabled(false);
            /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
            tv_add.setTextColor(Color.GRAY);
            /* 2012-04-07 Added end by e13775 for MMCPPROJECT-28*/
        } else {
            imgbtn_add.setEnabled(true);
            /* 2012-04-07 Added by e13775 for MMCPPROJECT-28*/
            tv_add.setTextColor(Color.WHITE);
            /* 2012-04-07 Added end by e13775 for MMCPPROJECT-28*/
        }

    	for (int i = 0 ; i <= MaxCount; i++ ) {
            ImageView img = items[i];
            if ( i <= count) {
                img.setImageBitmap(bmps.get(i));
                /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
                /*
                //added by RJG678 for IKSWITCHUI-2
                //2012-03-06 added by RJG678 for IKSWITCHUI-95
                if(LauncherApplication.isScreenLarge()){
                    img.setPadding(28, 15, 18, 18);
                }else{
                    img.setPadding(8, 5, 8, 8);
                }
                */
                /*2012-06-06, end*/
                //added by RJG678 end
                LayoutParams lp = (LayoutParams) img.getLayoutParams();
                lp.width = item_width;
                lp.height = item_height;
                lp.leftMargin = position2x[positions[count][i][0]];
                lp.topMargin = position2y[positions[count][i][1]];
                lp.rightMargin = panel_width - lp.leftMargin - item_width;
                lp.bottomMargin = panel_height - lp.topMargin - item_height;
                img.setLayoutParams(lp);
                img.setVisibility(View.VISIBLE);
            } else {
                img.setVisibility(View.INVISIBLE);
            }
            /*2012-3-28, add by bvq783 for switchui-257*/
            img.setTag(img.getId(), "false");
            /*2012-3-28, add end*/
    	}
        /*2012-3-28, add by bvq783 for switchui-257*/
        hasDown = false;;
        /*2012-3-28, add end*/
    }
    
    /**
     * Update one the item's bitmap
     *
     * @param index The item index to be update
     * @param bmp The bitmap set to item
     */
    public void updateOne(int index, Bitmap bmp){
    	items[index].setImageBitmap(bmp);
    }
    
/*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
    public void show(){
        setVisibility(View.VISIBLE);
        final ValueAnimator ani = ValueAnimator.ofFloat(0f, 1f)
                .setDuration(300);
        ani.addUpdateListener(new LauncherAnimatorUpdateListener() {
            int x = positions[count][highlight][0];
            int tranX = position2x[1]-position2x[x];
            int y = positions[count][highlight][1];
            int tranY = position2y[1]-position2y[y];
            float scale = widthPixels/item_width*1.1f;

            @Override
            public void onAnimationUpdate(float a, float b) {
                items[highlight].getBackground().setAlpha((int)(255 * b));
                setScaleX((scale-0.98f)*a+0.98f);
                setScaleY((scale-0.98f)*a+0.98f);
                setTranslationY((compensateY+tranY*scale)*a);
                setTranslationX((-2+tranX*scale)*a);
            }
        });
        ani.addListener(new AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationStart(Animator animation) {
                /*2012-08-09, added by Bai Jian SWITCHUI-2528*/
                Drawable d;
                for ( int i = 0; i <= count; i++) {
                    d = items[i].getBackground();
                    if ( i!=highlight && d!=null ) d.setAlpha(255);
                }
                /*2012-08-09, add end*/
            }
            @Override
            public void onAnimationRepeat(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                // TODO Auto-generated method stub

                final ValueAnimator ani = ValueAnimator.ofFloat(0f, 1f)
                        .setDuration(200);
                ani.addUpdateListener(new LauncherAnimatorUpdateListener() {

                    @Override
                    void onAnimationUpdate(float a, float b) {
                        // TODO Auto-generated method stub
                        setScaleX(0.02f*b+0.98f);
                        setScaleY(0.02f*b+0.98f);
                    }
                });
                ani.start();
            }
        });
        ani.start();
    }

    public void hide(boolean animated, final int index){

    	setVisibility(View.INVISIBLE);
        if (!animated) {
            setVisibility(View.INVISIBLE);
            mListener.onHideEnd(false, index);
            return;
        }

        final ValueAnimator ani = ValueAnimator.ofFloat(0f, 1f)
                .setDuration(300);
        ani.addUpdateListener(new LauncherAnimatorUpdateListener() {
            int x = positions[count][index][0];
            int tranX = position2x[1]-position2x[x];
            int y = positions[count][index][1];
            int tranY = position2y[1]-position2y[y];
            float scale = widthPixels/item_width*1.1f;

            public void onAnimationUpdate(float a, float b) {
                setScaleX((scale-1)*b+1f);
                setScaleY((scale-1)*b+1f);
                setTranslationY((compensateY+tranY*scale)*b);
                setTranslationX((-2+tranX*scale)*b);
            }
        });
        ani.addListener(new AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationStart(Animator animation) {
                /*2012-08-09, added by Bai Jian SWITCHUI-2528*/
                Drawable d;
                for ( int i = 0; i <= count; i++) {
                    d = items[i].getBackground();
                    if ( d!=null ) d.setAlpha(0);
                }
                /*2012-08-09, add end*/
            }
            @Override
            public void onAnimationRepeat(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                // TODO Auto-generated method stub
                setVisibility(View.INVISIBLE);
                mListener.onHideEnd(true, index);
            }
        });
        ani.start();
        mListener.onHideAnimationStart(index);
    }
/*2012-06-06, end*/

    /**
     * Set the title TextView
     *
     * @param title The title string
     */
    /*public void setTitle(String title){
    	tv_title.setText(title);
    }*/

    public void setDragPanelListener (DragPanelListener listener) {
    	mListener = listener;
    }
    
    public void setHighlight(int index){
    	if (highlight != -1) items[highlight].setBackgroundResource(R.drawable.panel_box);
    	highlight = index;
    	items[highlight].setBackgroundResource(R.drawable.panel_box_highlight);
    }
    
    public int getHighlight(){
    	return highlight;
    }

    public int getItem_width() {
		return item_width;
    }

    public int getItem_height() {
		return item_height;
    }

    /*2012-11-26, add by 003033 for switchuitwo-121*/
    public boolean isDragging() {
        return dragging;
    }
    /*2012-11-26, add end*/

    public interface DragPanelListener {
/*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
        /**
         * Called when play hide animation
         * @param index The item index that is clicked
         */
        void onHideAnimationStart(int index);
        /**
         * Called when hide end
         * @param index The item index that is clicked
         */
        void onHideEnd(boolean animated, int index);
/*2012-06-06, end*/
        /**
         * Called when click a item
         * @param index The item index that is clicked
         */
        void onClickItem(int index);
        /**
         * Called when add a item
         */
    	void onAddItem();
        /**
         * Called when remove a item
         * @param index The item index that is removed
         */
    	void onRemoveItem(int index);
        /**
         * Called when move a item to a new position
         * @param from_index The item index that is dragged
         * @param to_index The destination index moved to
         */
    	void onReorderItems(int from_index, int to_index);
        /*2012-6-25, add by bvq783 for switchui-1581*/
        /**
         * Called when click transition button
         */
    	void onLaunchTransition();
        /*2012-6-25, add end*/
    }
}
