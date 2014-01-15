package com.motorola.mmsp.render;

import android.view.MotionEvent;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoInteractionMoveByOffset extends MotoInteraction{
	private float mOffsetXRate;
	private float mOffsetYRate;
	
	public MotoInteractionMoveByOffset(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
		String string_offset_x_rate = (String)attr.getValue("offset_x_rate");
		String string_offset_y_rate = (String)attr.getValue("offset_y_rate");
		if (string_offset_x_rate != null) {
			mOffsetXRate = MotoMathUtil.parseFloat(string_offset_x_rate);
			mOffsetYRate = MotoMathUtil.parseFloat(string_offset_y_rate);
		}
	}
	
	public MotoInteractionMoveByOffset(int interceptEventFlag,
			int handeEventFlag, boolean blockAnimation) {
		super(interceptEventFlag, handeEventFlag, blockAnimation);
	}
	
	public MotoInteractionMoveByOffset(int interceptEventFlag,
			int handeEventFlag, boolean blockAnimation, 
			float offset_x_rate, float offset_y_rate) {
		this(interceptEventFlag, handeEventFlag, blockAnimation);
		mOffsetXRate = offset_x_rate;
		mOffsetYRate = offset_y_rate;
	}

	@Override
	public boolean handleEvent(MotoInteractionEvent interactionEvent,
			MotoAnimationProperties propertiesOutput, MotoPicture bindPicture) {
		MotionEvent event = interactionEvent.mEvent;
		if (event == null || propertiesOutput == null) {
			return false;
		}
		
		float pictureX = propertiesOutput.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_X);
		float pictureY = propertiesOutput.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Y);
		float deltaX = interactionEvent.mOffserX * mOffsetXRate;
		float deltaY = interactionEvent.mOffserY * mOffsetYRate;
		
		propertiesOutput.setPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_X, MotoObjectAnimation.AFFECT_TYPE_SET);
		propertiesOutput.setPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_Y, MotoObjectAnimation.AFFECT_TYPE_SET);
		propertiesOutput.setPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_X, pictureX + deltaX);
		propertiesOutput.setPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Y, pictureY + deltaY);
		propertiesOutput.mHasChanged = true;
		return mBlockAnimation;
	}
}
