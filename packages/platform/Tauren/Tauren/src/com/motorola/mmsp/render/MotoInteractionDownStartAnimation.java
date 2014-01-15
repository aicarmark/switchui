package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoComponentBuilder;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoInteractionDownStartAnimation extends MotoInteraction{
	
	private MotoAnimationSet mAnimationSet;
	
	public MotoInteractionDownStartAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
		String string_animationSetId = (String)attr.getValue("start_animation");
		if (string_animationSetId != null) {
			string_animationSetId = string_animationSetId.replace("@", "");
			int id = MotoMathUtil.parseInt(string_animationSetId);
			MotoComponentBuilder builder = resInfo.mAnimationSetsBuilders.get(id);
			if (builder != null) {
				try {
					mAnimationSet = (MotoAnimationSet) builder.build(resInfo);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public MotoInteractionDownStartAnimation(int interceptEventFlag,
			int handeEventFlag, boolean blockAnimation) {
		super(interceptEventFlag, handeEventFlag, blockAnimation);
	}
	
	public void setStartAnimation(MotoAnimationSet animationSet) {
		mAnimationSet = animationSet;
	}

	@Override
	public boolean handleEvent(MotoInteractionEvent event,
			MotoAnimationProperties propertiesOutput, MotoPicture bindPicture) {
		if (bindPicture != null && mAnimationSet != null) {
			bindPicture.addInteractionAnimation(mAnimationSet);
			mAnimationSet.reset();
			mAnimationSet.start();
		}
		return mBlockAnimation;
	}
}
