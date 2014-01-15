package com.motorola.contextual.smartrules.widget;

import java.util.Date;
import java.util.Vector;

import com.motorola.contextual.smartrules.Constants;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


public class SchematicView extends View implements Constants {

    public static final String TAG = SchematicView.class.getSimpleName();

    private static Palette palette;

    private WorkingCanvas debugCanvas = null;
    private Vector<DrawableContainer> components = new Vector<DrawableContainer>(3,3);


    private Paint 	mFramePaint;
    private Paint 	mFillPaint;

    public static final Rect CIRCLE_SIZE 				= new Rect(0,0,95,70);
    public static final Rect DEFAULT_RECTANGLE_SIZE 	= new Rect(0,0,100,100);
    public static final Rect DEFAULT_HEADER_RECT_SIZE 	= new Rect(0,0,200,50);
    public static final int	 PORT_VERT_GAP				= 10;
    public static final int	 OVERLAP_PIXELS				= 5;
    public static final int	 GAP_PIXELS 				= 25;
    public static final int	 FRAME_WIDTH				= 2;


    public SchematicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SchematicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SchematicView(Context context) {
        super(context);
        init();
    }


    private void init() {

        debugCanvas = new WorkingCanvas(components);

        mFramePaint = new Paint();
        mFramePaint.setAntiAlias(true);
        mFramePaint.setColor(0xFFFFFFFF); // white
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setStrokeWidth(4);

        mFillPaint = new Paint();
        mFillPaint.setAntiAlias(true);
        mFillPaint.setColor(0xFF88FFFF);  // greenish-blue
        mFillPaint.setStyle(Paint.Style.FILL);

        palette = new Palette(mFramePaint, mFillPaint);

        this.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent e) {

                if(LOG_DEBUG) Log.d(TAG, "ZZZ onTouch e-"+e.toString());
                boolean result = debugCanvas.onTouch(e);
                if (result) {
                    view.invalidate();
                    view.postInvalidate();
                    if(LOG_DEBUG) Log.d(TAG, "ZZZ onTouch invalidate()");
                }
                return result;
            }

        });

    }


    /**
     * @return the components
     */
    public Vector<DrawableContainer> getComponents() {
        return components;
    }

    /**
     * @param components the components to set
     */
    public void setComponents(Vector<DrawableContainer> components) {
        this.components = components;
    }

    /**
     * @return the mFramePaint
     */
    public Paint getFramePaint() {
        return mFramePaint;
    }

    /**
     * @param framePaint the mFramePaint to set
     */
    public void setFramePaint(Paint framePaint) {
        this.mFramePaint = framePaint;
    }

    /**
     * @return the mFillPaint
     */
    public Paint getFillPaint() {
        return mFillPaint;
    }

    /**
     * @param fillPaint the mFillPaint to set
     */
    public void setFillPaint(Paint fillPaint) {
        this.mFillPaint = fillPaint;
    }



    @Override
    protected void onDraw(Canvas canvas) {

        paint(canvas);
        invalidate();

    }


    public void paint(Canvas canvas) {

        for (int i=0; i<components.size(); i++) {
            components.get(i).draw(canvas);
        }
    }


    /** this class abstracts a single typeless block - which appears on
     * screen as a circle or rectangle
     */
    public static class IOBlock {

        private String 				name;
        private String				currentState;
        private String				waitingForState;
        private String  			key;
        private String 				onClickWhereClause;
        private OnClickDrawableListener onClickListener;
        private BlockColor			fillColor;
        private BlockColor			frameColor;


        /** basic constructor */
        public IOBlock() {
            super();
            fillColor = BlockColor.getDefaultFillColor();
            frameColor = BlockColor.getDefaultFrameColor();
        }


        /** constructor with name */
        public IOBlock(String name) {
            super();
            this.name = name;
        }


        /**
         * @return the name
         */
        public String getName() {
            return name;
        }


        /**
         * @param name the name to set
         */
        public IOBlock setName(String name) {
            this.name = name;
            return this;
        }


        /**
         * @return the name
         */
        public String getKey() {
            return key;
        }


        /**
         * @param name the name to set
         */
        public IOBlock setKey(String key) {
            this.key = key;
            return this;
        }


        /**
         * @return the currentState
         */
        public String getCurrentState() {
            return currentState;
        }


        /**
         * @param currentState the currentState to set
         */
        public IOBlock setCurrentState(String currentState) {
            this.currentState = currentState;
            return this;
        }


        /**
         * @return the waitingForState
         */
        public String getWaitingForState() {
            return waitingForState;
        }


        /**
         * @param waitingForState the waitingForState to set
         */
        public IOBlock setWaitingForState(String waitingForState) {
            this.waitingForState = waitingForState;
            return this;
        }


        /**
         * @return the onClickWhereClause
         */
        public String getOnClickWhereClause() {
            return onClickWhereClause;
        }


        /**
         * @param onClickWhereClause the onClickWhereClause to set
         */
        public IOBlock setOnClickWhereClause(String onClickWhereClause) {
            this.onClickWhereClause = onClickWhereClause;
            return this;
        }


        /**
         * @return the onClickListener
         */
        public OnClickDrawableListener getOnClickListener() {
            return onClickListener;
        }


        /**
         * @param onClickListener the onClickListener to set
         */
        public IOBlock setOnClickListener(OnClickDrawableListener onClickListener) {
            this.onClickListener = onClickListener;
            return this;
        }


        /**
         * @return the blockColor
         */
        public BlockColor getFillColor() {
            return fillColor;
        }


        /**
         * @param blockColor the blockColor to set
         */
        public IOBlock setFillColor(BlockColor blockColor) {
            this.fillColor = blockColor;
            return this;
        }


        /**
         * @return the frameColor
         */
        public BlockColor getFrameColor() {
            return frameColor;
        }


        /**
         * @param frameColor the frameColor to set
         */
        public IOBlock setFrameColor(BlockColor frameColor) {
            this.frameColor = frameColor;
            return this;
        }


        public Paint getFillPaint() {
            return palette.getPaintFillColor(fillColor);
        }


        public Paint getFramePaint() {
            return palette.getPaintFrameColor(frameColor);
        }



    }


    /** IOComponent Set
     *
     * <pre><code>
     * example:
     * Rect r = new Rect(left, top, right, bottom);
     */
    public static class IOComponentSet extends DrawableContainerGroup {

        //TODO: class cannot be made static unless we can get static paints or write an interface to get the paint
        private DrawableContainerGroup 	inputs = null;
        private IOComponent				component = null;
        private DrawableContainerGroup 	outputs = null;

        public IOComponentSet(Rect bounds, String text) {

            super(bounds, text);
            inputs = new DrawableContainerGroup(CIRCLE_SIZE, null);
            outputs = new DrawableContainerGroup(CIRCLE_SIZE, null);
        }


        /** adds an input Port to the diagram
         *
         * @param tag
         * @return - unique key for later retrieval
         */
        public long addInput(IOBlock block) {

            int left 	= this.getBounds().left;
            int top 	= this.getBounds().top+inputs.group.size()*(CIRCLE_SIZE.bottom+PORT_VERT_GAP);
            int right 	= left+CIRCLE_SIZE.width();
            int bottom 	= top+CIRCLE_SIZE.bottom;
            Rect rect = new Rect(left, top, right, bottom);

            Port port = null;
            inputs.group.add(port = new Port(rect, FRAME_WIDTH, block.getName(), true));
            port.setIoDebugBlock(block);

            inputs.setBounds();
            this.setBounds();
            if(LOG_DEBUG) Log.d(TAG, "IOC inputs bounds="+inputs.getBounds()+ " Rect="+rect+ "  group="+this.getBounds());
            return (key = 100*(new Date().getTime())+10+inputs.group.size());
        }


        /** adds a component container to the diagram
         *
         * @param tag
         * @return - unique key for later retrieval
         */
        public long setComponent(final IOBlock block) {

            int left 	= inputs.getBounds().right-OVERLAP_PIXELS;
            int top 	= inputs.getBounds().centerY()-DEFAULT_RECTANGLE_SIZE.height();
            top			= Math.max(top, DEFAULT_RECTANGLE_SIZE.centerY());
            int right	= left	+DEFAULT_RECTANGLE_SIZE.width();
            int bottom 	= top	+DEFAULT_RECTANGLE_SIZE.bottom;
            Rect rect 	= new Rect(left, top, right, bottom);

            component = new IOComponent(rect, FRAME_WIDTH, block.getName());
            component.setIoDebugBlock(block);
            this.setBounds();
            if(LOG_DEBUG) Log.d(TAG, "IOC component bounds="+component.getBounds()+ "  Rect="+rect+"  REC_SIZE="+DEFAULT_RECTANGLE_SIZE+ " group="+this.getBounds());
            return (key = 100*(new Date().getTime())+20+group.size());
        }

        /** adds a component container to the diagram
         *
         * @param tag
         * @return - unique key for later retrieval
         */
        public long setHeaderComponent(final IOBlock block) {

            int left 	= 0;
            int top 	= 0;
            if(inputs.group.size() < 1) {
                //no input components added lets use the default bounds for calculation
                left =  this.getBounds().left;
                top  =  this.getBounds().centerY()-DEFAULT_RECTANGLE_SIZE.height();
            } else {
                left 	= inputs.getBounds().right-OVERLAP_PIXELS;
                top 	= inputs.getBounds().centerY()-DEFAULT_RECTANGLE_SIZE.height();
            }
            top			= Math.max(top, DEFAULT_HEADER_RECT_SIZE.centerY());
            int right	= left	+DEFAULT_HEADER_RECT_SIZE.width();
            int bottom 	= top	+DEFAULT_HEADER_RECT_SIZE.bottom;
            Rect rect 	= new Rect(left, top, right, bottom);

            component = new IOComponent(rect, FRAME_WIDTH, block.getName());
            component.setIoDebugBlock(block);
            this.setBounds();
            if(LOG_DEBUG) Log.d(TAG, "IOC component bounds="+component.getBounds()+ "  Rect="+rect+"  REC_SIZE="+DEFAULT_HEADER_RECT_SIZE+ " group="+this.getBounds());
            return (key = 100*(new Date().getTime())+20+group.size());
        }


        /** adds an output port to the diagram
         *
         * @param tag
         * @return - unique key for later retrieval
         */
        public long addOutput(IOBlock block) {

            int left 	= component.getBounds().right-OVERLAP_PIXELS;
            int top 	= inputs.getBounds().top+(outputs.group.size()*(CIRCLE_SIZE.height()+PORT_VERT_GAP));
            int right 	= left+CIRCLE_SIZE.width();
            int bottom 	= top+CIRCLE_SIZE.height();
            Rect rect 	= new Rect(left, top, right, bottom);

            Port port = null;
            outputs.group.add(port = new Port(rect, FRAME_WIDTH, block.getName(), false));
            port.setIoDebugBlock(block);
            outputs.setBounds();
            this.setBounds();
            if(LOG_DEBUG) Log.d(TAG, "IOC outputs bounds="+outputs.getBounds()+ " Rect="+rect+ " group="+this.getBounds());
            return (key = 100*(new Date().getTime())+30+group.size());
        }

        /** finds a container by key */
        public DrawableContainer findByKey(long key) {

            DrawableContainer result = inputs.findByKey(key);
            if (result == null && component.getKey() == key)
                result = component;
            if (result == null)
                result = outputs.findByKey(key);
            return result;
        }

        /** sets the bounds of the union of the input, component, output
         */
        @Override
        public void setBounds() {
            Rect result;

            if(inputs!=null && inputs.group.size() >0) {
                result= new Rect(inputs.getBounds());
            } else {
                result = new Rect(this.getBounds());
            }
            if (component != null)
                result.union(component.getBounds());
            if (outputs != null && outputs.group.size() > 0)
                result.union(outputs.getBounds());
            setBounds(result);
        }


        /** draws the input, output and component containers.
         */
        @Override
        public void draw(Canvas canvas) {

            if (component != null)	component.draw(canvas);
            if (inputs != null)		inputs.draw(canvas);
            if (outputs != null)	outputs.draw(canvas);
        }


        /**
         * @return the inputs
         */
        public DrawableContainerGroup getInputs() {
            return inputs;
        }


        /**
         * @param inputs the inputs to set
         */
        public void setInputs(DrawableContainerGroup inputs) {
            this.inputs = inputs;
        }


        /**
         * @return the component
         */
        public IOComponent getComponent() {
            return component;
        }


        /**
         * @param component the component to set
         */
        public void setComponent(IOComponent component) {
            this.component = component;
        }


        /**
         * @return the outputs
         */
        public DrawableContainerGroup getOutputs() {
            return outputs;
        }


        /**
         * @param outputs the outputs to set
         */
        public void setOutputs(DrawableContainerGroup outputs) {
            this.outputs = outputs;
        }


        /* (non-Javadoc)
         * @see com.motorola.contextual.smartrules.widget.SchematicView.DrawableContainerGroup#onTouch(android.view.MotionEvent)
         */

        @Override
        public boolean onTouch(MotionEvent e) {

            boolean result = inputs.onTouch(e);
            if (!result)
                result = outputs.onTouch(e);
            if (!result)
                result = component.onTouch(e);
            return result;
        }
    }


    /** Class that contains multiple containers
     *
     *
     */
    public static class DrawableContainerGroup extends DrawableContainer {

        protected Vector<DrawableContainer> group = new Vector<DrawableContainer>(3,3);

        public DrawableContainerGroup(Rect bounds, String text) {
            super(bounds, text);
            //this.text = "Group";
        }


        @Override
        public void draw(Canvas canvas) {

            //TODO: need to implement that the origin of the group is the origin for drawing of the children
            for (int i=0; i<group.size(); i++) {
                group.get(i).draw(canvas);
            }
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }

        @Override
        public void setAlpha(int alpha) {
            // TODO Auto-generated method stub
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            // TODO Auto-generated method stub
        }


        /** handles the onTouch event of only the DrawableContainer group */
        /*
        public boolean onTouch(MotionEvent e) {

        	for (int i=0; i<group.size(); i++) {
        		group.get(i).onTouch(e);
        	}
        	if (LOG_DEBUG) Log.i(TAG, "GroupTouched -"+this.getClass().getSimpleName()+" "+this.getBounds().toString()+" "+e.toString());
        	return false;
        }
        */

        /** sets bounds to the union of all the boundaries of the items stored in group */
        public void setBounds() {
            this.setBounds(getBoundsOutline());
        }

        /** gets the union of all the boundaries of the items stored in group */
        public Rect getBoundsOutline() {

            Rect result = new Rect();
            if (group.size() > 0) {
                result = new Rect (group.get(0).getBounds());
                for (int i = 0; i< group.size(); i++) {
                    result.union(group.get(i).getBounds());
                }
            }
            return result;
        }

        /** Finds a specific container by a key.
         *
         * @param key
         * @return
         */
        public DrawableContainer findByKey(long key) {

            DrawableContainer result = null;
            for (int i=0; i<group.size(); i++) {
                if (group.get(i).getKey() == key)
                    result = group.get(i);
            }
            return result;
        }


        public boolean isChildClicked(Rect touchRect) {

            boolean result = false;

            DrawableContainer dc = null;
            OnClickDrawableListener onClickListener = null;
            IOBlock iodb 						= null;

            if (group.size() > 0) {
                int i = 0;
                while (!result && i<group.size()) {
                    dc = group.get(i);
                    Rect r = new Rect(dc.getBounds());
                    if (r.intersect(touchRect) &&
                            ((iodb = dc.getIoDebugBlock()) != null) &&
                            (onClickListener = iodb.getOnClickListener()) != null)
                        result = onClickListener.onClick(dc);
                    i++;
                }
            }
            return result;
        }

    }


    /** Port abstracts an input or output for the debug canvas.
     *  drawn component on screen which is circular in shape */
    public static class Port extends Circle {

        protected boolean input;

        public Port(Rect boundary, int frameWidth, String text, boolean input) {
            super(boundary, frameWidth, text);
            this.input = input;
        }

        /**
         * @return the input
         */
        public boolean isInput() {
            return input;
        }

        /**
         * @param input the input to set
         */
        public void setInput(boolean input) {
            this.input = input;
        }

    }


    /** circle abstracts a drawn component on screen which is circular in shape */
    public static class Circle extends DrawableContainer {


        public Circle(Rect boundary, int frameWidth, String text) {
            super(boundary, text);
            //this.text = "Circle";
            textPaint.setTextSize(30);
        }


        @Override
        public void draw(Canvas canvas) {

            RectF rectf = new RectF(this.getBounds());
            canvas.drawArc(rectf, 0, 360, true, ioDebugBlock.getFillPaint());
            canvas.drawArc(rectf, 0, 360, false, ioDebugBlock.getFramePaint());

            if (this.text != null)
                canvas.drawText(this.text, rectf.centerX(), (rectf.centerY()+rectf.bottom)/2, textPaint);
        }


        @Override
        public int getOpacity() {
            return 0;
        }


        @Override
        public void setAlpha(int arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void setColorFilter(ColorFilter arg0) {
            // TODO Auto-generated method stub
        }


    }



    public static class IOComponent extends Rectangle  {

        public IOComponent(Rect boundary,
                           int frameWidth, String text) {
            super(boundary, frameWidth, text);
            // TODO Auto-generated constructor stub
        }

    }



    public static class Rectangle extends DrawableContainer {


        public Rectangle(Rect boundary, int frameWidth, String text) {
            super(boundary, text);
            //this.text = "Rect";
            textPaint.setTextSize(30);
        }


        @Override
        public void draw(Canvas canvas) {

            RectF rectf = new RectF(this.getBounds());
            canvas.drawRect(rectf, ioDebugBlock.getFramePaint());
            RectF rfill = new RectF(rectf);
            rfill.left = frameWidth+rectf.left;
            rfill.right = rectf.right - frameWidth;
            rfill.top = rectf.top + frameWidth;
            rfill.bottom = rectf.bottom - frameWidth;
            canvas.drawRect(rfill, ioDebugBlock.getFillPaint());

            if (this.text != null) {
                canvas.drawText(this.text, rfill.centerX(), rectf.centerY()+textPaint.descent(), textPaint);
            }
        }

        @Override
        public int getOpacity() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            // TODO Auto-generated method stub

        }


    }


    public static abstract class DrawableContainer extends Drawable {

//    	protected Paint 	framePaint;
//    	protected Paint 	fillPaint;
        protected Paint 	textPaint;

        protected String 	text;
        protected int 		textOffsetAngle;
        protected int 		textOffsetPixels;
        protected int 		frameWidth = 2;

        protected long 		key;
        protected Object 	tag;

        protected IOBlock 	ioDebugBlock;


        public DrawableContainer(Rect bounds, String text) {
            super();
//			this.framePaint = framePaint;
//			this.fillPaint = fillPaint;
            this.text = text;
            this.setBounds(bounds);

            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            textPaint.setColor(0xFF222222);
            //textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextAlign(Paint.Align.CENTER);
            // make it bolder
            textPaint.setStrokeWidth(2);

        }


        public DrawableContainer setFontSize(int s) {
            textPaint.setTextSize(s);
            return this;
        }


        /**
         * @return the text
         */
        public String getText() {
            return text;
        }

        /**
         * @param text the text to set
         */
        public DrawableContainer setText(String text) {
            this.text = text;
            return this;
        }

        /**
         * @return the textOffsetAngle
         */
        public int getTextOffsetAngle() {
            return textOffsetAngle;
        }

        /**
         * @param textOffsetAngle the textOffsetAngle to set
         */
        public void setTextOffsetAngle(int textOffsetAngle) {
            this.textOffsetAngle = textOffsetAngle;
        }

        /**
         * @return the textOffsetPixels
         */
        public int getTextOffsetPixels() {
            return textOffsetPixels;
        }

        /**
         * @param textOffsetPixels the textOffsetPixels to set
         */
        public void setTextOffsetPixels(int textOffsetPixels) {
            this.textOffsetPixels = textOffsetPixels;
        }


        /**
         * @return the paint
         */
        public Paint getPaint() {
            return ioDebugBlock.getFramePaint();
        }


        /**
         * @return the color
         */
        public Paint getFillPaint() {
            return ioDebugBlock.getFillPaint();
        }

        /**
         * @return the tag
         */
        public Object getTag() {
            return tag;
        }


        /**
         * @param tag the tag to set
         */
        public DrawableContainer setTag(Object tag) {
            this.tag = tag;
            return this;
        }


        /**
         * @return the key
         */
        public long getKey() {
            return key;
        }


        /**
         * @param key the key to set
         */
        public void setKey(long key) {
            this.key = key;
        }

        /**
         * @return the ioDebugBlock
         */
        public IOBlock getIoDebugBlock() {
            return ioDebugBlock;
        }

        /**
         * @param ioDebugBlock the ioDebugBlock to set
         */
        public DrawableContainer setIoDebugBlock(IOBlock ioDebugBlock) {
            this.ioDebugBlock = ioDebugBlock;
            return this;
        }


        public DrawableContainer moveTo(int x, int y) {

            Rect temp = new Rect(this.getBounds());
            this.setBounds(x, y, x+temp.right-temp.left, y+temp.bottom-temp.top);
            this.invalidateSelf();
            return this;
        }


        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(text != null ? text : "null");
            builder.append(tag != null ? tag : "null");
            builder.append(" key="+key);
            builder.append(" ioDebugBlock="+(ioDebugBlock!=null ? ioDebugBlock.toString(): "null"));

            return builder.toString();
        }

        public boolean onTouch(MotionEvent e) {

            if (LOG_DEBUG) Log.d(TAG, "B Touched -"+this.getClass().getSimpleName()+" "+
                                     this.getBounds().toString()+" "+e.toString()+
                                     this.toString());
            return false;
        }


        public boolean onClick(Point point) {

            boolean result = false;
            Rect touchRect = new Rect(point.x, point.y, point.x, point.y);
            IOBlock iodb 	= null;
            IOComponent ioc 	= null;


            // we only get click events on the Group level, check inputs, outputs, then component
            if (this instanceof IOComponentSet) {
                IOComponentSet set 	= (IOComponentSet)this;

                // check inputs - has highest priority
                DrawableContainerGroup containerGroup = set.getInputs();
                if (containerGroup != null)
                    result = containerGroup.isChildClicked(touchRect);

                // check outputs (second priority)
                if (!result) {
                    containerGroup = set.getOutputs();
                    if (containerGroup != null)
                        result = containerGroup.isChildClicked(touchRect);
                }

                // check component (lowest priority)
                if (!result && (ioc = set.getComponent()) != null) {
                    iodb 	= ioc.getIoDebugBlock();
                    Rect r = new Rect(ioc.getBounds());
                    if (r.intersect(touchRect)) {
                        if (iodb != null && iodb.getOnClickListener() != null) {
                            result = iodb.onClickListener.onClick(ioc);
                        }
                    }
                }
                set.getInputs();
            }
            //if (ioDebugBlock!= null && ioDebugBlock.onClickListener != null)
            //	ioDebugBlock.onClickListener.onClick(this);

            return result;
        }


    }



    /** supports operations around the entire canvas such as touch events
     */
    private static class WorkingCanvas {

        private Vector<DrawableContainer> components = null;
        private MotionEvent motionDownEvent;

        public WorkingCanvas(Vector<DrawableContainer> components) {
            super();
            this.components = components;
        }

        /** handles the onTouch event
         * <pre><code>
         * First, need to decide if this is a scroll event, or click event.
         *
         * If the user has moved their finger more than a few pixels, it's a scroll event
         * otherwise, if it's short enough to be a click event, does it intersect a DrawableGroup,
         * if it does, then pass the click event down.
         *
         * @param e - motion event
         * @return - true if handled
         */
        public boolean onTouch(MotionEvent e) {

            if (LOG_DEBUG) Log.d(TAG, "DC.onTouch -"+e.toString());
            boolean handled = false;

            switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                motionDownEvent = e;
                break;

            case MotionEvent.ACTION_UP:
                if (motionDownEvent != null &&
                        (Math.abs(motionDownEvent.getX()-e.getX()) <3) &&
                        (Math.abs(motionDownEvent.getY()-e.getY()) <3) &&
                        (e.getDownTime() -e.getEventTime()) < 200) {

                    // detected a click event, scroll events will take care of themselves via HorizontalScrollView
                    int i = 0;
                    Rect r = new Rect((int)e.getX(), (int)e.getY(), (int)e.getX()+1, (int)e.getY()+1);
                    boolean intersect = false;
                    while (! intersect && i<components.size()) {
                        if (r.intersect(components.get(i).getBounds())) {
                            handled = components.get(i).onClick(
                                          new Point(Math.round(e.getX()),Math.round(e.getY())));
                            intersect = true;
                            if (LOG_DEBUG) Log.d(TAG, "ZZZ DC.intersected -"+e.toString()+" handled="+handled);
                        }
                        i++;
                    }
                }
                break;
            }
            return handled;
        }


    }


    /* (non-Javadoc)
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                             measureHeight(heightMeasureSpec));
        if (LOG_DEBUG) Log.d(TAG, "ZZZ onMeasure: widthms,heightms:"+widthMeasureSpec+", "+heightMeasureSpec);
    }

    private int measureHeight(int heightMeasureSpec) {
        //TODO: make this a variable
        return 800;
    }

    private int measureWidth(int widthMeasureSpec) {
        //TODO: make this a variable
        return 1250;
    }


    /* (non-Javadoc)
     * @see android.view.View#onSizeChanged(int, int, int, int)
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (LOG_DEBUG) Log.d(TAG, "ZZZ onSizeChanged: w,h:"+w+","+h+", old="+oldw+","+oldh);
    }



    public interface OnClickDrawableListener {

        /** @return true if handled */
        public boolean onClick(DrawableContainer drawable);
    }


    /** <pre><code>
     * GREY - is the state when the block is waiting for something to happen
     * GREEN - the state is active
     * YELLOW - TBD
     * RED - there was an error
     */
    public enum BlockColor {WHITE, GRAY, GREEN, YELLOW, RED, BLUE, BLACK;
    public static BlockColor convert(int value) {
        return BlockColor.class.getEnumConstants()[value];
    }
    public static String toString(int value) {
        return convert(value).toString();
    }
    public static BlockColor getDefaultFillColor() {
        return GRAY;
    }
    public static BlockColor getDefaultFrameColor() {
        return WHITE;
    }
                           };


    private static class Palette {

        static final int WHITE 	= 0xFFFFFFFF;
        static final int GRAY 	= 0xFF888888;
        static final int GREEN 	= 0xFF00FF00;
        static final int YELLOW	= 0xFFFFFF00;
        static final int RED 	= 0xFFFF0000;
        static final int BLUE	= 0xFF33FFFF;
        static final int BLACK	= 0xFF000000;

        private Paint 	mWhiteFramePaint;
        private Paint 	mGreenFramePaint;
        private Paint 	mRedFramePaint;
        private Paint 	mYellowFramePaint;
        private Paint 	mBlueFramePaint;
        private Paint 	mGrayFramePaint;
        private Paint 	mBlackFramePaint;


        private Paint 	mWhiteFillPaint;
        private Paint 	mGreenFillPaint;
        private Paint 	mRedFillPaint;
        private Paint 	mYellowFillPaint;
        private Paint 	mBlueFillPaint;
        private Paint 	mGrayFillPaint;
        private Paint 	mBlackFillPaint;

        Palette(Paint frame, Paint fill) {

            // frame
            mWhiteFramePaint = new Paint(frame);
            mGrayFramePaint = new Paint(frame);
            mGreenFramePaint = new Paint(frame);
            mYellowFramePaint = new Paint(frame);
            mRedFramePaint = new Paint(frame);
            mBlueFramePaint = new Paint(frame);
            mBlackFramePaint = new Paint(frame);

            mWhiteFramePaint.setColor(WHITE);
            mGrayFramePaint.setColor(GRAY);
            mGreenFramePaint.setColor(GREEN);
            mYellowFramePaint.setColor(YELLOW);
            mRedFramePaint.setColor(RED);
            mBlueFramePaint.setColor(BLUE);
            mBlackFramePaint.setColor(BLACK);

            // fill
            mWhiteFillPaint = new Paint(fill);
            mGrayFillPaint = new Paint(fill);
            mGreenFillPaint = new Paint(fill);
            mYellowFillPaint = new Paint(fill);
            mRedFillPaint = new Paint(fill);
            mBlueFillPaint = new Paint(fill);
            mBlackFillPaint = new Paint(fill);

            mWhiteFillPaint.setColor(WHITE);
            mGrayFillPaint.setColor(GRAY);
            mGreenFillPaint.setColor(GREEN);
            mYellowFillPaint.setColor(YELLOW);
            mRedFillPaint.setColor(RED);
            mBlueFillPaint.setColor(BLUE);
            mBlackFillPaint.setColor(BLACK);
        }

        public Paint getPaintFrameColor(final BlockColor frameColor) {

            switch (frameColor) {
            case WHITE:
                return mWhiteFramePaint;
            case GRAY:
                return mGrayFramePaint;
            case GREEN:
                return mGreenFramePaint;
            case YELLOW:
                return mYellowFramePaint;
            case RED:
                return mRedFramePaint;
            case BLUE:
                return mBlueFramePaint;
            case BLACK:
                return mBlackFramePaint;

            }
            return mWhiteFramePaint;
        }


        public Paint getPaintFillColor(final BlockColor fillColor) {

            switch (fillColor) {
            case WHITE:
                return mWhiteFillPaint;
            case GRAY:
                return mGrayFillPaint;
            case GREEN:
                return mGreenFillPaint;
            case YELLOW:
                return mYellowFillPaint;
            case RED:
                return mRedFillPaint;
            case BLUE:
                return mBlueFillPaint;
            case BLACK:
                return mBlackFillPaint;

            }
            return mGrayFillPaint;
        }
    }

    // Palette colors
    // mFramePaint.setColor(0x00FFFFFF);100% Transparent
    // mFramePaint.setColor(0x00FFFFFF); Half Transparent white
    // mFramePaint.setColor(0xFFFFFFFF); White
    // mFramePaint.setColor(0xFF000000); Black, non transparent
    // mFramePaint.setColor(0xFFFF0000); Red
    // mFramePaint.setColor(0xFF00FF00); Green
    // mFramePaint.setColor(0xFF0000FF); Blue
    // mFramePaint.setColor(0xFF00FFFF); Turquoise
    // mFramePaint.setColor(0xFFFF00FF); Bright violet
    // mFramePaint.setColor(0xFFFFFF00); Bright yellow
    // mFramePaint.setColor(0xFF88FFFF); Light blue
    // mFramePaint.setColor(0xFFFF88FF); Pink/violet
    // mFramePaint.setColor(0xFFFFFF88); Yellowish
    // mFramePaint.setColor(0xFFFF8888); Pink
    // mFramePaint.setColor(0xFF8888FF); Neon violet
    // mFramePaint.setColor(0xFF888FFF); Dim Violet
    // mFramePaint.setColor(0xFFFFF000); Bright Yellow
    // mFramePaint.setColor(0xFF000FFF); Bright true Blue
    // mFramePaint.setColor(0xFF888888); Gray
    // mFramePaint.setColor(0xFF444444); Dark Gray
    // mFramePaint.setColor(0xFFCCCCCC); Off white /Lt Gray
    // mFramePaint.setColor(0xFF0088FF); Nice blue green
    // mFramePaint.setColor(0xFF00FF44); Bright green blue
}

