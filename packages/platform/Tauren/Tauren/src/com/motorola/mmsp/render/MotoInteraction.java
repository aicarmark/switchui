package com.motorola.mmsp.render;

import android.view.MotionEvent;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;

public abstract class MotoInteraction{
	public static final int INTERCEPT_EVENT_NEVER = 0;
	public static final int INTERCEPT_EVENT_ALWAYS = 1;
	public static final int INTERCEPT_EVENT_PREVIOUS_INTERCEPTED = 2;
	
	public static final int HANDLE_EVENT_NEVER = 0;
	public static final int HANDLE_EVENT_ALWAYS = 1;
	public static final int HANDLE_EVENT_PREVIOUS_INTERCEPTED = 2;
	
	protected int mInterceptEventFlag;
	protected int mHandeEventFlag;
	protected boolean mBlockAnimation;
	protected int mType = -1;
	protected String mTag;
	
	public MotoInteraction(MotoAttributeArray attr, ResOutputInfo resInfo) {
		String interceptFlag = (String)attr.getValue("intercept_event");
		String handleEvent = (String)attr.getValue("handle_event");
		String type = (String)attr.getValue("type");
		String blockAniamtion = (String)attr.getValue("block_animation");
		
		if ("always".equals(interceptFlag)) {
			mInterceptEventFlag = INTERCEPT_EVENT_ALWAYS;
		} else if ("previous_intercepted".equals(interceptFlag)) {
			mInterceptEventFlag = INTERCEPT_EVENT_PREVIOUS_INTERCEPTED;
		} else {
			mInterceptEventFlag = INTERCEPT_EVENT_NEVER;
		}
		
		if ("always".equals(handleEvent)) {
			mHandeEventFlag = INTERCEPT_EVENT_ALWAYS;
		} else if ("previous_intercepted".equals(handleEvent)) {
			mHandeEventFlag = INTERCEPT_EVENT_PREVIOUS_INTERCEPTED;
		} else {
			mHandeEventFlag = INTERCEPT_EVENT_NEVER;
		}
		
		if ("down".equals(type)) {
			mType = MotionEvent.ACTION_DOWN;
		} else if ("move".equals(type)) {
			mType = MotionEvent.ACTION_MOVE;
		} else if ("up".equals(type)) {
			mType = MotionEvent.ACTION_UP;
		}
		
		if ("true".equalsIgnoreCase(blockAniamtion) || "1".equals(blockAniamtion)) {
			mBlockAnimation = true;
		}
		
		mTag = (String)attr.getValue("tag");
	}
	
	public MotoInteraction(int interceptEventFlag, int handeEventFlag, boolean blockAnimation) {
		mInterceptEventFlag = interceptEventFlag;
		mHandeEventFlag = handeEventFlag;
		mBlockAnimation = blockAnimation;
	}
	
	public boolean isInterceptEvent(MotoInteractionEvent event, boolean isProvioursIntercepted) {
		if ((event.mEvent != null && event.mEvent.getAction() != getEventType())
				|| (event.mEvent == null) && (event.mTag == null || (!event.mTag.equals(getTag())))) {
			return false;
		}
		switch (mInterceptEventFlag) {
		case INTERCEPT_EVENT_NEVER:
			return false;
		case INTERCEPT_EVENT_ALWAYS:
			return true;
		case INTERCEPT_EVENT_PREVIOUS_INTERCEPTED:
			return isProvioursIntercepted;
		default:
			return false;
		}
	}
	public boolean isHandleEvent(MotoInteractionEvent event, boolean isProvioursIntercepted) {
		if ((event.mEvent != null && event.mEvent.getAction() != getEventType())
				|| (event.mEvent == null) && (event.mTag == null || (!event.mTag.equals(getTag())))) {
			return false;
		}
		switch (mHandeEventFlag) {
		case HANDLE_EVENT_NEVER:
			return false;
		case HANDLE_EVENT_ALWAYS:
			return true;
		case HANDLE_EVENT_PREVIOUS_INTERCEPTED:
			return isProvioursIntercepted;
		default:
			return false;
		}
	}
	
	public int getEventType() {
		return mType;
	}
	
	public void setEventType(int type) {
		mType = type;
	}
	
	public String getTag() {
		return mTag;
	}
	
	public void setTag(String tag) {
		mTag = tag;
	}
	
	/**
	 * @param event the input event from MotoInteractionSet;
	 * @param propertiesOutput the output to be changed by this interaction.
	 * @param bindPicture the picture which the interaction will affect to;
	 * @param data userData.
	 * @return Indicate the interaction will block animation or not, 
	 * If it return true, the output animation properties of animation set will not affect the object's properties;
	 * If it return false, the output animation properties of animation set will affect the object's properties.
	 * The return value may be different among each MotionEvent.
	 * If it return true, the interaction will block the animation effect all the time, until it returns false afterwards. 
	 */
	public abstract boolean handleEvent(MotoInteractionEvent event, MotoAnimationProperties propertiesOutput, MotoPicture bindPicture);
}
